package jacz.peerengineclient.libraries.synch;

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

    private static final long SYNCH_DELAY = 2000;

    private static final int MAX_CONCURRENT_SYNCH_PROCESSES = 10;


    private final PeerEngineClient peerEngineClient;

    private final LibrarySynchManager librarySynchManager;

    private final Queue<PeerID> peersToSynch;

    private PeerID lastSynchedPeerID;

    private final Timer timer;

    private final Daemon daemon;

    private final ConcurrencyController remoteSynchConcurrencyController;

    public RemoteSynchReminder(PeerEngineClient peerEngineClient, LibrarySynchManager librarySynchManager) {
        this.peerEngineClient = peerEngineClient;
        this.librarySynchManager = librarySynchManager;
        peersToSynch = new ConcurrentLinkedDeque<>();
        lastSynchedPeerID = PeerID.generateRandomPeerId(new byte[0]);
        timer = new Timer(SYNCH_DELAY, this);
        daemon = new Daemon(this);
        remoteSynchConcurrencyController = new ConcurrencyController(new ConcurrencyControllerAction() {
            @Override
            public int maxNumberOfExecutionsAllowed() {
                return MAX_CONCURRENT_SYNCH_PROCESSES;
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

    @Override
    public Long wakeUp(Timer timer) {
        lastSynchedPeerID = peerEngineClient.getNextConnectedPeer(lastSynchedPeerID);
        peersToSynch.add(lastSynchedPeerID);
        daemon.stateChange();
        return null;
    }

    @Override
    public boolean solveState() {
        try {
            PeerID peerID = peersToSynch.remove();
            ParallelTaskExecutor.executeTask(
                    () -> librarySynchManager.synchRemoteLibrary(peerID),
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
