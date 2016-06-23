package jacz.peerengineclient.databases.integration;

import jacz.database.*;
import jacz.peerengineclient.databases.Databases;
import jacz.peerengineclient.databases.ItemRelations;
import jacz.peerengineclient.images.ImageDownloader;
import jacz.peerengineservice.PeerId;
import jacz.util.concurrency.concurrency_controller.ConcurrencyController;
import jacz.util.hash.SHA_1;
import jacz.util.lists.tuple.Duple;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Code for building the integrated database
 */
public class ItemIntegrator {

    private static class CreationDateComparator implements Comparator<Duple<PeerId, DatabaseItem>> {
        @Override
        public int compare(Duple<PeerId, DatabaseItem> o1, Duple<PeerId, DatabaseItem> o2) {
            return o1.element2.getCreationDate().compareTo(o2.element2.getCreationDate());
        }
    }


    private static final CreationDateComparator creationDateComparator = new CreationDateComparator();

    private final ConcurrencyController concurrencyController;

    private final IntegrationEventsBridge integrationEvents;

    private ImageDownloader imageDownloader;

    public ItemIntegrator(
            ConcurrencyController concurrencyController,
            IntegrationEvents integrationEvents) {
        this.concurrencyController = concurrencyController;
        this.integrationEvents = new IntegrationEventsBridge(integrationEvents);
    }

    public void setImageDownloader(ImageDownloader imageDownloader) {
        this.imageDownloader = imageDownloader;
    }

    public void stop() {
        integrationEvents.stop();
    }

    public DatabaseItem integrateLocalItem(
            Databases databases,
            DatabaseItem localItem) throws IllegalStateException {
        concurrencyController.beginActivity(IntegrationConcurrencyController.Activity.LOCAL_TO_INTEGRATED.name());

        DatabaseMediator.ItemType type = localItem.getItemType();
        DatabaseItem integratedItem;
        boolean isNew = !databases.getItemRelations().getLocalToIntegrated().contains(type, localItem.getId());
        if (isNew) {
            // define a new integrated item for this local item
            // we assume that the use has checked that this local item does not merge with any existing
            // integrated item
            integratedItem = DatabaseMediator.createNewItem(databases.getIntegratedDB(), type);
            databases.getItemRelations().getLocalToIntegrated().put(type, localItem.getId(), integratedItem.getId());
            databases.getItemRelations().getIntegratedToLocal().put(type, integratedItem.getId(), localItem.getId());
        } else {
            // we have a matching integrated item -> use it
            integratedItem = DatabaseMediator.getItem(
                    databases.getIntegratedDB(),
                    type,
                    databases.getItemRelations().getLocalToIntegrated().get(type, localItem.getId()));
        }
        processIntegratedItem(databases, integratedItem, isNew);

        concurrencyController.endActivity(IntegrationConcurrencyController.Activity.LOCAL_TO_INTEGRATED.name());

        return integratedItem;
    }

    /**
     * Removes the local content (local and deleted databases) associated to an integrated item
     *
     * @param databases      database paths
     * @param integratedItem integrated item
     * @return true if the integrated item has been removed as a consequence of removing its local content
     */
    public boolean removeLocalContent(
            Databases databases,
            DatabaseItem integratedItem) {
        concurrencyController.beginActivity(IntegrationConcurrencyController.Activity.LOCAL_TO_INTEGRATED.name());

        DatabaseMediator.ItemType type = integratedItem.getItemType();
        if (databases.getItemRelations().getIntegratedToLocal().contains(type, integratedItem.getId())) {
            // there is a local item -> remote it
            DatabaseItem localItem = DatabaseMediator.getItem(
                    databases.getLocalDB(),
                    type,
                    databases.getItemRelations().getIntegratedToLocal().get(type, integratedItem.getId()));
            databases.getItemRelations().getLocalToIntegrated().remove(type, localItem.getId());
            databases.getItemRelations().getIntegratedToLocal().remove(type, integratedItem.getId());
        }
        if (databases.getItemRelations().getIntegratedToDeleted().contains(type, integratedItem.getId())) {
            // there is a deleted item -> remove it too
            DatabaseItem deletedItem = DatabaseMediator.getItem(
                    databases.getDeletedDB(),
                    type,
                    databases.getItemRelations().getIntegratedToDeleted().get(type, integratedItem.getId()));
            databases.getItemRelations().getIntegratedToDeleted().remove(type, integratedItem.getId());
            databases.getItemRelations().getDeletedToIntegrated().remove(type, deletedItem.getId());
            deletedItem.delete();
        }
        integratedItem = processIntegratedItem(databases, integratedItem, false);

        concurrencyController.endActivity(IntegrationConcurrencyController.Activity.LOCAL_TO_INTEGRATED.name());

        return integratedItem == null;
    }


