package jacz.peerengineclient.libraries;

import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineclient.libraries.library_images.IntegratedDatabase;
import jacz.peerengineclient.libraries.library_images.LocalDatabase;
import jacz.peerengineclient.libraries.library_images.RemoteDatabase;
import jacz.peerengineclient.libraries.synch.LibrarySynchEvents;
import jacz.peerengineservice.PeerID;
import jacz.store.database.DatabaseMediator;
import jacz.util.files.FileUtil;
import jacz.util.io.object_serialization.VersionedObjectSerializer;
import jacz.util.io.object_serialization.VersionedSerializationException;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class handles saving and restoring the data of the library manager. For all handled peers,
 * their remote databases are stored in disk and retrieved in each new session.
 * The integrated database is also stored in disk.
 * <p>
 * This IO requires a directory path for managing all the saved and restored information.
 * Each peer uses a different sub-directory, and the integrated database uses its own sub-directory
 * <p>
 * todo store the db version
 */
public class LibraryManagerIO {

    private static final String INTEGRATED_FILENAME = "integrated";

    private static final String LOCAL_FILENAME = "local";

    private static final String REMOTE_DIRECTORY = "remote";

    private static final String DATABASE_EXTENSION = "db";

    private static final String ANNOTATED_DATABASE_EXTENSION = "lib";

    private static final String BACKUP_EXTENSION = "bak";

    private static final int CRCBytes = 4;

    private static final int ID_LENGTH = 12;

    public static LibraryManager load(String basePath, LibrarySynchEvents librarySynchEvents, PeerEngineClient peerEngineClient) throws IOException, VersionedSerializationException {
        IntegratedDatabase integratedDatabase = new IntegratedDatabase(generateIntegratedDatabasePath(basePath));
        VersionedObjectSerializer.deserialize(integratedDatabase, generateAnnotatedIntegratedDatabasePath(basePath), generateBackupIntegratedDatabasePath(basePath));

        LocalDatabase localDatabase = new LocalDatabase(generateLocalDatabasePath(basePath));
        VersionedObjectSerializer.deserialize(localDatabase, generateAnnotatedLocalDatabasePath(basePath), generateBackupLocalDatabasePath(basePath));

        Set<String> remoteDatabasePeers = listRemoteDatabasePeers(basePath);
        Map<PeerID, RemoteDatabase> remoteDatabases = new HashMap<>();
        for (String peerIDStr : remoteDatabasePeers) {
            PeerID peerID = new PeerID(peerIDStr);
            RemoteDatabase remoteDatabase = new RemoteDatabase(generateRemoteDatabasePath(basePath, peerIDStr), peerID);
            VersionedObjectSerializer.deserialize(remoteDatabase, generateAnnotatedRemoteDatabasePath(basePath, peerIDStr), generateBackupRemoteDatabasePath(basePath, peerIDStr));
            remoteDatabases.put(peerID, remoteDatabase);
        }
        return new LibraryManager(integratedDatabase, localDatabase, remoteDatabases, librarySynchEvents, peerEngineClient);
    }

    public static void save(String basePath, LibraryManager libraryManager) throws IOException {
        saveIntegratedDatabase(basePath, libraryManager.getIntegratedDatabase());
        saveLocalDatabase(basePath, libraryManager.getLocalDatabase());
        saveRemoteDatabases(basePath, libraryManager.getRemoteDatabases());
    }

    private static void saveIntegratedDatabase(String basePath, IntegratedDatabase integratedDatabase) throws IOException {
        VersionedObjectSerializer.serialize(integratedDatabase, CRCBytes, generateAnnotatedIntegratedDatabasePath(basePath), generateBackupIntegratedDatabasePath(basePath));
    }

    private static void saveLocalDatabase(String basePath, LocalDatabase localDatabase) throws IOException {
        VersionedObjectSerializer.serialize(localDatabase, CRCBytes, generateAnnotatedLocalDatabasePath(basePath), generateBackupLocalDatabasePath(basePath));
    }

    private static void saveRemoteDatabases(String basePath, Map<PeerID, RemoteDatabase> remoteDatabases) throws IOException {
        for (Map.Entry<PeerID, RemoteDatabase> remoteDatabase : remoteDatabases.entrySet()) {
            String peerIDStr = remoteDatabase.getKey().toString();
            VersionedObjectSerializer.serialize(remoteDatabase.getValue(), CRCBytes, generateAnnotatedRemoteDatabasePath(basePath, peerIDStr), generateBackupRemoteDatabasePath(basePath, peerIDStr));
        }
    }

