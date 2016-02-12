package jacz.peerengineclient;

import jacz.util.log.ErrorFactory;
import jacz.util.log.ErrorHandler;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;

/**
 * Created by Alberto on 02/01/2016.
 */
public class ErrorHandlerBridge implements ErrorHandler {

    private final PeerEngineClient peerEngineClient;

    private final ErrorHandler errorHandler;

    public ErrorHandlerBridge(PeerEngineClient peerEngineClient, ErrorHandler errorHandler) {
        this.peerEngineClient = peerEngineClient;
        this.errorHandler = errorHandler;
    }

    @Override
    public void errorRaised(String errorMessage) {
        errorHandler.errorRaised(errorMessage);
        try {
            peerEngineClient.stop();
        } catch (IOException | XMLStreamException e) {
            ErrorFactory.reportError(errorHandler, "Could not save session data", e);
        }
    }
}
