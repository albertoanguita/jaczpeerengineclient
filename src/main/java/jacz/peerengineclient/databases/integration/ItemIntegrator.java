package jacz.peerengineclient.databases.integration;

import jacz.database.*;
import jacz.peerengineclient.databases.Databases;
import jacz.peerengineclient.databases.ItemRelations;
import jacz.peerengineservice.PeerID;
import jacz.util.concurrency.concurrency_controller.ConcurrencyController;
import jacz.util.hash.SHA_1;
import jacz.util.lists.tuple.Duple;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Alberto on 06/12/2015.
 */
public class ItemIntegrator {

    private static class CreationDateComparator implements Comparator<DatabaseItem> {

        @Override
        public int compare(DatabaseItem o1, DatabaseItem o2) {
            return o1.getCreationDate().compareTo(o2.getCreationDate());
        }
    }


    private static final float MATCH_THRESHOLD = 0.9f;

    private static final CreationDateComparator creationDateComparator = new CreationDateComparator();

    private final ConcurrencyController concurrencyController;

    // todo use
    private final IntegrationEventsBridge integrationEvents;

    public ItemIntegrator(IntegrationEvents integrationEvents) {
        concurrencyController = new ConcurrencyController(new IntegrationConcurrencyController());
        this.integrationEvents = new IntegrationEventsBridge(integrationEvents);
    }

    public void stop() {
        // todo we might still receive jobs!!! we should be disconnected from all peers
        concurrencyController.stopAndWaitForFinalization();
        integrationEvents.stop();
    }

    void integrateLocalItem(
            Databases databases,
            DatabaseMediator.ItemType type,
            DatabaseItem localItem,
            boolean isNew) {
        concurrencyController.beginActivity(IntegrationConcurrencyController.Activity.LOCAL_TO_INTEGRATED.name());

        DatabaseItem integratedItem;
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
        Duple<Boolean, Boolean> isAliveAndHasNewContent = inflateIntegratedItem(databases, integratedItem);
        reportIntegrationResult(integratedItem.getItemType(), integratedItem.getId(), isNew, isAliveAndHasNewContent.element2);

        concurrencyController.endActivity(IntegrationConcurrencyController.Activity.LOCAL_TO_INTEGRATED.name());
    }

    void removeLocalContent(
            Databases databases,
            DatabaseMediator.ItemType type,
            DatabaseItem localItem) {
        concurrencyController.beginActivity(IntegrationConcurrencyController.Activity.LOCAL_TO_INTEGRATED.name());

        DatabaseItem integratedItem = DatabaseMediator.getItem(
                databases.getIntegratedDB(),
                type,
                databases.getItemRelations().getLocalToIntegrated().get(type, localItem.getId()));
        databases.getItemRelations().getLocalToIntegrated().remove(type, localItem.getId());
        databases.getItemRelations().getIntegratedToLocal().remove(type, integratedItem.getId());
        if (databases.getItemRelations().getIntegratedToDeleted().contains(type, integratedItem.getId())) {
            // there is a deleted item -> remove it too
            DatabaseItem deletedItem = DatabaseMediator.getItem(
                    databases.getDeletedDB(),
                    type,
                    databases.getItemRelations().getIntegratedToDeleted().get(type, integratedItem.getId()));
            databases.getItemRelations().getIntegratedToDeleted().remove(type, integratedItem.getId());
            deletedItem.delete();
        }
        inflateIntegratedItem(databases, integratedItem);
        // todo report

        concurrencyController.endActivity(IntegrationConcurrencyController.Activity.LOCAL_TO_INTEGRATED.name());
    }


