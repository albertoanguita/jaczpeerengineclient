package jacz.peerengineclient.test;

import jacz.peerengineclient.JPeerEngineClient;
import jacz.peerengineclient.SessionManager;

/**
 * test
 */
public class TestLoadSave {

    public static void main(String[] args) throws Exception {

        JPeerEngineClient jPeerEngineClient = SessionManager.load("./examples/user_0", new SimpleJacuzziPeerClientAction("init"));

        jPeerEngineClient.stop();
        SessionManager.save(jPeerEngineClient);

    }
}
