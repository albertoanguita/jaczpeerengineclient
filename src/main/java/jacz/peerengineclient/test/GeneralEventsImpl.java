package jacz.peerengineclient.test;

import jacz.commengine.communication.CommError;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.client.GeneralEvents;
import jacz.peerengineservice.client.PeerRelations;
import jacz.peerengineservice.util.ConnectionStatus;

/**
 * Created by Alberto on 24/12/2015.
 */
public class GeneralEventsImpl implements GeneralEvents {
    
    @Override
    public void peerAddedAsFriend(PeerID peerID, PeerRelations peerRelations) {
        System.out.println("peer added as friend: " + formatPeer(peerID));
    }

    @Override
    public void peerRemovedAsFriend(PeerID peerID, PeerRelations peerRelations) {
        System.out.println("peer removed as friend: " + formatPeer(peerID));
    }

    @Override
    public void peerAddedAsBlocked(PeerID peerID, PeerRelations peerRelations) {
        System.out.println("peer added as blocked: " + formatPeer(peerID));
    }

    @Override
    public void peerRemovedAsBlocked(PeerID peerID, PeerRelations peerRelations) {
        System.out.println("peer removed as blocked: " + formatPeer(peerID));
    }

    @Override
    public void newPeerConnected(PeerID peerID, ConnectionStatus status) {
        System.out.println("New peer connected: " + formatPeer(peerID) + ", " + status);
    }

    @Override
    public void newObjectMessage(PeerID peerID, Object message) {
        System.out.println("New object message from " + formatPeer(peerID) + ": " + message);
    }

    @Override
    public void newPeerNick(PeerID peerID, String nick) {
        System.out.println("Peer " + formatPeer(peerID) + " changed his nick to " + nick);
    }

    @Override
    public void peerValidatedUs(PeerID peerID) {
        System.out.println("Peer " + formatPeer(peerID) + " has validated us");
    }

    @Override
    public void peerDisconnected(PeerID peerID, CommError error) {
        System.out.println("Peer disconnected (" + formatPeer(peerID) + "). Error = " + error);
    }

    @Override
    public void stop() {
        System.out.println("Stop");
    }

    private String formatPeer(PeerID peerID) {
        return "{" + peerID.toString().substring(40) + "}";
    }
}
