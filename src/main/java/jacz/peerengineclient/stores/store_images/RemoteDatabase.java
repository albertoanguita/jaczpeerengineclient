package jacz.peerengineclient.stores.store_images;

import jacz.peerengineservice.PeerID;
import jacz.store.Database;

import java.util.HashMap;

/**
 * A database of a remote peer
 */
public class RemoteDatabase extends LocalDatabase {

    /**
     * The peer to which this remote store belongs to
     */
    private final PeerID remotePeerID;

    public RemoteDatabase(Database database, PeerID remotePeerID) {
        this(database, new HashMap<String, String>(), remotePeerID);
    }

    public RemoteDatabase(Database database, HashMap<String, String> itemsToIntegratedItems, PeerID remotePeerID) {
        super(database, itemsToIntegratedItems);
        this.remotePeerID = remotePeerID;
    }
}
