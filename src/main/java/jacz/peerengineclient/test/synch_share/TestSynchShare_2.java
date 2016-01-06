package jacz.peerengineclient.test.synch_share;

import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineclient.data.MoveFileAction;
import jacz.peerengineclient.test.Client;
import jacz.util.concurrency.ThreadUtil;

import java.io.IOException;

/**
 * Created by Alberto on 02/01/2016.
 */
public class TestSynchShare_2 {

    public static void main(String[] args) throws IOException {

        PeerEngineClient peerEngineClient = Client.loadClient("./etc/user_1");

//        peerEngineClient.addLocalFile("./etc/user_1/config/config.xml", MoveFileAction.DO_NOT_MOVE);
//        peerEngineClient.addLocalFile("./logConfig/log4j.properties", MoveFileAction.DO_NOT_MOVE);
//        peerEngineClient.addLocalFile("./etc/user_1/data/hash-db.vso", MoveFileAction.DO_NOT_MOVE);
        peerEngineClient.removeLocalFile("488EA4974F18D02935AE34772167C6CF", false);

        peerEngineClient.connect();

        ThreadUtil.safeSleep(30000);
        peerEngineClient.stop();

        System.out.println("END");
    }
}
