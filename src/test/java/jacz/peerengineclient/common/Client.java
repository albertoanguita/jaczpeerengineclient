package jacz.peerengineclient.common;

import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineclient.SessionManager;
import jacz.util.lists.tuple.Duple;

import java.io.IOException;
import java.util.List;

/**
 * Created by Alberto on 28/04/2016.
 */
public class Client {

    public static PeerEngineClient loadClient(String userPath) throws IOException {
        Duple<PeerEngineClient, List<String>> duple = SessionManager.load(
                userPath,
                new GeneralEventsImpl(),
                new ConnectionEventsImpl(),
                new PeersEventsImpl(),
                new ResourceTransferEventsImpl(),
                new TempFileManagerEventsImpl(),
                new DatabaseSynchEventsImpl(),
                new DownloadEventsImpl(),
                new IntegrationEventsImpl(),
                new ErrorEventsImpl());

        if (!duple.element2.isEmpty()) {
            System.err.println("REPAIRED FILES: " + duple.element2);
        }
        return duple.element1;
    }
}
