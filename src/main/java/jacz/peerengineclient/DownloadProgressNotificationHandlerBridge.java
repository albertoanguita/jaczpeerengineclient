package jacz.peerengineclient;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.datatransfer.DownloadProgressNotificationHandler;
import jacz.peerengineservice.util.datatransfer.master.DownloadManager;
import jacz.peerengineservice.util.datatransfer.master.ProviderStatistics;
import jacz.peerengineservice.util.datatransfer.master.ResourcePart;
import jacz.peerengineservice.util.datatransfer.resource_accession.ResourceWriter;
import jacz.util.files.FileUtil;
import jacz.util.numeric.range.LongRange;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;

/**
 * todo
 */
public class DownloadProgressNotificationHandlerBridge implements DownloadProgressNotificationHandler {

    private DownloadEvents downloadEvents;

    public DownloadProgressNotificationHandlerBridge(DownloadEvents downloadEvents) {
        this.downloadEvents = downloadEvents;
    }

    static DownloadInfo buildDownloadInfo(HashMap<String, Serializable> userDictionary) {
        // todo
        return null;
    }

    @Override
    public void started(String resourceID, String storeName, DownloadManager downloadManager) {
        downloadEvents.started(resourceID, handleStore(storeName), downloadManager, buildDownloadInfo(downloadManager.getResourceWriter().getUserDictionary()));
    }

    @Override
    public void resourceSize(String resourceID, String storeName, DownloadManager downloadManager, long resourceSize) {
        downloadEvents.resourceSize(resourceID, handleStore(storeName), downloadManager, resourceSize, buildDownloadInfo(downloadManager.getResourceWriter().getUserDictionary()));
    }

    @Override
    public void providerAdded(String resourceID, String storeName, ProviderStatistics providerStatistics, DownloadManager downloadManager, PeerID provider) {
        downloadEvents.providerAdded(resourceID, handleStore(storeName), providerStatistics, downloadManager, provider, buildDownloadInfo(downloadManager.getResourceWriter().getUserDictionary()));
    }

    @Override
    public void providerRemoved(String resourceID, String storeName, ProviderStatistics providerStatistics, DownloadManager downloadManager, PeerID provider) {
        downloadEvents.providerRemoved(resourceID, handleStore(storeName), providerStatistics, downloadManager, provider, buildDownloadInfo(downloadManager.getResourceWriter().getUserDictionary()));
    }

    @Override
    public void providerReportedSharedPart(String resourceID, String storeName, ProviderStatistics providerStatistics, DownloadManager downloadManager, ResourcePart sharedPart) {
        downloadEvents.providerReportedSharedPart(resourceID, handleStore(storeName), providerStatistics, downloadManager, sharedPart, buildDownloadInfo(downloadManager.getResourceWriter().getUserDictionary()));
    }

    @Override
    public void providerWasAssignedSegment(String resourceID, String storeName, ProviderStatistics providerStatistics, DownloadManager downloadManager, LongRange assignedSegment) {
        downloadEvents.providerWasAssignedSegment(resourceID, handleStore(storeName), providerStatistics, downloadManager, assignedSegment, buildDownloadInfo(downloadManager.getResourceWriter().getUserDictionary()));
    }

    @Override
    public void providerWasClearedAssignation(String resourceID, String storeName, ProviderStatistics providerStatistics, DownloadManager downloadManager) {
        downloadEvents.providerWasClearedAssignation(resourceID, handleStore(storeName), providerStatistics, downloadManager, buildDownloadInfo(downloadManager.getResourceWriter().getUserDictionary()));
    }

    @Override
    public void paused(String resourceID, String storeName, DownloadManager downloadManager) {
        downloadEvents.paused(resourceID, handleStore(storeName), downloadManager, buildDownloadInfo(downloadManager.getResourceWriter().getUserDictionary()));
    }

    @Override
    public void resumed(String resourceID, String storeName, DownloadManager downloadManager) {
        downloadEvents.resumed(resourceID, handleStore(storeName), downloadManager, buildDownloadInfo(downloadManager.getResourceWriter().getUserDictionary()));
    }

