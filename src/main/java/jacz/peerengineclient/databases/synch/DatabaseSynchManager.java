package jacz.peerengineclient.databases.synch;

import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineclient.databases.DatabaseManager;
import jacz.peerengineclient.databases.Databases;
import jacz.peerengineclient.util.synch.DataAccessorController;
import jacz.peerengineclient.util.synch.SynchMode;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.UnavailablePeerException;
import jacz.peerengineservice.util.data_synchronization.DummyProgress;
import jacz.peerengineservice.util.data_synchronization.ServerBusyException;
import jacz.peerengineservice.util.data_synchronization.SynchError;
import jacz.util.notification.ProgressNotificationWithError;

import java.io.IOException;

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
        public ProgressNotificationWithError<Integer, SynchError> getLocalSynchProgress(PeerId peerID) {
            return new DatabaseSynchProgress(databaseSynchManager, SynchMode.LOCAL, peerID);
        }

        @Override
        public DatabaseAccessor getLocalDataAccessor(PeerId peerID, ProgressNotificationWithError<Integer, SynchError> progress) {
            return new DatabaseAccessor(
                    databaseManager,
                    peerID,
                    databases.getSharedDB(),
                    progress);
        }

        @Override
        public ProgressNotificationWithError<Integer, SynchError> getRemoteSynchProgress(PeerId peerID) throws UnavailablePeerException {
            return new DummyProgress();
        }

        @Override
        public DatabaseAccessor getRemoteDataAccessor(PeerId peerID) throws IOException {
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
    public DatabaseAccessor requestForSharedDatabaseSynch(PeerId peerID) throws ServerBusyException {
        return databaseSynchAccessorController.requestForLocalHashSynch(peerID);
    }

    public void synchRemoteDatabase(PeerId peerID) {
        databaseSynchAccessorController.synchRemoteShare(peerID);
    }

    void sharedDatabaseSynchBegins(PeerId remotePeerId) {
        databaseSynchEvents.sharedSynchStarted(remotePeerId);
    }

    void sharedDatabaseSynchProgress(PeerId remotePeerId, Integer progress) {
        databaseSynchEvents.sharedSynchProgress(remotePeerId, progress);
    }

    void sharedDatabaseSynchComplete(PeerId remotePeerId) {
        databaseSynchEvents.sharedSynchCompleted(remotePeerId);
    }

    void sharedDatabaseSynchFailed(PeerId remotePeerId, SynchError error) {
        // todo check errors (fatal with DATA_ACCESS_ERROR)
        databaseSynchEvents.sharedSynchError(remotePeerId, error);
    }

    void sharedDatabaseSynchTimedOut(PeerId remotePeerId) {
        databaseSynchEvents.sharedSynchTimeout(remotePeerId);
    }

    void remoteDatabaseSynchBegins(PeerId remotePeerId) {
        databaseSynchEvents.remoteSynchStarted(remotePeerId);
    }

    void remoteDatabaseSynchProgress(PeerId remotePeerId, Integer progress) {
        databaseSynchEvents.remoteSynchProgress(remotePeerId, progress);
    }

    void remoteDatabaseSynchComplete(PeerId remotePeerId) {
        databaseSynchEvents.remoteSynchCompleted(remotePeerId);
    }

    void remoteDatabaseSynchFailed(PeerId remotePeerId, SynchError error) {
        // todo check errors (fatal with DATA_ACCESS_ERROR)
        databaseSynchEvents.remoteSynchError(remotePeerId, error);
    }

    void remoteDatabaseSynchTimedOut(PeerId remotePeerId) {
        databaseSynchEvents.remoteSynchTimeout(remotePeerId);
    }

    public void stop() {
        databaseSynchAccessorController.stop();
    }
}
