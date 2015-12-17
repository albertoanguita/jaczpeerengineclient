package jacz.peerengineclient.peer_share;

/**
 * Stores and loads peer share objects. These objects are the file hash database (with timestamp) used to index
 * all local files, and the list of hashes shared by the other peers, modelled by a DoubleMap object for each peer.
 * <p>
 * These objects are located in the /data/hash route.
 */
public class PeerShareIO {

    private static final String FILE_HASH_DATABASE = "hash-db";

    private static final String REMOTE_SHARES_PATH = "remote-shares";

    public static void createUserShare(String basePath) {

    }

    public static void loadUserShare(String basePath) {

    }

    public static void saveUserShare(String basePath) {

    }

    private static String generateFileHashDatabasePath(String basePath) {
        return generateFilePath(basePath, INTEGRATED_FILENAME, ANNOTATED_DATABASE_EXTENSION);
    }

    private static String generateBackupFileHashDatabasePath(String basePath) {
        return generateFilePath(basePath, INTEGRATED_FILENAME, BACKUP_EXTENSION);
    }


}
