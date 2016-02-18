package jacz.peerengineclient.util.synch;

import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineclient.data.PeerShareManager;
import jacz.peerengineclient.databases.synch.DatabaseSynchManager;
import jacz.peerengineservice.PeerID;
import jacz.util.concurrency.concurrency_controller.ConcurrencyController;
import jacz.util.concurrency.concurrency_controller.ConcurrencyControllerMaxActivities;
import jacz.util.concurrency.daemon.Daemon;
import jacz.util.concurrency.daemon.DaemonAction;
import jacz.util.concurrency.task_executor.ParallelTaskExecutor;
import jacz.util.concurrency.timer.SimpleTimerAction;
import jacz.util.concurrency.timer.Timer;

import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Reminder for remote synch processes
 */
public class PeriodicTaskReminder implements SimpleTimerAction, DaemonAction {

    private static final long REMOTE_SYNCH_DELAY = 5000;

    private final PeerEngineClient peerEngineClient;

    private final DatabaseSynchManager databaseSynchManager;

    private final PeerShareManager peerShareManager;

    private final Queue<PeerID> peersToSynch;

    private PeerID lastSynchedPeerID;

    private final Timer timer;

    private final Daemon daemon;

    public PeriodicTaskReminder(
            PeerEngineClient peerEngineClient,
            DatabaseSynchManager databaseSynchManager,
            PeerShareManager peerShareManager) {
        this.peerEngineClient = peerEngineClient;
        this.databaseSynchManager = databaseSynchManager;
        this.peerShareManager = peerShareManager;
        peersToSynch = new ConcurrentLinkedDeque<>();
        lastSynchedPeerID = null;
        timer = new Timer(REMOTE_SYNCH_DELAY, this, false, "PeriodicTaskReminder");
        daemon = new Daemon(this);
    }

    public void start() {
        timer.reset();
    }

    @Override
    public Long wakeUp(Timer timer) {
        lastSynchedPeerID = peerEngineClient.getNextConnectedPeer(lastSynchedPeerID);
        if (lastSynchedPeerID != null) {
            peersToSynch.add(lastSynchedPeerID);
            daemon.stateChange();
        }
        return null;
    }

    @Override
    public boolean solveState() {
        try {
            PeerID peerID = peersToSynch.remove();
            ParallelTaskExecutor.executeTask(
                    () -> databaseSynchManager.synchRemoteDatabase(peerID));
            ParallelTaskExecutor.executeTask(
                    () -> peerShareManager.synchRemoteShare(peerID));
            ParallelTaskExecutor.executeTask(
                    () -> peerShareManager.synchRemoteTempFiles(peerID));
            return false;
        } catch (NoSuchElementException e) {
            return true;
        }
    }

    public void stop() {
        timer.stop();
        daemon.blockUntilStateIsSolved();
    }
}
