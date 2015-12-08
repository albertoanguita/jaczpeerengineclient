package jacz.peerengineclient.libraries.integration;

import jacz.peerengineclient.dbs_old.ItemLockManager;
import jacz.peerengineclient.libraries.library_images.*;
import jacz.peerengineservice.PeerID;
import jacz.store.Chapter;
import jacz.store.LibraryItem;
import jacz.store.Movie;
import jacz.store.VideoFile;
import jacz.store.database.DatabaseMediator;
import jacz.store.old2.IllegalDataException;
import jacz.store.old2.db_mediator.CorruptDataException;
import jacz.util.concurrency.concurrency_controller.ConcurrencyController;
import jacz.util.hash.SHA_1;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

/**
 * Created by Alberto on 06/12/2015.
 */
public class ItemIntegrator {

    public static class IntegrationResult {

        public final LibraryItem integratedItem;

        public final boolean isNewIntegratedItem;

        public final boolean hasNewContent;

        public IntegrationResult(LibraryItem integratedItem, boolean isNewIntegratedItem, boolean hasNewContent) {
            this.integratedItem = integratedItem;
            this.isNewIntegratedItem = isNewIntegratedItem;
            this.hasNewContent = hasNewContent;
        }
    }

    private static class CreationDateComparator implements Comparator<LibraryItem> {

        @Override
        public int compare(LibraryItem o1, LibraryItem o2) {
            return o1.getCreationDate().compareTo(o2.getCreationDate());
        }
    }


    private static final float MATCH_THRESHOLD = 0.9f;

    private static final CreationDateComparator creationDateComparator = new CreationDateComparator();



    private final ConcurrencyController concurrencyController;

    public ItemIntegrator() {
        concurrencyController = new ConcurrencyController(new IntegrationConcurrencyController());
    }

    void integrateLocalItem() {
        concurrencyController.beginActivity(IntegrationConcurrencyController.Activity.LOCAL_TO_INTEGRATED.name());
        concurrencyController.endActivity(IntegrationConcurrencyController.Activity.LOCAL_TO_INTEGRATED.name());
    }


    private static IntegrationResult integrateExternalItem(
            IntegratedDatabase integratedDatabase,
            LocalDatabase localDatabase,
            Map<PeerID, RemoteDatabase> remoteDatabases,
            DeletedRemoteItemsLibrary deletedRemoteItemsLibrary,
            SharedLibrary sharedLibrary,
            DatabaseMediator.ItemType type,
            ItemLockManager itemLockManager,
            PeerID remotePeerID,
            LibraryItem externalItem,
            Map<Integer, Integer> externalToIntegratedItems) throws ParseException, IOException, IllegalDataException {
        LibraryItem matchedIntegratedItem;

        boolean isNewIntegratedItem = false;
        if (!externalToIntegratedItems.containsKey(externalItem.getId())) {
            // the given external item is not mapped to any integrated item
            // (the remote item is new and it is not linked to any integrated item)
            // we must find its corresponding integrated item, or create a new one
            List<LibraryItem> allIntegratedItems =...
            matchedIntegratedItem = null;
            for (LibraryItem integratedItem : allIntegratedItems) {
                if (externalItem.match(integratedItem) >= MATCH_THRESHOLD) {
                    // match found! -> remember the integrated item
                    matchedIntegratedItem = integratedItem;
                    break;
                }
            }
            if (matchedIntegratedItem == null) {
                // we did not find any match -> create a new integrated item
                matchedIntegratedItem = integratedDatabase.createNewItem(type);
                isNewIntegratedItem = true;
            }
            externalToIntegratedItems.put(externalItem.getId(), matchedIntegratedItem.getId());
            // we must put the equivalent pointer in the integrated database. Different cases for local item (remotePeerID == null) or remote item
            if (remotePeerID == null) {
                // local item
                integratedDatabase.putItemToLocalItem(type, matchedIntegratedItem.getId(), externalItem.getId());
            } else {
                // remote item
                integratedDatabase.addRemoteLink(type, matchedIntegratedItem.getId(), remotePeerID, externalItem.getId());
            }
        } else {
            // we have a matching integrated item -> use it
            matchedIntegratedItem = integratedDatabase.getDatabase().getItem(type, externalToIntegratedItems.get(externalItem.getId()));
        }

        // now copy the required information from the remote item to the matched integrated item
        // this requires creating the integrated item from zero, with all the information of its local and remote composers
        boolean hasNewContent = populateIntegratedItemStatic(integratedDatabase, localDatabase, remoteDatabases, deletedRemoteItemsLibrary, sharedLibrary, matchedIntegratedItem, type);
        return new IntegrationResult(matchedIntegratedItem, isNewIntegratedItem, hasNewContent);
//        return new Triple<>(changedFields, matchedIntegratedItem.getIdentifier(), integratedDatabase.containsKeyToLocalItem(matchedIntegratedItem.getIdentifier()));
    }

