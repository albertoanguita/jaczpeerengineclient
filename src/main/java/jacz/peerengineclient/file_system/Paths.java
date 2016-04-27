package jacz.peerengineclient.file_system;

import jacz.database.util.ImageHash;
import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineservice.PeerId;
import jacz.util.lists.tuple.Triple;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.IOFileFilter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/**
 * API hard-coded paths
 */
public class Paths {

    /**********************
     * directory paths
     **********************/

    // todo remove
    private static final String CONFIG_DIR = "config";

    private static final String ENCRYPTION_DIR = "encryption";

    private static final String STATISTICS_DIR = "stats";

    private static final String DATA_DIR = "data";

    private static final String DATABASES_DIR = FileUtils.getFile(DATA_DIR, "databases").getName();

    private static final String REMOTE_DATABASES_DIR = FileUtils.getFile(DATABASES_DIR, "remote").getName();

    private static final String REMOTE_SHARES_DIR = FileUtils.getFile(DATA_DIR, "remote-shares").getName();

    private static final String DEFAULT_TEMP_DIR = "temp";

    private static final String DEFAULT_MEDIA_DIR = "media";

    private static final String IMAGES_DIR = "images";

    private static final String MOVIES_DIR = "movies";

    private static final String TV_SERIES_DIR = "series";

    private static final String UNKNOWN_TITLE_DIR = "untitled_item";


    /**********************
     * file names
     **********************/



    private static final String CONNECTION_CONFIG_FILE = "config";
    private static final String PEER_KNOWLEDGE_BASE_DATABASE_FILE = "peer-kb";
    private static final String NETWORK_CONFIG_FILE = "network";
    private static final String PERSONAL_DATA_FILE = "personal-data";
    ///////
    private static final String CONFIG_FILE = "config";

    private static final String PEER_ID_CONFIG_FILE = "id";

//    private static final String NETWORK_CONFIG_FILE = "network";

    private static final String NICK_CONFIG_FILE = "nick";

    private static final String ENGINE_CONFIG_FILE = "limits";

    private static final String MEDIA_PATHS_CONFIG_FILE = "paths";

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
        return FileUtils.getFile(basePath, dir, fileName + extension).getName();
    }

    /**********************
     * directories
     **********************/

    public static File getConfigDir(String basePath) {
        return FileUtils.getFile(basePath, CONFIG_DIR);
    }

    public static File getEncryptionDir(String basePath) {
        return FileUtils.getFile(basePath, ENCRYPTION_DIR);
    }

    public static File getStatisticsDir(String basePath) {
        return FileUtils.getFile(basePath, STATISTICS_DIR);
    }

    public static File getDataDir(String basePath) {
        return FileUtils.getFile(basePath, DATA_DIR);
    }

    public static File getDatabasesDir(String basePath) {
        return FileUtils.getFile(basePath, DATABASES_DIR);
    }

    public static File getRemoteDatabasesDir(String basePath) {
        return FileUtils.getFile(basePath, REMOTE_DATABASES_DIR);
    }

    public static File getRemoteSharesDir(String basePath) {
        return FileUtils.getFile(basePath, REMOTE_SHARES_DIR);
    }

    public static File getDefaultTempDir(String basePath) {
        return FileUtils.getFile(basePath, DEFAULT_TEMP_DIR);
    }

    public static File getDefaultMediaDir(String basePath) {
        return FileUtils.getFile(basePath, DEFAULT_MEDIA_DIR);
    }

    public static List<File> getOrderedDirectories(String basePath) {
        List<File> directories = new ArrayList<>();
        directories.add(getConfigDir(basePath));
        directories.add(getEncryptionDir(basePath));
        directories.add(getStatisticsDir(basePath));
        directories.add(getDataDir(basePath));
        directories.add(getDatabasesDir(basePath));
        directories.add(getRemoteDatabasesDir(basePath));
        directories.add(getRemoteSharesDir(basePath));
        directories.add(getDefaultTempDir(basePath));
        directories.add(getDefaultMediaDir(basePath));
        return directories;
    }


    /********************** files **********************/

    /************************
     * general config
     ***********************/

    public static String connectionConfigPath(String basePath) {
        return getFilePath(basePath, CONFIG_DIR, CONNECTION_CONFIG_FILE, EXT_DB);
    }

    public static String networkConfigPath(String basePath) {
        return getFilePath(basePath, CONFIG_DIR, NETWORK_CONFIG_FILE, EXT_DB);
    }

    public static String personalDataPath(String basePath) {
        return getFilePath(basePath, CONFIG_DIR, PERSONAL_DATA_FILE, EXT_DB);
    }

    public static String configPath(String basePath) {
        return getFilePath(basePath, CONFIG_DIR, CONFIG_FILE, EXT_XML);
    }

    public static String configBackupPath(String basePath) {
        return getFilePath(basePath, CONFIG_DIR, CONFIG_FILE, EXT_BACKUP);
    }

    public static String peerIdConfigPath(String basePath) {
        return getFilePath(basePath, CONFIG_DIR, PEER_ID_CONFIG_FILE, EXT_XML);
    }

    public static String peerIdConfigBackupPath(String basePath) {
        return getFilePath(basePath, CONFIG_DIR, PEER_ID_CONFIG_FILE, EXT_BACKUP);
    }

//    public static String networkConfigPath(String basePath) {
//        return getFilePath(basePath, CONFIG_DIR, NETWORK_CONFIG_FILE, EXT_XML);
//    }
//
//    public static String networkConfigBackupPath(String basePath) {
//        return getFilePath(basePath, CONFIG_DIR, NETWORK_CONFIG_FILE, EXT_BACKUP);
//    }

