package jacz.peerengineclient.data;

import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineclient.data.synch.FileHashDatabaseAccessor;
import jacz.peerengineclient.data.synch.RemotePeerShareAccessor;
import jacz.peerengineclient.data.synch.TempFilesAccessor;
import jacz.peerengineclient.file_system.PathConstants;
import jacz.peerengineclient.util.synch.DataAccessorController;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.UnavailablePeerException;
import jacz.peerengineservice.client.PeerClient;
import jacz.peerengineservice.util.data_synchronization.DummyProgress;
import jacz.peerengineservice.util.data_synchronization.ServerBusyException;
import jacz.peerengineservice.util.data_synchronization.SynchError;
import jacz.util.notification.ProgressNotificationWithError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by Alberto on 13/12/2015.
 */
public class PeerShareManager {

    private static class FileHashDataAccessorController extends DataAccessorController<FileHashDatabaseAccessor, RemotePeerShareAccessor> {

        private final FileHashDatabaseWithTimestamp fileHash;

        private final Map<PeerId, RemotePeerShare> remotePeerShares;

        public FileHashDataAccessorController(
                PeerEngineClient peerEngineClient,
                FileHashDatabaseWithTimestamp fileHash,
                Map<PeerId, RemotePeerShare> remotePeerShares) {
            super(RECENTLY_THRESHOLD, LARGE_SHARED_SYNCH_COUNT, VERY_LARGE_SHARED_SYNCH_COUNT, SYNCH_TIMEOUT, MAX_SHARE_SYNCH_TASKS, peerEngineClient, "FileHashSynch");
            this.fileHash = fileHash;
            this.remotePeerShares = remotePeerShares;
        }

        @Override
        public ProgressNotificationWithError<Integer, SynchError> getLocalSynchProgress(PeerId peerID) {
            return new DummyProgress();
        }

        @Override
        public FileHashDatabaseAccessor getLocalDataAccessor(PeerId peerID, ProgressNotificationWithError<Integer, SynchError> progress) {
            return new FileHashDatabaseAccessor(fileHash, progress);
        }

        @Override
        public ProgressNotificationWithError<Integer, SynchError> getRemoteSynchProgress(PeerId peerID) throws UnavailablePeerException {
            return new DummyProgress();
        }

        @Override
        public RemotePeerShareAccessor getRemoteDataAccessor(PeerId peerID) throws UnavailablePeerException {
            RemotePeerShare remotePeerShare = remotePeerShares.get(peerID);
            if (remotePeerShare != null) {
                return new RemotePeerShareAccessor(remotePeerShare);
            } else {
                // remote share not stored for this peer!!!
                throw new UnavailablePeerException();
            }
        }
    }

    private static class TempFilesDataAccessorController extends DataAccessorController<TempFilesAccessor, TempFilesAccessor> {

        private final ForeignShares foreignShares;

        public TempFilesDataAccessorController(
                PeerEngineClient peerEngineClient,
                ForeignShares foreignShares) {
            super(RECENTLY_THRESHOLD, LARGE_SHARED_SYNCH_COUNT, VERY_LARGE_SHARED_SYNCH_COUNT, SYNCH_TIMEOUT, MAX_SHARE_SYNCH_TASKS, peerEngineClient, "TempFilesSynch");
            this.foreignShares = foreignShares;
        }

        @Override
        public ProgressNotificationWithError<Integer, SynchError> getLocalSynchProgress(PeerId peerID) {
            return new DummyProgress();
        }

        @Override
        public TempFilesAccessor getLocalDataAccessor(PeerId peerID, ProgressNotificationWithError<Integer, SynchError> progress) {
            return new TempFilesAccessor(peerEngineClient.getFileAPI(), progress);
        }

        @Override
        public ProgressNotificationWithError<Integer, SynchError> getRemoteSynchProgress(PeerId peerID) throws UnavailablePeerException {
            return new DummyProgress();
        }

        @Override
        public TempFilesAccessor getRemoteDataAccessor(PeerId peerID) throws UnavailablePeerException {
            return new TempFilesAccessor(new RemotePeerTempShare(peerID, foreignShares));
        }
    }

    private final static Logger logger = LoggerFactory.getLogger(PeerShareManager.class);

    private static final long RECENTLY_THRESHOLD = 15000;