    public void integrateRemoteItem(
            Databases databases,
            PeerId remotePeerId,
            DatabaseItem remoteItem) {
        concurrencyController.beginActivity(IntegrationConcurrencyController.Activity.REMOTE_TO_INTEGRATED.name());

        DatabaseMediator.ItemType type = remoteItem.getItemType();
        DatabaseItem integratedItem;
        boolean isNewIntegratedItem = false;
        ItemRelations.ItemRelationsMap remoteToIntegratedItems = databases.getItemRelations().getRemoteToIntegrated(remotePeerId);
        if (!remoteToIntegratedItems.contains(type, remoteItem.getId())) {
            // the given external item is not mapped to any integrated item
            // (the remote item is new and it is not linked to any integrated item)
            // we must find its corresponding integrated item, or create a new one
            List<? extends DatabaseItem> allIntegratedItems =
                    DatabaseMediator.getItems(databases.getIntegratedDB(), type);
            integratedItem = null;
            float maxMatch = -1;
            for (DatabaseItem anIntegratedItem : allIntegratedItems) {
                float match = remoteItem.match(anIntegratedItem);
                if (match >= jacz.database.util.ItemIntegrator.THRESHOLD && match > maxMatch) {
                    // match found! -> remember the integrated item and update the max match
                    integratedItem = anIntegratedItem;
                    maxMatch = match;
                }
            }
            if (integratedItem == null) {
                // we did not find any match -> create a new integrated item
                integratedItem = DatabaseMediator.createNewItem(databases.getIntegratedDB(), type);
                isNewIntegratedItem = true;
            }
            remoteToIntegratedItems.put(type, remoteItem.getId(), integratedItem.getId());
            // we must put the equivalent pointer in the integrated database.
            databases.getItemRelations().getIntegratedToRemote().add(
                    type,
                    integratedItem.getId(),
                    remotePeerId,
                    remoteItem.getId());
        } else {
            // we have a matching integrated item -> use it
            integratedItem = DatabaseMediator.getItem(
                    databases.getIntegratedDB(),
                    type,
                    remoteToIntegratedItems.get(type, remoteItem.getId()));
        }
        processIntegratedItem(databases, integratedItem, isNewIntegratedItem);

        concurrencyController.endActivity(IntegrationConcurrencyController.Activity.REMOTE_TO_INTEGRATED.name());
    }

