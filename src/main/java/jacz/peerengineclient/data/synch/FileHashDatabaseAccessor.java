package jacz.peerengineclient.data.synch;

import jacz.peerengineclient.data.FileHashDatabaseWithTimestamp;
import jacz.peerengineclient.data.SerializedHashItem;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.util.data_synchronization.DataAccessException;
import jacz.peerengineservice.util.data_synchronization.DataAccessor;
import jacz.peerengineservice.util.data_synchronization.SynchError;
import jacz.util.notification.ProgressNotificationWithError;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Accessor for the file hash database (with timestamp). Works in server mode only
 * <p>
 * What resources we share with our friend peers
 */
public class FileHashDatabaseAccessor implements DataAccessor {

    public static final String NAME = "FILE_HASH_DATA_ACCESSOR";

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
        List<SerializedHashItem> items = fileHash.getHashesFrom(fromTimestamp);
        Collections.sort(items);
        return items;
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
    public ProgressNotificationWithError<Integer, SynchError> getServerSynchProgress(PeerId clientPeerId) {
        return progress;
    }
}
