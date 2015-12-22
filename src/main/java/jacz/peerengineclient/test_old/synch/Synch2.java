package jacz.peerengineclient.test_old.synch;

import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineclient.SessionManager;
import jacz.util.concurrency.ThreadUtil;

/**
 * Synch 2
 */
public class Synch2 {

    public static void main(String[] args) throws Exception {

        SynchAction synchAction = new SynchAction("P2: ");
        PeerEngineClient peerEngineClient = SessionManager.load("./examples/configs/user_1", synchAction);
        synchAction.setPeerEngineClient(peerEngineClient);

        peerEngineClient.connect();

        ThreadUtil.safeSleep(45000);
        System.out.println("STOPPING...");
        peerEngineClient.stop();
        System.out.println("END");
    }
}
