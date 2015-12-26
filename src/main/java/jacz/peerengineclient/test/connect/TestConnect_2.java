package jacz.peerengineclient.test.connect;

import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineclient.SessionManager;
import jacz.peerengineclient.test.ConnectionEventsImpl;
import jacz.peerengineclient.test.GeneralEventsImpl;
import jacz.peerengineclient.test.IntegrationEventsImpl;
import jacz.peerengineclient.test.DatabaseSynchEventsImpl;
import jacz.peerengineservice.test.ResourceTransferEventsImpl;
import jacz.peerengineservice.test.TempFileManagerEventsImpl;
import jacz.util.concurrency.ThreadUtil;

import java.io.IOException;

/**
 * Created by Alberto on 26/12/2015.
 */
public class TestConnect_2 {

    public static void main(String[] args) throws IOException {

        PeerEngineClient peerEngineClient = SessionManager.load(
                "./etc/user_1",
                new GeneralEventsImpl(),
                new ConnectionEventsImpl(),
                new ResourceTransferEventsImpl(),
                new TempFileManagerEventsImpl(),
                new DatabaseSynchEventsImpl(),
                new IntegrationEventsImpl());
        peerEngineClient.connect();

        ThreadUtil.safeSleep(30000);
        peerEngineClient.stop();

        System.out.println("END");
    }
}