    public void removeRemoteItem(
            Databases databases,
            PeerId remotePeerId,
            DatabaseItem remoteItem) {
        concurrencyController.beginActivity(IntegrationConcurrencyController.Activity.REMOTE_TO_INTEGRATED.name());

        DatabaseMediator.ItemType type = remoteItem.getItemType();
        DatabaseItem integratedItem;
        ItemRelations.ItemRelationsMap remoteToIntegratedItems = databases.getItemRelations().getRemoteToIntegrated(remotePeerId);
        if (remoteToIntegratedItems.contains(type, remoteItem.getId())) {
            // we have a matching integrated item -> move the contents of the removed item to a deleted item
            // and re-inflate the integrated item
            integratedItem = DatabaseMediator.getItem(
                    databases.getIntegratedDB(),
                    type,
                    remoteToIntegratedItems.get(type, remoteItem.getId()));

            // get the deleted item
            DatabaseItem deletedItem;
            if (databases.getItemRelations().getIntegratedToDeleted().contains(type, integratedItem.getId())) {
                // we have a deleted item
                deletedItem = DatabaseMediator.getItem(
                        databases.getDeletedDB(),
                        type,
                        databases.getItemRelations().getIntegratedToDeleted().get(type, integratedItem.getId()));
            } else {
                // no deleted item -> build a new one
                deletedItem = DatabaseMediator.createNewItem(databases.getDeletedDB(), type);
                databases.getItemRelations().getIntegratedToDeleted().put(type, integratedItem.getId(), deletedItem.getId());
                databases.getItemRelations().getDeletedToIntegrated().put(type, deletedItem.getId(), integratedItem.getId());
            }

            // copy the contents from the removed remote item to the deleted item
            deletedItem.merge(remoteItem);

            // remove the links from the remote item to the integrated item
            databases.getItemRelations().getIntegratedToRemote().remove(type, integratedItem.getId(), remotePeerId);
            databases.getItemRelations().getRemoteToIntegrated(remotePeerId).remove(type, remoteItem.getId());

            processIntegratedItem(databases, integratedItem, false);
        }

        concurrencyController.endActivity(IntegrationConcurrencyController.Activity.REMOTE_TO_INTEGRATED.name());
    }

    private DatabaseItem processIntegratedItem(Databases databases, DatabaseItem integratedItem, boolean isNew) {
        Duple<Boolean, Boolean> isAliveAndHasNewContent = inflateIntegratedItem(databases, integratedItem, imageDownloader);
        if (isAliveAndHasNewContent.element1) {
            // the integrated item is alive
            // check if it has to be merged with another integrated item
            DatabaseItem matchedIntegratedItem = checkIntegratedItemMatches(databases, integratedItem);
            if (matchedIntegratedItem != null) {
                // we have a match -> merge with the match
                // delete the item and report the matched item
                // the item is no longer new
                isAliveAndHasNewContent = mergeIntegratedItems(databases, integratedItem, matchedIntegratedItem);
                deleteIntegratedItem(integratedItem);
                isNew = false;
                integratedItem = matchedIntegratedItem;
//                if (isAliveAndHasNewContent.element2) {
//                    integrationEvents.integratedItemHasNewMediaContent(matchedIntegratedItem.getItemType(), matchedIntegratedItem.getId());
//                }
            }
            // notify this item
            if (isNew) {
                integrationEvents.newIntegratedItem(integratedItem.getItemType(), integratedItem.getId());
            } else {
                integrationEvents.integratedItemHasBeenModified(integratedItem.getItemType(), integratedItem.getId(), isAliveAndHasNewContent.element2);
            }
            return integratedItem;
        } else {
            // the integrated item has died
            deleteIntegratedItem(integratedItem);
            return null;
        }
    }

    public void reportNewMedia(DatabaseItem item) {
        integrationEvents.integratedItemHasNewMedia(item.getItemType(), item.getId());
    }

    public void reportNewImage(DatabaseItem item) {
        integrationEvents.integratedItemHasNewImage(item.getItemType(), item.getId());
    }

    private void deleteIntegratedItem(DatabaseItem integratedItem) {
        DatabaseMediator.ItemType type = integratedItem.getItemType();
        Integer id = integratedItem.getId();
        integratedItem.delete();
        integrationEvents.integratedItemRemoved(type, id);
    }

