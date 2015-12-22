package jacz.peerengineclient;

import jacz.peerengineservice.client.PeerClient;
import jacz.util.identifier.UniqueIdentifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles all active downloads
 */
public class DownloadsManager {

    private PeerClient peerClient;

    private final Map<UniqueIdentifier, DownloadManagerOLD> activeDownloads;

    public DownloadsManager(PeerClient peerClient) {
        this.peerClient = peerClient;
        activeDownloads = new HashMap<>();
    }

    void setPeerClient(PeerClient peerClient) {
        this.peerClient = peerClient;
    }

    synchronized void addDownload(UniqueIdentifier uniqueIdentifier, DownloadManagerOLD downloadManager) {
        activeDownloads.put(uniqueIdentifier, downloadManager);
    }

    synchronized void removeDownload(UniqueIdentifier uniqueIdentifier) {
        activeDownloads.remove(uniqueIdentifier);
    }

    public synchronized List<DownloadManagerOLD> getDownloads(String store) {
        List<DownloadManagerOLD> downloads = new ArrayList<>();
        for (jacz.peerengineservice.util.datatransfer.master.DownloadManager downloadManager : peerClient.getVisibleDownloads(store)) {
            downloads.add(activeDownloads.get(downloadManager.getId()));
        }
        return downloads;
    }
}
