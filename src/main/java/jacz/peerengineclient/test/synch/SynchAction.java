package jacz.peerengineclient.test.synch;

import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.ConnectionStatus;
import jacz.peerengineclient.test.SimpleJacuzziPeerClientAction;

/**
 *
 */
public class SynchAction extends SimpleJacuzziPeerClientAction {

    private PeerEngineClient peerEngineClient;

    public SynchAction(String initMessage) {
        super(initMessage);
    }

    public void setPeerEngineClient(PeerEngineClient peerEngineClient) {
        this.peerEngineClient = peerEngineClient;
    }

    @Override
    public void newPeerConnected(PeerID peerID, ConnectionStatus status) {
        super.newPeerConnected(peerID, status);
//        if (status == ConnectionStatus.CORRECT) {
//            // synch person db
//            peerEngineClient.synchAllPeerLibraries(peerID);
//        }
    }
}
