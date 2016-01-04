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
        generalEvents.peerAddedAsFriend(peerID, peerRelations);
    }

    @Override
    public void peerRemovedAsFriend(PeerID peerID, PeerRelations peerRelations) {
        peerEngineClient.peerIsNoLongerFriend(peerID);
        generalEvents.peerRemovedAsFriend(peerID, peerRelations);
    }

    @Override
    public void peerAddedAsBlocked(PeerID peerID, PeerRelations peerRelations) {
        peerEngineClient.peerIsNoLongerFriend(peerID);
        generalEvents.peerAddedAsBlocked(peerID, peerRelations);
    }

    @Override
    public void peerRemovedAsBlocked(PeerID peerID, PeerRelations peerRelations) {
        generalEvents.peerRemovedAsBlocked(peerID, peerRelations);
    }

    @Override
    public void newPeerConnected(PeerID peerID, ConnectionStatus status) {
        peerEngineClient.peerConnected(peerID);
        generalEvents.newPeerConnected(peerID, status);
    }

    @Override
    public void newObjectMessage(PeerID peerID, Object message) {
        generalEvents.newObjectMessage(peerID, message);
    }

    @Override
    public void newPeerNick(PeerID peerID, String nick) {
        generalEvents.newPeerNick(peerID, nick);
    }

    @Override
    public void peerValidatedUs(PeerID peerID) {
        generalEvents.peerValidatedUs(peerID);
    }

    @Override
    public void peerDisconnected(PeerID peerID, CommError error) {
        peerEngineClient.peerDisconnected(peerID);
        generalEvents.peerDisconnected(peerID, error);
    }

    @Override
    public void stop() {
        generalEvents.stop();
    }
}
