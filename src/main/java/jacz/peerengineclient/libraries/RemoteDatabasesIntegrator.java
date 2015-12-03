package jacz.peerengineclient.libraries;

import jacz.peerengineclient.dbs_old.ItemLockManager;
import jacz.peerengineclient.dbs_old.LibraryManagerConcurrencyController;
import jacz.peerengineclient.libraries.library_images.IntegratedDatabase;
import jacz.peerengineclient.libraries.library_images.LocalDatabase;
import jacz.peerengineclient.libraries.library_images.RemoteDatabase;
import jacz.peerengineservice.PeerID;
import jacz.store.Database;
import jacz.store.common.LibraryItem;
import jacz.util.bool.MutableBoolean;
import jacz.util.concurrency.concurrency_controller.ConcurrencyController;
import jacz.util.concurrency.daemon.Daemon;
import jacz.util.concurrency.daemon.DaemonAction;
import jacz.util.lists.Triple;

import java.util.*;

/**
 * Integrates items from remote databases into the integrated database
 */
public class RemoteDatabasesIntegrator implements DaemonAction {

    /**
     * Class representing an item from a remote database that has been modified, for a specific peer.
     */
    public static class PeerRemoteModifiedItems {

        /**
         * Relation of items that have been modified, organized by item container. If a container has no modified items, there is no such
         * entry in the map
         */
        final Map<String, List<String>> modifiedItemsByContainer;

        public PeerRemoteModifiedItems() {
            modifiedItemsByContainer = new HashMap<>();
        }
    }

    public static class RemoteModifiedItems {

        /**
         * Relation of remote items that have been modified since the last library integration, and thus need to be re-integrated. We collect all these
         * items during the remote synching process, and once synching is complete, we use this information to integrate the libraries
         */
        private final Map<PeerID, PeerRemoteModifiedItems> remoteModifiedItems;

        private PeerID firstPeer;

        private List<String> libraryIntegrationOrder;

        public RemoteModifiedItems() {
            remoteModifiedItems = new HashMap<>();
            firstPeer = null;
        }

        public void addItem(PeerID peerID, String library, String elementIndex) {
            if (!remoteModifiedItems.containsKey(peerID)) {
                remoteModifiedItems.put(peerID, new PeerRemoteModifiedItems());
            }
            if (!remoteModifiedItems.get(peerID).modifiedItemsByContainer.containsKey(library)) {
                remoteModifiedItems.get(peerID).modifiedItemsByContainer.put(library, new ArrayList<String>());
            }
            remoteModifiedItems.get(peerID).modifiedItemsByContainer.get(library).add(elementIndex);
        }

        public void beginIntegrationOfItems() {
            libraryIntegrationOrder = Database.libraryIntegrationOrder();
            evaluateFirstPeer();
        }

        public Triple<PeerID, String, String> retrieveItem() {
            if (firstPeer != null) {
                if (!libraryIntegrationOrder.isEmpty()) {
                    // there are libraries left to check for the current peer
                    PeerRemoteModifiedItems peerRemoteModifiedItems = remoteModifiedItems.get(firstPeer);
                    String library = libraryIntegrationOrder.get(0);

                    if (peerRemoteModifiedItems.modifiedItemsByContainer.containsKey(library)) {
                        // this library is registered for this peer
                        if (!peerRemoteModifiedItems.modifiedItemsByContainer.get(library).isEmpty()) {
                            // there are items left in this library -> get the first one
                            String elementIndex = peerRemoteModifiedItems.modifiedItemsByContainer.get(library).remove(0);
                            return new Triple<>(firstPeer, library, elementIndex);
                        } else {
                            // no items left for this library -> remove library and continue
                            peerRemoteModifiedItems.modifiedItemsByContainer.remove(library);
                            return retrieveItem();
                        }
                    } else {
                        // this library is not registered for this peer -> remove from library order
                        libraryIntegrationOrder.remove(0);
                        return retrieveItem();
                    }
                } else {
                    // move to next peer
                    remoteModifiedItems.remove(firstPeer);
                    libraryIntegrationOrder = Database.libraryIntegrationOrder();
                    evaluateFirstPeer();
                    return retrieveItem();
                }
            } else {
                // no items to retrieve
                return null;
            }
        }

