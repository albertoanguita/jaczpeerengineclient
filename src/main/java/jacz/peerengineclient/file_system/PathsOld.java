package jacz.peerengineclient.file_system;

import jacz.util.files.FileUtil;

import java.io.File;

/**
 * Contains the relative paths of all the user files, and allows retrieving them from a given user directory
 *
 * todo add file exist checking (all files) and CRC checking
 */
public final class PathsOld {

    private static final String PEER_ID_FILE = "peer_id.xml";

    private static final String KEYS_FILE = "encryption.bin";

    private static final String PERSONAL_DATA_FILE = "personal_data.xml";

    private static final String NETWORK_CONFIG_FILE = "network_config.xml";

    private static final String ENGINE_CONFIG_FILE = "engine_config.xml";

    private static final String GENERAL_CONFIG_FILE = "general_config.xml";

    private static final String SERVERS_FILE = "servers.xml";

    private static final String PEER_RELATIONS_FILE = "peer_relations.xml";

    private static final String FILE_HASH_DATABASE_FILE = "file_hash_database.bin";

    private static final String CONFIG_FILE = "config.xml";

    private static final String DATABASES_PATH = "databases/";



    private static final String FILE_HASH_DB_FILE = "file_hash_db.bin";


    public static String getPeerClientData(String userPath) {
        // todo
        userPath = checkUserPath(userPath);
        return FileUtil.generatePath(PEER_ID_FILE, userPath);
    }

    public static String getPeerIdFile(String userPath) {
        userPath = checkUserPath(userPath);
        return FileUtil.generatePath(PEER_ID_FILE, userPath);
    }

    public static String getEncryptionFile(String userPath) {
        userPath = checkUserPath(userPath);
        return FileUtil.generatePath(KEYS_FILE, userPath);
    }

    public static String getPersonalDataFile(String userPath) {
        userPath = checkUserPath(userPath);
        return FileUtil.generatePath(PERSONAL_DATA_FILE, userPath);
    }

    public static String getNetworkConfigFile(String userPath) {
        userPath = checkUserPath(userPath);
        return FileUtil.generatePath(NETWORK_CONFIG_FILE, userPath);
    }

    public static String getEngineConfigFile(String userPath) {
        userPath = checkUserPath(userPath);
        return FileUtil.generatePath(ENGINE_CONFIG_FILE, userPath);
    }

    public static String getGeneralConfigFile(String userPath) {
        userPath = checkUserPath(userPath);
        return FileUtil.generatePath(GENERAL_CONFIG_FILE, userPath);
    }

    public static String getServersFile(String userPath) {
        userPath = checkUserPath(userPath);
        return FileUtil.generatePath(SERVERS_FILE, userPath);
    }

    public static String getPeerRelationsFile(String userPath) {
        userPath = checkUserPath(userPath);
        return FileUtil.generatePath(PEER_RELATIONS_FILE, userPath);
    }

    public static String getFileHashDatabaseFile(String userPath) {
        userPath = checkUserPath(userPath);
        return FileUtil.generatePath(FILE_HASH_DATABASE_FILE, userPath);
    }

    public static String getDatabasesPath(String userPath) {
        userPath = checkUserPath(userPath);
        return new File(userPath, DATABASES_PATH).getPath();
    }

    private static String checkUserPath(String userPath) {
        return userPath;
    }
}
