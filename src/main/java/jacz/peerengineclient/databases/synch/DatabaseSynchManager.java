package jacz.peerengineclient.databases.synch;

import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineclient.databases.Databases;
import jacz.peerengineclient.util.synch.RemoteSynchReminder;
import jacz.peerengineclient.util.synch.SynchMode;
import jacz.peerengineclient.util.synch.SynchRecord;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.UnavailablePeerException;
import jacz.peerengineservice.util.data_synchronization.*;

import java.util.HashSet;
import java.util.Set;

/**
 * This class manages all the ongoing synch processes, maintaining a table with the active synch processes
 * <p>
 * It also handles the synch requests, being able to deny them if they conflict with the ongoing synch processes
 * <p>
 * Finally, this class is in charge of reporting the progress of the active synch processes
 */
public class DatabaseSynchManager {

    private static final long REMOTE_SYNCH_DELAY = 2000;

    private static final int MAX_CONCURRENT__REMOTE_SYNCHS = 10;


    private static final long RECENTLY_THRESHOLD = 30000;


    private static final int LARGE_SHARED_SYNCH_COUNT = 5;

    private static final int VERY_LARGE_SHARED_SYNCH_COUNT = 10;

    private static final long DATABASE_SYNCH_TIMEOUT = 15000L;

    private final DatabaseSynchEvents databaseSynchEvents;

    private final PeerEngineClient peerEngineClient;

    private final Databases databases;

    private final Set<PeerID> activeSharedSynchs;

    private final Set<PeerID> activeRemoteSynchs;

    private final RemoteSynchReminder remoteSynchReminder;

    private final SynchRecord sharedSynchRecord;

    private final SynchRecord remoteSynchRecord;

    public DatabaseSynchManager(DatabaseSynchEvents databaseSynchEvents, PeerEngineClient peerEngineClient, Databases databases) {
        this.databaseSynchEvents = databaseSynchEvents;
        this.peerEngineClient = peerEngineClient;
        this.databases = databases;
        activeSharedSynchs = new HashSet<>();
        activeRemoteSynchs = new HashSet<>();
        remoteSynchReminder = new RemoteSynchReminder(
                peerEngineClient,
                this::synchRemoteDatabase,
                REMOTE_SYNCH_DELAY,
                MAX_CONCURRENT__REMOTE_SYNCHS);
        sharedSynchRecord = new SynchRecord(RECENTLY_THRESHOLD);
        remoteSynchRecord = new SynchRecord(RECENTLY_THRESHOLD);
    }

    public void start() {
        remoteSynchReminder.start();
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
        synchronized (activeSharedSynchs) {
            if (activeSharedSynchs.contains(peerID) ||
                    activeSharedSynchs.size() > VERY_LARGE_SHARED_SYNCH_COUNT ||
                    (activeSharedSynchs.size() > LARGE_SHARED_SYNCH_COUNT && sharedSynchRecord.lastSynchIsRecent(peerID))) {
                // if we are already synching with this peer, or there are many ongoing synchs, deny
                throw new ServerBusyException();
            } else {
                // synch process can proceed
                activeSharedSynchs.add(peerID);
                sharedSynchRecord.newSynchWithPeer(peerID);
                return new DatabaseAccessor(this, databases.getSharedDB(), new DatabaseSynchProgress(this, SynchMode.SHARED, peerID));
            }
        }
    }

