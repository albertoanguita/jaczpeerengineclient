package jacz.peerengineclient.util.synch;

import jacz.peerengineclient.PeerEngineClient;
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
 * Created by Alberto on 03/12/2015.
 */
public class RemoteSynchReminder implements SimpleTimerAction, DaemonAction {

    public interface RemoteSynchTask {

        void executeRemoteSynch(PeerID peerID);
    }

    private final PeerEngineClient peerEngineClient;

    private final RemoteSynchTask remoteSynchTask;

    private final Queue<PeerID> peersToSynch;

    private PeerID lastSynchedPeerID;

    private final Timer timer;

    private final Daemon daemon;

    private final ConcurrencyController remoteSynchConcurrencyController;

    public RemoteSynchReminder(
            PeerEngineClient peerEngineClient,
            RemoteSynchTask remoteSynchTask,
            long synchDelay,
            int maxConcurrentTasks) {
        this.peerEngineClient = peerEngineClient;
        this.remoteSynchTask = remoteSynchTask;
        peersToSynch = new ConcurrentLinkedDeque<>();
        lastSynchedPeerID = null;
        timer = new Timer(synchDelay, this, false, "RemoteSynchReminder");
        daemon = new Daemon(this);
        remoteSynchConcurrencyController = new ConcurrencyController(new ConcurrencyControllerAction() {
            @Override
            public int maxNumberOfExecutionsAllowed() {
                return maxConcurrentTasks;
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
//                    () -> librarySynchManager.synchRemoteDatabase(peerID),
                    () -> remoteSynchTask.executeRemoteSynch(peerID),
                    remoteSynchConcurrencyController,
                    "SYNCH");
            return false;
        } catch (NoSuchElementException e) {
            return true;
        }
    }

    public void stop() {
        timer.stop();
        remoteSynchConcurrencyController.stopAndWaitForFinalization();
        daemon.blockUntilStateIsSolved();
    }
}
