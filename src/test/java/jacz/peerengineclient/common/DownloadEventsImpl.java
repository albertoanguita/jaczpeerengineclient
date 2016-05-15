package jacz.peerengineclient.common;

import jacz.peerengineclient.DownloadEvents;
import jacz.peerengineclient.DownloadInfo;
import jacz.peerengineservice.util.datatransfer.DownloadProgressNotificationHandler;
import jacz.peerengineservice.util.datatransfer.master.DownloadManager;

/**
 *
 */
public class DownloadEventsImpl implements DownloadEvents {

    @Override
    public void started(DownloadInfo downloadInfo, DownloadManager downloadManager) {
        System.out.println("Download started: " + downloadInfo);
    }

    @Override
    public void paused(DownloadInfo downloadInfo, DownloadManager downloadManager) {
        System.out.println("Download paused: " + downloadInfo);
    }

    @Override
    public void resumed(DownloadInfo downloadInfo, DownloadManager downloadManager) {
        System.out.println("Download resumed: " + downloadInfo);
    }

    @Override
    public void cancelled(DownloadInfo downloadInfo, DownloadManager downloadManager, DownloadProgressNotificationHandler.CancellationReason reason) {
        System.out.println("Download cancelled: " + downloadInfo);
    }

    @Override
    public void stopped(DownloadInfo downloadInfo, DownloadManager downloadManager) {
        System.out.println("Download stopped: " + downloadInfo);
    }

    @Override
    public void completed(DownloadInfo downloadInfo, String path, DownloadManager downloadManager) {
        System.out.println("Download completed: " + downloadInfo);
    }
}
