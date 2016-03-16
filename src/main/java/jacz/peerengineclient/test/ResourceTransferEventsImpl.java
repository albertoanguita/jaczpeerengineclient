package jacz.peerengineclient.test;

import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.util.datatransfer.*;
import jacz.peerengineservice.util.datatransfer.master.DownloadManager;

/**
 * todo
 */
public class ResourceTransferEventsImpl implements ResourceTransferEvents {

    @Override
    public void addLocalResourceStore(String name) {

    }

    @Override
    public void setLocalGeneralResourceStore() {

    }

    @Override
    public void addForeignResourceStore(String name) {

    }

    @Override
    public void removeLocalResourceStore(String name) {

    }

    @Override
    public void removeLocalGeneralResourceStore() {

    }

    @Override
    public void removeForeignResourceStore(String name) {

    }

    @Override
    public void globalDownloadInitiated(String resourceStoreName, String resourceID, double streamingNeed, String totalHash, String totalHashAlgorithm) {

    }

    @Override
    public void peerDownloadInitiated(PeerId serverPeerId, String resourceStoreName, String resourceID, double streamingNeed, String totalHash, String totalHashAlgorithm) {

    }

    @Override
    public void setMaxDesiredDownloadSpeed(Float totalMaxDesiredSpeed) {

    }

    @Override
    public void setMaxDesiredUploadSpeed(Float totalMaxDesiredSpeed) {

    }

    @Override
    public void setAccuracy(double accuracy) {

    }

    @Override
    public void approveResourceRequest(ResourceRequest request, ResourceStoreResponse response) {

    }

    @Override
    public void denyUnavailableSubchannelResourceRequest(ResourceRequest request, ResourceStoreResponse response) {

    }

    @Override
    public void deniedResourceRequest(ResourceRequest request, ResourceStoreResponse response) {

    }

    @Override
    public void periodicDownloadsNotification(DownloadsManager downloadsManager) {
        for (DownloadManager downloadManager : downloadsManager.getAllDownloads()) {
            Double speed = downloadManager.getStatistics().getSpeed();
            speed /= 1024d;
            long size = downloadManager.getStatistics().getDownloadedSizeThisResource();
            Long length = downloadManager.getLength();
            Double part = null;
            if (length != null) {
                part = (double) size / (double) length * 100d;
            }
            System.out.println("Speed for " + downloadManager.getResourceID() + " (store " + downloadManager.getStoreName() + "): " + speed + "KB, downloaded part: " + part);
        }
    }

    @Override
    public void periodicUploadsNotification(UploadsManager uploadsManager) {

    }
}
