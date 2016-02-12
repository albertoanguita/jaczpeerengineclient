package jacz.peerengineclient.test.synch_db;

import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineclient.SessionManager;
import jacz.peerengineclient.test.*;
import jacz.peerengineservice.test.ResourceTransferEventsImpl;
import jacz.peerengineservice.test.TempFileManagerEventsImpl;
import jacz.util.concurrency.ThreadUtil;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;

/**
 * Created by Alberto on 27/12/2015.
 */
public class TestSynch_1 {

    public static void main(String[] args) throws IOException, XMLStreamException {

        PeerEngineClient peerEngineClient = Client.loadClient("./etc/user_0");
        peerEngineClient.connect();

        ThreadUtil.safeSleep(50000);
        peerEngineClient.stop();

        System.out.println("END");
    }
}
