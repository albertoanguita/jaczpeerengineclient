package jacz.peerengineclient.test;

import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineclient.SessionManager;
import jacz.peerengineservice.test.*;
import jacz.peerengineservice.test.TempFileManagerEventsImpl;

import java.io.IOException;

/**
 * Created by Alberto on 02/01/2016.
 */
public class Client {

    public static PeerEngineClient loadClient(String userPath) throws IOException {
        return SessionManager.load(
                userPath,
                new GeneralEventsImpl(),
                new ConnectionEventsImpl(),
                new jacz.peerengineservice.test.ResourceTransferEventsImpl(),
                new TempFileManagerEventsImpl(),
                new DatabaseSynchEventsImpl(),
                new DownloadEventsImpl(),
                new IntegrationEventsImpl(),
                new ErrorHandlerImpl());
    }
}
