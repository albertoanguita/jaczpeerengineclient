package jacz.peerengineclient;

import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.connection.peers.PeerInfo;
import jacz.peerengineservice.client.connection.peers.PeersEvents;
import org.aanguita.jtcpserver.communication.CommError;

/**
 * This class captures connections and disconnections of peers
 */
public class PeersEventsBridge implements PeersEvents {

    private final PeerEngineClient peerEngineClient;

    private final PeersEvents peersEvents;

    public PeersEventsBridge(PeerEngineClient peerEngineClient, PeersEvents peersEvents) {
        this.peerEngineClient = peerEngineClient;
        this.peersEvents = peersEvents;
    }

    @Override
    public void newPeerConnected(PeerId peerId, PeerInfo peerInfo) {
        peerEngineClient.peerConnected(peerId);
        peersEvents.newPeerConnected(peerId, peerInfo);
    }

    @Override
    public void modifiedPeerRelationship(PeerId peerId, PeerInfo peerInfo) {
        peersEvents.modifiedPeerRelationship(peerId, peerInfo);
    }

    @Override
    public void modifiedMainCountry(PeerId peerId, PeerInfo peerInfo) {
        peersEvents.modifiedMainCountry(peerId, peerInfo);
    }

    @Override
    public void modifiedAffinity(PeerId peerId, PeerInfo peerInfo) {
        peersEvents.modifiedAffinity(peerId, peerInfo);
    }

    @Override
    public void newPeerNick(PeerId peerId, PeerInfo peerInfo) {
        peersEvents.newPeerNick(peerId, peerInfo);
    }

    @Override
    public void peerDisconnected(PeerId peerId, PeerInfo peerInfo, CommError error) {
        peerEngineClient.peerDisconnected(peerId);
        peersEvents.peerDisconnected(peerId, peerInfo, error);
    }
}
