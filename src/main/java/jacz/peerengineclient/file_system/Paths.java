package jacz.peerengineclient.file_system;

import jacz.database.util.ImageHash;
import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineservice.PeerID;
import jacz.util.files.FileUtil;
import jacz.util.lists.tuple.Triple;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * API hard-coded paths
 */
public class Paths {

    /**********************
     * directory paths
     **********************/

    private static final String CONFIG_DIR = "config";

    private static final String ENCRYPTION_DIR = "encryption";

    private static final String STATISTICS_DIR = "stats";

    private static final String DATA_DIR = "data";

    private static final String DATABASES_DIR = FileUtil.joinPaths(DATA_DIR, "databases");

    private static final String REMOTE_DATABASES_DIR = FileUtil.joinPaths(DATABASES_DIR, "remote");

    private static final String REMOTE_SHARES_DIR = FileUtil.joinPaths(DATA_DIR, "remote-shares");

    private static final String DEFAULT_TEMP_DIR = "temp";

    private static final String DEFAULT_DOWNLOADS_DIR = FileUtil.joinPaths(DATA_DIR, "remote-shares");

    private static final String IMAGES_DIR = "images";

    private static final String MOVIES_DIR = "movies";

    private static final String TV_SERIES_DIR = "series";

    private static final String UNKNOWN_TITLE_DIR = "untitled_item";


    /**********************
     * file names
     **********************/

    private static final String CONFIG_FILE = "config";

    private static final String ENCRYPTION_KEYS_FILE = "keys";

    private static final String STATISTICS_FILE = "statistics";

    private static final String INTEGRATED_DB_FILE = "integrated";

    private static final String LOCAL_DB_FILE = "local";

    private static final String SHARED_DB_FILE = "shared";

    private static final String DELETED_DB_FILE = "deleted";

    private static final String ITEM_RELATIONS_FILE = "item-relations";

    private static final String FILE_HASH_DATABASE_FILE = "hash-db";

    /**********************
     * file extensions
     **********************/

    private static final String EXT_VERSIONED = ".vso";

    private static final String EXT_XML = ".xml";

    private static final String EXT_DB = ".db";

    private static final String EXT_BACKUP = ".bak";


    private static String getFilePath(String basePath, String dir, String fileName, String extension) {
        return FileUtil.joinPaths(basePath, dir, fileName) + extension;
    }

    /**********************
     * directories
     **********************/

    public static String getConfigDir(String basePath) {
        return FileUtil.joinPaths(basePath, CONFIG_DIR);
    }

    public static String getEncryptionDir(String basePath) {
        return FileUtil.joinPaths(basePath, ENCRYPTION_DIR);
    }

    public static String getStatisticsDir(String basePath) {
        return FileUtil.joinPaths(basePath, STATISTICS_DIR);
    }

    public static String getDataDir(String basePath) {
        return FileUtil.joinPaths(basePath, DATA_DIR);
    }

    public static String getDatabasesDir(String basePath) {
        return FileUtil.joinPaths(basePath, DATABASES_DIR);
    }

    public static String getRemoteDatabasesDir(String basePath) {
        return FileUtil.joinPaths(basePath, REMOTE_DATABASES_DIR);
    }

    public static String getRemoteSharesDir(String basePath) {
        return FileUtil.joinPaths(basePath, REMOTE_SHARES_DIR);
    }

    public static String getDefaultTempDir(String basePath) {
        return FileUtil.joinPaths(basePath, DEFAULT_TEMP_DIR);
    }

    public static String getDefaultDownloadsDir(String basePath) {
        return FileUtil.joinPaths(basePath, DEFAULT_DOWNLOADS_DIR);
    }

    public static List<String> getOrderedDirectories(String basePath) {
        List<String> directories = new ArrayList<>();
        directories.add(getConfigDir(basePath));
        directories.add(getEncryptionDir(basePath));
        directories.add(getStatisticsDir(basePath));
        directories.add(getDataDir(basePath));
        directories.add(getDatabasesDir(basePath));
        directories.add(getRemoteDatabasesDir(basePath));
        directories.add(getRemoteSharesDir(basePath));
        return directories;
    }


    /********************** files **********************/

    /************************
     * general config
     ***********************/

    public static String configPath(String basePath) {
        return getFilePath(basePath, CONFIG_DIR, CONFIG_FILE, EXT_XML);
    }

    public static String configBackupPath(String basePath) {
        return getFilePath(basePath, CONFIG_DIR, CONFIG_FILE, EXT_BACKUP);
    }

    /************************
     * encryption
     ***********************/

    public static String encryptionPath(String basePath) {
        return getFilePath(basePath, ENCRYPTION_DIR, ENCRYPTION_KEYS_FILE, EXT_VERSIONED);
    }

    public static String encryptionBackupPath(String basePath) {
        return getFilePath(basePath, ENCRYPTION_DIR, ENCRYPTION_KEYS_FILE, EXT_BACKUP);
    }

    /************************
     * statistics
     ***********************/

    public static String statisticsPath(String basePath) {
        return getFilePath(basePath, STATISTICS_DIR, STATISTICS_FILE, EXT_VERSIONED);
    }

    public static String statisticsBackupPath(String basePath) {
        return getFilePath(basePath, STATISTICS_DIR, STATISTICS_FILE, EXT_BACKUP);
    }

