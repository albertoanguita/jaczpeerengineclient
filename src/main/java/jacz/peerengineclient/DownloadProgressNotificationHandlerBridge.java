package jacz.peerengineclient;

import jacz.database.Chapter;
import jacz.database.DatabaseMediator;
import jacz.database.Movie;
import jacz.database.TVSeries;
import jacz.peerengineclient.file_system.Paths;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.datatransfer.DownloadProgressNotificationHandler;
import jacz.peerengineservice.util.datatransfer.master.DownloadManager;
import jacz.peerengineservice.util.datatransfer.master.ProviderStatistics;
import jacz.peerengineservice.util.datatransfer.master.ResourcePart;
import jacz.peerengineservice.util.datatransfer.resource_accession.ResourceWriter;
import jacz.util.files.FileUtil;
import jacz.util.lists.tuple.Triple;
import jacz.util.numeric.range.LongRange;

import java.io.IOException;

/**
 * Handles download events for a file and appropriately redirects them to the client
 */
public class DownloadProgressNotificationHandlerBridge implements DownloadProgressNotificationHandler {

    private final DownloadEvents downloadEvents;

    private final String integratedPath;

    private final String downloadsPath;

    public DownloadProgressNotificationHandlerBridge(DownloadEvents downloadEvents, String integratedPath, String downloadsPath) {
        this.downloadEvents = downloadEvents;
        this.integratedPath = integratedPath;
        this.downloadsPath = downloadsPath;
    }

    @Override
    public void started(String resourceID, String storeName, DownloadManager downloadManager) {
        downloadEvents.started(DownloadInfo.buildDownloadInfo(downloadManager.getResourceWriter().getUserDictionary()), downloadManager);
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
        downloadEvents.paused(DownloadInfo.buildDownloadInfo(downloadManager.getResourceWriter().getUserDictionary()), downloadManager);
    }

    @Override
    public void resumed(String resourceID, String storeName, DownloadManager downloadManager) {
        downloadEvents.resumed(DownloadInfo.buildDownloadInfo(downloadManager.getResourceWriter().getUserDictionary()), downloadManager);
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
        DownloadInfo downloadInfo = DownloadInfo.buildDownloadInfo(downloadManager.getResourceWriter().getUserDictionary());
        if (downloadInfo.type.isMedia()) {
            // move file to required location
            try {
                Triple<String, String, String> location;
                if (downloadInfo.containerType == DatabaseMediator.ItemType.MOVIE) {
                    Movie movie = Movie.getMovieById(integratedPath, downloadInfo.containerId);
                    location = Paths.movieFilePath(downloadsPath, movie.getId(), movie.getTitle(), downloadInfo.fileName);
                } else if (downloadInfo.containerType == DatabaseMediator.ItemType.CHAPTER) {
                    TVSeries tvSeries = TVSeries.getTVSeriesById(integratedPath, downloadInfo.superContainerId);
                    Chapter chapter = Chapter.getChapterById(integratedPath, downloadInfo.containerId);
                    location = Paths.seriesFilePath(downloadsPath, tvSeries.getId(), tvSeries.getTitle(), chapter.getId(), chapter.getTitle(), downloadInfo.fileName);
                } else {
                    // todo error
                    return;
                }
                finalPath = FileUtil.createFile(location.element1, location.element2, location.element3, "(", ")", true).element1;
                FileUtil.move(resourceWriter.getPath(), finalPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        downloadEvents.completed(DownloadInfo.buildDownloadInfo(downloadManager.getResourceWriter().getUserDictionary()), finalPath, downloadManager);
    }

    @Override
    public void cancelled(String resourceID, String storeName, CancellationReason reason, DownloadManager downloadManager) {
        downloadEvents.cancelled(DownloadInfo.buildDownloadInfo(downloadManager.getResourceWriter().getUserDictionary()), downloadManager, reason);
    }

    @Override
    public void stopped(String resourceID, String storeName, DownloadManager downloadManager) {
        downloadEvents.stopped(DownloadInfo.buildDownloadInfo(downloadManager.getResourceWriter().getUserDictionary()), downloadManager);
    }
}