    private static void saveRemoteDatabase(String basePath, String peerIDStr, RemoteDatabase remoteDatabase) throws IOException {
        VersionedObjectSerializer.serialize(remoteDatabase, CRCBytes, generateAnnotatedRemoteDatabasePath(basePath, peerIDStr), generateBackupRemoteDatabasePath(basePath, peerIDStr));
    }


    private static String generateIntegratedDatabasePath(String basePath) {
        return generateFilePath(basePath, INTEGRATED_FILENAME, DATABASE_EXTENSION);
    }

    private static String generateAnnotatedIntegratedDatabasePath(String basePath) {
        return generateFilePath(basePath, INTEGRATED_FILENAME, ANNOTATED_DATABASE_EXTENSION);
    }

    private static String generateBackupIntegratedDatabasePath(String basePath) {
        return generateFilePath(basePath, INTEGRATED_FILENAME, BACKUP_EXTENSION);
    }

    private static String generateLocalDatabasePath(String basePath) {
        return generateFilePath(basePath, LOCAL_FILENAME, DATABASE_EXTENSION);
    }

    private static String generateAnnotatedLocalDatabasePath(String basePath) {
        return generateFilePath(basePath, LOCAL_FILENAME, ANNOTATED_DATABASE_EXTENSION);
    }

    private static String generateBackupLocalDatabasePath(String basePath) {
        return generateFilePath(basePath, LOCAL_FILENAME, BACKUP_EXTENSION);
    }

    private static String generateRemoteDatabasePath(String basePath, String peerID) {
        return FileUtil.joinPaths(basePath, REMOTE_DIRECTORY, peerID) + "." + DATABASE_EXTENSION;
    }

    private static String generateAnnotatedRemoteDatabasePath(String basePath, String peerID) {
        return FileUtil.joinPaths(basePath, REMOTE_DIRECTORY, peerID) + "." + ANNOTATED_DATABASE_EXTENSION;
    }

    private static String generateBackupRemoteDatabasePath(String basePath, String peerID) {
        return FileUtil.joinPaths(basePath, REMOTE_DIRECTORY, peerID) + "." + BACKUP_EXTENSION;
    }

    private static String generateFilePath(String basePath, String filePath, String extension) {
        return FileUtil.joinPaths(basePath, filePath) + "." + extension;
    }

    private static Set<String> listRemoteDatabasePeers(String basePath) throws FileNotFoundException {
        String[] filesInRemoteDir = FileUtil.getDirectoryContents(FileUtil.joinPaths(basePath, REMOTE_DIRECTORY));
        Set<String> remoteDatabasePeers = new HashSet<>();
        for (String file : filesInRemoteDir) {
            remoteDatabasePeers.add(FileUtil.getFileNameWithoutExtension(file));
        }
        return remoteDatabasePeers;
    }

    public static void createNewDatabaseFileStructure(String basePath, String version) throws IOException {
        // create local and integrated database. Create directories. Generate random identifiers
        DatabaseMediator.dropAndCreate(generateIntegratedDatabasePath(basePath), version, RandomStringUtils.randomAlphanumeric(ID_LENGTH));
        IntegratedDatabase integratedDatabase = new IntegratedDatabase(generateIntegratedDatabasePath(basePath), new HashMap<>(), new HashMap<>());
        saveIntegratedDatabase(basePath, integratedDatabase);

        DatabaseMediator.dropAndCreate(generateLocalDatabasePath(basePath), version, RandomStringUtils.randomAlphanumeric(ID_LENGTH));
        LocalDatabase localDatabase = new LocalDatabase(generateLocalDatabasePath(basePath), new HashMap<>());
        saveLocalDatabase(basePath, localDatabase);

        FileUtil.createDirectory(FileUtil.joinPaths(basePath, REMOTE_DIRECTORY));
    }

    static RemoteDatabase createNewRemoteDatabase(String basePath, PeerID peerID, String version) throws IOException {
        DatabaseMediator.dropAndCreate(generateRemoteDatabasePath(basePath, peerID.toString()), version, RandomStringUtils.randomAlphanumeric(ID_LENGTH));
        RemoteDatabase remoteDatabase = new RemoteDatabase(generateAnnotatedRemoteDatabasePath(basePath, peerID.toString()), new HashMap<>(), peerID);
        saveRemoteDatabase(basePath, peerID.toString(), remoteDatabase);
        return remoteDatabase;
    }

    public static void removeRemoteDatabase(String basePath, PeerID peerID) {
        //noinspection ResultOfMethodCallIgnored
        new File(generateAnnotatedRemoteDatabasePath(basePath, peerID.toString())).delete();
        //noinspection ResultOfMethodCallIgnored
        new File(generateBackupRemoteDatabasePath(basePath, peerID.toString())).delete();
    }

}