    /**
     * @param databases      paths to databases, and respective relations between items
     * @param integratedItem integrated item to populate
     * @return a duple: integrated item is alive (if fed by any element) / integrated item has new media content
     */
    private static Duple<Boolean, Boolean> inflateIntegratedItem(
            Databases databases,
            DatabaseItem integratedItem,
            ImageDownloader imageDownloader) {
        // copy the information from the composers. First, the local item (if any).
        // Then, the remote items by order of creation date (older to newer)
        // Finally, the deleted remote item (if any)

        DatabaseMediator.ItemType type = integratedItem.getItemType();
        boolean isAlive = false;

        // we must check if, at the end, the media content has changed with respect to the initial state
        String oldVideoFilesHash = getMediaContentHash(integratedItem, type);

        integratedItem.resetPostponed();

        // local item
        if (databases.getItemRelations().getIntegratedToLocal().contains(type, integratedItem.getId())) {
            DatabaseItem localItem = DatabaseMediator.getItem(
                    databases.getLocalDB(),
                    type,
                    databases.getItemRelations().getIntegratedToLocal().get(type, integratedItem.getId()));
            integratedItem.mergeBasicPostponed(localItem);
            DatabaseMediator.ReferencedElements referencedElements = localItem.getReferencedElements();
            referencedElements.mapIds(databases.getItemRelations().getLocalToIntegrated().getTypeMappings());
            integratedItem.mergeReferencedElementsPostponed(referencedElements);
            isAlive = true;
        }

        // remote items
        List<Duple<PeerId, DatabaseItem>> remoteItems = new ArrayList<>();
        for (Duple<PeerId, Integer> peerAndId : databases.getItemRelations().getIntegratedToRemote().get(type, integratedItem.getId())) {
            DatabaseItem remoteItem;
            try {
                remoteItem = DatabaseMediator.getItem(
                        databases.getRemoteDB(peerAndId.element1),
                        type,
                        peerAndId.element2);
            } catch (IOException e) {
                // could not retrieve its remote database -> skip
                continue;
            }
            remoteItems.add(new Duple<>(peerAndId.element1, remoteItem));
            isAlive = true;
        }
        Collections.sort(remoteItems, creationDateComparator);
        for (Duple<PeerId, DatabaseItem> peerAndRemoteItem : remoteItems) {
            integratedItem.mergeBasicPostponed(peerAndRemoteItem.element2);
            DatabaseMediator.ReferencedElements referencedElements = peerAndRemoteItem.element2.getReferencedElements();
            referencedElements.mapIds(databases.getItemRelations().getRemoteToIntegrated(peerAndRemoteItem.element1).getTypeMappings());
            integratedItem.mergeReferencedElementsPostponed(referencedElements);
        }

        // deleted item
        if (databases.getItemRelations().getIntegratedToDeleted().contains(type, integratedItem.getId())) {
            DatabaseItem deletedItem = DatabaseMediator.getItem(
                    databases.getDeletedDB(),
                    type,
                    databases.getItemRelations().getIntegratedToDeleted().get(type, integratedItem.getId()));
            integratedItem.mergeBasicPostponed(deletedItem);
            DatabaseMediator.ReferencedElements referencedElements = deletedItem.getReferencedElements();
            referencedElements.mapIds(databases.getItemRelations().getDeletedToIntegrated().getTypeMappings());
            integratedItem.mergeReferencedElementsPostponed(referencedElements);
            isAlive = true;
        }

        // flush all changes in the integrated item
        integratedItem.flushChanges();

        // if there have been changes in media content, notify them
        String newVideoFilesHash = getMediaContentHash(integratedItem, type);

        // if the integrated item has an image hash, check if we have to download the image
        if (integratedItem.getItemType().hasImageHash()) {
            ProducedCreationItem producedCreationItem = (ProducedCreationItem) integratedItem;
            imageDownloader.checkImageHash(producedCreationItem.getImageHash());
        }
        return new Duple<>(isAlive, !newVideoFilesHash.equals(oldVideoFilesHash));

        // the modification date of the integrated item is modified regardless of whether there have been changes or not. This way, this value
        // actually stores the date of the last integration of the item. Modification can be checked by looking at the remote items and local item
    }

    private static String getMediaContentHash(DatabaseItem item, DatabaseMediator.ItemType type) {
        List<VideoFile> videoFiles = null;
        switch (type) {

            case MOVIE:
                Movie movie = (Movie) item;
                videoFiles = movie.getVideoFiles();
                break;
            case CHAPTER:
                Chapter chapter = (Chapter) item;
                videoFiles = chapter.getVideoFiles();
                break;
        }
        return generateVideoFilesHash(videoFiles);
    }

    private static String generateVideoFilesHash(List<VideoFile> videoFiles) {
        if (videoFiles == null) {
            return "";
        } else {
            SHA_1 sha_1 = new SHA_1();
            for (VideoFile videoFile : videoFiles) {
                sha_1.update(videoFile.getHash());
            }
            return sha_1.digestAsHex();
        }
    }

