package jacz.peerengineclient.test.synch;

import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineclient.SessionManager;
import jacz.util.concurrency.ThreadUtil;

/**
 * Synch 3
 */
public class Synch3 {

    public static void main(String[] args) throws Exception {


        SynchAction synchAction = new SynchAction("P3: ");
        PeerEngineClient peerEngineClient = SessionManager.load("./examples/configs/user_2", synchAction);
        synchAction.setPeerEngineClient(peerEngineClient);

        peerEngineClient.connect();

        ThreadUtil.safeSleep(45000);
        System.out.println("STOPPING...");
        peerEngineClient.stop();
        System.out.println("END");
    }
}