    /************************
     * databases
     ***********************/

    public static String integratedDBPath(String basePath) {
        return getFilePath(basePath, DATABASES_DIR, INTEGRATED_DB_FILE, EXT_DB);
    }

    public static String localDBPath(String basePath) {
        return getFilePath(basePath, DATABASES_DIR, LOCAL_DB_FILE, EXT_DB);
    }

    public static Set<PeerID> listRemoteDBPeers(String basePath) throws FileNotFoundException {
        String[] filesInRemoteDir = FileUtil.getDirectoryContents(getRemoteDatabasesDir(basePath));
        Set<PeerID> remoteDatabasePeers = new HashSet<>();
        for (String file : filesInRemoteDir) {
            remoteDatabasePeers.add(new PeerID(FileUtil.getFileNameWithoutExtension(file)));
        }
        return remoteDatabasePeers;
    }

    public static String remoteDBPath(String basePath, PeerID peerID) {
        return getFilePath(basePath, REMOTE_DATABASES_DIR, peerID.toString(), EXT_DB);
    }

    public static String sharedDBPath(String basePath) {
        return getFilePath(basePath, DATABASES_DIR, SHARED_DB_FILE, EXT_DB);
    }

    public static String deletedDBPath(String basePath) {
        return getFilePath(basePath, DATABASES_DIR, DELETED_DB_FILE, EXT_DB);
    }

    public static String itemRelationsPath(String basePath) {
        return getFilePath(basePath, DATABASES_DIR, ITEM_RELATIONS_FILE, EXT_DB);
    }

    public static String itemRelationsBackupPath(String basePath) {
        return getFilePath(basePath, DATABASES_DIR, ITEM_RELATIONS_FILE, EXT_BACKUP);
    }

    /************************
     * file hash database
     ***********************/

    public static String fileHashPath(String basePath) {
        return getFilePath(basePath, DATA_DIR, FILE_HASH_DATABASE_FILE, EXT_VERSIONED);
    }

    public static String fileHashBackupPath(String basePath) {
        return getFilePath(basePath, DATA_DIR, FILE_HASH_DATABASE_FILE, EXT_BACKUP);
    }

    /************************
     * peer shares
     ***********************/

    public static String remoteSharePath(String basePath, PeerID peerID) {
        return getFilePath(basePath, REMOTE_SHARES_DIR, peerID.toString(), EXT_VERSIONED);
    }

    public static String remoteShareBackupPath(String basePath, PeerID peerID) {
        return getFilePath(basePath, REMOTE_SHARES_DIR, peerID.toString(), EXT_BACKUP);
    }

    /************************
     * downloads
     ***********************/

    public static String imagesDir(String downloadsDir) {
        return FileUtil.joinPaths(downloadsDir, IMAGES_DIR);
    }

    public static String moviesDir(String downloadsDir) {
        return FileUtil.joinPaths(downloadsDir, MOVIES_DIR);
    }

    public static String seriesDir(String downloadsDir) {
        return FileUtil.joinPaths(downloadsDir, TV_SERIES_DIR);
    }

    private static String generateTitleDir(String baseDir, int itemId, String itemTitle) {
        String dir = itemTitle != null ? itemTitle : UNKNOWN_TITLE_DIR;
        dir += "_" + itemId;
        return FileUtil.joinPaths(baseDir, dir);
    }

    private static void createDir(String dir) throws IOException {
        File file = new File(dir);
        if (!file.isDirectory()) {
            if (!file.mkdir()) {
                throw new IOException("Could not create directory: " + dir);
            }
        }
    }

    public static String imageFilePath(String downloadsDir, String filePath) throws IOException {
        String hash = PeerEngineClient.getHashFunction().digestAsHex(filePath);
        String extension = FileUtil.getFileExtension(FileUtil.getFileName(filePath));
        return FileUtil.joinPaths(imagesDir(downloadsDir), imageFileName(downloadsDir, new ImageHash(hash, extension)));
    }

    public static String imageFileName(String downloadsDir, ImageHash imageHash) throws IOException {
        createDir(imagesDir(downloadsDir));
        return imageHash.getHash() + "." + imageHash.getExtension();
    }

    public static Triple<String, String, String> movieFilePath(String downloadsDir, int movieId, String movieTitle, String fileName) throws IOException {
        createDir(moviesDir(downloadsDir));
        String titleDir = generateTitleDir(downloadsDir, movieId, movieTitle);
        createDir(titleDir);
        return new Triple<>(titleDir, FileUtil.getFileNameWithoutExtension(fileName), FileUtil.getFileExtension(fileName));
    }

    public static Triple<String, String, String> seriesFilePath(String downloadsDir, int seriesId, String seriesTitle, int chapterId, String chapterTitle, String fileName) throws IOException {
        createDir(seriesDir(downloadsDir));
        String seriesTitledDir = generateTitleDir(downloadsDir, seriesId, seriesTitle);
        createDir(seriesTitledDir);
        String chapterTitledDir = generateTitleDir(seriesTitledDir, chapterId, chapterTitle);
        createDir(chapterTitledDir);
        return new Triple<>(chapterTitledDir, FileUtil.getFileNameWithoutExtension(fileName), FileUtil.getFileExtension(fileName));
    }
}
