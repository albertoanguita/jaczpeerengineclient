package jacz.peerengineclient.dbs;

import jacz.store.Database;
import jacz.peerengineservice.PeerID;

import java.util.HashMap;

/**
 * A database of a remote peer
 */
public class RemoteDatabase extends LocalDatabase {

    private final PeerID remotePeerID;

    public RemoteDatabase(Database database, PeerID remotePeerID) {
        super(database);
        this.remotePeerID = remotePeerID;
    }

    public RemoteDatabase(Database database, HashMap<String, String> itemsToIntegratedItems, PeerID remotePeerID) {
        super(database, itemsToIntegratedItems);
        this.remotePeerID = remotePeerID;
    }
}
