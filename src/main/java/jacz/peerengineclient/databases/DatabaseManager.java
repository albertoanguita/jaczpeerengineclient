package jacz.peerengineclient.databases;

import jacz.database.DatabaseItem;
import jacz.database.Movie;
import jacz.database.TVSeries;
import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineclient.databases.integration.IntegrationConcurrencyController;
import jacz.peerengineclient.databases.integration.IntegrationEvents;
import jacz.peerengineclient.databases.integration.ItemIntegrator;
import jacz.peerengineclient.databases.integration.SharedDatabaseGenerator;
import jacz.peerengineclient.databases.synch.DatabaseAccessor;
import jacz.peerengineclient.databases.synch.DatabaseSynchEvents;
import jacz.peerengineclient.databases.synch.DatabaseSynchManager;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.util.data_synchronization.ServerBusyException;
import jacz.util.concurrency.concurrency_controller.ConcurrencyController;

import java.io.IOException;
import java.util.stream.Stream;

/**
 * This class manages data databases and their proper synchronization and integration
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
            PeerEngineClient peerEngineClient) throws IOException {
        this.peerEngineClient = peerEngineClient;
        this.databases = databases;
        this.databaseSynchManager = new DatabaseSynchManager(this, databaseSynchEvents, peerEngineClient, databases);
        dataIntegrationConcurrencyController = new ConcurrencyController(new IntegrationConcurrencyController());
        sharedDatabaseGenerator = new SharedDatabaseGenerator(databases, dataIntegrationConcurrencyController);
        itemIntegrator = new ItemIntegrator(dataIntegrationConcurrencyController, integrationEvents);
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
    public void localItemModified(DatabaseItem item) throws IllegalStateException {
        itemIntegrator.integrateLocalItem(databases, item);
    }

    public void reportNewMedia(DatabaseItem item) {
        itemIntegrator.reportNewMedia(item);
    }

    public void reportNewImage(String hash) {
        Stream.concat(Movie.getMovies(databases.getIntegratedDB()).stream(), TVSeries.getTVSeries(databases.getIntegratedDB()).stream())
                .filter(item -> item.getImageHash() != null && item.getImageHash().getHash().equals(hash))
                .forEach(itemIntegrator::reportNewImage);
    }

    public void removeLocalItem(DatabaseItem item) {
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
    public void remoteItemModified(PeerId peerID, DatabaseItem item) {
        itemIntegrator.integrateRemoteItem(databases, peerID, item);
    }

    /**
     * A remote item is about to be removed
     *
     * @param peerID
     */
    public void remoteItemWillBeRemoved(PeerId peerID, DatabaseItem item) {
        itemIntegrator.removeRemoteItem(databases, peerID, item);
    }

    /**
     * A remote peer is requesting to get access to the shared library for synchronizing it with us
     */
    public DatabaseAccessor requestForSharedDatabaseSynchFromRemotePeer(PeerId peerID) throws ServerBusyException {
        return databaseSynchManager.requestForSharedDatabaseSynch(peerID);
    }

    /**
     * Permanently removes a peer from the database handling.
     * Its stored databases will be removed and erased from the integrated information
     * <p>
     * This method should be invoked before running any integration tasks (e.g. at startup), for old peers for
     * example
     * <p>
     * Items that have some media downloaded will be moved to the deleted database. The rest of items will be simple
     * removed
     *
     * @param peerID peer which is no longer friend.
     */
    public synchronized void removePeer(String path, PeerId peerID) {
        // @FUTURE@ todo invoke and complete code. Remove items and move those with media to deleted. Fix item relations. Invoke for very old peers
        // databases.removeRemoteDB(peerID);
        // DatabaseIO.removeRemoteDatabase(path, peerID);
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
