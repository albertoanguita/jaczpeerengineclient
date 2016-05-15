package jacz.peerengineclient.common;

import jacz.peerengineservice.PeerId;

/**
 * Created by Alberto on 28/04/2016.
 */
public class TestUtil {

    public static byte[] randomBytes() {
        byte[] random = new byte[1];
        random[0] = 5;
        return random;
    }

    public static String formatPeer(PeerId peerID) {
        return "{" + peerID.toString().substring(40) + "}";
    }

    public static PeerId peerID(int b) {
        String pid = "" + b;
        while (pid.length() < 43) {
            pid = "0" + pid;
        }
        return new PeerId(pid);
    }
}
