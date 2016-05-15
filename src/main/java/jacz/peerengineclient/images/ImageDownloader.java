package jacz.peerengineclient.images;

import jacz.database.DatabaseMediator;
import jacz.database.util.ImageHash;
import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineclient.util.FileAPI;
import jacz.peerengineservice.NotAliveException;
import jacz.util.date_time.TimedEventRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Alberto on 18/02/2016.
 */
public class ImageDownloader {

    private final static Logger logger = LoggerFactory.getLogger(ImageDownloader.class);

    private static final long RECENT_IMAGE_CHECK_THRESHOLD = 60000;

    private final PeerEngineClient peerEngineClient;

    private final String integratedDb;

    private final FileAPI fileAPI;

    private final TimedEventRecord timedEventRecord;

    public ImageDownloader(PeerEngineClient peerEngineClient, String integratedDb, FileAPI fileAPI) {
        this.peerEngineClient = peerEngineClient;
        this.integratedDb = integratedDb;
        this.fileAPI = fileAPI;
        timedEventRecord = new TimedEventRecord(RECENT_IMAGE_CHECK_THRESHOLD);
    }

    public void downloadMissingImages() {
        if (!timedEventRecord.lastEventIsRecent()) {
            logger.info("Searching for missing images...");
            Set<ImageHash> missingImages = getMissingImages();
            for (ImageHash imageHash : missingImages) {
                try {
                    logger.info("Downloading missing image " + imageHash);
                    peerEngineClient.downloadImage(imageHash);
                } catch (IOException | NotAliveException e) {
                    // ignore
                }
            }
            timedEventRecord.newEvent();
        }
    }

    public void checkImageHash(ImageHash imageHash) {
        if (imageHash != null && isImageHashMissing(imageHash)) {
            downloadImage(imageHash);
        }
    }

    private void downloadImage(ImageHash imageHash) {
        try {
            peerEngineClient.downloadImage(imageHash);
        } catch (IOException | NotAliveException e) {
            // ignore
        }
    }

    private Set<ImageHash> getMissingImages() {
        Set<ImageHash> missingImages = new HashSet<>();
        for (ImageHash imageHash : DatabaseMediator.getAllImageHashes(integratedDb)) {
            if (imageHash != null && isImageHashMissing(imageHash)) {
                missingImages.add(imageHash);
            }
        }
        return missingImages;
    }

    private boolean isImageHashMissing(ImageHash imageHash) {
        return !fileAPI.isHashAvailable(imageHash.getHash());
    }
}
