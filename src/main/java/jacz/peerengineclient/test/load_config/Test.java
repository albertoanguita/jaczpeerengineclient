package jacz.peerengineclient.test.load_config;

import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineclient.SessionManager;

import java.io.IOException;

/**
 * Created by Alberto on 24/12/2015.
 */
public class Test {

    public static void main(String[] args) throws IOException {

        System.out.println(SessionManager.listAvailableConfigs("./etc"));

        PeerEngineClient peerEngineClient = SessionManager.load("./etc/user_0", null, null, null, null, null, null);

        System.out.println("END");
    }
}
