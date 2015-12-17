package jacz.peerengineclient.test.transfer;

import jacz.peerengineclient.DownloadManagerOLD;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.datatransfer.DownloadProgressNotificationHandler;
import jacz.peerengineservice.util.datatransfer.master.ResourcePart;
import jacz.peerengineclient.DownloadEvents;
import jacz.util.numeric.range.LongRange;

import java.io.Serializable;
import java.util.Map;

/**
 *
 */
public class DownloadEventsImpl implements DownloadEvents {

    private String initMessage;

    public DownloadEventsImpl(PeerID peerID) {
        initMessage = peerID + " downloading resource: ";
    }

    @Override
    public void started(String resourceID, String storeName, DownloadManagerOLD downloadManager, Map<String, Serializable> genericUserData) {
        System.out.println(initMessage + "started download of resource " + resourceID);
    }

    @Override
    public void resourceSize(String resourceID, String storeName, DownloadManagerOLD downloadManager, long resourceSize) {
        System.out.println(initMessage + "reported the resource size of resource " + resourceID + ": " + resourceSize);
    }

    @Override
    public void providerAdded(String resourceID, String storeName, jacz.peerengineservice.util.datatransfer.master.ProviderStatistics providerStatistics, DownloadManagerOLD downloadManager, String providerId) {
        System.out.println(initMessage + "provider added to download of resource " + resourceID);
    }

    @Override
    public void providerRemoved(String resourceID, String storeName, jacz.peerengineservice.util.datatransfer.master.ProviderStatistics providerStatistics, DownloadManagerOLD downloadManager, String providerId) {
        System.out.println(initMessage + "provider removed from download of resource " + resourceID);
    }

    @Override
    public void providerReportedSharedPart(String resourceID, String storeName, jacz.peerengineservice.util.datatransfer.master.ProviderStatistics providerStatistics, DownloadManagerOLD downloadManager, ResourcePart sharedPart) {
        System.out.println(initMessage + "provider reported its shared part: " + providerStatistics.getResourceProvider().getID() + " / " + sharedPart);
    }

    @Override
    public void providerWasAssignedSegment(String resourceID, String storeName, jacz.peerengineservice.util.datatransfer.master.ProviderStatistics providerStatistics, DownloadManagerOLD downloadManager, LongRange assignedSegment) {
        System.out.println(initMessage + "provider was assigned a new segment: " + providerStatistics.getResourceProvider().getID() + " / " + assignedSegment);
    }

    @Override
    public void providerWasClearedAssignation(String resourceID, String storeName, jacz.peerengineservice.util.datatransfer.master.ProviderStatistics providerStatistics, DownloadManagerOLD downloadManager) {
        System.out.println(initMessage + "providers assignation was cleared: " + providerStatistics.getResourceProvider().getID());
    }

    @Override
    public void paused(String resourceID, String storeName, DownloadManagerOLD downloadManager) {
        System.out.println(initMessage + "download paused for resource " + resourceID);
    }

    @Override
    public void resumed(String resourceID, String storeName, DownloadManagerOLD downloadManager) {
        System.out.println(initMessage + "download resumed for resource " + resourceID);
    }

    @Override
    public void downloadedSegment(String resourceID, String storeName, LongRange segment, jacz.peerengineservice.util.datatransfer.master.DownloadManager downloadManager) {
    }

    @Override
    public void successIntermediateHash(String resourceID, String storeName, LongRange range, jacz.peerengineservice.util.datatransfer.master.DownloadManager downloadManager) {
    }

    @Override
    public void failedIntermediateHash(String resourceID, String storeName, LongRange range, jacz.peerengineservice.util.datatransfer.master.DownloadManager downloadManager) {
        System.out.println(initMessage + "FAIL intermediate hash for resource " + resourceID + ". " + range);
    }

    @Override
    public void invalidIntermediateHashAlgorithm(String resourceID, String storeName, LongRange range, String hashAlgorithm, jacz.peerengineservice.util.datatransfer.master.DownloadManager downloadManager) {
        System.out.println(initMessage + "invalid intermediate hash algorithm for resource " + resourceID + ". " + range + ". " + hashAlgorithm);
    }

    @Override
    public void checkingTotalHash(String resourceID, String storeName, int percentage, jacz.peerengineservice.util.datatransfer.master.DownloadManager downloadManager) {
        System.out.println(initMessage + "checking total hash for resource " + resourceID + ". " + percentage + "%");
    }

    @Override
    public void successTotalHash(String resourceID, String storeName, jacz.peerengineservice.util.datatransfer.master.DownloadManager downloadManager) {
        System.out.println(initMessage + "OK total hash for resource " + resourceID);
    }

    @Override
    public void failedTotalHash(String resourceID, String storeName, jacz.peerengineservice.util.datatransfer.master.DownloadManager downloadManager) {
        System.out.println(initMessage + "FAIL total hash for resource " + resourceID);
    }

    @Override
    public void invalidTotalHashAlgorithm(String resourceID, String storeName, String hashAlgorithm, jacz.peerengineservice.util.datatransfer.master.DownloadManager downloadManager) {
        System.out.println(initMessage + "invalid total hash algorithm for resource " + resourceID + ". " + hashAlgorithm);
    }

    @Override
    public void completed(String resourceID, String storeName, String path, DownloadManagerOLD downloadManager, Map<String, Serializable> genericUserData) {
        System.out.println(initMessage + "download completed for resource " + resourceID + ". File available at '" + path + "'");
        System.out.println(initMessage + "generic data received: " + genericUserData.toString());
    }

    @Override
    public void cancelled(String resourceID, String storeName, DownloadProgressNotificationHandler.CancellationReason reason, DownloadManagerOLD downloadManager, Map<String, Serializable> genericUserData) {
        System.out.println(initMessage + "download cancelled for resource " + resourceID + ". Reason: " + reason);
    }

    @Override
    public void stopped(String resourceID, String storeName, DownloadManagerOLD downloadManager, Map<String, Serializable> genericUserData) {
        System.out.println(initMessage + "download stopped for resource " + resourceID);

    }
}
