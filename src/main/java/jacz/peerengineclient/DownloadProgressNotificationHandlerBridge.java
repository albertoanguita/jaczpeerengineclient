package jacz.peerengineclient;

import jacz.database.Chapter;
import jacz.database.DatabaseMediator;
import jacz.database.Movie;
import jacz.peerengineclient.file_system.Paths;
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
        downloadEvents.started(buildDownloadInfo(downloadManager.getResourceWriter().getUserDictionary()), downloadManager);
    }

    @Override
    public void resourceSize(String resourceID, String storeName, DownloadManager downloadManager, long resourceSize) {
        // ignore
    }

    @Override
    public void providerAdded(String resourceID, String storeName, ProviderStatistics providerStatistics, DownloadManager downloadManager, PeerID provider) {
        // ignore
    }

    @Override
    public void providerRemoved(String resourceID, String storeName, ProviderStatistics providerStatistics, DownloadManager downloadManager, PeerID provider) {
        // ignore
    }

    @Override
    public void providerReportedSharedPart(String resourceID, String storeName, ProviderStatistics providerStatistics, DownloadManager downloadManager, ResourcePart sharedPart) {
        // ignore
    }

    @Override
    public void providerWasAssignedSegment(String resourceID, String storeName, ProviderStatistics providerStatistics, DownloadManager downloadManager, LongRange assignedSegment) {
        // ignore
    }

    @Override
    public void providerWasClearedAssignation(String resourceID, String storeName, ProviderStatistics providerStatistics, DownloadManager downloadManager) {
        // ignore
    }

    @Override
    public void paused(String resourceID, String storeName, DownloadManager downloadManager) {
        downloadEvents.paused(buildDownloadInfo(downloadManager.getResourceWriter().getUserDictionary()), downloadManager);
    }

    @Override
    public void resumed(String resourceID, String storeName, DownloadManager downloadManager) {
        downloadEvents.resumed(buildDownloadInfo(downloadManager.getResourceWriter().getUserDictionary()), downloadManager);
    }

    @Override
    public void downloadedSegment(String resourceID, String storeName, LongRange segment, DownloadManager downloadManager) {
        // ignore
    }

    @Override
    public void checkingTotalHash(String resourceID, String storeName, int percentage, DownloadManager downloadManager) {
        // ignore
    }

    @Override
    public void successTotalHash(String resourceID, String storeName, DownloadManager downloadManager) {
        // ignore
    }

    @Override
    public void failedTotalHash(String resourceID, String storeName, DownloadManager downloadManager) {
        // ignore
    }

    @Override
    public void invalidTotalHashAlgorithm(String resourceID, String storeName, String hashAlgorithm, DownloadManager downloadManager) {
        // ignore
    }

    @Override
    public synchronized void completed(String resourceID, String storeName, ResourceWriter resourceWriter, DownloadManager downloadManager) {
        String finalPath = null;
//        if (this.downloadManager.getFinalPath() != null && !this.downloadManager.getCurrentPath().equals(this.downloadManager.getFinalPath())) {
//            try {
//                String finalDir = FileUtil.getFileDirectory(this.downloadManager.getFinalPath());
//                String finalFile = FileUtil.getFileName(this.downloadManager.getFinalPath());
//
//
//                if (finalFile.length() == 0) {
//                    finalFile = "downloadedFile";
//                }
//                // divide file name of extension
//                int indexOfPoint = finalFile.lastIndexOf(FileUtil.FILE_EXTENSION_SEPARATOR_CHAR);
//                String baseFileName;
//                String extension;
//                if (indexOfPoint > -1) {
//                    baseFileName = finalFile.substring(0, indexOfPoint);
//                    extension = finalFile.substring(indexOfPoint + 1);
//                } else {
//                    baseFileName = finalFile;
//                    extension = "";
//                }
//                finalPath = FileUtil.createNonExistingFileNameWithIndex(finalDir, baseFileName, extension, " (", ")", true);
//                FileUtil.move(this.downloadManager.getCurrentPath(), finalPath, true);
//            } catch (IOException e) {
//                // any error in the operation -> leave the file where it is
//                System.out.println("ERROR AL TRANSFERIR EL FICHERO!!! finalPath=" + this.downloadManager.getFinalPath() + ", currentPath=" + this.downloadManager.getCurrentPath() + ", localFinalPath=" + finalPath);
//                e.printStackTrace();
//                finalPath = this.downloadManager.getCurrentPath();
//            }
//        } else {
//            finalPath = this.downloadManager.getCurrentPath();
//        }
        DownloadInfo downloadInfo = buildDownloadInfo(downloadManager.getResourceWriter().getUserDictionary());
        if (downloadInfo.type.isMedia()) {
            // move file to required location
            if (downloadInfo.containerType == DatabaseMediator.ItemType.MOVIE) {
                Movie movie = Movie.getMovieById(, downloadInfo.containerId);
                finalPath = Paths.movieFilePath(, downloadInfo.containerId, movie.getTitle(), downloadInfo.fileName);
            } else if (downloadInfo.containerType == DatabaseMediator.ItemType.CHAPTER) {
                Chapter chapter = Chapter.getChapterById(, downloadInfo.containerId);
                finalPath = Paths.seriesFilePath(, downloadInfo.containerId, chapter.getTitle(), downloadInfo.fileName);
            } else {
                // todo error
            }
            try {
                FileUtil.move(resourceWriter.getPath(), finalPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        downloadEvents.completed(buildDownloadInfo(downloadManager.getResourceWriter().getUserDictionary()), finalPath, downloadManager);
    }

    @Override
    public void cancelled(String resourceID, String storeName, CancellationReason reason, DownloadManager downloadManager) {
        downloadEvents.cancelled(buildDownloadInfo(downloadManager.getResourceWriter().getUserDictionary()), downloadManager, reason);
    }

    @Override
    public void stopped(String resourceID, String storeName, DownloadManager downloadManager) {
        downloadEvents.stopped(buildDownloadInfo(downloadManager.getResourceWriter().getUserDictionary()), downloadManager);
    }

    private String handleStore(String storeName) {
        return storeName.equals(PeerEngineClient.DEFAULT_STORE) ? null : storeName;
    }
}
