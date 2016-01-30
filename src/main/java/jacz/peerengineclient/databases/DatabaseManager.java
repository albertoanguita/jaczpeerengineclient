package jacz.peerengineclient.databases;

import jacz.database.DatabaseItem;
import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineclient.databases.integration.IntegrationConcurrencyController;
import jacz.peerengineclient.databases.integration.IntegrationEvents;
import jacz.peerengineclient.databases.integration.ItemIntegrator;
import jacz.peerengineclient.databases.integration.SharedDatabaseGenerator;
import jacz.peerengineclient.databases.synch.DatabaseAccessor;
import jacz.peerengineclient.databases.synch.DatabaseSynchEvents;
import jacz.peerengineclient.databases.synch.DatabaseSynchManager;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.data_synchronization.ServerBusyException;
import jacz.util.concurrency.concurrency_controller.ConcurrencyController;

import java.io.IOException;
import java.util.Set;

/**
 * This class manages data databases and their proper synchronization and integration
 * todo check synch statements, maybe not needed (e.g. integrate local item)
 */
public class DatabaseManager {

    private final Databases databases;

    /**
     * Management of synch processes
     */
    private final DatabaseSynchManager databaseSynchManager;

    /**
     * Relation of remote items that have been modified since the last library integration, and thus need to be re-integrated. We collect all these
     * items during the remote synching process, and once synching is complete, we use this information to integrate the databases
     * todo remove
     */
//    private RemoteDatabasesIntegrator.RemoteModifiedItems remoteModifiedItems;

    /**
     * A concurrency controller for controlling the execution of the different integration processes
     * The controller allows several simultaneous integrations of remote items, but one local item integration which pauses all remote integrations
     * Local items have higher priorities than remote items
     */
    // todo move to integrator, we only use it there
//    private final ConcurrencyController concurrencyController;

//    private final RemoteDatabasesIntegrator remoteDatabasesIntegrator;

    private final ConcurrencyController concurrencyController;

    private final SharedDatabaseGenerator sharedDatabaseGenerator;

    private final ItemIntegrator itemIntegrator;

    private boolean alive;


    public DatabaseManager(
            Databases databases,
            DatabaseSynchEvents databaseSynchEvents,
            IntegrationEvents integrationEvents,
            PeerEngineClient peerEngineClient,
            String basePath,
            Set<PeerID> friendPeers) throws IOException {
        this.databases = databases;
        this.databaseSynchManager = new DatabaseSynchManager(this, databaseSynchEvents, peerEngineClient, databases);
        concurrencyController = new ConcurrencyController(new IntegrationConcurrencyController());
        sharedDatabaseGenerator = new SharedDatabaseGenerator(databases, concurrencyController);
        itemIntegrator = new ItemIntegrator(sharedDatabaseGenerator, concurrencyController, integrationEvents);
        alive = true;
        // just in case, try to add databases for all registered friend peers
        for (PeerID friendPeer : friendPeers) {
            addPeer(basePath, friendPeer);
        }
    }

    public DatabaseSynchManager getDatabaseSynchManager() {
        return databaseSynchManager;
    }

    public Databases getDatabases() {
        return databases;
    }

//    IntegratedDatabase getIntegratedDatabase() {
//        return integratedDatabase;
//    }
//
//    LocalDatabase getLocalDatabase() {
//        return localDatabase;
//    }
//
//    Map<PeerID, RemoteDatabase> getRemoteDatabases() {
//        return remoteDatabases;
//    }

    /**
     * An item in the local database was modified, and it must be reflected in the integrated database immediately
     * <p>
     * This method is blocking, and execution completes once the integrated database has been updated with the local changes. The request must
     * therefore be immediately attended
     *
     * @param item the modified item
     */
    public synchronized void localItemModified(DatabaseItem item) {
        itemIntegrator.integrateLocalItem(databases, item);
    }

    /**
     * An item from a remote database has been modified, and therefore integration is required with the integrated database
     * This method is invoked by a synch task being currently run. It is the only way this can happen.
     * <p>
     * This method is invoked by the list accessors of the remote databases
     *
     * @param peerID peer whose library must be synched
     */
    public synchronized void remoteItemModified(PeerID peerID, DatabaseItem item) {
        itemIntegrator.integrateRemoteItem(databases, peerID, item);
    }

    /**
     * A remote item is about to be removed
     *
     * @param peerID
     */
    public synchronized void remoteItemWillBeRemoved(PeerID peerID, DatabaseItem item) {
        itemIntegrator.removeRemoteItem(databases, peerID, item);
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
//    public synchronized DataAccessor getSharedListAccessor() {
//        return localDatabase.getDataAccessor();
//    }

//    public synchronized DataAccessor getRemoteListAccessor(PeerID peerID) throws AccessorNotFoundException {
//        if (remoteDatabases.containsKey(peerID)) {
//            return remoteDatabases.get(peerID).getDataAccessor();
//        } else {
//            throw new AccessorNotFoundException();
//        }
//    }

    /**
     * A remote peer is requesting to get access to the shared library for synchronizing it with us
     */
    public synchronized DatabaseAccessor requestForSharedDatabaseSynchFromRemotePeer(PeerID peerID) throws ServerBusyException {
        return databaseSynchManager.requestForSharedDatabaseSynch(peerID);
    }

    /**
     * Adds a new peer in the list of peers with databases. This should be invoked when we have a new friend peer.
     * This information is stored in the config information
     *
     * @param peerID new friend peer
     */
    public synchronized void addPeer(String path, PeerID peerID) throws IOException {
        if (!databases.getRemoteDBs().containsKey(peerID)) {
            String dbPath = DatabaseIO.createNewRemoteDatabase(path, peerID);
            databases.getRemoteDBs().put(peerID, dbPath);
        }
    }

    /**
     * Permanently removes a peer from the database handling.
     * Its stored databases will be removed and erased from the integrated information
     *
     * @param peerID peer which is no longer friend.
     */
    public synchronized void removePeer(String path, PeerID peerID) {
        databases.getRemoteDBs().remove(peerID);
        DatabaseIO.removeRemoteDatabase(path, peerID);
        // todo remove from integrated, and copy necessary info to local database
    }

//    private boolean isRemoteDatabaseBeingIntegrated() {
//        return remoteDatabasesIntegrator.isCurrentlyIntegrating();
//    }

    /**
     * Issues an order for stopping all actions. Databases will be no longer modified. The method blocks until operation is complete
     * <p>
     * This must be invoked before saving the state of the library manager
     */
    public void stop() {
        synchronized (this) {
            alive = false;
        }
        sharedDatabaseGenerator.stop();
        itemIntegrator.stop();
        concurrencyController.stopAndWaitForFinalization();
    }
}
