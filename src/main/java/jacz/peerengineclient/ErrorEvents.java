package jacz.peerengineclient;

import java.util.List;

/**
 * Created by Alberto on 09/05/2016.
 */
public interface ErrorEvents {

    void fatalError(String message);

    void sessionDataCouldNotBeSaved();

    void downloadedFileCouldNotBeLoaded(String path, String expectedFileName);

    void temporaryDownloadFileCouldNotBeRecovered(String path);
}
