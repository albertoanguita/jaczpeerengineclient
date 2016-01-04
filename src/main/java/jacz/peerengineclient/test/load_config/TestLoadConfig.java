package jacz.peerengineclient.test.load_config;

import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineclient.SessionManager;
import jacz.peerengineclient.test.*;
import jacz.peerengineservice.test.ResourceTransferEventsImpl;
import jacz.peerengineservice.test.TempFileManagerEventsImpl;
import jacz.util.concurrency.ThreadUtil;

import java.io.IOException;

/**
 * Created by Alberto on 24/12/2015.
 */
public class TestLoadConfig {

    public static void main(String[] args) throws IOException {

        System.out.println(SessionManager.listAvailableConfigs("./etc"));

        PeerEngineClient peerEngineClient = Client.loadClient("./etc/user_0");

        ThreadUtil.safeSleep(5000);
        peerEngineClient.stop();

        System.out.println("END");
    }
}
