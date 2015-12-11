package jacz.peerengineclient.file_system;

import jacz.util.files.FileUtil;

/**
 * API hard-coded paths
 */
public class Paths {

    private static final String CONFIG_PATH = FileUtil.joinPaths("config", "config");

    private static final String ENCRYPTION_PATH = FileUtil.joinPaths("encryption", "keys");

    private static final String STATISTICS_PATH = FileUtil.joinPaths("stats", "statistics");

    private static final String LOG_PATH = FileUtil.joinPaths("log", "log");

    private static final String LIBRARIES_PATH = FileUtil.joinPaths("data", "libraries");

    private static final String HASH_PATH = FileUtil.joinPaths("data", "hash", "file-database");

    private static final String DOT = "vso";

    private static final String VERSIONED = DOT + "vso";

    private static final String XML = DOT + "xml";

    private static final String TXT = DOT + "txt";

    private static final String BACKUP = DOT + "bak";

    public static String getConfigPath(String path) {
        return FileUtil.joinPaths(path, CONFIG_PATH + XML);
    }

    public static String getConfigBackupPath(String path) {
        return FileUtil.joinPaths(path, CONFIG_PATH + BACKUP);
    }

    public static String getEncryptionPath(String path) {
        return FileUtil.joinPaths(path, ENCRYPTION_PATH + VERSIONED);
    }

    public static String getEncryptionBackupPath(String path) {
        return FileUtil.joinPaths(path, ENCRYPTION_PATH + BACKUP);
    }

    public static String getStatisticsPath(String path) {
        return FileUtil.joinPaths(path, STATISTICS_PATH + VERSIONED);
    }

    public static String getStatisticsBackupPath(String path) {
        return FileUtil.joinPaths(path, STATISTICS_PATH + BACKUP);
    }

    public static String getLogPath(String path) {
        return FileUtil.joinPaths(path, LOG_PATH + TXT);
    }

    public static String getLogBackupPath(String path) {
        return FileUtil.joinPaths(path, LOG_PATH + BACKUP);
    }

    public static String getLibrariesPath(String path) {
        return FileUtil.joinPaths(path, LIBRARIES_PATH);
    }

    public static String getHashPath(String path) {
        return FileUtil.joinPaths(path, HASH_PATH + VERSIONED);
    }

    public static String getHashBackupPath(String path) {
        return FileUtil.joinPaths(path, HASH_PATH + BACKUP);
    }
}
