package jacz.peerengineclient.data;

import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineclient.file_system.Paths;
import jacz.peerengineservice.PeerID;
import jacz.util.io.object_serialization.VersionedObjectSerializer;
import jacz.util.io.object_serialization.VersionedSerializationException;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Stores and loads peer share objects. These objects are the file hash database (with timestamp) used to index
 * all local files, and the list of hashes shared by the other peers, modelled by a DoubleMap object for each peer.
 * <p>
 * These objects are located in the /data/hash route.
 */
public class PeerShareIO {

    private static final int CRCBytes = 4;

    private static final int ID_LENGTH = 12;

    public static void createNewFileStructure(String basePath) throws IOException {
        // create an empty file hash
        FileHashDatabaseWithTimestamp fileHash = new FileHashDatabaseWithTimestamp(RandomStringUtils.randomAlphanumeric(ID_LENGTH));
        saveLocalHash(basePath, fileHash);
    }

    public static PeerShareManager load(String basePath, PeerEngineClient peerEngineClient) throws IOException, VersionedSerializationException {
        FileHashDatabaseWithTimestamp fileHash = new FileHashDatabaseWithTimestamp(Paths.fileHashPath(basePath), Paths.fileHashBackupPath(basePath));
        Map<PeerID, RemotePeerShare> remotePeerShares = new HashMap<>();
        for (PeerID peerID : Paths.listRemoteSharePeers(basePath)) {
            remotePeerShares.put(
                    peerID,
                    new RemotePeerShare(
                            peerEngineClient.getPeerClient(),
                            Paths.remoteSharePath(basePath, peerID),
                            Paths.remoteShareBackupPath(basePath, peerID)));
        }
        return new PeerShareManager(peerEngineClient, fileHash, remotePeerShares);
    }

    public static void save(String basePath, PeerShareManager peerShareManager) throws IOException {
        saveLocalHash(basePath, peerShareManager.getFileHash());
        for (Map.Entry<PeerID, RemotePeerShare> entry : peerShareManager.getRemotePeerShares().entrySet()) {
            saveRemotePeerShare(basePath, entry.getKey(), entry.getValue());
        }
    }

    private static void saveLocalHash(String basePath, FileHashDatabaseWithTimestamp fileHash) throws IOException {
        VersionedObjectSerializer.serialize(fileHash, CRCBytes, Paths.fileHashPath(basePath), Paths.fileHashBackupPath(basePath));
    }

    private static void saveRemotePeerShare(String basePath, PeerID peerID, RemotePeerShare remotePeerShare) throws IOException {
        VersionedObjectSerializer.serialize(
                remotePeerShare,
                CRCBytes,
                Paths.remoteSharePath(basePath, peerID),
                Paths.remoteShareBackupPath(basePath, peerID));
    }

    static RemotePeerShare createNewRemotePeerShare(String basePath, PeerEngineClient peerEngineClient, PeerID peerID) throws IOException {
        // todo use
        RemotePeerShare remotePeerShare = new RemotePeerShare(peerEngineClient.getPeerClient(), peerID);
        saveRemotePeerShare(basePath, peerID, remotePeerShare);
        return remotePeerShare;
    }

    public static void removeRemotePeerShare(String basePath, PeerID peerID) {
        // todo use
        //noinspection ResultOfMethodCallIgnored
        new File(Paths.remoteSharePath(basePath, peerID)).delete();
        //noinspection ResultOfMethodCallIgnored
        new File(Paths.remoteShareBackupPath(basePath, peerID)).delete();
    }
}
