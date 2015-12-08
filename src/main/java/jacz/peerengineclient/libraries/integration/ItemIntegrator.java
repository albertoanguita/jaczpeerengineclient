package jacz.peerengineclient.libraries.integration;

import jacz.peerengineclient.dbs_old.ItemLockManager;
import jacz.peerengineclient.dbs_old.sorting.ComparatorHandler;
import jacz.peerengineclient.libraries.library_images.DeletedRemoteItemsLibrary;
import jacz.peerengineclient.libraries.library_images.IntegratedDatabase;
import jacz.peerengineclient.libraries.library_images.LocalDatabase;
import jacz.peerengineclient.libraries.library_images.RemoteDatabase;
import jacz.peerengineservice.PeerID;
import jacz.store.LibraryItem;
import jacz.store.database.DatabaseMediator;
import jacz.store.old2.IllegalDataException;
import jacz.store.old2.db_mediator.CorruptDataException;
import jacz.util.concurrency.concurrency_controller.ConcurrencyController;
import jacz.util.lists.Triple;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

/**
 * Created by Alberto on 06/12/2015.
 */
public class ItemIntegrator {

    public static class IntegrationResult {

        public final LibraryItem integratedItem;

        public final boolean isNew;

        public final boolean hasNewContent;

        public IntegrationResult(LibraryItem integratedItem, boolean isNew, boolean hasNewContent) {
            this.integratedItem = integratedItem;
            this.isNew = isNew;
            this.hasNewContent = hasNewContent;
        }
    }

    private static final float MATCH_THRESHOLD = 0.9f;


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
            DatabaseMediator.ItemType type,
            ItemLockManager itemLockManager,
            PeerID remotePeerID,
            LibraryItem externalItem,
            Map<Integer, Integer> externalToIntegratedItems) throws ParseException, IOException, IllegalDataException {
        LibraryItem matchedIntegratedItem;

        boolean isNewIntegratedItem = !externalToIntegratedItems.containsKey(externalItem.getId());
        if (isNewIntegratedItem) {
            // the given external item is not mapped to any integrated item
            // (the remote item is new and it is not linked to any integrated item)
            // we must find its corresponding integrated item, or create a new one
            List<LibraryItem> allIntegratedItems = ...
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
        boolean hasNewContent = populateIntegratedItemStatic(integratedDatabase, localDatabase, remoteDatabases, library, itemLockManager, matchedIntegratedItem);
        return new Triple<>(changedFields, matchedIntegratedItem.getIdentifier(), integratedDatabase.containsKeyToLocalItem(matchedIntegratedItem.getIdentifier()));
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
     * @param library
     * @param itemLockManager
     * @param integratedItem
     * @return a set containing the levels that have changed
     * @throws IllegalDataException
     * @throws DBException
     * @throws ParseException
     * @throws IOException
     * @throws CorruptDataException
     */
    private static boolean populateIntegratedItemStatic(IntegratedDatabase integratedDatabase, LocalDatabase localDatabase, Map<PeerID, RemoteDatabase> remoteDatabases, String library, ItemLockManager itemLockManager, LibraryItem integratedItem) throws IllegalDataException, DBException, ParseException, IOException, CorruptDataException {
        // copy the information from the composers. First, the local item (if any). Then, the remote items by order of creation date (older to newer)
        synchronized (itemLockManager.getLock(library)) {

            String itemID = integratedItem.getIdentifier();
            Map<String, String> oldStateHashMap = integratedItem.getStateHashPerField();
            integratedItem.reset();
            if (integratedDatabase.containsKeyToLocalItem(itemID)) {
                merge(integratedItem, localDatabase.getDatabase().getItem(library, integratedDatabase.getItemToLocalItem(itemID)));
            }
            List<LibraryItem> remoteItems = new ArrayList<>();
            for (IntegratedDatabase.PeerAndId peerAndId : integratedDatabase.getRemotePeerAndID(itemID)) {
                remoteItems.add(remoteDatabases.get(peerAndId.peerID).getDatabase().getItem(library, peerAndId.id));
            }
            Collections.sort(remoteItems, ComparatorHandler.getComparator(ComparatorHandler.Comparison.CREATION_DATE));
            for (LibraryItem remoteItem : remoteItems) {
                merge(integratedItem, remoteItem);
            }
            // if there have been changes, notify them

//        boolean hasChanged = !oldItemState.equals(integratedItem.getStateHash());
            Map<String, String> newStateHashMap = integratedItem.getStateHashPerField();
            Set<String> modifiedFields = new HashSet<>();
            for (String field : oldStateHashMap.keySet()) {
                if (!oldStateHashMap.get(field).equals(newStateHashMap.get(field))) {
                    modifiedFields.add(field);
                }
            }

            return modifiedFields;

            // the modification date of the integrated item is modified regardless of whether there have been changes or not. This way, this value
            // actually stores the date of the last integration of the item. Modification can be checked by looking at the remote items and local item
        }
    }

    private static void merge(LibraryItem item, LibraryItem anotherItem) throws DBException, ParseException, IllegalDataException, IOException {
        // todo these methods should be prepared for inconsistencies (a pointer to a non existing item)
        item.merge(anotherItem, null);
    }

}
