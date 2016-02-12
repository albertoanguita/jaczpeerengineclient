package jacz.peerengineclient.test.synch_share;

import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineclient.test.Client;
import jacz.util.concurrency.ThreadUtil;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;

/**
 * Created by Alberto on 02/01/2016.
 */
public class TestSynchShare_1 {

    public static void main(String[] args) throws IOException, XMLStreamException {

        PeerEngineClient peerEngineClient = Client.loadClient("./etc/user_0");
        peerEngineClient.connect();

        ThreadUtil.safeSleep(25000);
        peerEngineClient.stop();

        System.out.println("END");
    }
}
