package jacz.peerengineclient.stores;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.data_synchronization.DataAccessException;
import jacz.peerengineservice.util.data_synchronization.DataAccessor;
import jacz.peerengineservice.util.data_synchronization.ServerSynchRequestAnswer;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Alberto on 28/11/2015.
 */
public class StoreAccessor implements DataAccessor {

    @Override
    public void beginSynchProcess(Mode mode) {
        // ignore
    }

    @Override
    public String getDatabaseID() {
        return null;
    }

    @Override
    public void setDatabaseID(String databaseID) {
        // todo
    }

    @Override
    public Integer getLastTimestamp() throws DataAccessException {
        // todo
        return null;
    }

    @Override
    public List<? extends Serializable> getElementsFrom(int fromTimestamp) throws DataAccessException {
        // todo
        return null;
    }

    @Override
    public int elementsPerMessage() {
        return 10;
    }

    @Override
    public int CRCBytes() {
        return 4;
    }

    @Override
    public void setElement(Object element) throws DataAccessException {
        SerializedItem item = (SerializedItem) element;
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
