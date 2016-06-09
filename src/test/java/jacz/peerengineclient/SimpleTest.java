package jacz.peerengineclient;

import jacz.peerengineclient.common.Client;
import jacz.peerengineservice.PeerId;
import jacz.util.concurrency.ThreadUtil;

import java.io.IOException;

/**
 * Created by alberto on 6/2/16.
 */
public class SimpleTest {

    public static void main(String[] args) {
        try {
            PeerEngineClient peerEngineClient = Client.loadClient("./etc/user_0");
            peerEngineClient.addFavoritePeer(new PeerId("Yi8j6ITdv3UP0oP6tKKhXj5-x97_3l_oEDvabmfDyN8"));
            peerEngineClient.connect();
            ThreadUtil.safeSleep(35000);
            peerEngineClient.stop();





//            PeerEngineClient client;
//            Duple<PeerEngineClient, List<String>> duple = SessionManager.load(
//                    "./etc/user_0",
//                    null,
//                    null,
//                    null,
//                    null,
//                    null,
//                    null,
//                    null,
//                    null,
//                    null);
//            client = duple.element1;
//
//            ThreadUtil.safeSleep(5000);
//            client.stop();
            System.out.println("client stopped!");

            /*while (true) {
                System.out.println(ThreadExecutor.getRegisteredClients());
                ThreadUtil.safeSleep(1000);
            }*/

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
