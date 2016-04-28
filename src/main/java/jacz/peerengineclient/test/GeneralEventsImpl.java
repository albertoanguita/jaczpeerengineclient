package jacz.peerengineclient.test;

import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.GeneralEvents;

/**
 * Created by Alberto on 24/12/2015.
 */
public class GeneralEventsImpl implements GeneralEvents {

    @Override
    public void newObjectMessage(PeerId peerID, Object message) {
        System.out.println("New object message from " + TestUtil.formatPeer(peerID) + ": " + message);
    }

    @Override
    public void stop() {
        System.out.println("Stop");
    }
}
