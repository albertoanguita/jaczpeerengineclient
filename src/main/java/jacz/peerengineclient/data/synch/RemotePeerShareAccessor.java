package jacz.peerengineclient.data.synch;

import jacz.peerengineclient.data.RemotePeerShare;
import jacz.peerengineclient.data.SerializedHashItem;
import jacz.peerengineclient.images.ImageDownloader;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.util.data_synchronization.DataAccessException;
import jacz.peerengineservice.util.data_synchronization.DataAccessor;
import jacz.peerengineservice.util.data_synchronization.SynchError;
import jacz.util.notification.ProgressNotificationWithError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.List;

/**
 * Accessor (client) for updating the resource share of our friend peers (resources that they share with us)
 */
public class RemotePeerShareAccessor implements DataAccessor {

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
    public ProgressNotificationWithError<Integer, SynchError> getServerSynchProgress(PeerId clientPeerId) {
        // ignore, cannot happen
        return null;
    }
}
