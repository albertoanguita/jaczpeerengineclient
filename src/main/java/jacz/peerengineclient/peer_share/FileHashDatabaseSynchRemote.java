package jacz.peerengineclient.peer_share;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.data_synchronization.DataAccessException;
import jacz.util.maps.DoubleMap;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Alberto on 03/12/2015.
 */
public class FileHashDatabaseSynchRemote extends FileHashDatabaseSynch {

    private PeerID remotePeerID;

    private Integer maxStoredTimestamp;

    public FileHashDatabaseSynchRemote(DoubleMap<Integer, String> activeHashes, String id) {
        super(activeHashes, id);
    }

    @Override
    public Integer getLastTimestamp() throws DataAccessException {
        return maxStoredTimestamp;
    }

    @Override
    public List<? extends Serializable> getElementsFrom(int fromTimestamp) throws DataAccessException {
        // cannot happen in remote
        return null;
    }

    @Override
    public void setElement(Object element) throws DataAccessException {
        super.setElement(element);
        SerializedHashItem item = (SerializedHashItem) element;
        // after adding or removing the item, update our maxStoredTimestamp
        maxStoredTimestamp = item.timestamp > maxStoredTimestamp ? item.timestamp : maxStoredTimestamp;
    }
}
