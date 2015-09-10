package jacz.peerengineclient.test.synch;

import jacz.peerengineclient.JPeerEngineClient;
import jacz.peerengineclient.SessionManager;
import jacz.util.concurrency.ThreadUtil;

/**
 * Synch 1
 */
public class Synch1 {

    public static void main(String[] args) throws Exception {

        SynchAction synchAction = new SynchAction("P1: ");
        JPeerEngineClient jPeerEngineClient = SessionManager.load("./examples/configs/user_0", synchAction);
        synchAction.setjPeerEngineClient(jPeerEngineClient);

        jPeerEngineClient.connect();

        ThreadUtil.safeSleep(45000);
        System.out.println("STOPPING...");
        jPeerEngineClient.stop();
        System.out.println("END");
    }

}
