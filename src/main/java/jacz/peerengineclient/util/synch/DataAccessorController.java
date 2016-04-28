package jacz.peerengineclient.util.synch;

import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.UnavailablePeerException;
import jacz.peerengineservice.util.data_synchronization.DataAccessor;
import jacz.peerengineservice.util.data_synchronization.ServerBusyException;
import jacz.peerengineservice.util.data_synchronization.SynchError;
import jacz.util.concurrency.concurrency_controller.ConcurrencyController;
import jacz.util.concurrency.concurrency_controller.ConcurrencyControllerMaxActivities;
import jacz.util.notification.ProgressNotificationWithError;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * A generic controller for the issuing of client accessor and the reception of synch requests
 * <p>
 * Controls excessive requests
 */
public abstract class DataAccessorController<LOCAL extends DataAccessor, REMOTE extends DataAccessor> {

    private static final String SYNCH_ACTIVITY = "SYNCH";

    private final int largeSharedSynchCount;

    private final int veryLargeSharedSynchCount;

    private final long synchTimeout;

    protected final PeerEngineClient peerEngineClient;

    private final Set<PeerId> activeLocalHashSynchs;

    private final Set<PeerId> activeRemoteShareSynchs;

    private final SynchRecord sharedSynchRecord;

    private final SynchRecord remoteSynchRecord;

    private final ConcurrencyController concurrencyController;


    public DataAccessorController(long recentlyThreshold, int largeSharedSynchCount, int veryLargeSharedSynchCount, long synchTimeout, int maxConcurrentSynchs, PeerEngineClient peerEngineClient) {
        this.largeSharedSynchCount = largeSharedSynchCount;
        this.veryLargeSharedSynchCount = veryLargeSharedSynchCount;
        this.synchTimeout = synchTimeout;
        this.peerEngineClient = peerEngineClient;
        activeLocalHashSynchs = new HashSet<>();
        activeRemoteShareSynchs = new HashSet<>();
        sharedSynchRecord = new SynchRecord(recentlyThreshold);
        remoteSynchRecord = new SynchRecord(recentlyThreshold);
        concurrencyController = new ConcurrencyControllerMaxActivities(maxConcurrentSynchs);
    }

    public LOCAL requestForLocalHashSynch(PeerId peerID) throws ServerBusyException {
        synchronized (activeLocalHashSynchs) {
            if (activeLocalHashSynchs.contains(peerID) ||
                    activeLocalHashSynchs.size() > veryLargeSharedSynchCount ||
                    (activeLocalHashSynchs.size() > largeSharedSynchCount && sharedSynchRecord.lastSynchIsRecent(peerID))) {
                // if we are already synching with this peer, or there are many ongoing synchs, deny
                throw new ServerBusyException();
            } else {
                // synch process can proceed
                activeLocalHashSynchs.add(peerID);
                sharedSynchRecord.newSynchWithPeer(peerID);
                ProgressNotificationWithError<Integer, SynchError> progress = new DataAccessorControllerProgress(this, SynchMode.LOCAL, peerID, getLocalSynchProgress(peerID));
                return getLocalDataAccessor(peerID, progress);
            }
        }
    }

    public abstract ProgressNotificationWithError<Integer, SynchError> getLocalSynchProgress(PeerId peerID);

    public abstract LOCAL getLocalDataAccessor(PeerId peerID, ProgressNotificationWithError<Integer, SynchError> progress);

    public void synchRemoteShare(PeerId peerID) {
        synchronized (activeRemoteShareSynchs) {
            // we only consider this request if we are not currently synching with this peer and
            // we did not recently synched with this peer
            if (!activeRemoteShareSynchs.contains(peerID) &&
                    !remoteSynchRecord.lastSynchIsRecent(peerID)) {
                try {
                    // initiate concurrent activity
                    concurrencyController.beginActivity(SYNCH_ACTIVITY);

                    boolean success = peerEngineClient.synchronizeList(
                            peerID,
                            getRemoteDataAccessor(peerID),
                            synchTimeout,
                            new DataAccessorControllerProgress(this, SynchMode.REMOTE, peerID, getRemoteSynchProgress(peerID)));
                    if (success) {
                        // synch process has been successfully registered
                        activeRemoteShareSynchs.add(peerID);
                        remoteSynchRecord.newSynchWithPeer(peerID);
                    } else {
                        // immediately finish the concurrent activity, as it did not take place
                        concurrencyController.endActivity(SYNCH_ACTIVITY);
                    }
                } catch (Exception e) {
                    // peer is no longer connected, or could not retrieve its remote database -> ignore request
                }
            }
        }
    }

    public abstract ProgressNotificationWithError<Integer, SynchError> getRemoteSynchProgress(PeerId peerID) throws UnavailablePeerException;

    public abstract REMOTE getRemoteDataAccessor(PeerId peerID) throws UnavailablePeerException, IOException;

    public void localHashSynchFinished(PeerId remotePeerId) {
        synchronized (activeLocalHashSynchs) {
            activeLocalHashSynchs.remove(remotePeerId);
        }
    }

    public void remoteShareSynchFinished(PeerId remotePeerId) {
        synchronized (activeRemoteShareSynchs) {
            activeRemoteShareSynchs.remove(remotePeerId);
        }
        concurrencyController.endActivity(SYNCH_ACTIVITY);
    }

    public void stop() {
        concurrencyController.stopAndWaitForFinalization();
    }
}
