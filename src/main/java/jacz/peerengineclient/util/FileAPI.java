package jacz.peerengineclient.util;

import jacz.peerengineclient.data.FileHashDatabaseWithTimestamp;
import jacz.peerengineservice.client.PeerClient;
import jacz.peerengineservice.util.datatransfer.master.DownloadManager;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Alberto on 18/02/2016.
 */
public class FileAPI {

    private final FileHashDatabaseWithTimestamp fileHashDatabaseWithTimestamp;

    private final PeerClient peerClient;

    public FileAPI(FileHashDatabaseWithTimestamp fileHashDatabaseWithTimestamp, PeerClient peerClient) {
        this.fileHashDatabaseWithTimestamp = fileHashDatabaseWithTimestamp;
        this.peerClient = peerClient;
    }

    public Set<String> getAvailableHashes() {
        Set<String> availableHashes = new HashSet<>();
        availableHashes.addAll(fileHashDatabaseWithTimestamp.getActiveHashesSetCopy());
        availableHashes.addAll(getTempHashes());
        return availableHashes;
    }

    public Set<String> getTempHashes() {
        Set<String> tempHashes = new HashSet<>();
        for (DownloadManager downloadManager : peerClient.getAllDownloads()) {
            if (downloadManager.getLength() != null) {
                tempHashes.add(downloadManager.getResourceID());
            }
        }
        return tempHashes;
    }

    public boolean isHashAvailable(String hash) {
        return getTempHashes().contains(hash) || isHashLocallyAvailable(hash);
    }

    public boolean isHashBeingDownloaded(String hash) {
        return getHashDownloadManager(hash) != null;
    }

    public DownloadManager getHashDownloadManager(String hash) {
        for (DownloadManager downloadManager : peerClient.getAllDownloads()) {
            if (downloadManager.getResourceID().equals(hash)) {
                return downloadManager;
            }
        }
        return null;
    }

    public boolean isHashLocallyAvailable(String hash) {
        return fileHashDatabaseWithTimestamp.containsKey(hash);
    }
}
