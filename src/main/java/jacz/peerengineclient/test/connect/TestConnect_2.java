package jacz.peerengineclient.test.connect;

import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineclient.SessionManager;
import jacz.peerengineclient.test.*;
import jacz.peerengineservice.test.ResourceTransferEventsImpl;
import jacz.peerengineservice.test.TempFileManagerEventsImpl;
import jacz.util.concurrency.ThreadUtil;

import java.io.IOException;

/**
 * Created by Alberto on 26/12/2015.
 */
public class TestConnect_2 {

    public static void main(String[] args) throws IOException {

        PeerEngineClient peerEngineClient = Client.loadClient("./etc/user_1");
        peerEngineClient.connect();

        ThreadUtil.safeSleep(30000);
        peerEngineClient.stop();

        System.out.println("END");
    }
}
