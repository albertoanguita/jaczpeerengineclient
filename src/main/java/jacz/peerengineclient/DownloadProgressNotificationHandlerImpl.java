package jacz.peerengineclient;

import jacz.peerengineservice.util.datatransfer.DownloadProgressNotificationHandler;
import jacz.peerengineservice.util.datatransfer.master.DownloadManager;
import jacz.peerengineservice.util.datatransfer.master.ProviderStatistics;
import jacz.peerengineservice.util.datatransfer.master.ResourcePart;
import jacz.peerengineservice.util.datatransfer.resource_accession.ResourceWriter;
import jacz.util.concurrency.execution_control.PausableElement;
import jacz.util.files.FileUtil;
import jacz.util.numeric.range.LongRange;

import java.io.IOException;

/**
 * todo
 */
public class DownloadProgressNotificationHandlerImpl implements DownloadProgressNotificationHandler {

    private DownloadEvents downloadEvents;

    private jacz.peerengineclient.DownloadManager downloadManager = null;

    /**
     * This pausable elements allows ensuring that the started method is not executed before the download manager has been set
     */
    private PausableElement pausableElement;

    public DownloadProgressNotificationHandlerImpl(DownloadEvents downloadEvents) {
        this.downloadEvents = downloadEvents;
        pausableElement = new PausableElement();
        pausableElement.pause();
    }

    public void setDownloadManager(jacz.peerengineclient.DownloadManager downloadManager) {
        this.downloadManager = downloadManager;
        pausableElement.resume();
    }

    @Override
    public void started(String resourceID, String storeName, DownloadManager downloadManager) {
        pausableElement.access();
        downloadEvents.started(resourceID, handleStore(storeName), this.downloadManager, this.downloadManager.getUserGenericData());
    }

    @Override
    public void resourceSize(String resourceID, String storeName, DownloadManager downloadManager, long resourceSize) {
        downloadEvents.resourceSize(resourceID, handleStore(storeName), this.downloadManager, resourceSize);
    }

    @Override
    public void providerAdded(String resourceID, String storeName, ProviderStatistics providerStatistics, DownloadManager downloadManager, String providerId) {
        downloadEvents.providerAdded(resourceID, handleStore(storeName), providerStatistics, this.downloadManager, providerId);
    }

    @Override
    public void providerRemoved(String resourceID, String storeName, ProviderStatistics providerStatistics, DownloadManager downloadManager, String providerId) {
        downloadEvents.providerRemoved(resourceID, handleStore(storeName), providerStatistics, this.downloadManager, providerId);
    }

    @Override
    public void providerReportedSharedPart(String resourceID, String storeName, ProviderStatistics providerStatistics, DownloadManager downloadManager, ResourcePart sharedPart) {
        downloadEvents.providerReportedSharedPart(resourceID, handleStore(storeName), providerStatistics, this.downloadManager, sharedPart);
    }

    @Override
    public void providerWasAssignedSegment(String resourceID, String storeName, ProviderStatistics providerStatistics, DownloadManager downloadManager, LongRange assignedSegment) {
        downloadEvents.providerWasAssignedSegment(resourceID, handleStore(storeName), providerStatistics, this.downloadManager, assignedSegment);
    }

    @Override
    public void providerWasClearedAssignation(String resourceID, String storeName, ProviderStatistics providerStatistics, DownloadManager downloadManager) {
        downloadEvents.providerWasClearedAssignation(resourceID, handleStore(storeName), providerStatistics, this.downloadManager);
    }

    @Override
    public void paused(String resourceID, String storeName, DownloadManager downloadManager) {
        downloadEvents.paused(resourceID, handleStore(storeName), this.downloadManager);
    }

    @Override
    public void resumed(String resourceID, String storeName, DownloadManager downloadManager) {
        downloadEvents.resumed(resourceID, handleStore(storeName), this.downloadManager);
    }

    @Override
    public void downloadedSegment(String resourceID, String storeName, LongRange segment, DownloadManager downloadManager) {
        downloadEvents.downloadedSegment(resourceID, storeName, segment, downloadManager);
    }

    @Override
    public void successIntermediateHash(String resourceID, String storeName, LongRange range, DownloadManager downloadManager) {
        downloadEvents.successIntermediateHash(resourceID, storeName, range, downloadManager);
    }

    @Override
    public void failedIntermediateHash(String resourceID, String storeName, LongRange range, DownloadManager downloadManager) {
        downloadEvents.failedIntermediateHash(resourceID, storeName, range, downloadManager);
    }

    @Override
    public void invalidIntermediateHashAlgorithm(String resourceID, String storeName, LongRange range, String hashAlgorithm, DownloadManager downloadManager) {
        downloadEvents.invalidIntermediateHashAlgorithm(resourceID, storeName, range, hashAlgorithm, downloadManager);
    }

    @Override
    public void checkingTotalHash(String resourceID, String storeName, int percentage, DownloadManager downloadManager) {
        downloadEvents.checkingTotalHash(resourceID, storeName, percentage, downloadManager);
    }

    @Override
    public void successTotalHash(String resourceID, String storeName, DownloadManager downloadManager) {
        downloadEvents.successTotalHash(resourceID, storeName, downloadManager);
    }

    @Override
    public void failedTotalHash(String resourceID, String storeName, DownloadManager downloadManager) {
        downloadEvents.failedTotalHash(resourceID, storeName, downloadManager);
    }

    @Override
    public void invalidTotalHashAlgorithm(String resourceID, String storeName, String hashAlgorithm, DownloadManager downloadManager) {
        downloadEvents.invalidTotalHashAlgorithm(resourceID, storeName, hashAlgorithm, downloadManager);
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
        downloadEvents.completed(resourceID, handleStore(storeName), finalPath, this.downloadManager, this.downloadManager.getUserGenericData());
    }

    @Override
    public void cancelled(String resourceID, String storeName, CancellationReason reason, DownloadManager downloadManager) {
        if (this.downloadManager.isErrorFlag()) {
            // the cancel was actually provoked by an IO failure, but the PeerEngine did not know it
            reason = CancellationReason.IO_FAILURE;
        }
        downloadEvents.cancelled(resourceID, handleStore(storeName), DownloadEvents.CancellationReason.generateCancellationReason(reason), this.downloadManager, this.downloadManager.getUserGenericData());
    }

    @Override
    public void stopped(String resourceID, String storeName, DownloadManager downloadManager) {
        downloadEvents.stopped(resourceID, handleStore(storeName), this.downloadManager, this.downloadManager.getUserGenericData());
    }

    private String handleStore(String storeName) {
        return storeName.equals(JPeerEngineClient.DEFAULT_STORE) ? null : storeName;
    }
}
