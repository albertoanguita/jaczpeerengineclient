package jacz.peerengineclient.util;

import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineclient.affinity.AffinityCalculator;
import jacz.peerengineclient.data.PeerShareManager;
import jacz.peerengineclient.databases.synch.DatabaseSynchManager;
import jacz.peerengineclient.images.ImageDownloader;
import jacz.peerengineservice.PeerId;
import org.aanguita.jacuzzi.concurrency.daemon.Daemon;
import org.aanguita.jacuzzi.concurrency.daemon.DaemonAction;
import org.aanguita.jacuzzi.concurrency.timer.Timer;
import org.aanguita.jacuzzi.concurrency.timer.TimerAction;

import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Reminder for remote synch processes
 */
public class PeriodicTaskReminder implements TimerAction {

    private abstract static class PeerSpecificTask implements DaemonAction {

        private final Queue<PeerId> peersToSynch;

        private final Daemon daemon;

        public PeerSpecificTask() {
            peersToSynch = new ConcurrentLinkedDeque<>();
            daemon = new Daemon(this);
        }

        public void addTaskForPeer(PeerId peerID) {
            peersToSynch.add(peerID);
            daemon.stateChange();
        }

        @Override
        public boolean solveState() {
            try {
                PeerId peerID = peersToSynch.remove();
                performTask(peerID);
                return false;
            } catch (NoSuchElementException e) {
                return true;
            }
        }

        public abstract void performTask(PeerId peerID);

        public void stop() {
            daemon.blockUntilStateIsSolved();
            daemon.stop();
        }
    }

    private static final long REMOTE_SYNCH_DELAY = 5000;

    private final PeerEngineClient peerEngineClient;

    private PeerId lastSynchedPeerId;

    private final ImageDownloader imageDownloader;

    private final AffinityCalculator affinityCalculator;

    private final RedundantFileChecker redundantFileChecker;

    private final Timer timer;

    private final AtomicBoolean alive;


    private final PeerSpecificTask databaseSynchManagerTask;
    private final PeerSpecificTask peerShareManagerRemoteShareTask;
    private final PeerSpecificTask peerShareManagerTempFilesTask;

    private final ExecutorService imageDownloaderTask;

    private final ExecutorService affinityCalculatorTask;

    private final ExecutorService redundantFileCheckerTask;

    public PeriodicTaskReminder(
            PeerEngineClient peerEngineClient,
            DatabaseSynchManager databaseSynchManager,
            PeerShareManager peerShareManager,
            ImageDownloader imageDownloader,
            RedundantFileChecker redundantFileChecker) {
        this.peerEngineClient = peerEngineClient;
        lastSynchedPeerId = null;
        this.imageDownloader = imageDownloader;
        this.affinityCalculator = new AffinityCalculator(peerEngineClient);
        this.redundantFileChecker = redundantFileChecker;
        timer = new Timer(REMOTE_SYNCH_DELAY, this, false, "PeriodicTaskReminder");
        alive = new AtomicBoolean(true);
        databaseSynchManagerTask = new PeerSpecificTask() {
            @Override
            public void performTask(PeerId peerID) {
                databaseSynchManager.synchRemoteDatabase(peerID);
            }
        };
        peerShareManagerRemoteShareTask = new PeerSpecificTask() {
            @Override
            public void performTask(PeerId peerID) {
                peerShareManager.synchRemoteShare(peerID);
            }
        };
        peerShareManagerTempFilesTask = new PeerSpecificTask() {
            @Override
            public void performTask(PeerId peerID) {
                peerShareManager.synchRemoteTempFiles(peerID);
            }
        };
        imageDownloaderTask = Executors.newSingleThreadExecutor();
        affinityCalculatorTask = Executors.newSingleThreadExecutor();
        redundantFileCheckerTask = Executors.newSingleThreadExecutor();
    }

    public void start() {
        timer.reset();
    }

    @Override
    public synchronized Long wakeUp(Timer timer) {
        if (alive.get()) {
            lastSynchedPeerId = peerEngineClient.getNextConnectedPeer(lastSynchedPeerId);
            if (lastSynchedPeerId != null) {
                synchWithPeer(lastSynchedPeerId);
            }
            imageDownloaderTask.submit(imageDownloader::downloadMissingImages);
            redundantFileCheckerTask.submit(redundantFileChecker::requestPeriodicCheck);
            return null;
        } else {
            return 0L;
        }
    }

    public synchronized void synchWithPeer(PeerId peerId) {
        databaseSynchManagerTask.addTaskForPeer(peerId);
        peerShareManagerRemoteShareTask.addTaskForPeer(peerId);
        peerShareManagerTempFilesTask.addTaskForPeer(peerId);
        affinityCalculatorTask.submit(() -> affinityCalculator.updateAffinity(peerId));
    }

    public synchronized void stop() {
        if (alive.getAndSet(false)) {
            timer.kill();
            databaseSynchManagerTask.stop();
            peerShareManagerRemoteShareTask.stop();
            peerShareManagerTempFilesTask.stop();
            imageDownloaderTask.shutdown();
            affinityCalculatorTask.shutdown();
            redundantFileCheckerTask.shutdown();
        }
    }
}
