package jacz.peerengineclient.test;

import jacz.peerengineservice.PeerID;

/**
 * Created by Alberto on 24/12/2015.
 */
public class TestUtil {

    public static byte[] randomBytes() {
        byte[] random = new byte[1];
        random[0] = 5;
        return random;
    }

    public static String formatPeer(PeerID peerID) {
        return "{" + peerID.toString().substring(40) + "}";
    }

}
