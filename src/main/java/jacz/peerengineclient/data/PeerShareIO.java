package jacz.peerengineclient.data;

import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineclient.file_system.PathConstants;
import jacz.peerengineservice.PeerId;
import jacz.util.io.serialization.VersionedSerializationException;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.File;
import java.io.IOException;

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
        new FileHashDatabaseWithTimestamp(PathConstants.fileHashPath(basePath), RandomStringUtils.randomAlphanumeric(ID_LENGTH));
    }

    public static PeerShareManager load(String basePath, PeerEngineClient peerEngineClient, FileHashDatabaseEvents fileHashDatabaseEvents) throws IOException, VersionedSerializationException {
        FileHashDatabaseWithTimestamp fileHash = new FileHashDatabaseWithTimestamp(PathConstants.fileHashPath(basePath), fileHashDatabaseEvents);
        return new PeerShareManager(peerEngineClient, fileHash);
    }

    static RemotePeerShare loadRemoteShare(String basePath, PeerId peerID, ForeignShares foreignShares) throws IOException {
        return new RemotePeerShare(
                foreignShares,
                PathConstants.remoteSharePath(basePath, peerID));
    }

//    public static void save(String basePath, PeerShareManager peerShareManager) throws IOException {
//        for (Map.Entry<PeerId, RemotePeerShare> entry : peerShareManager.getRemotePeerShares()) {
//            saveRemotePeerShare(basePath, entry.getKey(), entry.getValue());
//        }
//    }

//    static void saveRemotePeerShare(String basePath, PeerId peerID, RemotePeerShare remotePeerShare) throws IOException {
//        VersionedObjectSerializer.serialize(
//                remotePeerShare,
//                CRCBytes,
//                PathConstants.remoteSharePath(basePath, peerID),
//                PathConstants.remoteShareBackupPath(basePath, peerID));
//    }

    static void removeRemotePeerShare(String basePath, PeerId peerID) {
        //noinspection ResultOfMethodCallIgnored
        new File(PathConstants.remoteSharePath(basePath, peerID)).delete();
        //noinspection ResultOfMethodCallIgnored
//        new File(PathConstants.remoteShareBackupPath(basePath, peerID)).delete();
    }
}
