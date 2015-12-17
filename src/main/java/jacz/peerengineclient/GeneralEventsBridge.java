package jacz.peerengineclient;

import jacz.commengine.communication.CommError;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.client.GeneralEvents;
import jacz.peerengineservice.client.PeerRelations;
import jacz.peerengineservice.util.ConnectionStatus;

/**
 * Created by Alberto on 10/12/2015.
 */
public class GeneralEventsBridge implements GeneralEvents {

    private final PeerEngineClient peerEngineClient;

    private final GeneralEvents generalEvents;

    public GeneralEventsBridge(PeerEngineClient peerEngineClient, GeneralEvents generalEvents) {
        this.peerEngineClient = peerEngineClient;
        this.generalEvents = generalEvents;
    }

    @Override
    public void peerAddedAsFriend(PeerID peerID, PeerRelations peerRelations) {
        peerEngineClient.peerIsNowFriend(peerID);
    }

    @Override
    public void peerRemovedAsFriend(PeerID peerID, PeerRelations peerRelations) {
        peerEngineClient.peerIsNoLongerFriend(peerID);
    }

    @Override
    public void peerAddedAsBlocked(PeerID peerID, PeerRelations peerRelations) {

    }

    @Override
    public void peerRemovedAsBlocked(PeerID peerID, PeerRelations peerRelations) {

    }

    @Override
    public void newPeerConnected(PeerID peerID, ConnectionStatus status) {
        peerEngineClient.newPeerConnected(peerID);
    }

    @Override
    public void newObjectMessage(PeerID peerID, Object message) {

    }

    @Override
    public void newPeerNick(PeerID peerID, String nick) {

    }

    @Override
    public void peerValidatedUs(PeerID peerID) {

    }

    @Override
    public void peerDisconnected(PeerID peerID, CommError error) {

    }

    @Override
    public void stop() {

    }
}
