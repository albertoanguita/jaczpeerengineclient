package jacz.peerengineclient.dbs.data_synch;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.data_synchronization.premade_lists.old.BasicListContainer;

/**
 * Created with IntelliJ IDEA.
 * User: Alberto
 * Date: 16/06/14
 * Time: 13:00
 * To change this template use File | Settings | File Templates.
 */
public class PeerListContainer {

    private final BasicListContainer basicListContainer;

    public PeerListContainer() {
        basicListContainer = new BasicListContainer();
    }

    public BasicListContainer getBasicListContainer() {
        return basicListContainer;
    }

    public void peerConnected(PeerID peerID) {

    }

    public void peerDisconnected(PeerID peerID) {

    }
}
