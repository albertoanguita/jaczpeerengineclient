package jacz.peerengineclient.databases.integration;

import jacz.database.*;
import jacz.peerengineclient.databases.Databases;
import jacz.peerengineclient.databases.ItemRelations;
import jacz.peerengineservice.PeerID;
import jacz.util.concurrency.concurrency_controller.ConcurrencyController;
import jacz.util.concurrency.task_executor.SequentialTaskExecutor;
import jacz.util.hash.SHA_1;
import jacz.util.lists.tuple.Duple;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Alberto on 06/12/2015.
 */
public class ItemIntegrator {

    public static class IntegrationResult {

        public final DatabaseItem integratedItem;

        public final boolean isNewIntegratedItem;

        public final boolean hasNewContent;

        public IntegrationResult(DatabaseItem integratedItem, boolean isNewIntegratedItem, boolean hasNewContent) {
            this.integratedItem = integratedItem;
            this.isNewIntegratedItem = isNewIntegratedItem;
            this.hasNewContent = hasNewContent;
        }
    }

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
    private final IntegrationEvents integrationEvents;

    private final SequentialTaskExecutor integrationEventsTaskExecutor;

    public ItemIntegrator(IntegrationEvents integrationEvents) {
        concurrencyController = new ConcurrencyController(new IntegrationConcurrencyController());
        this.integrationEvents = integrationEvents;
        integrationEventsTaskExecutor = new SequentialTaskExecutor();
    }

    public void stop() {
        // todo we might still receive jobs!!! we should be disconnected from all peers
        concurrencyController.stopAndWaitForFinalization();
    }

    void integrateLocalItem() {
        concurrencyController.beginActivity(IntegrationConcurrencyController.Activity.LOCAL_TO_INTEGRATED.name());
        concurrencyController.endActivity(IntegrationConcurrencyController.Activity.LOCAL_TO_INTEGRATED.name());
    }


    private static IntegrationResult integrateExternalItem(
            Databases databases,
            DatabaseMediator.ItemType type,
            PeerID remotePeerID,
            DatabaseItem externalItem) throws ParseException, IOException {
        DatabaseItem matchedIntegratedItem;

        boolean isNewIntegratedItem = false;
        ItemRelations.ItemRelationsMap remoteToIntegratedItems = databases.getItemRelations().getRemoteToIntegrated(remotePeerID);
        if (!remoteToIntegratedItems.contains(type, externalItem.getId())) {
            // the given external item is not mapped to any integrated item
            // (the remote item is new and it is not linked to any integrated item)
            // we must find its corresponding integrated item, or create a new one
            List<? extends DatabaseItem> allIntegratedItems =
                    DatabaseMediator.getItems(databases.getIntegratedDB(), type);
            matchedIntegratedItem = null;
            for (DatabaseItem integratedItem : allIntegratedItems) {
                // todo check lists of external references. We don't need to access the db, we just need the ids
                if (externalItem.match(integratedItem) >= MATCH_THRESHOLD) {
                    // todo we should search all items, and get the one with MAX match value
                    // match found! -> remember the integrated item
                    matchedIntegratedItem = integratedItem;
                    break;
                }
            }
            if (matchedIntegratedItem == null) {
                // we did not find any match -> create a new integrated item
                matchedIntegratedItem = DatabaseMediator.createNewItem(databases.getIntegratedDB(), type);
                isNewIntegratedItem = true;
            }
            remoteToIntegratedItems.put(type, externalItem.getId(), matchedIntegratedItem.getId());
            // we must put the equivalent pointer in the integrated database.
            // Different cases for local item (remotePeerID == null) or remote item
            if (remotePeerID == null) {
                // local item
                databases.getItemRelations().getIntegratedToLocal().put(type, matchedIntegratedItem.getId(), externalItem.getId());
            } else {
                // remote item
                databases.getItemRelations().getIntegratedToRemote().add(
                        type,
                        matchedIntegratedItem.getId(),
                        remotePeerID,
                        externalItem.getId());
            }
        } else {
            // we have a matching integrated item -> use it
            matchedIntegratedItem = DatabaseMediator.getItem(
                    databases.getIntegratedDB(),
                    type,
                    remoteToIntegratedItems.get(type, externalItem.getId()));
        }

        // now copy the required information from the remote item to the matched integrated item
        // this requires creating the integrated item from zero,
        // with all the information of its local and remote composers
        boolean hasNewContent = populateIntegratedItemStatic(
                databases,
                matchedIntegratedItem,
                type);
        return new IntegrationResult(matchedIntegratedItem, isNewIntegratedItem, hasNewContent);
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
     * @return a set containing the levels that have changed
     * @throws IOException
     */
    private static boolean populateIntegratedItemStatic(
            Databases databases,
            DatabaseItem integratedItem,
            DatabaseMediator.ItemType type
    ) throws IOException {
        // copy the information from the composers. First, the local item (if any).
        // Then, the remote items by order of creation date (older to newer)
        // Finally, the deleted remote item (if any)

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
        }

        // remote items
        List<DatabaseItem> remoteItems = new ArrayList<>();
        for (Duple<PeerID, Integer> peerAndId : databases.getItemRelations().getIntegratedToRemote().get(type, integratedItem.getId())) {
            DatabaseItem remoteItem = DatabaseMediator.getItem(
                    databases.getRemoteDBs().get(peerAndId.element1),
                    type,
                    peerAndId.element2);
            remoteItems.add(remoteItem);
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
        }

        // flush all changes in the integrated item
        integratedItem.flushChanges();

        // if there have been changes in media content, notify them
        String newVideoFilesHash = getMediaContentHash(integratedItem, type);
        return !newVideoFilesHash.equals(oldVideoFilesHash);

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
}
