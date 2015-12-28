package jacz.peerengineclient.test.synch_db;

import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineclient.SessionManager;
import jacz.peerengineclient.test.ConnectionEventsImpl;
import jacz.peerengineclient.test.DatabaseSynchEventsImpl;
import jacz.peerengineclient.test.GeneralEventsImpl;
import jacz.peerengineclient.test.IntegrationEventsImpl;
import jacz.peerengineservice.test.ResourceTransferEventsImpl;
import jacz.peerengineservice.test.TempFileManagerEventsImpl;
import jacz.util.concurrency.ThreadUtil;

import java.io.IOException;

/**
 * Created by Alberto on 27/12/2015.
 */
public class TestSynch_1 {

    public static void main(String[] args) throws IOException {

        PeerEngineClient peerEngineClient = SessionManager.load(
                "./etc/user_0",
                new GeneralEventsImpl(),
                new ConnectionEventsImpl(),
                new ResourceTransferEventsImpl(),
                new TempFileManagerEventsImpl(),
                new DatabaseSynchEventsImpl(),
                new IntegrationEventsImpl());
        peerEngineClient.connect();

        ThreadUtil.safeSleep(50000);
        peerEngineClient.stop();

        System.out.println("END");
    }
}
