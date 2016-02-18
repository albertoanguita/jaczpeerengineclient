package jacz.peerengineclient.test;

import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineclient.SessionManager;
import jacz.peerengineservice.test.TempFileManagerEventsImpl;

import java.io.IOException;

/**
 * Created by Alberto on 02/01/2016.
 */
public class Client {

    public static PeerEngineClient loadClient(String userPath) throws IOException {
        GeneralEventsImpl generalEvents = new GeneralEventsImpl();
        PeerEngineClient peerEngineClient = SessionManager.load(
                userPath,
                generalEvents,
                new ConnectionEventsImpl(),
                new ResourceTransferEventsImpl(),
                new TempFileManagerEventsImpl(),
                new DatabaseSynchEventsImpl(),
                new DownloadEventsImpl(),
                new IntegrationEventsImpl(),
                new ErrorHandlerImpl());

        generalEvents.setPeerEngineClient(peerEngineClient);
        return peerEngineClient;
    }
}
