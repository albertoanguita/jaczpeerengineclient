package jacz.peerengineclient.libraries.library_images;

import jacz.peerengineservice.PeerID;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * A database of a remote peer
 */
public class RemoteDatabase extends LocalDatabase {

    /**
     * The peer to which this remote store belongs to
     */
    private PeerID remotePeerID;

    public RemoteDatabase(String databasePath, PeerID remotePeerID) {
        super(databasePath);
        this.remotePeerID = remotePeerID;
    }

    public RemoteDatabase(String databasePath, HashMap<LibraryId, LibraryId> itemsToIntegratedItems, PeerID remotePeerID) {
        super(databasePath, itemsToIntegratedItems);
        this.remotePeerID = remotePeerID;
    }
}
