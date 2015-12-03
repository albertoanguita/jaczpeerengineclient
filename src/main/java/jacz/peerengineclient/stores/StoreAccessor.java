package jacz.peerengineclient.stores;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.data_synchronization.DataAccessException;
import jacz.peerengineservice.util.data_synchronization.DataAccessor;
import jacz.peerengineservice.util.data_synchronization.ServerSynchRequestAnswer;

import java.io.Serializable;
import java.util.List;

/**
 * Accessor implementation for data stores
 */
public class StoreAccessor implements DataAccessor {

    private static final int ELEMENTS_PER_MESSAGE = 10;

    private static final int CRC_BYTES = 4;

    @Override
    public void beginSynchProcess(Mode mode) {
        // ignore
    }

    @Override
    public String getDatabaseID() {
        // todo get metadata id
        return null;
    }

    @Override
    public void setDatabaseID(String databaseID) {
        // todo set metadata id
    }

    @Override
    public Integer getLastTimestamp() throws DataAccessException {
        // todo in client get stored last timestamp
        return null;
    }

    @Override
    public List<? extends Serializable> getElementsFrom(int fromTimestamp) throws DataAccessException {
        // todo ask server to collect elements from timestamp, order them by timestamp
        return null;
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
        SerializedItem item = (SerializedItem) element;
        if (!item.isAlive()) {
            // delete item
            // todo
        } else {
            switch (item.getType()) {
                case MOVIE:
                    break;
                case TV_SERIES:
                    break;
                case CHAPTER:
                    break;
                case PERSON:
                    break;
                case COMPANY:
                    break;
                case VIDEO_FILE:
                    break;
                case SUBTITLE_FILE:
                    break;
                case IMAGE_FILE:
                    break;
            }
        }
        // todo write item to db
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