    private IntegrationResult removeExternalItem(
            IntegratedDatabase integratedDatabase,
            LocalDatabase localDatabase,
            Map<PeerID, RemoteDatabase> remoteDatabases,
            String library,
            ItemLockManager itemLockManager,
            PeerID remotePeerID,
            String externalItemID,
            LibraryItem externalItem,
            Map<String, String> externalToIntegratedItems) throws ParseException, IOException, IllegalDataException {
        LibraryItem matchedIntegratedItem;
        // todo
        return null;
    }

    /**
     * @param integratedDatabase
     * @param localDatabase
     * @param remoteDatabases
     * @param integratedItem
     * @return a set containing the levels that have changed
     * @throws IllegalDataException
     * @throws IOException
     * @throws CorruptDataException
     */
    private static boolean populateIntegratedItemStatic(
            IntegratedDatabase integratedDatabase,
            LocalDatabase localDatabase,
            Map<PeerID, RemoteDatabase> remoteDatabases,
            DeletedRemoteItemsLibrary deletedRemoteItemsLibrary,
            SharedLibrary sharedLibrary,
            LibraryItem integratedItem,
            DatabaseMediator.ItemType type
    ) throws IllegalDataException, IOException {
        // copy the information from the composers. First, the local item (if any).
        // Then, the remote items by order of creation date (older to newer)
        // Finally, the deleted remote item (if any)

        // we must check if, at the end, the media content has changed with respect to the initial state
        // todo transactions
        String oldVideoFilesHash = getMediaContentHash(integratedItem, type);

        GenericDatabase.LibraryId integratedId = new GenericDatabase.LibraryId(type, integratedItem.getId());
        integratedItem.reset();

        // local item
        if (integratedDatabase.containsKeyToLocalItem(integratedId)) {
            integratedItem.merge(localDatabase.getDatabase().getItem(type, integratedDatabase.getItemToLocalItem(integratedId)));
        }

        // remote items
        List<LibraryItem> remoteItems = new ArrayList<>();
        for (IntegratedDatabase.PeerAndLibraryId peerAndLibraryId : integratedDatabase.getRemotePeerAndID(integratedId)) {
            remoteItems.add(remoteDatabases.get(peerAndLibraryId.peerID).getDatabase().getItem(type, peerAndLibraryId.id));
        }
        Collections.sort(remoteItems, creationDateComparator);
        for (LibraryItem remoteItem : remoteItems) {
            integratedItem.merge(remoteItem);
        }

        // deleted item
        if (integratedDatabase.containsKeyToDeletedItem(integratedId)) {
            integratedItem.merge(deletedRemoteItemsLibrary.getDatabase().getItem(type, integratedDatabase.getItemToLocalItem(integratedId)));
        }

        // if there have been changes in media content, notify them
        String newVideoFilesHash = getMediaContentHash(integratedItem, type);
        return !newVideoFilesHash.equals(oldVideoFilesHash);

        // the modification date of the integrated item is modified regardless of whether there have been changes or not. This way, this value
        // actually stores the date of the last integration of the item. Modification can be checked by looking at the remote items and local item
    }

    private static String getMediaContentHash(LibraryItem item, DatabaseMediator.ItemType type) {
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
