package jacz.peerengineclient;

import java.io.IOException;
import java.util.List;

/**
 * Created by Alberto on 09/05/2016.
 */
public interface ErrorEvents {

    void fatalError(String message);

    void sessionDataCouldNotBeSaved();

    void downloadedFileCouldNotBeLoaded(String path, String expectedFileName, IOException e);

    void temporaryDownloadFileCouldNotBeRecovered(String path);
}
