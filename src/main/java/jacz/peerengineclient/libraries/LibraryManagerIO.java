package jacz.peerengineclient.libraries;

import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineclient.file_system.Paths;
import jacz.peerengineclient.libraries.integration.IntegrationEvents;
import jacz.peerengineclient.libraries.library_images.*;
import jacz.peerengineclient.libraries.synch.LibrarySynchEvents;
import jacz.peerengineservice.PeerID;
import jacz.store.database.DatabaseMediator;
import jacz.util.files.FileUtil;
import jacz.util.io.object_serialization.VersionedObjectSerializer;
import jacz.util.io.object_serialization.VersionedSerializationException;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
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

    private static final int CRCBytes = 4;

    private static final int ID_LENGTH = 12;

    public static LibraryManager load(
            String basePath,
            LibrarySynchEvents librarySynchEvents,
            IntegrationEvents integrationEvents,
            PeerEngineClient peerEngineClient
    ) throws IOException, VersionedSerializationException {
        IntegratedDatabase integratedDatabase = new IntegratedDatabase(Paths.getIntegratedDatabasePath(basePath));
        VersionedObjectSerializer.deserialize(integratedDatabase, Paths.getAnnotatedIntegratedDatabasePath(basePath), Paths.getBackupIntegratedDatabasePath(basePath));

        LocalDatabase localDatabase = new LocalDatabase(Paths.getLocalDatabasePath(basePath));
        VersionedObjectSerializer.deserialize(localDatabase, Paths.getAnnotatedLocalDatabasePath(basePath), Paths.getBackupLocalDatabasePath(basePath));

        Set<String> remoteDatabasePeers = Paths.listRemoteDatabasePeers(basePath);
        Map<PeerID, RemoteDatabase> remoteDatabases = new HashMap<>();
        for (String peerIDStr : remoteDatabasePeers) {
            PeerID peerID = new PeerID(peerIDStr);
            RemoteDatabase remoteDatabase = new RemoteDatabase(Paths.getRemoteDatabasePath(basePath, peerIDStr), peerID);
            VersionedObjectSerializer.deserialize(remoteDatabase, Paths.getAnnotatedRemoteDatabasePath(basePath, peerIDStr), Paths.getBackupRemoteDatabasePath(basePath, peerIDStr));
            remoteDatabases.put(peerID, remoteDatabase);
        }

        SharedLibrary sharedLibrary = new SharedLibrary(Paths.getSharedDatabasePath(basePath));
        DeletedRemoteItemsLibrary deletedRemoteItemsLibrary = new DeletedRemoteItemsLibrary(Paths.getDeletedDatabasePath(basePath));

        return new LibraryManager(
                integratedDatabase,
                localDatabase,
                remoteDatabases,
                sharedLibrary,
                deletedRemoteItemsLibrary,
                librarySynchEvents,
                integrationEvents,
                peerEngineClient);
    }

    public static void save(String basePath, LibraryManager libraryManager) throws IOException {
        saveIntegratedDatabase(basePath, libraryManager.getIntegratedDatabase());
        saveLocalDatabase(basePath, libraryManager.getLocalDatabase());
        saveRemoteDatabases(basePath, libraryManager.getRemoteDatabases());
    }

    private static void saveIntegratedDatabase(String basePath, IntegratedDatabase integratedDatabase) throws IOException {
        VersionedObjectSerializer.serialize(integratedDatabase, CRCBytes, Paths.getAnnotatedIntegratedDatabasePath(basePath), Paths.getBackupIntegratedDatabasePath(basePath));
    }

    private static void saveLocalDatabase(String basePath, LocalDatabase localDatabase) throws IOException {
        VersionedObjectSerializer.serialize(localDatabase, CRCBytes, Paths.getAnnotatedLocalDatabasePath(basePath), Paths.getBackupLocalDatabasePath(basePath));
    }

    private static void saveRemoteDatabases(String basePath, Map<PeerID, RemoteDatabase> remoteDatabases) throws IOException {
        for (Map.Entry<PeerID, RemoteDatabase> remoteDatabase : remoteDatabases.entrySet()) {
            String peerIDStr = remoteDatabase.getKey().toString();
            VersionedObjectSerializer.serialize(remoteDatabase.getValue(), CRCBytes, Paths.getAnnotatedRemoteDatabasePath(basePath, peerIDStr), Paths.getBackupRemoteDatabasePath(basePath, peerIDStr));
        }
    }

    private static void saveRemoteDatabase(String basePath, String peerIDStr, RemoteDatabase remoteDatabase) throws IOException {
        VersionedObjectSerializer.serialize(remoteDatabase, CRCBytes, Paths.getAnnotatedRemoteDatabasePath(basePath, peerIDStr), Paths.getBackupRemoteDatabasePath(basePath, peerIDStr));
    }


    public static void createNewDatabaseFileStructure(String basePath) throws IOException {
        // create local and integrated database. Create directories. get random identifiers
        DatabaseMediator.dropAndCreate(Paths.getIntegratedDatabasePath(basePath), RandomStringUtils.randomAlphanumeric(ID_LENGTH));
        IntegratedDatabase integratedDatabase = new IntegratedDatabase(Paths.getIntegratedDatabasePath(basePath), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>());
        saveIntegratedDatabase(basePath, integratedDatabase);

        DatabaseMediator.dropAndCreate(Paths.getLocalDatabasePath(basePath), RandomStringUtils.randomAlphanumeric(ID_LENGTH));
        LocalDatabase localDatabase = new LocalDatabase(Paths.getLocalDatabasePath(basePath), new HashMap<>());
        saveLocalDatabase(basePath, localDatabase);

        FileUtil.createDirectory(FileUtil.joinPaths(basePath, REMOTE_DIRECTORY));

        DatabaseMediator.dropAndCreate(Paths.getSharedDatabasePath(basePath), RandomStringUtils.randomAlphanumeric(ID_LENGTH));

        DatabaseMediator.dropAndCreate(Paths.getDeletedDatabasePath(basePath), RandomStringUtils.randomAlphanumeric(ID_LENGTH));
    }

    static RemoteDatabase createNewRemoteDatabase(String basePath, PeerID peerID) throws IOException {
        DatabaseMediator.dropAndCreate(Paths.getRemoteDatabasePath(basePath, peerID.toString()), RandomStringUtils.randomAlphanumeric(ID_LENGTH));
        RemoteDatabase remoteDatabase = new RemoteDatabase(Paths.getAnnotatedRemoteDatabasePath(basePath, peerID.toString()), new HashMap<>(), peerID);
        saveRemoteDatabase(basePath, peerID.toString(), remoteDatabase);
        return remoteDatabase;
    }

    public static void removeRemoteDatabase(String basePath, PeerID peerID) {
        //noinspection ResultOfMethodCallIgnored
        new File(Paths.getAnnotatedRemoteDatabasePath(basePath, peerID.toString())).delete();
        //noinspection ResultOfMethodCallIgnored
        new File(Paths.getBackupRemoteDatabasePath(basePath, peerID.toString())).delete();
    }

}
