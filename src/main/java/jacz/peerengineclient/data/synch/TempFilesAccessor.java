package jacz.peerengineclient.data.synch;

import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineclient.data.RemotePeerTempShare;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.data_synchronization.DataAccessException;
import jacz.peerengineservice.util.data_synchronization.DataAccessor;
import jacz.peerengineservice.util.data_synchronization.SynchError;
import jacz.peerengineservice.util.datatransfer.master.DownloadManager;
import jacz.util.notification.ProgressNotificationWithError;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Data accessor implementation for tempo files shares (as this is a very simple implementation, both client and
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
     * When in server mode, the peer engine client allows retrieving the current temp files
     */
    private final PeerEngineClient peerEngineClient;

    /**
     * Constructor for client mode
     *
     * @param remotePeerTempShare
     */
    public TempFilesAccessor(RemotePeerTempShare remotePeerTempShare) {
        this.remotePeerTempShare = remotePeerTempShare;
        this.peerEngineClient = null;
    }

    /**
     * Constructor for server mode
     *
     * @param peerEngineClient
     */
    public TempFilesAccessor(PeerEngineClient peerEngineClient) {
        this.remotePeerTempShare = null;
        this.peerEngineClient = peerEngineClient;
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
        List<String> tempHashes = new ArrayList<>();
        for (DownloadManager downloadManager : peerEngineClient.getPeerClient().getAllDownloads()) {
            tempHashes.add(downloadManager.getResourceID());
        }
        return tempHashes;
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
            remotePeerTempShare.startNewSynch();
        }
    }

    @Override
    public ProgressNotificationWithError<Integer, SynchError> getServerSynchProgress(PeerID clientPeerID) {
        return null;
    }
}
