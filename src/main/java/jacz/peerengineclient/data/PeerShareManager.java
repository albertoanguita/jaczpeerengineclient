package jacz.peerengineclient.data;

import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineclient.data.synch.FileHashDatabaseAccessor;
import jacz.peerengineclient.data.synch.RemotePeerShareAccessor;
import jacz.peerengineclient.data.synch.SynchProgress;
import jacz.peerengineclient.util.synch.SynchMode;
import jacz.peerengineclient.util.synch.SynchRecord;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.UnavailablePeerException;
import jacz.peerengineservice.client.PeerClient;
import jacz.peerengineservice.util.data_synchronization.ServerBusyException;
import jacz.util.io.serialization.VersionedSerializationException;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Alberto on 13/12/2015.
 */
public class PeerShareManager {

    private static final long RECENTLY_THRESHOLD = 15000;

    private static final int LARGE_SHARED_SYNCH_COUNT = 10;

    private static final int VERY_LARGE_SHARED_SYNCH_COUNT = 20;

    private static final long SYNCH_TIMEOUT = 15000L;


    private final PeerEngineClient peerEngineClient;

    private final FileHashDatabaseWithTimestamp fileHash;

    private ForeignShares foreignShares;

    private final Map<PeerID, RemotePeerShare> remotePeerShares;

    private final Set<PeerID> activeLocalHashSynchs;

    private final Set<PeerID> activeRemoteShareSynchs;

    private final SynchRecord sharedSynchRecord;

    private final SynchRecord remoteSynchRecord;

    public PeerShareManager(
            PeerEngineClient peerEngineClient,
            FileHashDatabaseWithTimestamp fileHash) {
        this.peerEngineClient = peerEngineClient;
        this.fileHash = fileHash;
        this.foreignShares = null;
        this.remotePeerShares = new HashMap<>();
        activeLocalHashSynchs = new HashSet<>();
        activeRemoteShareSynchs = new HashSet<>();
        sharedSynchRecord = new SynchRecord(RECENTLY_THRESHOLD);
        remoteSynchRecord = new SynchRecord(RECENTLY_THRESHOLD);
    }

    public void setPeerClient(PeerClient peerClient) {
        this.foreignShares = new ForeignShares(peerClient);
    }

    public FileHashDatabaseWithTimestamp getFileHash() {
        return fileHash;
    }

    public synchronized Set<Map.Entry<PeerID, RemotePeerShare>> getRemotePeerShares() {
        return remotePeerShares.entrySet();
    }

    public synchronized void peerConnected(String basePath, PeerID peerID) {
        RemotePeerShare remotePeerShare;
        try {
            remotePeerShare = PeerShareIO.loadRemoteShare(basePath, peerID, foreignShares);
        } catch (IOException | VersionedSerializationException e) {
            // could not load the share (maybe it did not exist) -> create a new one
            remotePeerShare = new RemotePeerShare(peerID, foreignShares);
        }
        remotePeerShares.put(peerID, remotePeerShare);
    }

    public synchronized void peerDisconnected(String basePath, PeerID peerID) {
        RemotePeerShare remotePeerShare = remotePeerShares.remove(peerID);
        if (remotePeerShare != null) {
            remotePeerShare.notifyPeerDisconnected();
            try {
                PeerShareIO.saveRemotePeerShare(basePath, peerID, remotePeerShare);
            } catch (IOException e) {
                // error writing the remote peer share to disk -> remove the files, if any
                // a new peer share will be created at some time in the future
                PeerShareIO.removeRemotePeerShare(basePath, peerID);
            }
        }
    }

    public synchronized void removeRemotePeer(String basePath, PeerID remotePeerID) {
        if (remotePeerShares.containsKey(remotePeerID)) {
            remotePeerShares.get(remotePeerID).clear();
        }
        PeerShareIO.removeRemotePeerShare(basePath, remotePeerID);
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
                    RemotePeerShare remotePeerShare = remotePeerShares.get(peerID);
                    if (remotePeerShare != null) {
                        RemotePeerShareAccessor remotePeerShareAccessor = new RemotePeerShareAccessor(remotePeerShare);
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
                    } else {
                        // remote share not stored for this peer!!!
                        System.out.println("HELP!!!");
                    }
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
}
