package jacz.peerengineclient.test;

import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineclient.SessionManager;

/**
 * test
 */
public class TestLoadSave {

    public static void main(String[] args) throws Exception {

        PeerEngineClient peerEngineClient = SessionManager.load("./examples/user_0", new SimpleJacuzziPeerClientAction("init"));

        peerEngineClient.stop();
        SessionManager.save(peerEngineClient);

    }
}
