package jacz.peerengineclient.libraries;

import jacz.peerengineclient.JPeerEngineClient;
import jacz.peerengineclient.dbs_old.LibraryManagerConcurrencyController;
import jacz.peerengineclient.dbs_old.LibraryManagerIO;
import jacz.peerengineclient.dbs_old.LibraryManagerNotifications;
import jacz.peerengineclient.libraries.library_images.IntegratedDatabase;
import jacz.peerengineclient.libraries.library_images.LocalDatabase;
import jacz.peerengineclient.libraries.library_images.RemoteDatabase;
import jacz.peerengineclient.libraries.synch.LibrarySynchEvents;
import jacz.peerengineclient.libraries.synch.LibrarySynchManager;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.data_synchronization.AccessorNotFoundException;
import jacz.peerengineservice.util.data_synchronization.DataAccessor;
import jacz.peerengineservice.util.data_synchronization.ServerSynchRequestAnswer;
import jacz.store.database.DatabaseMediator;
import jacz.util.concurrency.concurrency_controller.ConcurrencyController;

import java.io.IOException;
import java.util.Map;

/**
 * Created by Alberto on 02/12/2015.
 */
public class LibraryManager {

    /**
     * The integrated database, composed by the local database and the remote databases. This is what the user visualizes
     * <p>
     * The items from the integrated database which have a related local item are the ones shared to other peers
     */
    private IntegratedDatabase integratedDatabase;

    /**
     * The local database, with items created by the user
     */
    private LocalDatabase localDatabase;

    /**
     * The remote databases, with libraries shared to us by friend peers
     */
    private Map<PeerID, RemoteDatabase> remoteDatabases;

    /**
     * Management of synch processes
     */
    private final LibrarySynchManager librarySynchManager;

    /**
     * Relation of remote items that have been modified since the last library integration, and thus need to be re-integrated. We collect all these
     * items during the remote synching process, and once synching is complete, we use this information to integrate the libraries
     */
    private RemoteDatabasesIntegrator.RemoteModifiedItems remoteModifiedItems;

    /**
     * A concurrency controller for controlling the execution of the different integration processes
     * The controller allows several simultaneous integrations of remote items, but one local item integration which pauses all remote integrations
     * Local items have higher priorities than remote items
     */
    // todo move to integrator, we only use it there
    private final ConcurrencyController concurrencyController;

    private final RemoteDatabasesIntegrator remoteDatabasesIntegrator;

    private final LibraryManagerNotifications libraryManagerNotifications;

    private boolean alive;


    public LibraryManager(
            IntegratedDatabase integratedDatabase,
            LocalDatabase localDatabase,
            Map<PeerID, RemoteDatabase> remoteDatabases,
            LibraryManagerNotifications libraryManagerNotifications,
            LibrarySynchEvents librarySynchEvents,
            JPeerEngineClient peerEngineClient) {
        this.integratedDatabase = integratedDatabase;
        this.localDatabase = localDatabase;
        this.remoteDatabases = remoteDatabases;
        this.librarySynchManager = new LibrarySynchManager(librarySynchEvents, peerEngineClient);
        this.libraryManagerNotifications = libraryManagerNotifications;
        remoteModifiedItems = new RemoteDatabasesIntegrator.RemoteModifiedItems();
        concurrencyController = new LibraryManagerConcurrencyController();
        remoteDatabasesIntegrator = new RemoteDatabasesIntegrator(concurrencyController, this, remoteModifiedItems, integratedDatabase, localDatabase, remoteDatabases, itemLockManager);
        alive = true;
    }

    IntegratedDatabase getIntegratedDatabase() {
        return integratedDatabase;
    }

    LocalDatabase getLocalDatabase() {
        return localDatabase;
    }

    Map<PeerID, RemoteDatabase> getRemoteDatabases() {
        return remoteDatabases;
    }

