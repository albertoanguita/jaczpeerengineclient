package jacz.peerengineclient.common;

import jacz.peerengineclient.ErrorEvents;

import java.util.List;

/**
 * Created by Alberto on 28/04/2016.
 */
public class ErrorEventsImpl implements ErrorEvents {

    @Override
    public void fatalError(String errorMessage) {
        System.err.println(errorMessage);
    }

    @Override
    public void sessionDataCouldNotBeSaved() {
        System.err.println("Session could not be saved");
    }

    @Override
    public void downloadedFileCouldNotBeLoaded(String path, String expectedFileName) {
        System.err.println("Session could not be saved, path: " + path + ", expectedFileName: " + expectedFileName);
    }

    @Override
    public void temporaryDownloadFileCouldNotBeRecovered(String path) {
        System.err.println("Temporary download file could not be recovered: " + path);
    }
}
