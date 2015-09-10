package jacz.peerengineclient.dbs;

import jacz.store.IllegalDataException;
import jacz.store.ResultSet;
import jacz.store.common.LibraryItem;
import jacz.store.db_mediator.CorruptDataException;
import jacz.store.db_mediator.DBException;
import jacz.peerengineservice.PeerID;
import jacz.peerengineclient.dbs.sorting.ComparatorHandler;
import jacz.util.lists.Triple;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

/**
 * Utility methods for integrating items
 */
public class ItemIntegrator {

    private static final float MATCH_THRESHOLD = 0.9f;


    static Triple<Set<String>, String, Boolean> integrateExternalItem(
            IntegratedDatabase integratedDatabase,
            LocalDatabase localDatabase,
            Map<PeerID, RemoteDatabase> remoteDatabases,
            String library,
            ItemLockManager itemLockManager,
            PeerID remotePeerID,
            String externalItemID,
            LibraryItem externalItem,
            Map<String, String> externalToIntegratedItems) throws ParseException, IOException, DBException, IllegalDataException, CorruptDataException {
        LibraryItem matchedIntegratedItem;

        if (externalItem != null) {
            // there is an external item -> propagate its changes to the corresponding integrated item
            if (!externalToIntegratedItems.containsKey(externalItemID)) {
                // not mapped to any integrated item (the remote item is new and it is not linked to any integrated item)
                ResultSet integratedItems = integratedDatabase.getDatabase().getAllItems(library);
                matchedIntegratedItem = null;
                while (integratedItems.hasNext()) {
                    LibraryItem integratedItem = integratedItems.next();
                    if (externalItem.match(integratedItem) >= MATCH_THRESHOLD) {
                        // match found! -> remember the integrated item
                        matchedIntegratedItem = integratedItem;
                        break;
                    }
                }
                // todo check with rest of integrated items again
                if (matchedIntegratedItem == null) {
                    // we did not find any match -> create a new integrated item
                    matchedIntegratedItem = integratedDatabase.getDatabase().createNewItem(library);
                }
                externalToIntegratedItems.put(externalItemID, matchedIntegratedItem.getIdentifier());
                // we must put the equivalent pointer in the integrated database. Different cases for local item (remotePeerID == null) or remote item
                if (remotePeerID == null) {
                    // local item
                    integratedDatabase.putItemToLocalItem(matchedIntegratedItem.getIdentifier(), externalItemID);
                } else {
                    // remote item
                    integratedDatabase.addRemoteLink(matchedIntegratedItem.getIdentifier(), remotePeerID, externalItemID);
                }
            } else {
                matchedIntegratedItem = integratedDatabase.getDatabase().getItem(library, externalToIntegratedItems.get(externalItemID));
            }

            // now copy the required information from the remote item to the matched integrated item
            // this requires creating the integrated item from zero, with all the information of its local and remote composers
            Set<String> changedFields = populateIntegratedItemStatic(integratedDatabase, localDatabase, remoteDatabases, library, itemLockManager, matchedIntegratedItem);
            return new Triple<>(changedFields, matchedIntegratedItem.getIdentifier(), integratedDatabase.containsKeyToLocalItem(matchedIntegratedItem.getIdentifier()));
        } else {
            // the modified remote item was removed. Check if there is an integrated item, and propagate changes if so
            if (externalToIntegratedItems.containsKey(externalItemID)) {
                // there was an integrated item for this external item
                matchedIntegratedItem = integratedDatabase.getDatabase().getItem(library, externalToIntegratedItems.get(externalItemID));
                Set<String> changedFields = populateIntegratedItemStatic(integratedDatabase, localDatabase, remoteDatabases, library, itemLockManager, matchedIntegratedItem);
                return new Triple<>(changedFields, matchedIntegratedItem.getIdentifier(), integratedDatabase.containsKeyToLocalItem(matchedIntegratedItem.getIdentifier()));
            } else {
                // there is no integrated item, nothing to do
                return new Triple<>(null, null, false);
            }
        }
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
    private static Set<String> populateIntegratedItemStatic(IntegratedDatabase integratedDatabase, LocalDatabase localDatabase, Map<PeerID, RemoteDatabase> remoteDatabases, String library, ItemLockManager itemLockManager, LibraryItem integratedItem) throws IllegalDataException, DBException, ParseException, IOException, CorruptDataException {
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
