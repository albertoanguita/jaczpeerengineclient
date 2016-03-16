package jacz.peerengineclient.test;

import jacz.commengine.communication.CommError;
import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.GeneralEvents;
import jacz.peerengineservice.client.PeerRelations;
import jacz.peerengineservice.util.ConnectionStatus;

/**
 * Created by Alberto on 24/12/2015.
 */
public class GeneralEventsImpl implements GeneralEvents {

    private PeerEngineClient peerEngineClient;

    public void setPeerEngineClient(PeerEngineClient peerEngineClient) {
        this.peerEngineClient = peerEngineClient;
    }

    @Override
    public void peerAddedAsFriend(PeerId peerID, PeerRelations peerRelations) {
        System.out.println("peer added as friend: " + TestUtil.formatPeer(peerID));
    }

    @Override
    public void peerRemovedAsFriend(PeerId peerID, PeerRelations peerRelations) {
        System.out.println("peer removed as friend: " + TestUtil.formatPeer(peerID));
    }

    @Override
    public void peerAddedAsBlocked(PeerId peerID, PeerRelations peerRelations) {
        System.out.println("peer added as blocked: " + TestUtil.formatPeer(peerID));
    }

    @Override
    public void peerRemovedAsBlocked(PeerId peerID, PeerRelations peerRelations) {
        System.out.println("peer removed as blocked: " + TestUtil.formatPeer(peerID));
    }

    @Override
    public void newPeerConnected(PeerId peerID, ConnectionStatus status) {
        System.out.println("New peer connected: " + TestUtil.formatPeer(peerID) + ", " + status);
        if (status == ConnectionStatus.UNVALIDATED) {
            peerEngineClient.addFriendPeer(peerID);
        }
    }

    @Override
    public void newObjectMessage(PeerId peerID, Object message) {
        System.out.println("New object message from " + TestUtil.formatPeer(peerID) + ": " + message);
    }

    @Override
    public void newPeerNick(PeerId peerID, String nick) {
        System.out.println("Peer " + TestUtil.formatPeer(peerID) + " changed his nick to " + nick);
    }

    @Override
    public void peerValidatedUs(PeerId peerID) {
        System.out.println("Peer " + TestUtil.formatPeer(peerID) + " has validated us");
    }

    @Override
    public void peerDisconnected(PeerId peerID, CommError error) {
        System.out.println("Peer disconnected (" + TestUtil.formatPeer(peerID) + "). Error = " + error);
    }

    @Override
    public void stop() {
        System.out.println("Stop");
    }
}
