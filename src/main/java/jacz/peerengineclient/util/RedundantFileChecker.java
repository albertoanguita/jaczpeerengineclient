package jacz.peerengineclient.util;

import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineservice.util.datatransfer.master.DownloadManager;
import org.aanguita.jacuzzi.date_time.TimedEventRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

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
        Map<String, DownloadManager> hashToDownloadMap = new HashMap<>();
        for (DownloadManager downloadManager : peerEngineClient.getAllDownloads()) {
            String hash = downloadManager.getResourceID();
            if (peerEngineClient.getFileAPI().isHashLocallyAvailable(hash)) {
                logger.info("Redundant download found with hash " + hash + ". Cancelling download");
                downloadManager.cancel();
            } else {
                if (hashToDownloadMap.containsKey(hash)) {
                    // there is another download for the same file -> keep the one with bigger downloaded size
                    hashToDownloadMap.put(hash, selectDownloadWithGreaterProgress(downloadManager, hashToDownloadMap.get(hash)));
                } else {
                    // just include this download in the downloads map
                    hashToDownloadMap.put(hash, downloadManager);
                }
            }
        }
        timedEventRecord.newEvent();
    }

    private DownloadManager selectDownloadWithGreaterProgress(DownloadManager downloadManager, DownloadManager anotherDownloadManager) {
        if (downloadManager.getStatistics().getDownloadedSizeThisResource() > anotherDownloadManager.getStatistics().getDownloadedSizeThisResource()) {
            anotherDownloadManager.cancel();
            return downloadManager;
        } else {
            downloadManager.cancel();
            return anotherDownloadManager;
        }
    }
}
