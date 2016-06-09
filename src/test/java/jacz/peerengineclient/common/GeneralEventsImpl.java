package jacz.peerengineclient.common;

import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.GeneralEvents;

/**
 * Created by Alberto on 28/04/2016.
 */
public class GeneralEventsImpl implements GeneralEvents {

    @Override
    public void newOwnNick(String nick) {
        System.out.println("New own nick: " + nick);
    }

    @Override
    public void newObjectMessage(PeerId peerID, Object message) {
        System.out.println("New object message from " + TestUtil.formatPeer(peerID) + ": " + message);
    }

    @Override
    public void stop() {
        System.out.println("Stop");
    }
}
