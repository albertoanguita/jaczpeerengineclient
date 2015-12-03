package jacz.peerengineclient.peer_share;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.data_synchronization.DataAccessException;
import jacz.peerengineservice.util.data_synchronization.DataAccessor;
import jacz.peerengineservice.util.data_synchronization.ServerSynchRequestAnswer;
import jacz.util.maps.DoubleMap;

/**
 * Created by Alberto on 03/12/2015.
 */
public abstract class FileHashDatabaseSynch implements DataAccessor {

    private static final int ELEMENTS_PER_MESSAGE = 20;

    private static final int CRC_BYTES = 2;

    protected final DoubleMap<Integer, String> activeHashes;

    private String id;

    public FileHashDatabaseSynch(DoubleMap<Integer, String> activeHashes, String id) {
        this.activeHashes = activeHashes;
        this.id = id;
    }

    @Override
    public void beginSynchProcess(Mode mode) {
        // ignore
    }

    @Override
    public String getDatabaseID() {
        return id;
    }

    @Override
    public void setDatabaseID(String databaseID) {
        id = databaseID;
    }

    @Override
    public int elementsPerMessage() {
        return ELEMENTS_PER_MESSAGE;
    }

    @Override
    public int CRCBytes() {
        return CRC_BYTES;
    }

    @Override
    public void setElement(Object element) throws DataAccessException {
        SerializedHashItem item = (SerializedHashItem) element;
        if (item.alive) {
            // add to active hashes
            activeHashes.put(item.timestamp, item.hash);
        } else {
            // delete from active hashes
            activeHashes.removeReverse(item.hash);
        }
    }

    @Override
    public void endSynchProcess(Mode mode, boolean success) {
        // ignore
    }

    @Override
    public ServerSynchRequestAnswer initiateListSynchronizationAsServer(PeerID clientPeerID) {
        // todo
        return null;
    }
}