    @Override
    public void downloadedSegment(String resourceID, String storeName, LongRange segment, DownloadManager downloadManager) {
        downloadEvents.downloadedSegment(resourceID, storeName, segment, downloadManager, buildDownloadInfo(downloadManager.getResourceWriter().getUserDictionary()));
    }

    @Override
    public void checkingTotalHash(String resourceID, String storeName, int percentage, DownloadManager downloadManager) {
        downloadEvents.checkingTotalHash(resourceID, storeName, percentage, downloadManager, buildDownloadInfo(downloadManager.getResourceWriter().getUserDictionary()));
    }

    @Override
    public void successTotalHash(String resourceID, String storeName, DownloadManager downloadManager) {
        downloadEvents.successTotalHash(resourceID, storeName, downloadManager, buildDownloadInfo(downloadManager.getResourceWriter().getUserDictionary()));
    }

    @Override
    public void failedTotalHash(String resourceID, String storeName, DownloadManager downloadManager) {
        downloadEvents.failedTotalHash(resourceID, storeName, downloadManager, buildDownloadInfo(downloadManager.getResourceWriter().getUserDictionary()));
    }

    @Override
    public void invalidTotalHashAlgorithm(String resourceID, String storeName, String hashAlgorithm, DownloadManager downloadManager) {
        downloadEvents.invalidTotalHashAlgorithm(resourceID, storeName, hashAlgorithm, downloadManager, buildDownloadInfo(downloadManager.getResourceWriter().getUserDictionary()));
    }

    @Override
    public synchronized void completed(String resourceID, String storeName, ResourceWriter resourceWriter, DownloadManager downloadManager) {
        String finalPath = null;
        if (this.downloadManager.getFinalPath() != null && !this.downloadManager.getCurrentPath().equals(this.downloadManager.getFinalPath())) {
            try {
                String finalDir = FileUtil.getFileDirectory(this.downloadManager.getFinalPath());
                String finalFile = FileUtil.getFileName(this.downloadManager.getFinalPath());


                if (finalFile.length() == 0) {
                    finalFile = "downloadedFile";
                }
                // divide file name of extension
                int indexOfPoint = finalFile.lastIndexOf(FileUtil.FILE_EXTENSION_SEPARATOR_CHAR);
                String baseFileName;
                String extension;
                if (indexOfPoint > -1) {
                    baseFileName = finalFile.substring(0, indexOfPoint);
                    extension = finalFile.substring(indexOfPoint + 1);
                } else {
                    baseFileName = finalFile;
                    extension = "";
                }
                finalPath = FileUtil.createNonExistingFileNameWithIndex(finalDir, baseFileName, extension, " (", ")", true);
                FileUtil.move(this.downloadManager.getCurrentPath(), finalPath, true);
            } catch (IOException e) {
                // any error in the operation -> leave the file where it is
                System.out.println("ERROR AL TRANSFERIR EL FICHERO!!! finalPath=" + this.downloadManager.getFinalPath() + ", currentPath=" + this.downloadManager.getCurrentPath() + ", localFinalPath=" + finalPath);
                e.printStackTrace();
                finalPath = this.downloadManager.getCurrentPath();
            }
        } else {
            finalPath = this.downloadManager.getCurrentPath();
        }
        downloadEvents.completed(resourceID, handleStore(storeName), finalPath, downloadManager, buildDownloadInfo(downloadManager.getResourceWriter().getUserDictionary()));
    }

    @Override
    public void cancelled(String resourceID, String storeName, CancellationReason reason, DownloadManager downloadManager) {
        downloadEvents.cancelled(resourceID, handleStore(storeName), reason, downloadManager, buildDownloadInfo(downloadManager.getResourceWriter().getUserDictionary()));
    }

    @Override
    public void stopped(String resourceID, String storeName, DownloadManager downloadManager) {
        downloadEvents.stopped(resourceID, handleStore(storeName), downloadManager, buildDownloadInfo(downloadManager.getResourceWriter().getUserDictionary()));
    }

    private String handleStore(String storeName) {
        return storeName.equals(PeerEngineClient.DEFAULT_STORE) ? null : storeName;
    }
}
