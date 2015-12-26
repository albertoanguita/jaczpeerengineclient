package jacz.peerengineclient.data;

import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineclient.data.synch.FileHashDatabaseAccessor;
import jacz.peerengineclient.data.synch.RemotePeerShareAccessor;
import jacz.peerengineclient.data.synch.SynchProgress;
import jacz.peerengineclient.util.synch.RemoteSynchReminder;
import jacz.peerengineclient.util.synch.SynchMode;
import jacz.peerengineclient.util.synch.SynchRecord;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.UnavailablePeerException;
import jacz.peerengineservice.util.data_synchronization.AccessorNotFoundException;
import jacz.peerengineservice.util.data_synchronization.ServerBusyException;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Alberto on 13/12/2015.
 */
public class PeerShareManager {

    private static final long REMOTE_SYNCH_DELAY = 1000;

    private static final int MAX_CONCURRENT__REMOTE_SYNCHS = 20;

    private static final long RECENTLY_THRESHOLD = 15000;

    private static final int LARGE_SHARED_SYNCH_COUNT = 10;

    private static final int VERY_LARGE_SHARED_SYNCH_COUNT = 20;

    private static final String ACCESSOR_NAME = "FILE_HASH_DATA_ACCESSOR";

    private static final long SYNCH_TIMEOUT = 15000L;



    private final PeerEngineClient peerEngineClient;

    private final FileHashDatabaseWithTimestamp fileHash;

    private final Map<PeerID, RemotePeerShare> remotePeerShares;

    private final Set<PeerID> activeLocalHashSynchs;

    private final Set<PeerID> activeRemoteShareSynchs;

    private final RemoteSynchReminder remoteSynchReminder;

    private final SynchRecord sharedSynchRecord;

    private final SynchRecord remoteSynchRecord;

    public PeerShareManager(
            PeerEngineClient peerEngineClient,
            FileHashDatabaseWithTimestamp fileHash,
            Map<PeerID, RemotePeerShare> remotePeerShares) {
        this.peerEngineClient = peerEngineClient;
        this.fileHash = fileHash;
        this.remotePeerShares = remotePeerShares;
        activeLocalHashSynchs = new HashSet<>();
        activeRemoteShareSynchs = new HashSet<>();
        remoteSynchReminder = new RemoteSynchReminder(
                peerEngineClient,
                this::synchRemoteShare,
                REMOTE_SYNCH_DELAY,
                MAX_CONCURRENT__REMOTE_SYNCHS);
        sharedSynchRecord = new SynchRecord(RECENTLY_THRESHOLD);
        remoteSynchRecord = new SynchRecord(RECENTLY_THRESHOLD);
    }

    public void start() {
        remoteSynchReminder.start();
    }

    public FileHashDatabaseWithTimestamp getFileHash() {
        return fileHash;
    }

    public Map<PeerID, RemotePeerShare> getRemotePeerShares() {
        return remotePeerShares;
    }

    public FileHashDatabaseAccessor requestForLocalHashSynch(PeerID peerID) throws ServerBusyException {
        synchronized (activeLocalHashSynchs) {
            if (activeLocalHashSynchs.contains(peerID) ||
                    activeLocalHashSynchs.size() > VERY_LARGE_SHARED_SYNCH_COUNT ||
                    (activeLocalHashSynchs.size() > LARGE_SHARED_SYNCH_COUNT && sharedSynchRecord.lastSynchIsRecent(peerID))) {
                // if we are already synching with this peer, or there are many ongoing synchs, deny
                throw new ServerBusyException();
            } else {
                // synch process can proceed
                activeLocalHashSynchs.add(peerID);
                sharedSynchRecord.newSynchWithPeer(peerID);
                return new FileHashDatabaseAccessor(fileHash, new SynchProgress(this, SynchMode.SHARED, peerID));
            }
        }
    }

    public void synchRemoteShare(PeerID peerID) {
        synchronized (activeRemoteShareSynchs) {
            // we only consider this request if we are not currently synching with this peer and
            // we did not recently synched with this peer
            if (!activeRemoteShareSynchs.contains(peerID) &&
                    !remoteSynchRecord.lastSynchIsRecent(peerID)) {
                try {
                    RemotePeerShareAccessor remotePeerShareAccessor = new RemotePeerShareAccessor(remotePeerShares.get(peerID));
                    boolean success = peerEngineClient.synchronizeList(
                            peerID,
                            remotePeerShareAccessor,
                            SYNCH_TIMEOUT,
                            new SynchProgress(this, SynchMode.REMOTE, peerID));

                    if (success) {
                        // synch process has been successfully registered
                        activeRemoteShareSynchs.add(peerID);
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

    public void localHashSynchFinished(PeerID remotePeerID) {
        synchronized (activeLocalHashSynchs) {
            activeLocalHashSynchs.remove(remotePeerID);
        }
    }

    public void remoteShareSynchFinished(PeerID remotePeerID) {
        synchronized (activeRemoteShareSynchs) {
            activeRemoteShareSynchs.remove(remotePeerID);
        }
    }

    public void stop() {
        remoteSynchReminder.stop();
    }
}
