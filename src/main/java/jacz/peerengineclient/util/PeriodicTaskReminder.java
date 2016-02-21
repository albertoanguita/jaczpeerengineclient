package jacz.peerengineclient.util;

import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineclient.data.PeerShareManager;
import jacz.peerengineclient.databases.synch.DatabaseSynchManager;
import jacz.peerengineclient.images.ImageDownloader;
import jacz.peerengineservice.PeerID;
import jacz.util.concurrency.daemon.Daemon;
import jacz.util.concurrency.daemon.DaemonAction;
import jacz.util.concurrency.task_executor.SequentialTaskExecutor;
import jacz.util.concurrency.timer.SimpleTimerAction;
import jacz.util.concurrency.timer.Timer;

import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Reminder for remote synch processes
 */
public class PeriodicTaskReminder implements SimpleTimerAction {

    private abstract static class PeerSpecificTask implements DaemonAction {

        private final Queue<PeerID> peersToSynch;

        private final Daemon daemon;

        public PeerSpecificTask() {
            peersToSynch = new ConcurrentLinkedDeque<>();
            daemon = new Daemon(this);
        }

        public void addTaskForPeer(PeerID peerID) {
            peersToSynch.add(peerID);
            daemon.stateChange();
        }

        @Override
        public boolean solveState() {
            try {
                PeerID peerID = peersToSynch.remove();
                performTask(peerID);
                return false;
            } catch (NoSuchElementException e) {
                return true;
            }
        }

        public abstract void performTask(PeerID peerID);

        public void stop() {
            daemon.blockUntilStateIsSolved();
        }
    }

    private static final long REMOTE_SYNCH_DELAY = 5000;

    private final PeerEngineClient peerEngineClient;

    private PeerID lastSynchedPeerID;

    private final ImageDownloader imageDownloader;

    private final Timer timer;

//    private final Daemon daemon;

    private final PeerSpecificTask databaseSynchManagerTask;
    private final PeerSpecificTask peerShareManagerRemoteShareTask;
    private final PeerSpecificTask peerShareManagerTempFilesTask;

    private final SequentialTaskExecutor imageDownloaderTask;

    // todo 3 daemons and 3 queues -> new inner class
    // for image downloader, a sequential task executor

    public PeriodicTaskReminder(
            PeerEngineClient peerEngineClient,
            DatabaseSynchManager databaseSynchManager,
            PeerShareManager peerShareManager,
            ImageDownloader imageDownloader) {
        this.peerEngineClient = peerEngineClient;
        lastSynchedPeerID = null;
        this.imageDownloader = imageDownloader;
        timer = new Timer(REMOTE_SYNCH_DELAY, this, false, "PeriodicTaskReminder");
        databaseSynchManagerTask = new PeerSpecificTask() {
            @Override
            public void performTask(PeerID peerID) {
                databaseSynchManager.synchRemoteDatabase(peerID);
            }
        };
        peerShareManagerRemoteShareTask = new PeerSpecificTask() {
            @Override
            public void performTask(PeerID peerID) {
                peerShareManager.synchRemoteShare(peerID);
            }
        };
        peerShareManagerTempFilesTask = new PeerSpecificTask() {
            @Override
            public void performTask(PeerID peerID) {
                peerShareManager.synchRemoteTempFiles(peerID);
            }
        };
        imageDownloaderTask = new SequentialTaskExecutor();
    }

    public void start() {
        timer.reset();
    }

    @Override
    public Long wakeUp(Timer timer) {
        lastSynchedPeerID = peerEngineClient.getNextConnectedPeer(lastSynchedPeerID);
        if (lastSynchedPeerID != null) {
            databaseSynchManagerTask.addTaskForPeer(lastSynchedPeerID);
            peerShareManagerRemoteShareTask.addTaskForPeer(lastSynchedPeerID);
            peerShareManagerTempFilesTask.addTaskForPeer(lastSynchedPeerID);
        }
        imageDownloaderTask.executeTask(imageDownloader::downloadMissingImages);
        return null;
    }

    public void stop() {
        timer.stop();
        databaseSynchManagerTask.stop();
        peerShareManagerRemoteShareTask.stop();
        peerShareManagerTempFilesTask.stop();
        imageDownloaderTask.stopAndWaitForFinalization();
    }
}
