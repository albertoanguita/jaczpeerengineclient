package jacz.peerengineclient.common;

import jacz.peerengineclient.ErrorEvents;

import java.io.IOException;

/**
 * Created by Alberto on 28/04/2016.
 */
public class ErrorEventsImpl implements ErrorEvents {

    @Override
    public void fatalError(String errorMessage) {
        System.err.println(errorMessage);
    }

    @Override
    public void downloadedFileCouldNotBeLoaded(String path, String expectedFileName, IOException e) {
        System.err.println("Downloaded file could not be loaded, path: " + path + ", expectedFileName: " + expectedFileName);
        e.printStackTrace();
    }

    @Override
    public void temporaryDownloadFileCouldNotBeRecovered(String path) {
        System.err.println("Temporary download file could not be recovered: " + path);
    }
}
