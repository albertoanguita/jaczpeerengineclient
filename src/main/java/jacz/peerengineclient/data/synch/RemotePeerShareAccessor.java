package jacz.peerengineclient.data.synch;

import jacz.peerengineclient.data.RemotePeerShare;
import jacz.peerengineclient.data.SerializedHashItem;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.data_synchronization.DataAccessException;
import jacz.peerengineservice.util.data_synchronization.DataAccessor;
import jacz.peerengineservice.util.data_synchronization.SynchError;
import jacz.util.notification.ProgressNotificationWithError;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Alberto on 22/12/2015.
 */
public class RemotePeerShareAccessor implements DataAccessor {

//    private static final int ELEMENTS_PER_MESSAGE = 20;
//
//    private static final int CRC_BYTES = 2;

    private final RemotePeerShare remotePeerShare;

    public RemotePeerShareAccessor(RemotePeerShare remotePeerShare) {
        this.remotePeerShare = remotePeerShare;
    }

    @Override
    public String getName() {
        return FileHashDatabaseAccessor.NAME;
    }

    @Override
    public void beginSynchProcess(Mode mode) {
        // ignore
    }

    @Override
    public String getDatabaseID() {
        return remotePeerShare.getId();
    }

    @Override
    public void setDatabaseID(String databaseID) {
        remotePeerShare.setId(databaseID);
    }

    @Override
    public Long getLastTimestamp() throws DataAccessException {
        return remotePeerShare.getMaxStoredTimestamp();
    }

    @Override
    public List<? extends Serializable> getElementsFrom(long fromTimestamp) throws DataAccessException {
        // ignore, cannot happen
        return null;
    }

    @Override
    public int elementsPerMessage() {
        // ignore
        return 0;
    }

    @Override
    public int CRCBytes() {
        // ignore
        return 0;
    }

    @Override
    public void setElement(Object element) throws DataAccessException {
        SerializedHashItem item = (SerializedHashItem) element;
        if (item.alive) {
            System.out.println("New item: " + item.hash);
            remotePeerShare.addHash(item.timestamp, item.hash);
        } else {
            remotePeerShare.removeHash(item.timestamp, item.hash);
        }
    }

    @Override
    public void endSynchProcess(Mode mode, boolean success) {
        // ignore
    }

    @Override
    public ProgressNotificationWithError<Integer, SynchError> getServerSynchProgress(PeerID clientPeerID) {
        // ignore, cannot happen
        return null;
    }
}
