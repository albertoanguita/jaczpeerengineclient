package jacz.peerengineclient;

import java.io.IOException;

/**
 * Created by Alberto on 09/05/2016.
 */
public interface ErrorEvents {

    void fatalError(String message);

    void downloadedFileCouldNotBeLoaded(String path, String expectedFileName, IOException e);

    void temporaryDownloadFileCouldNotBeRecovered(String path);
}