    private static final int LARGE_SHARED_SYNCH_COUNT = 10;

    private static final int VERY_LARGE_SHARED_SYNCH_COUNT = 20;

    private static final long SYNCH_TIMEOUT = 10000L;

    private static final int MAX_SHARE_SYNCH_TASKS = 20;

    private final PeerEngineClient peerEngineClient;

    private final FileHashDatabaseWithTimestamp fileHash;

    private ForeignShares foreignShares;

    private final Map<PeerId, RemotePeerShare> remotePeerShares;

    private final FileHashDataAccessorController fileHashDataAccessorController;

    private TempFilesDataAccessorController tempFilesDataAccessorController;

    public PeerShareManager(
            PeerEngineClient peerEngineClient,
            FileHashDatabaseWithTimestamp fileHash) {
        this.peerEngineClient = peerEngineClient;
        this.fileHash = fileHash;
        this.foreignShares = null;
        this.remotePeerShares = new HashMap<>();
        fileHashDataAccessorController = new FileHashDataAccessorController(peerEngineClient, fileHash, remotePeerShares);
    }

    public void setPeerClient(PeerClient peerClient) {
        this.foreignShares = new ForeignShares(peerClient);
        this.tempFilesDataAccessorController = new TempFilesDataAccessorController(peerEngineClient, foreignShares);
    }

    public FileHashDatabaseWithTimestamp getFileHash() {
        return fileHash;
    }

    public synchronized Set<Map.Entry<PeerId, RemotePeerShare>> getRemotePeerShares() {
        return remotePeerShares.entrySet();
    }

    public synchronized void peerConnected(String basePath, PeerId peerID) {
        RemotePeerShare remotePeerShare;
        try {
            logger.info("Loading remote peer share for peer " + peerID);
            remotePeerShare = PeerShareIO.loadRemoteShare(basePath, peerID, foreignShares);
        } catch (IOException e) {
            // could not load the share (maybe it did not exist) -> create a new one
            try {
                logger.info("Creating new remote peer share for peer " + peerID);
                remotePeerShare = new RemotePeerShare(peerID, foreignShares, PathConstants.remoteSharePath(basePath, peerID));
            } catch (IOException e1) {
                peerEngineClient.reportFatalError("Could not create remote share for peer", peerID, PathConstants.remoteSharePath(basePath, peerID));
                return;
            }
        }
        remotePeerShares.put(peerID, remotePeerShare);
    }

    public synchronized void peerDisconnected(String basePath, PeerId peerID) {
        RemotePeerShare remotePeerShare = remotePeerShares.remove(peerID);
        if (remotePeerShare != null) {
            remotePeerShare.notifyPeerDisconnected();
//            try {
//                PeerShareIO.saveRemotePeerShare(basePath, peerID, remotePeerShare);
//            } catch (IOException e) {
//                // error writing the remote peer share to disk -> remove the files, if any
//                // a new peer share will be created at some time in the future
//                PeerShareIO.removeRemotePeerShare(basePath, peerID);
//            }
        }
    }

    public synchronized void removeRemotePeer(String basePath, PeerId remotePeerId) {
        if (remotePeerShares.containsKey(remotePeerId)) {
//            remotePeerShares.get(remotePeerId).clear();
            remotePeerShares.remove(remotePeerId);
        }
        PeerShareIO.removeRemotePeerShare(basePath, remotePeerId);
    }

    public FileHashDatabaseAccessor requestForLocalHashSynch(PeerId peerID) throws ServerBusyException {
        return fileHashDataAccessorController.requestForLocalHashSynch(peerID);
    }

    public TempFilesAccessor requestForLocalTempFilesSynch(PeerId peerID) throws ServerBusyException {
        return tempFilesDataAccessorController.requestForLocalHashSynch(peerID);
    }

    public void synchRemoteShare(PeerId peerID) {
        System.out.println("to synch remote share...");
        fileHashDataAccessorController.synchRemoteShare(peerID);
    }

    public void synchRemoteTempFiles(PeerId peerID) {
        tempFilesDataAccessorController.synchRemoteShare(peerID);
    }

    public void stop() {
        // @FUTURE@ todo once this stop could not wait because the cc inside was blocked in some unfinished synch process
        fileHashDataAccessorController.stop();
        tempFilesDataAccessorController.stop();
    }
}