//    public static String nickConfigPath(String basePath) {
//        return getFilePath(basePath, CONFIG_DIR, NICK_CONFIG_FILE, EXT_XML);
//    }
//
//    public static String nickConfigBackupPath(String basePath) {
//        return getFilePath(basePath, CONFIG_DIR, NICK_CONFIG_FILE, EXT_BACKUP);
//    }

    public static String engineConfigPath(String basePath) {
        return getFilePath(basePath, CONFIG_DIR, ENGINE_CONFIG_FILE, EXT_XML);
    }

    public static String engineConfigBackupPath(String basePath) {
        return getFilePath(basePath, CONFIG_DIR, ENGINE_CONFIG_FILE, EXT_BACKUP);
    }

    public static String mediaPathsConfigPath(String basePath) {
        return getFilePath(basePath, CONFIG_DIR, MEDIA_PATHS_CONFIG_FILE, EXT_DB);
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

//    public static String statisticsBackupPath(String basePath) {
//        return getFilePath(basePath, STATISTICS_DIR, STATISTICS_FILE, EXT_BACKUP);
//    }

    /************************
     * databases
     ***********************/

    public static String integratedDBPath(String basePath) {
        return getFilePath(basePath, DATABASES_DIR, INTEGRATED_DB_FILE, EXT_DB);
    }

    public static String localDBPath(String basePath) {
        return getFilePath(basePath, DATABASES_DIR, LOCAL_DB_FILE, EXT_DB);
    }

    public static Set<PeerId> listRemoteDBPeers(String basePath) throws FileNotFoundException {
        Collection<File> filesInRemoteDir = FileUtils.listFiles(getRemoteDatabasesDir(basePath), new IOFileFilter() {
            @Override
            public boolean accept(File file) {
                return true;
            }

            @Override
            public boolean accept(File file, String s) {
                return true;
            }
        }, null);
        Set<PeerId> remoteDatabasePeers = new HashSet<>();
        for (File file : filesInRemoteDir) {
            remoteDatabasePeers.add(new PeerId(FilenameUtils.getBaseName(file.getName())));
        }
        return remoteDatabasePeers;
    }

    public static String remoteDBPath(String basePath, PeerId peerID) {
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
     * peer knowledge base database
     ***********************/

    public static String peerKBPath(String basePath) {
        return getFilePath(basePath, DATA_DIR, PEER_KNOWLEDGE_BASE_DATABASE_FILE, EXT_DB);
    }

    /************************
     * peer shares
     ***********************/

    public static String remoteSharePath(String basePath, PeerId peerID) {
        return getFilePath(basePath, REMOTE_SHARES_DIR, peerID.toString(), EXT_VERSIONED);
    }

    public static String remoteShareBackupPath(String basePath, PeerId peerID) {
        return getFilePath(basePath, REMOTE_SHARES_DIR, peerID.toString(), EXT_BACKUP);
    }

    /************************
     * downloads
     ***********************/

    public static File imagesDir(String downloadsDir) {
        return FileUtils.getFile(downloadsDir, IMAGES_DIR);
    }

    public static File moviesDir(String downloadsDir) {
        return FileUtils.getFile(downloadsDir, MOVIES_DIR);
    }

    public static File seriesDir(String downloadsDir) {
        return FileUtils.getFile(downloadsDir, TV_SERIES_DIR);
    }

    private static File generateTitleDir(File baseDir, Integer itemId, String itemTitle) {
        String dir = (itemTitle != null && !itemTitle.isEmpty()) ? itemTitle : UNKNOWN_TITLE_DIR;
        if (itemId != null) {
            dir += "_" + itemId;
        }
        return FileUtils.getFile(baseDir, dir);
    }

    public static Triple<String, String, String> imageFilePath(String downloadsDir, String filePath) throws IOException {
        String hash = PeerEngineClient.getHashFunction().digestAsHex(new File(filePath));
        return imageFilePath(downloadsDir, FilenameUtils.getName(filePath), hash);
    }

    public static Triple<String, String, String> imageFilePath(String downloadsDir, String fileName, String hash) throws IOException {
        FileUtils.forceMkdir(imagesDir(downloadsDir));
        return new Triple<>(imagesDir(downloadsDir).getName(), hash, FilenameUtils.getExtension(fileName));
    }

    public static String imageFileName(String downloadsDir, ImageHash imageHash) throws IOException {
        FileUtils.forceMkdir(imagesDir(downloadsDir));
        return imageHash.getHash() + "." + imageHash.getExtension();
    }

    public static Triple<String, String, String> movieFilePath(String downloadsDir, int movieId, String movieTitle, String fileName) throws IOException {
        FileUtils.forceMkdir(moviesDir(downloadsDir));
        File titleDir = generateTitleDir(moviesDir(downloadsDir), movieId, movieTitle);
        FileUtils.forceMkdir(titleDir);
        return new Triple<>(titleDir.getName(), FilenameUtils.getBaseName(fileName), FilenameUtils.getExtension(fileName));
    }

    public static Triple<String, String, String> seriesFilePath(String downloadsDir, Integer seriesId, String seriesTitle, int chapterId, String chapterTitle, String fileName) throws IOException {
        FileUtils.forceMkdir(seriesDir(downloadsDir));
        File seriesTitledDir = generateTitleDir(seriesDir(downloadsDir), seriesId, seriesTitle);
        FileUtils.forceMkdir(seriesTitledDir);
        File chapterTitledDir = generateTitleDir(seriesTitledDir, chapterId, chapterTitle);
        FileUtils.forceMkdir(chapterTitledDir);
        return new Triple<>(chapterTitledDir.getName(), FilenameUtils.getBaseName(fileName), FilenameUtils.getExtension(fileName));
    }
}
