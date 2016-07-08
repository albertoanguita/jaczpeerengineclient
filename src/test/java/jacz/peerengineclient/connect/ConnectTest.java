package jacz.peerengineclient.connect;

import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineclient.common.Client;
import jacz.peerengineclient.common.TestUtil;
import org.aanguita.jacuzzi.concurrency.ThreadUtil;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by Alberto on 28/04/2016.
 */
public class ConnectTest {

    private static final long WARM_UP = 20000;

    private static final long CYCLE_LENGTH = 5000;

    @Test
    public void connect1() throws IOException {

        PeerEngineClient peerEngineClient = Client.loadClient("./etc/user_0");
        peerEngineClient.addFavoritePeer(TestUtil.peerID(2));
        peerEngineClient.connect();

        ThreadUtil.safeSleep(WARM_UP);
        Assert.assertTrue(peerEngineClient.isConnectedPeer(TestUtil.peerID(2)));

        ThreadUtil.safeSleep(CYCLE_LENGTH);

        System.out.println("stopping...");
        peerEngineClient.stop();

        System.out.println("END");
    }

    @Test
    public void connect2() throws IOException {

        PeerEngineClient peerEngineClient = Client.loadClient("./etc/user_1");
        peerEngineClient.addFavoritePeer(TestUtil.peerID(1));
        peerEngineClient.connect();

        ThreadUtil.safeSleep(WARM_UP);
        Assert.assertTrue(peerEngineClient.isConnectedPeer(TestUtil.peerID(1)));

        ThreadUtil.safeSleep(CYCLE_LENGTH);

        System.out.println("stopping...");
        peerEngineClient.stop();

        System.out.println("END");
    }
}