    private DatabaseItem checkIntegratedItemMatches(Databases databases, DatabaseItem integratedItem) {
        // compare the given integrated item with the rest of integrated items. See if it matches with any
        // integrated item (return the one with highest match)
        // we must note that two integrated items cannot match if they have sources of the same origin (local or remote)
        // (i.e. are fed by two items of the same peer)
        DatabaseMediator.ItemType type = integratedItem.getItemType();
        List<? extends DatabaseItem> allIntegratedItems = DatabaseMediator.getItems(databases.getIntegratedDB(), type);
        ItemRelations.ItemRelationsMap integratedToLocal = databases.getItemRelations().getIntegratedToLocal();
        ItemRelations.ItemToPeerListRelationsMap integratedToRemote = databases.getItemRelations().getIntegratedToRemote();
        DatabaseItem matchedIntegratedItem = null;
        float maxMatch = -1;
        for (DatabaseItem anIntegratedItem : allIntegratedItems) {
            if (anIntegratedItem.getId().equals(integratedItem.getId())) {
                // do not compare with itself
                continue;
            }
            if (haveSimilarSources(integratedItem, anIntegratedItem, integratedToLocal, integratedToRemote)) {
                // they both have local source -> cannot match
                continue;
            }
            float match = integratedItem.match(anIntegratedItem);
            if (match >= jacz.database.util.ItemIntegrator.THRESHOLD && match > maxMatch) {
                // match found! -> remember the integrated item and update the max match
                matchedIntegratedItem = anIntegratedItem;
                maxMatch = match;
            }
        }
        return matchedIntegratedItem;
    }

    private boolean haveSimilarSources(
            DatabaseItem integratedItem,
            DatabaseItem anotherIntegratedItem,
            ItemRelations.ItemRelationsMap integratedToLocal,
            ItemRelations.ItemToPeerListRelationsMap integratedToRemote) {
        DatabaseMediator.ItemType type = integratedItem.getItemType();
        if (integratedToLocal.contains(type, integratedItem.getId()) && integratedToLocal.contains(type, anotherIntegratedItem.getId())) {
            // they both have local source -> cannot match
            return true;
        }
        List<Duple<PeerId, Integer>> sources1 = integratedToRemote.get(type, integratedItem.getId());
        List<Duple<PeerId, Integer>> sources2 = integratedToRemote.get(type, anotherIntegratedItem.getId());
        for (Duple<PeerId, Integer> aSource1 : sources1) {
            for (Duple<PeerId, Integer> aSource2 : sources2) {
                if (aSource1.element1.equals(aSource2.element1)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Duple<Boolean, Boolean> mergeIntegratedItems(Databases databases, DatabaseItem fromItem, DatabaseItem toItem) {
        // all sources of fromItem are to be transferred to toItem. There cannot be similar sources (local and remote)
        DatabaseMediator.ItemType type = fromItem.getItemType();
        if (databases.getItemRelations().getIntegratedToLocal().contains(type, fromItem.getId())) {
            // transfer the local source
            int localId = databases.getItemRelations().getIntegratedToLocal().get(type, fromItem.getId());
            databases.getItemRelations().getIntegratedToLocal().put(type, toItem.getId(), localId);
            databases.getItemRelations().getLocalToIntegrated().put(type, localId, toItem.getId());
        }
        List<Duple<PeerId, Integer>> remoteSources = databases.getItemRelations().getIntegratedToRemote().get(type, fromItem.getId());
        for (Duple<PeerId, Integer> remoteSource : remoteSources) {
            PeerId peerID = remoteSource.element1;
            int remoteId = remoteSource.element2;
            databases.getItemRelations().getIntegratedToRemote().add(type, toItem.getId(), peerID, remoteId);
            databases.getItemRelations().getRemoteToIntegrated(peerID).put(type, remoteId, toItem.getId());
        }
        return inflateIntegratedItem(databases, toItem, imageDownloader);
    }
}
