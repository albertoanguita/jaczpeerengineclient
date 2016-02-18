package jacz.peerengineclient.databases.synch;

import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineclient.databases.DatabaseManager;
import jacz.peerengineclient.databases.Databases;
import jacz.peerengineclient.util.synch.DataAccessorController;
import jacz.peerengineclient.util.synch.SynchMode;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.UnavailablePeerException;
import jacz.peerengineservice.util.data_synchronization.DummyProgress;
import jacz.peerengineservice.util.data_synchronization.ServerBusyException;
import jacz.peerengineservice.util.data_synchronization.SynchError;
import jacz.util.notification.ProgressNotificationWithError;

/**
 * This class manages all the ongoing synch processes, maintaining a table with the active synch processes
 * <p>
 * It also handles the synch requests, being able to deny them if they conflict with the ongoing synch processes
 * <p>
 * Finally, this class is in charge of reporting the progress of the active synch processes
 */
public class DatabaseSynchManager {


    private static class DatabaseSynchAccessorController extends DataAccessorController<DatabaseAccessor, DatabaseAccessor> {

        private final DatabaseSynchManager databaseSynchManager;

        private final DatabaseManager databaseManager;

        private final Databases databases;


        public DatabaseSynchAccessorController(
                PeerEngineClient peerEngineClient,
                DatabaseSynchManager databaseSynchManager,
                DatabaseManager databaseManager,
                Databases databases) {
            super(RECENTLY_THRESHOLD, LARGE_SHARED_SYNCH_COUNT, VERY_LARGE_SHARED_SYNCH_COUNT, DATABASE_SYNCH_TIMEOUT, MAX_DATABASE_SYNCH_TASKS, peerEngineClient);
            this.databaseSynchManager = databaseSynchManager;
            this.databaseManager = databaseManager;
            this.databases = databases;
        }

        @Override
        public ProgressNotificationWithError<Integer, SynchError> getLocalSynchProgress(PeerID peerID) {
            return new DatabaseSynchProgress(databaseSynchManager, SynchMode.LOCAL, peerID);
        }

        @Override
        public DatabaseAccessor getLocalDataAccessor(PeerID peerID, ProgressNotificationWithError<Integer, SynchError> progress) {
            return new DatabaseAccessor(
                    databaseManager,
                    peerID,
                    databases.getSharedDB(),
                    progress);
        }

        @Override
        public ProgressNotificationWithError<Integer, SynchError> getRemoteSynchProgress(PeerID peerID) throws UnavailablePeerException {
            return new DummyProgress();
        }

        @Override
        public DatabaseAccessor getRemoteDataAccessor(PeerID peerID) throws UnavailablePeerException {
            return new DatabaseAccessor(
                    databaseManager,
                    peerID,
                    databases.getRemoteDB(peerID),
                    new DatabaseSynchProgress(databaseSynchManager, SynchMode.REMOTE, peerID));
        }
    }


    private static final long RECENTLY_THRESHOLD = 30000;

    private static final int LARGE_SHARED_SYNCH_COUNT = 5;

    private static final int VERY_LARGE_SHARED_SYNCH_COUNT = 10;

    private static final long DATABASE_SYNCH_TIMEOUT = 15000L;

    private static final int MAX_DATABASE_SYNCH_TASKS = 10;

    private final DatabaseSynchEvents databaseSynchEvents;

    private final DatabaseSynchAccessorController databaseSynchAccessorController;

    public DatabaseSynchManager(
            DatabaseManager databaseManager,
            DatabaseSynchEvents databaseSynchEvents,
            PeerEngineClient peerEngineClient,
            Databases databases) {
        this.databaseSynchEvents = databaseSynchEvents;
        databaseSynchAccessorController = new DatabaseSynchAccessorController(peerEngineClient, this, databaseManager, databases);
    }

    /**
     * A remote peer is requesting to get access to the shared database for synchronizing it with us
     * <p>
     * This process can happen along with any other process*. We just must take care that the retrieval of index and hash lists is properly
     * synchronized with other operations. A local or remote item integration might of course break the synchronization, but that is a risk that
     * we must assume, and the other peer will be notified of this.
     * <p>
     * The database manager will reject these requests if a remote integration is taking place, because it would most certainly break the synch
     * and we would be waisting bandwidth
     */
    public DatabaseAccessor requestForSharedDatabaseSynch(PeerID peerID) throws ServerBusyException {
        return databaseSynchAccessorController.requestForLocalHashSynch(peerID);
    }

    public void synchRemoteDatabase(PeerID peerID) {
        databaseSynchAccessorController.synchRemoteShare(peerID);
    }

    void sharedDatabaseSynchBegins(PeerID remotePeerID) {
        databaseSynchEvents.sharedSynchStarted(remotePeerID);
    }

    void sharedDatabaseSynchProgress(PeerID remotePeerID, Integer progress) {
        databaseSynchEvents.sharedSynchProgress(remotePeerID, progress);
    }

    void sharedDatabaseSynchComplete(PeerID remotePeerID) {
        databaseSynchEvents.sharedSynchCompleted(remotePeerID);
    }

    void sharedDatabaseSynchFailed(PeerID remotePeerID, SynchError error) {
        // todo check errors (fatal with DATA_ACCESS_ERROR)
        databaseSynchEvents.sharedSynchError(remotePeerID, error);
    }

    void sharedDatabaseSynchTimedOut(PeerID remotePeerID) {
        databaseSynchEvents.sharedSynchTimeout(remotePeerID);
    }

    void remoteDatabaseSynchBegins(PeerID remotePeerID) {
        databaseSynchEvents.remoteSynchStarted(remotePeerID);
    }

    void remoteDatabaseSynchProgress(PeerID remotePeerID, Integer progress) {
        databaseSynchEvents.remoteSynchProgress(remotePeerID, progress);
    }

    void remoteDatabaseSynchComplete(PeerID remotePeerID) {
        databaseSynchEvents.remoteSynchCompleted(remotePeerID);
    }

    void remoteDatabaseSynchFailed(PeerID remotePeerID, SynchError error) {
        // todo check errors (fatal with DATA_ACCESS_ERROR)
        databaseSynchEvents.remoteSynchError(remotePeerID, error);
    }

    void remoteDatabaseSynchTimedOut(PeerID remotePeerID) {
        databaseSynchEvents.remoteSynchTimeout(remotePeerID);
    }

    public void stop() {
        databaseSynchAccessorController.stop();
    }
}
