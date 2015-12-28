package jacz.peerengineclient.data.synch;

import jacz.peerengineclient.data.FileHashDatabaseWithTimestamp;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.data_synchronization.DataAccessException;
import jacz.peerengineservice.util.data_synchronization.DataAccessor;
import jacz.peerengineservice.util.data_synchronization.SynchError;
import jacz.util.notification.ProgressNotificationWithError;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Alberto on 03/12/2015.
 */
public class FileHashDatabaseAccessor implements DataAccessor {

    private static final String NAME = "FILE_HASH_DATA_ACCESSOR";

    private static final int ELEMENTS_PER_MESSAGE = 20;

    private static final int CRC_BYTES = 2;

    private final FileHashDatabaseWithTimestamp fileHash;

    private final ProgressNotificationWithError<Integer, SynchError> progress;

    public FileHashDatabaseAccessor(FileHashDatabaseWithTimestamp fileHash, ProgressNotificationWithError<Integer, SynchError> progress) {
        this.fileHash = fileHash;
        this.progress = progress;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void beginSynchProcess(Mode mode) {
        // ignore
    }

    @Override
    public String getDatabaseID() {
        return fileHash.getId();
    }

    @Override
    public void setDatabaseID(String databaseID) {
        // ignore, cannot happen
    }

    @Override
    public Long getLastTimestamp() throws DataAccessException {
        // ignore, cannot happen
        return null;
    }

    @Override
    public List<? extends Serializable> getElementsFrom(long fromTimestamp) throws DataAccessException {
        return fileHash.getElementsFrom(fromTimestamp);
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
        // ignore, cannot happen
    }

    @Override
    public void endSynchProcess(Mode mode, boolean success) {
        // ignore
    }

    @Override
    public ProgressNotificationWithError<Integer, SynchError> getServerSynchProgress(PeerID clientPeerID) {
        return progress;
    }
}
