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

    private final PeerEngineClient peerEngineClient;

    private final Databases databases;

    /**
     * Management of synch processes
     */
    private final DatabaseSynchManager databaseSynchManager;


    private final ConcurrencyController dataIntegrationConcurrencyController;

    private final SharedDatabaseGenerator sharedDatabaseGenerator;

    private final ItemIntegrator itemIntegrator;


    public DatabaseManager(
            Databases databases,
            DatabaseSynchEvents databaseSynchEvents,
            IntegrationEvents integrationEvents,
            PeerEngineClient peerEngineClient,
            String basePath,
            Set<PeerID> friendPeers) throws IOException {
        this.peerEngineClient = peerEngineClient;
        this.databases = databases;
        this.databaseSynchManager = new DatabaseSynchManager(this, databaseSynchEvents, peerEngineClient, databases);
        dataIntegrationConcurrencyController = new ConcurrencyController(new IntegrationConcurrencyController());
        sharedDatabaseGenerator = new SharedDatabaseGenerator(databases, dataIntegrationConcurrencyController);
        itemIntegrator = new ItemIntegrator(dataIntegrationConcurrencyController, integrationEvents);
        // just in case, try to add databases for all registered friend peers
        for (PeerID friendPeer : friendPeers) {
            addPeer(basePath, friendPeer);
        }
    }

    public void start() {
        sharedDatabaseGenerator.start(peerEngineClient.getFileAPI());
        itemIntegrator.setImageDownloader(peerEngineClient.getImageDownloader());
    }

    public DatabaseSynchManager getDatabaseSynchManager() {
        return databaseSynchManager;
    }

    public SharedDatabaseGenerator getSharedDatabaseGenerator() {
        return sharedDatabaseGenerator;
    }

    public Databases getDatabases() {
        return databases;
    }

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

    public synchronized void removeLocalItem(DatabaseItem item) {
        itemIntegrator.removeLocalContent(databases, item);
        item.delete();
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
    }

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
        if (!databases.containsRemoteDB(peerID)) {
            String dbPath = DatabaseIO.createNewRemoteDatabase(path, peerID);
            databases.addRemoteDB(peerID, dbPath);
        }
    }

    /**
     * Permanently removes a peer from the database handling.
     * Its stored databases will be removed and erased from the integrated information
     *
     * @param peerID peer which is no longer friend.
     */
    public synchronized void removePeer(String path, PeerID peerID) {
        databases.removeRemoteDB(peerID);
        // todo to avoid problems with other pieces of code that access remote dbs, we postpone the actual deletion
        // of the db file until the next startup
        // DatabaseIO.removeRemoteDatabase(path, peerID);
        // todo remove from integrated, and copy necessary info to local database
    }

    /**
     * Issues an order for stopping all actions. Databases will be no longer modified. The method blocks until operation is complete
     * <p>
     * This must be invoked before saving the state of the library manager
     */
    public void stop() {
        sharedDatabaseGenerator.stop();
        itemIntegrator.stop();
        dataIntegrationConcurrencyController.stopAndWaitForFinalization();
        databaseSynchManager.stop();
    }
}
