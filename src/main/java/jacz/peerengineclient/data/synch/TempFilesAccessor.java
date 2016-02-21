package jacz.peerengineclient.data.synch;

import jacz.peerengineclient.data.RemotePeerTempShare;
import jacz.peerengineclient.util.FileAPI;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.data_synchronization.DataAccessException;
import jacz.peerengineservice.util.data_synchronization.DataAccessor;
import jacz.peerengineservice.util.data_synchronization.SynchError;
import jacz.util.notification.ProgressNotificationWithError;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Data accessor implementation for temp files shares (as this is a very simple implementation, both client and
 * server are merged here)
 */
public class TempFilesAccessor implements DataAccessor {

    public static final String NAME = "TEMP_FILE_HASH_DATA_ACCESSOR";

    private static final int ELEMENTS_PER_MESSAGE = 20;

    private static final int CRC_BYTES = 2;

    /**
     * When in client mode, this object allows notifying about the remote peer temp share
     */
    private final RemotePeerTempShare remotePeerTempShare;

    /**
     * When in server mode, the fileAPI allows retrieving the current temp files
     */
    private final FileAPI fileAPI;

    private final ProgressNotificationWithError<Integer, SynchError> progress;

    /**
     * Constructor for client mode
     *
     * @param remotePeerTempShare
     */
    public TempFilesAccessor(RemotePeerTempShare remotePeerTempShare) {
        this.remotePeerTempShare = remotePeerTempShare;
        this.fileAPI = null;
        this.progress = null;
    }

    /**
     * Constructor for server mode
     *
     * @param fileAPI
     */
    public TempFilesAccessor(FileAPI fileAPI, ProgressNotificationWithError<Integer, SynchError> progress) {
        this.remotePeerTempShare = null;
        this.fileAPI = fileAPI;
        this.progress = progress;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void beginSynchProcess(Mode mode) {
        // notify the RemotePeerTempShare, when in client mode
        if (mode.isClient()) {
            remotePeerTempShare.startNewSynch();
        }
    }

    @Override
    public String getDatabaseID() {
        return null;
    }

    @Override
    public void setDatabaseID(String databaseID) {
        // ignore, cannot happen (we do not use id)
    }

    @Override
    public Long getLastTimestamp() throws DataAccessException {
        // since we always synch the full list of temp files, we do not need timestamp
        return null;
    }

    @Override
    public List<? extends Serializable> getElementsFrom(long fromTimestamp) throws DataAccessException {
        return new ArrayList<>(fileAPI.getTempHashes());
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
        remotePeerTempShare.addTempResource((String) element);
    }

    @Override
    public void endSynchProcess(Mode mode, boolean success) {
        // notify the RemotePeerTempShare, when in client mode
        if (mode.isClient()) {
            remotePeerTempShare.completeSynch();
        }
    }

    @Override
    public ProgressNotificationWithError<Integer, SynchError> getServerSynchProgress(PeerID clientPeerID) {
        return progress;
    }
}
