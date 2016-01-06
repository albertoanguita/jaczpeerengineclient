package jacz.peerengineclient.test;

import jacz.peerengineclient.DownloadEvents;
import jacz.peerengineclient.DownloadInfo;
import jacz.peerengineservice.util.datatransfer.DownloadProgressNotificationHandler;
import jacz.peerengineservice.util.datatransfer.master.DownloadManager;

/**
 * Created by Alberto on 05/01/2016.
 */
public class DownloadEventsImpl implements DownloadEvents {

    // todo
    @Override
    public void started(DownloadInfo downloadInfo, DownloadManager downloadManager) {

    }

    @Override
    public void paused(DownloadInfo downloadInfo, DownloadManager downloadManager) {

    }

    @Override
    public void resumed(DownloadInfo downloadInfo, DownloadManager downloadManager) {

    }

    @Override
    public void cancelled(DownloadInfo downloadInfo, DownloadManager downloadManager, DownloadProgressNotificationHandler.CancellationReason reason) {

    }

    @Override
    public void stopped(DownloadInfo downloadInfo, DownloadManager downloadManager) {

    }

    @Override
    public void completed(DownloadInfo downloadInfo, String path, DownloadManager downloadManager) {

    }
}