    /**
     * An item in the local database was modified, and it must be reflected in the integrated database immediately
     * <p>
     * This method is blocking, and execution completes once the integrated database has been updated with the local changes. The request must
     * therefore be immediately attended
     *
     * @param library      local library of the modified item
     * @param elementIndex index of the modified item
     */
    public synchronized void localItemModified(String library, String elementIndex) {
        // local item modifications are served as soon as possible, interrupting if necessary the integration of remote items
        // the method is blocking, waiting until the item has been integrated
        if (alive) {
            concurrencyController.beginActivity(LibraryManagerConcurrencyController.INTEGRATE_LOCAL_ITEM);
            // todo integrate local item
//            try {
//                Triple<Set<String>, String, Boolean> hasChangedItemIdAndHasLocal = integrateLocalItem(library, elementIndex);
//                if (hasChangedItemIdAndHasLocal.element1 != null) {
//                    reportIntegratedItemModified(library, hasChangedItemIdAndHasLocal.element1, hasChangedItemIdAndHasLocal.element2, hasChangedItemIdAndHasLocal.element3, true);
//                }
//            } catch (Exception e) {
//                databasesCannotBeAccessed();
//            }
            concurrencyController.endActivity(LibraryManagerConcurrencyController.INTEGRATE_LOCAL_ITEM);
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * Performs the integration of a local item in the integrated database. Permission must have been previously gained, as this method does not
     * check the conditions for it
     *
     * @param library      local library of the modified item
     * @param elementIndex index of the modified item
     */
//    private Triple<Set<String>, String, Boolean> integrateLocalItem(String library, String elementIndex) throws IllegalDataException, DBException, ParseException, IOException, CorruptDataException {
//        LibraryItem localItem = localDatabase.getDatabase().getItem(library, elementIndex);
//        return ItemIntegrator.integrateExternalItem(integratedDatabase, localDatabase, remoteDatabases, library, itemLockManager, null, elementIndex, localItem, localDatabase.getItemsToIntegratedItems());
//    }

    /**
     * An item from a remote database has been modified, and therefore integration is required with the integrated database
     * This method is invoked by a synch task being currently run. It is the only way this can happen.
     * <p>
     * This method is invoked by the list accessors of the remote databases
     *
     * @param peerID peer whose library must be synched
     * @param type   type (class) of the item
     * @param itemId id of the element to integrate
     * @param alive  if this item is alive (true) or has been deleted (false)
     */
    public synchronized void remoteItemModified(PeerID peerID, DatabaseMediator.ITEM_TYPE type, String itemId, boolean alive) {
        // todo integrate this single item
//        remoteModifiedItems.addItem(peerID, library, elementIndex);
//        remoteDatabasesIntegrator.remoteDatabaseIntegrationRequested();
    }
//    public synchronized void remoteItemModified(PeerID peerID, String library, String elementIndex) {
//        remoteModifiedItems.addItem(peerID, library, elementIndex);
//        remoteDatabasesIntegrator.remoteDatabaseIntegrationRequested();
//    }

    /**
     * Allows other sub-modules notifying that any of the databases could not be accessed
     */
//    synchronized void databasesCannotBeAccessed() {
//        // todo is this needed?
//        libraryManagerNotifications.reportErrorAccessingDatabases();
//    }
    public synchronized DataAccessor getSharedListAccessor() {
        return localDatabase.getDataAccessor();
    }

    public synchronized DataAccessor getRemoteListAccessor(PeerID peerID) throws AccessorNotFoundException {
        if (remoteDatabases.containsKey(peerID)) {
            return remoteDatabases.get(peerID).getDataAccessor();
        } else {
            throw new AccessorNotFoundException();
        }
    }

    /**
     * A remote peer is requesting to get access to the shared library for synchronizing it with us
     */
    public synchronized ServerSynchRequestAnswer requestForSharedLibrarySynchFromRemotePeer(PeerID peerID) {
        return librarySynchManager.requestForSharedLibrarySynch(peerID);
    }

    /**
     * Adds a new peer in the list of peers with databases. This should be invoked when we have a new friend peer.
     * This information is stored in the config information
     *
     * @param peerID new friend peer
     */
    public synchronized void addPeer(String path, PeerID peerID) throws IOException {
        if (!remoteDatabases.containsKey(peerID)) {
            RemoteDatabase remoteDatabase = LibraryManagerIO.createNewRemoteDatabase(path, peerID);
            remoteDatabases.put(peerID, remoteDatabase);
        }
    }

    /**
     * Permanently removes a peer from the database handling.
     * Its stored databases will be removed and erased from the integrated information
     *
     * @param peerID peer which is no longer friend.
     */
    public synchronized void removePeer(String path, PeerID peerID) {
        if (remoteDatabases.containsKey(peerID)) {
            jacz.peerengineclient.libraries.library_images.RemoteDatabase remoteDatabase = remoteDatabases.remove(peerID);
            LibraryManagerIO.removeRemoteDatabase(path, peerID);
            // todo remove from integrated, and copy necessary info to local database
        }
    }

    private boolean isRemoteDatabaseBeingIntegrated() {
        return remoteDatabasesIntegrator.isCurrentlyIntegrating();
    }

    /**
     * Issues an order for stopping all actions. Databases will be no longer modified. The method blocks until operation is complete
     * <p>
     * This must be invoked before saving the state of the library manager
     */
    public void stop() {
        synchronized (this) {
            alive = false;
        }
        concurrencyController.stopAndWaitForFinalization();
        librarySynchManager.stop();
    }
}
