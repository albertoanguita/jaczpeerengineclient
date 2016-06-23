package jacz.peerengineclient.util;

import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineservice.util.datatransfer.master.DownloadManager;
import jacz.util.date_time.TimedEventRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allows checking and erasing redundant downloads (file already available, or another download with
 * higher downloaded size existing)
 *
 * It allows a periodic checking of redundancies
 */
public class RedundantFileChecker {

    private final static Logger logger = LoggerFactory.getLogger(RedundantFileChecker.class);

    private static final long RECENT_CHECK_THRESHOLD = 30000;

    private final PeerEngineClient peerEngineClient;

    private final TimedEventRecord timedEventRecord;

    public RedundantFileChecker(PeerEngineClient peerEngineClient) {
        this.peerEngineClient = peerEngineClient;
        timedEventRecord = new TimedEventRecord(RECENT_CHECK_THRESHOLD);
    }

    public void requestPeriodicCheck() {
        if (!timedEventRecord.lastEventIsRecent()) {
            checkRedundantDownloads();
        }
    }

    public void checkRedundantDownloads() {
        logger.info("Checking for redundant downloads...");
        for (DownloadManager downloadManager : peerEngineClient.getAllDownloads()) {
            if (peerEngineClient.getFileAPI().isHashLocallyAvailable(downloadManager.getResourceID())) {
                logger.info("Redundant download found with hash " + downloadManager.getResourceID() + ". Cancellling download");
                downloadManager.cancel();
            }
        }
        timedEventRecord.newEvent();
    }
}
