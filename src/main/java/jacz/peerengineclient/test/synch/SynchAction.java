package jacz.peerengineclient.test.synch;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.ConnectionStatus;
import jacz.peerengineclient.JPeerEngineClient;
import jacz.peerengineclient.test.SimpleJacuzziPeerClientAction;

/**
 *
 */
public class SynchAction extends SimpleJacuzziPeerClientAction {

    private JPeerEngineClient jPeerEngineClient;

    public SynchAction(String initMessage) {
        super(initMessage);
    }

    public void setjPeerEngineClient(JPeerEngineClient jPeerEngineClient) {
        this.jPeerEngineClient = jPeerEngineClient;
    }

    @Override
    public void newPeerConnected(PeerID peerID, ConnectionStatus status) {
        super.newPeerConnected(peerID, status);
//        if (status == ConnectionStatus.CORRECT) {
//            // synch person db
//            jPeerEngineClient.synchAllPeerLibraries(peerID);
//        }
    }
}