    //    /**
//     * A remote peer is requesting to get access to the shared database for synchronizing it with us
//     * <p>
//     * This process can happen along with any other process*. We just must take care that the retrieval of index and hash lists is properly
//     * synchronized with other operations. A local or remote item integration might of course break the synchronization, but that is a risk that
//     * we must assume, and the other peer will be notified of this.
//     * <p>
//     * The database manager will reject these requests if a remote integration is taking place, because it would most certainly break the synch
//     * and we would be waisting bandwidth
//     */
//    public ServerSynchRequestAnswer requestForSharedDatabaseSynch(PeerID peerID) {
//        synchronized (activeSharedSynchs) {
//            if (activeSharedSynchs.contains(peerID) ||
//                    activeSharedSynchs.size() > VERY_LARGE_SHARED_SYNCH_COUNT ||
//                    (activeSharedSynchs.size() > LARGE_SHARED_SYNCH_COUNT && sharedSynchRecord.lastSynchIsRecent(peerID))) {
//                // if we are already synching with this peer, or there are many ongoing synchs, deny
//                return new ServerSynchRequestAnswer(ServerSynchRequestAnswer.Type.SERVER_BUSY, null);
//            } else {
//                // synch process can proceed
//                activeSharedSynchs.add(peerID);
//                sharedSynchRecord.newSynchWithPeer(peerID);
//                return new ServerSynchRequestAnswer(
//                        ServerSynchRequestAnswer.Type.OK,
//                        new DatabaseSynchProgress(this, DatabaseSynchProgress.Mode.SHARED, peerID));
//            }
//        }
//    }
//
    public void synchRemoteDatabase(PeerID peerID) {
        synchronized (activeRemoteSynchs) {
            // we only consider this request if we are not currently synching with this peer and
            // we did not recently synched with this peer
            if (!activeRemoteSynchs.contains(peerID) &&
                    !remoteSynchRecord.lastSynchIsRecent(peerID)) {
                try {
                    DatabaseAccessor databaseAccessor = new DatabaseAccessor(this, databases.getRemoteDBs().get(peerID), new DatabaseSynchProgress(this, SynchMode.REMOTE, peerID));
                    boolean success = peerEngineClient.synchronizeList(
                            peerID,
                            databaseAccessor,
                            DATABASE_SYNCH_TIMEOUT,
                            new DatabaseSynchProgress(this, SynchMode.REMOTE, peerID));

                    if (success) {
                        // synch process has been successfully registered
                        activeRemoteSynchs.add(peerID);
                        remoteSynchRecord.newSynchWithPeer(peerID);
                    }
                } catch (AccessorNotFoundException e) {
                    // todo fatal error
                    e.printStackTrace();
                } catch (UnavailablePeerException e) {
                    // peer is no longer connected, ignore request
                }
            }
        }
    }

    void sharedDatabaseSynchBegins(PeerID remotePeerID) {
        databaseSynchEvents.sharedSynchStarted(remotePeerID);
    }

    void sharedDatabaseSynchProgress(PeerID remotePeerID, Integer progress) {
        databaseSynchEvents.sharedSynchProgress(remotePeerID, progress);
    }

    void sharedDatabaseSynchComplete(PeerID remotePeerID) {
        synchronized (activeSharedSynchs) {
            activeSharedSynchs.remove(remotePeerID);
        }
        databaseSynchEvents.sharedSynchCompleted(remotePeerID);
    }

    void sharedDatabaseSynchFailed(PeerID remotePeerID, SynchError error) {
        // todo check errors (fatal with DATA_ACCESS_ERROR)
        synchronized (activeSharedSynchs) {
            activeSharedSynchs.remove(remotePeerID);
        }
        databaseSynchEvents.sharedSynchError(remotePeerID, error);
    }

    void sharedDatabaseSynchTimedOut(PeerID remotePeerID) {
        synchronized (activeSharedSynchs) {
            activeSharedSynchs.remove(remotePeerID);
        }
        databaseSynchEvents.sharedSynchTimeout(remotePeerID);
    }

    void remoteDatabaseSynchBegins(PeerID remotePeerID) {
        databaseSynchEvents.remoteSynchStarted(remotePeerID);
    }

    void remoteDatabaseSynchProgress(PeerID remotePeerID, Integer progress) {
        databaseSynchEvents.remoteSynchProgress(remotePeerID, progress);
    }

    void remoteDatabaseSynchComplete(PeerID remotePeerID) {
        synchronized (activeRemoteSynchs) {
            activeRemoteSynchs.remove(remotePeerID);
        }
        databaseSynchEvents.remoteSynchCompleted(remotePeerID);
    }

    void remoteDatabaseSynchFailed(PeerID remotePeerID, SynchError error) {
        // todo check errors (fatal with DATA_ACCESS_ERROR)
        synchronized (activeRemoteSynchs) {
            activeRemoteSynchs.remove(remotePeerID);
        }
        databaseSynchEvents.remoteSynchError(remotePeerID, error);
    }

    void remoteDatabaseSynchTimedOut(PeerID remotePeerID) {
        synchronized (activeRemoteSynchs) {
            activeRemoteSynchs.remove(remotePeerID);
        }
        databaseSynchEvents.remoteSynchTimeout(remotePeerID);
    }

    public void stop() {
        remoteSynchReminder.stop();
    }
}
