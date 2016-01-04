package jacz.peerengineclient.util.synch;

import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineclient.data.PeerShareManager;
import jacz.peerengineclient.databases.synch.DatabaseSynchManager;
import jacz.peerengineservice.PeerID;
import jacz.util.concurrency.concurrency_controller.ConcurrencyController;
import jacz.util.concurrency.concurrency_controller.ConcurrencyControllerAction;
import jacz.util.concurrency.daemon.Daemon;
import jacz.util.concurrency.daemon.DaemonAction;
import jacz.util.concurrency.task_executor.ParallelTaskExecutor;
import jacz.util.concurrency.timer.SimpleTimerAction;
import jacz.util.concurrency.timer.Timer;
import jacz.util.maps.ObjectCount;

import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Reminder for remote synch processes
 */
public class RemoteSynchReminder implements SimpleTimerAction, DaemonAction {

    private final int MAX_DATABASE_SYNCH_TASKS = 10;

    private final int MAX_SHARE_SYNCH_TASKS = 20;

    private static final long REMOTE_SYNCH_DELAY = 1000;

    private final PeerEngineClient peerEngineClient;

    private final DatabaseSynchManager databaseSynchManager;

    private final PeerShareManager peerShareManager;

    private final Queue<PeerID> peersToSynch;

    private PeerID lastSynchedPeerID;

    private final Timer timer;

    private final Daemon daemon;

    private final ConcurrencyController databaseSynchConcurrencyController;

    private final ConcurrencyController shareSynchConcurrencyController;

    public RemoteSynchReminder(
            PeerEngineClient peerEngineClient,
            DatabaseSynchManager databaseSynchManager,
            PeerShareManager peerShareManager) {
        this.peerEngineClient = peerEngineClient;
        this.databaseSynchManager = databaseSynchManager;
        this.peerShareManager = peerShareManager;
        peersToSynch = new ConcurrentLinkedDeque<>();
        lastSynchedPeerID = null;
        timer = new Timer(REMOTE_SYNCH_DELAY, this, false, "RemoteSynchReminder");
        daemon = new Daemon(this);
        databaseSynchConcurrencyController = new ConcurrencyController(new ConcurrencyControllerAction() {
            @Override
            public int maxNumberOfExecutionsAllowed() {
                return MAX_DATABASE_SYNCH_TASKS;
            }

            @Override
            public int getActivityPriority(String activity) {
                return 0;
            }

            @Override
            public boolean activityCanExecute(String activity, ObjectCount<String> numberOfExecutionsOfActivities) {
                return true;
            }

            @Override
            public void activityIsGoingToBegin(String activity, ObjectCount<String> numberOfExecutionsOfActivities) {
                // ignore
            }

            @Override
            public void activityHasEnded(String activity, ObjectCount<String> numberOfExecutionsOfActivities) {
                // ignore
            }
        });
        shareSynchConcurrencyController = new ConcurrencyController(new ConcurrencyControllerAction() {
            @Override
            public int maxNumberOfExecutionsAllowed() {
                return MAX_SHARE_SYNCH_TASKS;
            }

            @Override
            public int getActivityPriority(String activity) {
                return 0;
            }

            @Override
            public boolean activityCanExecute(String activity, ObjectCount<String> numberOfExecutionsOfActivities) {
                return true;
            }

            @Override
            public void activityIsGoingToBegin(String activity, ObjectCount<String> numberOfExecutionsOfActivities) {
                // ignore
            }

            @Override
            public void activityHasEnded(String activity, ObjectCount<String> numberOfExecutionsOfActivities) {
                // ignore
            }
        });
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
                    () -> databaseSynchManager.synchRemoteDatabase(peerID),
                    databaseSynchConcurrencyController,
                    "SYNCH");
            ParallelTaskExecutor.executeTask(
                    () -> peerShareManager.synchRemoteShare(peerID),
                    shareSynchConcurrencyController,
                    "SYNCH");
            return false;
        } catch (NoSuchElementException e) {
            return true;
        }
    }

    public void stop() {
        timer.stop();
        daemon.blockUntilStateIsSolved();
        databaseSynchConcurrencyController.stopAndWaitForFinalization();
        shareSynchConcurrencyController.stopAndWaitForFinalization();
    }
}