        private void evaluateFirstPeer() {
            if (!remoteModifiedItems.isEmpty()) {
                List<PeerID> peerIDList = new ArrayList<>(remoteModifiedItems.keySet());
                Collections.sort(peerIDList);
                firstPeer = peerIDList.get(0);
            } else {
                firstPeer = null;
            }
        }
    }


    private final ConcurrencyController concurrencyController;

    private final LibraryManager libraryManager;

    private final RemoteModifiedItems remoteModifiedItems;

    private final IntegratedDatabase integratedDatabase;

    private final LocalDatabase localDatabase;

    private final Map<PeerID, RemoteDatabase> remoteDatabases;

    private final ItemLockManager itemLockManager;

    private final Daemon daemon;

    private final MutableBoolean currentlyIntegrating;

    public RemoteDatabasesIntegrator(
            ConcurrencyController concurrencyController,
            LibraryManager libraryManager,
            RemoteModifiedItems remoteModifiedItems,
            IntegratedDatabase integratedDatabase,
            LocalDatabase localDatabase,
            Map<PeerID, RemoteDatabase> remoteDatabases,
            ItemLockManager itemLockManager) {
        this.concurrencyController = concurrencyController;
        this.libraryManager = libraryManager;
        this.remoteModifiedItems = remoteModifiedItems;
        this.integratedDatabase = integratedDatabase;
        this.localDatabase = localDatabase;
        this.remoteDatabases = remoteDatabases;
        this.itemLockManager = itemLockManager;
        currentlyIntegrating = new MutableBoolean(false);
        daemon = new Daemon(this);
    }

    public void remoteDatabaseIntegrationRequested() {
        daemon.stateChange();
    }

    public boolean isCurrentlyIntegrating() {
        synchronized (currentlyIntegrating) {
            return currentlyIntegrating.isValue();
        }
    }

    @Override
    public boolean solveState() {
        // this task performs the integration of all currently modified remote items in the integrated database
        // it notifies the concurrency controller to avoid collisions with other tasks
        concurrencyController.beginActivity(LibraryManagerConcurrencyController.INTEGRATE_REMOTE_LIBRARIES);
        synchronized (currentlyIntegrating) {
            currentlyIntegrating.setValue(true);
        }

        remoteModifiedItems.beginIntegrationOfItems();
        boolean finished = false;
        do {
            Triple<PeerID, String, String> modifiedItem = remoteModifiedItems.retrieveItem();
            if (modifiedItem != null) {
                try {
                    PeerID remotePeerID = modifiedItem.element1;
                    String library = modifiedItem.element2;
                    String modifiedItemID = modifiedItem.element3;
                    LibraryItem remoteItem = remoteDatabases.get(remotePeerID).getDatabase().getItem(library, modifiedItemID);
                    Triple<Set<String>, String, Boolean> hasChangedItemIdAndHasLocal = ItemIntegrator.integrateExternalItem(integratedDatabase, localDatabase, remoteDatabases, library, itemLockManager, remotePeerID, modifiedItemID, remoteItem, remoteDatabases.get(remotePeerID).getItemsToIntegratedItems());
                    if (hasChangedItemIdAndHasLocal.element1 != null) {
                        libraryManager.reportIntegratedItemModified(library, hasChangedItemIdAndHasLocal.element1, hasChangedItemIdAndHasLocal.element2, hasChangedItemIdAndHasLocal.element3, false);
                    }
                } catch (Exception e) {
                    // error accessing the database
                    libraryManager.databasesCannotBeAccessed();
                    finished = true;
                }
            } else {
                finished = true;
            }
        } while (!finished);

        // notify any possibly modified shared library
        libraryManager.flushSharedLibraryModificationsNotification();

        // conclude the activity
        synchronized (currentlyIntegrating) {
            currentlyIntegrating.setValue(false);
        }
        concurrencyController.endActivity(LibraryManagerConcurrencyController.INTEGRATE_REMOTE_LIBRARIES);
        return true;
    }
}
