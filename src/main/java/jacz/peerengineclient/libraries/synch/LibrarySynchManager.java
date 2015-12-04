package jacz.peerengineclient.libraries.synch;

import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.data_synchronization.DataSynchronizer;
import jacz.peerengineservice.util.data_synchronization.ServerSynchRequestAnswer;
import jacz.peerengineservice.util.data_synchronization.SynchError;

import java.util.HashSet;
import java.util.Set;

/**
 * This class manages all the ongoing synch processes, maintaining a table with the active synch processes
 * <p>
 * It also handles the synch requests, being able to deny them if they conflict with the ongoing synch processes
 * <p>
 * Finally, this class is in charge of reporting the progress of the active synch processes
 */
public class LibrarySynchManager {

    private static final int LARGE_SHARED_SYNCH_COUNT = 5;

    private static final int VERY_LARGE_SHARED_SYNCH_COUNT = 10;

    private static final String LIBRARY_ACCESSOR_NAME = "PEER_LIBRARY_DATA_ACCESSOR";

    private static final long LIBRARY_SYNCH_TIMEOUT = 15000L;

    private final LibrarySynchEvents librarySynchEvents;

    private final PeerEngineClient peerEngineClient;

    private final Set<PeerID> activeSharedSynchs;

    private final Set<PeerID> activeRemoteSynchs;

    private final RemoteSynchReminder remoteSynchReminder;

    private final SynchRecord sharedSynchRecord;

    private final SynchRecord remoteSynchRecord;

    public LibrarySynchManager(LibrarySynchEvents librarySynchEvents, PeerEngineClient peerEngineClient) {
        this.librarySynchEvents = librarySynchEvents;
        this.peerEngineClient = peerEngineClient;
        activeSharedSynchs = new HashSet<>();
        activeRemoteSynchs = new HashSet<>();
        remoteSynchReminder = new RemoteSynchReminder(peerEngineClient, this);
        sharedSynchRecord = new SynchRecord();
        remoteSynchRecord = new SynchRecord();
    }

    /**
     * A remote peer is requesting to get access to the shared library for synchronizing it with us
     * <p>
     * This process can happen along with any other process*. We just must take care that the retrieval of index and hash lists is properly
     * synchronized with other operations. A local or remote item integration might of course break the synchronization, but that is a risk that
     * we must assume, and the other peer will be notified of this.
     * <p>
     * The library manager will reject these requests if a remote integration is taking place, because it would most certainly break the synch
     * and we would be waisting bandwidth
     */
    public ServerSynchRequestAnswer requestForSharedLibrarySynch(PeerID peerID) {
        synchronized (activeSharedSynchs) {
            if (activeSharedSynchs.contains(peerID) ||
                    activeSharedSynchs.size() > VERY_LARGE_SHARED_SYNCH_COUNT ||
                    (activeSharedSynchs.size() > LARGE_SHARED_SYNCH_COUNT && sharedSynchRecord.lastSynchIsRecent(peerID))) {
                // if we are already synching with this peer, or there are many ongoing synchs, deny
                return new ServerSynchRequestAnswer(ServerSynchRequestAnswer.Type.SERVER_BUSY, null);
            } else {
                // synch process can proceed
                activeSharedSynchs.add(peerID);
                sharedSynchRecord.newSynchWithPeer(peerID);
                return new ServerSynchRequestAnswer(
                        ServerSynchRequestAnswer.Type.OK,
                        new LibrarySynchProgress(this, LibrarySynchProgress.Mode.SHARED, peerID));
            }
        }
    }

    public void synchRemoteLibrary(PeerID peerID) {
        synchronized (activeRemoteSynchs) {
            // we only consider this request if we are not currently synching with this peer and
            // we did not recently synched with this peer
            if (!activeRemoteSynchs.contains(peerID) &&
                    !remoteSynchRecord.lastSynchIsRecent(peerID)) {
                DataSynchronizer.SynchRequestResult synchRequestResult = peerEngineClient.synchronizeList(
                        peerID,
                        LIBRARY_ACCESSOR_NAME,
                        LIBRARY_SYNCH_TIMEOUT,
                        new LibrarySynchProgress(this, LibrarySynchProgress.Mode.REMOTE, peerID));
                switch (synchRequestResult) {
                    case OK:
                        // synch process has been successfully registered
                        activeRemoteSynchs.add(peerID);
                        remoteSynchRecord.newSynchWithPeer(peerID);
                        break;

                    case PEER_CLIENT_BUSY:
                        // ignore this request, will be issued later
                        break;

                    case DISCONNECTED:
                        // this peer is no longer connected, ignore request
                        break;

                    case UNKNOWN_ACCESSOR:
                        // todo fatal error
                        break;
                }
            }
        }
    }

    void sharedLibrarySynchBegins(PeerID remotePeerID) {
        librarySynchEvents.sharedSynchStarted(remotePeerID);
    }

    void sharedLibrarySynchProgress(PeerID remotePeerID, Integer progress) {
        librarySynchEvents.sharedSynchProgress(remotePeerID, progress);
    }

    void sharedLibrarySynchComplete(PeerID remotePeerID) {
        synchronized (activeSharedSynchs) {
            activeSharedSynchs.remove(remotePeerID);
        }
        librarySynchEvents.sharedSynchCompleted(remotePeerID);
    }

    void sharedLibrarySynchFailed(PeerID remotePeerID, SynchError error) {
        // todo check errors (fatal with DATA_ACCESS_ERROR)
        synchronized (activeSharedSynchs) {
            activeSharedSynchs.remove(remotePeerID);
        }
        librarySynchEvents.sharedSynchError(remotePeerID, error);
    }

    void sharedLibrarySynchTimedOut(PeerID remotePeerID) {
        synchronized (activeSharedSynchs) {
            activeSharedSynchs.remove(remotePeerID);
        }
        librarySynchEvents.sharedSynchTimeout(remotePeerID);
    }

    void remoteLibrarySynchBegins(PeerID remotePeerID) {
        librarySynchEvents.remoteSynchStarted(remotePeerID);
    }

    void remoteLibrarySynchProgress(PeerID remotePeerID, Integer progress) {
        librarySynchEvents.remoteSynchProgress(remotePeerID, progress);
    }

    void remoteLibrarySynchComplete(PeerID remotePeerID) {
        synchronized (activeRemoteSynchs) {
            activeRemoteSynchs.remove(remotePeerID);
        }
        librarySynchEvents.remoteSynchCompleted(remotePeerID);
    }

    void remoteLibrarySynchFailed(PeerID remotePeerID, SynchError error) {
        // todo check errors (fatal with DATA_ACCESS_ERROR)
        synchronized (activeRemoteSynchs) {
            activeRemoteSynchs.remove(remotePeerID);
        }
        librarySynchEvents.remoteSynchError(remotePeerID, error);
    }

    void remoteLibrarySynchTimedOut(PeerID remotePeerID) {
        synchronized (activeRemoteSynchs) {
            activeRemoteSynchs.remove(remotePeerID);
        }
        librarySynchEvents.remoteSynchTimeout(remotePeerID);
    }

    public void stop() {
        remoteSynchReminder.stop();
    }
}
