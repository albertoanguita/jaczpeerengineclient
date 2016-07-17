package jacz.peerengineclient;

import org.aanguita.jacuzzi.log.ErrorHandler;

import java.io.IOException;

/**
 * Created by Alberto on 02/01/2016.
 */
public class ErrorHandlerBridge implements ErrorHandler {

    private final PeerEngineClient peerEngineClient;

    private final ErrorEvents errorEvents;

    public ErrorHandlerBridge(PeerEngineClient peerEngineClient, ErrorEvents errorEvents) {
        this.peerEngineClient = peerEngineClient;
        this.errorEvents = errorEvents;
    }

    @Override
    public void errorRaised(String errorMessage) {
        errorEvents.fatalError(errorMessage);
        peerEngineClient.stop();
    }

    void sessionDataCouldNotBeSaved() {
        errorEvents.sessionDataCouldNotBeSaved();
    }

    void downloadedFileCouldNotBeLoaded(String path, String expectedFileName, IOException e) {
        errorEvents.downloadedFileCouldNotBeLoaded(path, expectedFileName, e);
    }

    void temporaryDownloadFileCouldNotBeRecovered(String path) {
        errorEvents.temporaryDownloadFileCouldNotBeRecovered(path);
    }
}
