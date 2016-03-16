package jacz.peerengineclient.test.load_config;

import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineclient.SessionManager;
import jacz.peerengineclient.test.Client;
import jacz.util.concurrency.ThreadUtil;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;

/**
 * Created by Alberto on 24/12/2015.
 */
public class TestLoadConfig {

    public static void main(String[] args) throws IOException, XMLStreamException {

        System.out.println(SessionManager.listAvailableConfigs("./etc"));

        PeerEngineClient peerEngineClient = Client.loadClient("./etc/user_0");

        ThreadUtil.safeSleep(5000);
        peerEngineClient.stop();

        System.out.println("END");
    }
}
