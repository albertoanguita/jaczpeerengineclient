package jacz.peerengineclient.util;

import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineclient.data.PeerShareManager;
import jacz.peerengineclient.databases.synch.DatabaseSynchManager;
import jacz.peerengineclient.images.ImageDownloader;
import jacz.peerengineservice.PeerId;
import jacz.util.concurrency.daemon.Daemon;
import jacz.util.concurrency.daemon.DaemonAction;
import jacz.util.concurrency.timer.Timer;
import jacz.util.concurrency.timer.TimerAction;

import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        }
    }

    private static final long REMOTE_SYNCH_DELAY = 5000;

    private final PeerEngineClient peerEngineClient;

    private PeerId lastSynchedPeerId;

    private final ImageDownloader imageDownloader;

    private final Timer timer;

//    private final Daemon daemon;

    private final PeerSpecificTask databaseSynchManagerTask;
    private final PeerSpecificTask peerShareManagerRemoteShareTask;
    private final PeerSpecificTask peerShareManagerTempFilesTask;

    private final ExecutorService imageDownloaderTask;

    // todo 3 daemons and 3 queues -> new inner class
    // for image downloader, a sequential task executor

    public PeriodicTaskReminder(
            PeerEngineClient peerEngineClient,
            DatabaseSynchManager databaseSynchManager,
            PeerShareManager peerShareManager,
            ImageDownloader imageDownloader) {
        this.peerEngineClient = peerEngineClient;
        lastSynchedPeerId = null;
        this.imageDownloader = imageDownloader;
        timer = new Timer(REMOTE_SYNCH_DELAY, this, false, "PeriodicTaskReminder");
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
    }

    public void start() {
        timer.reset();
    }

    @Override
    public Long wakeUp(Timer timer) {
        lastSynchedPeerId = peerEngineClient.getNextConnectedPeer(lastSynchedPeerId);
        if (lastSynchedPeerId != null) {
            databaseSynchManagerTask.addTaskForPeer(lastSynchedPeerId);
            peerShareManagerRemoteShareTask.addTaskForPeer(lastSynchedPeerId);
            peerShareManagerTempFilesTask.addTaskForPeer(lastSynchedPeerId);
        }
        imageDownloaderTask.submit(imageDownloader::downloadMissingImages);
        return null;
    }

    public void stop() {
        timer.stop();
        databaseSynchManagerTask.stop();
        peerShareManagerRemoteShareTask.stop();
        peerShareManagerTempFilesTask.stop();
        imageDownloaderTask.shutdown();
    }
}
