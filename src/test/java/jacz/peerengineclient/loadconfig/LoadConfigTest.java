package jacz.peerengineclient.loadconfig;

import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineclient.SessionManager;
import jacz.peerengineclient.common.Client;
import jacz.util.concurrency.ThreadUtil;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by Alberto on 28/04/2016.
 */
public class LoadConfigTest {

    @Test
    public void test() throws IOException {

        System.out.println(SessionManager.listAvailableConfigs("./etc"));

        PeerEngineClient peerEngineClient = Client.loadClient("./etc/user_0");

        ThreadUtil.safeSleep(5000);
        peerEngineClient.stop();

        System.out.println("END");
    }
}
