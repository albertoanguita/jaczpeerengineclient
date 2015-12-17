package jacz.peerengineclient.file_system;

import jacz.util.files.FileUtil;

import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;

/**
 * API hard-coded paths
 */
public class Paths {

    private static final String CONFIG_PATH = FileUtil.joinPaths("config", "config");

    private static final String ENCRYPTION_PATH = FileUtil.joinPaths("encryption", "keys");

    private static final String STATISTICS_PATH = FileUtil.joinPaths("stats", "statistics");

    private static final String LOG_PATH = FileUtil.joinPaths("log", "log");

    private static final String LIBRARIES_PATH = FileUtil.joinPaths("data", "libraries");

    private static final String INTEGRATED_FILENAME = "integrated";

    private static final String LOCAL_FILENAME = "local";

    private static final String REMOTE_DIRECTORY = "remote";

    private static final String SHARED_FILENAME = "shared";

    private static final String DELETED_FILENAME = "deleted";

    private static final String DATABASE_EXTENSION = "db";

    private static final String ANNOTATED_DATABASE_EXTENSION = "lbr";

    private static final String BACKUP_EXTENSION = "bak";

    private static final String SHARES_PATH = FileUtil.joinPaths("data", "hash");

//    private static final String HASH_PATH = FileUtil.joinPaths("data", "hash", "file-database");

    private static final String FILE_HASH_DATABASE = "hash-db";

    private static final String REMOTE_SHARES_PATH = "remote-shares";


    private static final String EXT_VERSIONED = ".vso";

    private static final String EXT_XML = ".xml";

    private static final String EXT_LOG = ".log";

    private static final String EXT_BACKUP = ".bak";


    private static String getFilePath(String basePath, String filePath, String extension) {
        return FileUtil.joinPaths(basePath, filePath) + extension;
    }

    private static String getFilePath(String basePath, String subPath, String filePath, String extension) {
        return FileUtil.joinPaths(basePath, filePath) + extension;
    }


    /********************** directories **********************/

    public static String getLibrariesPath(String path) {
        return FileUtil.joinPaths(path, LIBRARIES_PATH);
    }

    public static String getLocalSharePath(String path) {
        return FileUtil.joinPaths(path, SHARES_PATH);
    }

    public static String getRemoteSharesPath(String path) {
        return FileUtil.joinPaths(path, SHARES_PATH);
    }



    /********************** files **********************/

    /********* general config *********/

    public static String getConfigPath(String basePath) {
        return getFilePath(basePath, CONFIG_PATH, EXT_XML);
    }

    public static String getConfigBackupPath(String basePath) {
        return getFilePath(basePath, CONFIG_PATH, EXT_BACKUP);
    }

    public static String getEncryptionPath(String basePath) {
        return getFilePath(basePath, ENCRYPTION_PATH, EXT_VERSIONED);
    }

    public static String getEncryptionBackupPath(String basePath) {
        return getFilePath(basePath, ENCRYPTION_PATH, EXT_BACKUP);
    }

    public static String getStatisticsPath(String basePath) {
        return getFilePath(basePath, STATISTICS_PATH, EXT_VERSIONED);
    }

    public static String getStatisticsBackupPath(String basePath) {
        return getFilePath(basePath, STATISTICS_PATH, EXT_BACKUP);
    }

    public static String getLogPath(String basePath) {
        return getFilePath(basePath, LOG_PATH, EXT_LOG);
    }

    public static String getIntegratedDatabasePath(String basePath) {
        return getFilePath(basePath, LIBRARIES_PATH, INTEGRATED_FILENAME, DATABASE_EXTENSION);
    }

    public static String getAnnotatedIntegratedDatabasePath(String basePath) {
        return getFilePath(basePath, LIBRARIES_PATH, INTEGRATED_FILENAME, ANNOTATED_DATABASE_EXTENSION);
    }

    public static String getBackupIntegratedDatabasePath(String basePath) {
        return getFilePath(basePath, LIBRARIES_PATH, INTEGRATED_FILENAME, BACKUP_EXTENSION);
    }

    public static String getLocalDatabasePath(String basePath) {
        return getFilePath(basePath, LIBRARIES_PATH, LOCAL_FILENAME, DATABASE_EXTENSION);
    }

    public static String getAnnotatedLocalDatabasePath(String basePath) {
        return getFilePath(basePath, LIBRARIES_PATH, LOCAL_FILENAME, ANNOTATED_DATABASE_EXTENSION);
    }

    public static String getBackupLocalDatabasePath(String basePath) {
        return getFilePath(basePath, LIBRARIES_PATH, LOCAL_FILENAME, BACKUP_EXTENSION);
    }

    public static Set<String> listRemoteDatabasePeers(String basePath) throws FileNotFoundException {
        String[] filesInRemoteDir = FileUtil.getDirectoryContents(FileUtil.joinPaths(basePath, LIBRARIES_PATH, REMOTE_DIRECTORY));
        Set<String> remoteDatabasePeers = new HashSet<>();
        for (String file : filesInRemoteDir) {
            remoteDatabasePeers.add(FileUtil.getFileNameWithoutExtension(file));
        }
        return remoteDatabasePeers;
    }

    public static String getRemoteDatabasePath(String basePath, String peerID) {
        return FileUtil.joinPaths(basePath, LIBRARIES_PATH, REMOTE_DIRECTORY, peerID) + "." + DATABASE_EXTENSION;
    }

    public static String getAnnotatedRemoteDatabasePath(String basePath, String peerID) {
        return FileUtil.joinPaths(basePath, LIBRARIES_PATH, REMOTE_DIRECTORY, peerID) + "." + ANNOTATED_DATABASE_EXTENSION;
    }

    public static String getBackupRemoteDatabasePath(String basePath, String peerID) {
        return FileUtil.joinPaths(basePath, LIBRARIES_PATH, REMOTE_DIRECTORY, peerID) + "." + BACKUP_EXTENSION;
    }

    public static String getSharedDatabasePath(String basePath) {
        return getFilePath(basePath, LIBRARIES_PATH, SHARED_FILENAME, DATABASE_EXTENSION);
    }

    public static String getDeletedDatabasePath(String basePath) {
        return getFilePath(basePath, LIBRARIES_PATH, DELETED_FILENAME, DATABASE_EXTENSION);
    }

    public static String getHashPath(String path) {
        return FileUtil.joinPaths(path, HASH_PATH + EXT_VERSIONED);
    }

    public static String getHashBackupPath(String path) {
        return FileUtil.joinPaths(path, HASH_PATH + EXT_BACKUP);
    }

    private static String generateFileHashDatabasePath(String basePath) {
//        return getFilePath(basePath, INTEGRATED_FILENAME, ANNOTATED_DATABASE_EXTENSION);
    }

    private static String generateBackupFileHashDatabasePath(String basePath) {
//        return getFilePath(basePath, INTEGRATED_FILENAME, BACKUP_EXTENSION);
    }

    public static Set<String> listRemoteSharePeers(String basePath) throws FileNotFoundException {
        String[] filesInRemoteDir = FileUtil.getDirectoryContents(getFilePath(basePath, SHARES_PATH, REMOTE_DIRECTORY));
        Set<String> remoteDatabasePeers = new HashSet<>();
        for (String file : filesInRemoteDir) {
            remoteDatabasePeers.add(FileUtil.getFileNameWithoutExtension(file));
        }
        return remoteDatabasePeers;
    }


}
