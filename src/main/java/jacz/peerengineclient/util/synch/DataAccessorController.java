package jacz.peerengineclient.util.synch;

import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.UnavailablePeerException;
import jacz.peerengineservice.util.data_synchronization.DataAccessor;
import jacz.peerengineservice.util.data_synchronization.ServerBusyException;
import jacz.peerengineservice.util.data_synchronization.SynchError;
import jacz.util.concurrency.concurrency_controller.ConcurrencyController;
import jacz.util.concurrency.concurrency_controller.ConcurrencyControllerMaxActivities;
import jacz.util.date_time.TimedEventRecordSet;
import jacz.util.notification.ProgressNotificationWithError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * A generic controller for the issuing of client accessor and the reception of synch requests
 * <p>
 * Controls excessive requests
 */
public abstract class DataAccessorController<LOCAL extends DataAccessor, REMOTE extends DataAccessor> {

    private final static Logger logger = LoggerFactory.getLogger(DataAccessorController.class);

    private static final String SYNCH_ACTIVITY = "SYNCH";

    private final int largeSharedSynchCount;

    private final int veryLargeSharedSynchCount;

    private final long synchTimeout;

    protected final PeerEngineClient peerEngineClient;

    /**
     * Name for logging
     */
    private final String name;

    private final Set<PeerId> activeLocalHashSynchs;

    private final Set<PeerId> activeRemoteShareSynchs;

    private final TimedEventRecordSet<PeerId> sharedSynchRecord;

    private final TimedEventRecordSet<PeerId> remoteSynchRecord;

    private final ConcurrencyController concurrencyController;


    public DataAccessorController(long recentlyThreshold, int largeSharedSynchCount, int veryLargeSharedSynchCount, long synchTimeout, int maxConcurrentSynchs, PeerEngineClient peerEngineClient, String name) {
        this.largeSharedSynchCount = largeSharedSynchCount;
        this.veryLargeSharedSynchCount = veryLargeSharedSynchCount;
        this.synchTimeout = synchTimeout;
        this.peerEngineClient = peerEngineClient;
        this.name = name;
        activeLocalHashSynchs = new HashSet<>();
        activeRemoteShareSynchs = new HashSet<>();
        sharedSynchRecord = new TimedEventRecordSet<>(recentlyThreshold);
        remoteSynchRecord = new TimedEventRecordSet<>(recentlyThreshold);
        concurrencyController = new ConcurrencyControllerMaxActivities(maxConcurrentSynchs);
    }

    public LOCAL requestForLocalHashSynch(PeerId peerID) throws ServerBusyException {
        synchronized (activeLocalHashSynchs) {
            logMessage("request for local synch from " + peerID);
            if (activeLocalHashSynchs.contains(peerID) ||
                    activeLocalHashSynchs.size() > veryLargeSharedSynchCount ||
                    (activeLocalHashSynchs.size() > largeSharedSynchCount && sharedSynchRecord.lastEventIsRecent(peerID))) {
                // if we are already synching with this peer, or there are many ongoing synchs, deny
                logMessage("too busy");
                throw new ServerBusyException();
            } else {
                // synch process can proceed
                activeLocalHashSynchs.add(peerID);
                sharedSynchRecord.newEvent(peerID);
                ProgressNotificationWithError<Integer, SynchError> progress = new DataAccessorControllerProgress(this, SynchMode.LOCAL, peerID, getLocalSynchProgress(peerID));
                logMessage("request accepted");
                return getLocalDataAccessor(peerID, progress);
            }
        }
    }

    public abstract ProgressNotificationWithError<Integer, SynchError> getLocalSynchProgress(PeerId peerID);

    public abstract LOCAL getLocalDataAccessor(PeerId peerID, ProgressNotificationWithError<Integer, SynchError> progress);

    public void synchRemoteShare(PeerId peerID) {
        synchronized (activeRemoteShareSynchs) {
            logMessage("request for remote synch with " + peerID);
            // we only consider this request if we are not currently synching with this peer and
            // we did not recently synched with this peer
            if (!activeRemoteShareSynchs.contains(peerID) &&
                    !remoteSynchRecord.lastEventIsRecent(peerID)) {
                try {
                    // initiate concurrent activity
                    concurrencyController.beginActivity(SYNCH_ACTIVITY);

                    boolean success = peerEngineClient.synchronizeList(
                            peerID,
                            getRemoteDataAccessor(peerID),
                            synchTimeout,
                            new DataAccessorControllerProgress(this, SynchMode.REMOTE, peerID, getRemoteSynchProgress(peerID)));
                    if (success) {
                        logMessage("request successful");
                        // synch process has been successfully registered
                        activeRemoteShareSynchs.add(peerID);
                        remoteSynchRecord.newEvent(peerID);
                    } else {
                        logMessage("request unsuccessful -> terminating synch process");
                        // immediately finish the concurrent activity, as it did not take place
                        concurrencyController.endActivity(SYNCH_ACTIVITY);
                    }
                } catch (Exception e) {
                    // peer is no longer connected, or could not retrieve its remote database -> ignore request
                }
            } else {
                logMessage("request discarded");
            }
        }
    }

    public abstract ProgressNotificationWithError<Integer, SynchError> getRemoteSynchProgress(PeerId peerID) throws UnavailablePeerException;

    public abstract REMOTE getRemoteDataAccessor(PeerId peerID) throws UnavailablePeerException, IOException;

    public void localHashSynchFinished(PeerId remotePeerId) {
        synchronized (activeLocalHashSynchs) {
            logMessage("finished local synch process with " + remotePeerId);
            activeLocalHashSynchs.remove(remotePeerId);
        }
    }

    public void remoteShareSynchFinished(PeerId remotePeerId) {
        synchronized (activeRemoteShareSynchs) {
            logMessage("finished remote synch process with " + remotePeerId);
            activeRemoteShareSynchs.remove(remotePeerId);
        }
        concurrencyController.endActivity(SYNCH_ACTIVITY);
    }

    public void stop() {
        concurrencyController.stopAndWaitForFinalization();
    }

    private void logMessage(String message) {
        logger.info(name + ": " + message);
    }
}
