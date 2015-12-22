package jacz.peerengineclient.test_old;

import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineclient.SessionManager;

/**
 * test_old
 */
public class TestLoadSave {

    public static void main(String[] args) throws Exception {

        PeerEngineClient peerEngineClient = SessionManager.load("./examples/user_0", new SimpleJacuzziPeerClientAction("init"));

        peerEngineClient.stop();
        SessionManager.save(peerEngineClient);

    }
}