    private void integrateRemoteItem(
            Databases databases,
            DatabaseMediator.ItemType type,
            PeerID remotePeerID,
            DatabaseItem remoteItem) throws ParseException {
        concurrencyController.beginActivity(IntegrationConcurrencyController.Activity.REMOTE_TO_INTEGRATED.name());

        DatabaseItem integratedItem;
        boolean isNewIntegratedItem = false;
        ItemRelations.ItemRelationsMap remoteToIntegratedItems = databases.getItemRelations().getRemoteToIntegrated(remotePeerID);
        if (!remoteToIntegratedItems.contains(type, remoteItem.getId())) {
            // the given external item is not mapped to any integrated item
            // (the remote item is new and it is not linked to any integrated item)
            // we must find its corresponding integrated item, or create a new one
            List<? extends DatabaseItem> allIntegratedItems =
                    DatabaseMediator.getItems(databases.getIntegratedDB(), type);
            integratedItem = null;
            float maxMatch = -1;
            for (DatabaseItem anIntegratedItem : allIntegratedItems) {
                // todo check lists of external references. We don't need to access the db, we just need the ids
                float match = remoteItem.match(anIntegratedItem);
                if (match >= MATCH_THRESHOLD && match > maxMatch) {
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
                    remotePeerID,
                    remoteItem.getId());
        } else {
            // we have a matching integrated item -> use it
            integratedItem = DatabaseMediator.getItem(
                    databases.getIntegratedDB(),
                    type,
                    remoteToIntegratedItems.get(type, remoteItem.getId()));
        }

        // now copy the required information from the remote item to the matched integrated item
        // this requires creating the integrated item from zero,
        // with all the information of its local and remote composers
        Duple<Boolean, Boolean> isAliveAndHasNewContent = inflateIntegratedItem(databases, integratedItem);
        reportIntegrationResult(
                integratedItem.getItemType(),
                integratedItem.getId(),
                isNewIntegratedItem,
                isAliveAndHasNewContent.element2);

        concurrencyController.endActivity(IntegrationConcurrencyController.Activity.REMOTE_TO_INTEGRATED.name());
    }

//    private IntegrationResult removeExternalItem(
//            IntegratedDatabase integratedDatabase,
//            LocalDatabase localDatabase,
//            Map<PeerID, RemoteDatabase> remoteDatabases,
//            String library,
//            PeerID remotePeerID,
//            String externalItemID,
//            DatabaseItem externalItem,
//            Map<String, String> externalToIntegratedItems) throws ParseException, IOException {
//        DatabaseItem matchedIntegratedItem;
//        // todo
//        return null;
//    }

    /**
     * @param databases      paths to databases, and respective relations between items
     * @param integratedItem integrated item to populate
     * @return a duple: integrated item is alive (if fed by any element) / integrated item has new media content
     */
    private static Duple<Boolean, Boolean> inflateIntegratedItem(
            Databases databases,
            DatabaseItem integratedItem) {
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
            integratedItem.mergePostponed(localItem);
            isAlive = true;
        }

        // remote items
        List<DatabaseItem> remoteItems = new ArrayList<>();
        for (Duple<PeerID, Integer> peerAndId : databases.getItemRelations().getIntegratedToRemote().get(type, integratedItem.getId())) {
            DatabaseItem remoteItem = DatabaseMediator.getItem(
                    databases.getRemoteDBs().get(peerAndId.element1),
                    type,
                    peerAndId.element2);
            remoteItems.add(remoteItem);
            isAlive = true;
        }
        Collections.sort(remoteItems, creationDateComparator);
        for (DatabaseItem remoteItem : remoteItems) {
            integratedItem.mergePostponed(remoteItem);
        }

        // deleted item
        if (databases.getItemRelations().getIntegratedToDeleted().contains(type, integratedItem.getId())) {
            DatabaseItem deletedItem = DatabaseMediator.getItem(
                    databases.getDeletedDB(),
                    type,
                    databases.getItemRelations().getIntegratedToDeleted().get(type, integratedItem.getId()));
            integratedItem.mergePostponed(deletedItem);
            isAlive = true;
        }

        // flush all changes in the integrated item
        integratedItem.flushChanges();

        // if there have been changes in media content, notify them
        String newVideoFilesHash = getMediaContentHash(integratedItem, type);
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

    private void reportIntegrationResult(
            DatabaseMediator.ItemType itemType,
            int itemId,
            boolean isNewIntegratedItem,
            boolean hasNewMediaContent) {
        if (isNewIntegratedItem) {
            integrationEvents.newIntegratedItem(itemType, itemId);
        } else if (hasNewMediaContent) {
            integrationEvents.integratedItemHasNewMediaContent(itemType, itemId);
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
        boolean integratedItemHasLocalSource = integratedToLocal.contains(type, integratedItem.getId());
        DatabaseItem matchedIntegratedItem = null;
        float maxMatch = -1;
        for (DatabaseItem anIntegratedItem : allIntegratedItems) {
            // todo check lists of external references. We don't need to access the db, we just need the ids
            if (anIntegratedItem.getId().equals(integratedItem.getId())) {
                // do not compare with itself
                continue;
            }
            if (haveSimilarSources(integratedItem, anIntegratedItem, integratedToLocal, integratedToRemote)) {
                // they both have local source -> cannot match
                continue;
            }
            float match = integratedItem.match(anIntegratedItem);
            if (match >= MATCH_THRESHOLD && match > maxMatch) {
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
        List<Duple<PeerID, Integer>> sources1 = integratedToRemote.get(type, integratedItem.getId());
        List<Duple<PeerID, Integer>> sources2 = integratedToRemote.get(type, anotherIntegratedItem.getId());
        for (Duple<PeerID, Integer> aSource1 : sources1) {
            for (Duple<PeerID, Integer> aSource2 : sources2) {
                if (aSource1.element1.equals(aSource2.element1)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void mergeIntegratedItems(Databases databases, DatabaseItem fromItem, DatabaseItem toItem) {
        // all sources of fromItem are to be transferred to toItem. There cannot be similar sources (local and remote)
        DatabaseMediator.ItemType type = fromItem.getItemType();
        if (databases.getItemRelations().getIntegratedToLocal().contains(type, fromItem.getId())) {
            // transfer the local source
            int localId = databases.getItemRelations().getIntegratedToLocal().get(type, fromItem.getId());
            databases.getItemRelations().getIntegratedToLocal().put(type, toItem.getId(), localId);
            databases.getItemRelations().getLocalToIntegrated().put(type, localId, toItem.getId());
        }
        List<Duple<PeerID, Integer>> remoteSources = databases.getItemRelations().getIntegratedToRemote().get(type, fromItem.getId());
        for (Duple<PeerID, Integer> remoteSource : remoteSources) {
            PeerID peerID = remoteSource.element1;
            int remoteId = remoteSource.element2;
            databases.getItemRelations().getIntegratedToRemote().add(type, toItem.getId(), peerID, remoteId);
            databases.getItemRelations().getRemoteToIntegrated(peerID).put(type, remoteId, toItem.getId());
        }
    }
}
