package jacz.peerengineclient.databases;

import jacz.database.DatabaseMediator;
import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineclient.file_system.Paths;
import jacz.peerengineclient.databases.integration.IntegrationEvents;
import jacz.peerengineclient.databases.synch.LibrarySynchEvents;
import jacz.peerengineservice.PeerID;
import jacz.util.io.object_serialization.VersionedObjectSerializer;
import jacz.util.io.object_serialization.VersionedSerializationException;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.File;
import java.io.IOException;

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
public class DatabaseIO {

    private static final int CRCBytes = 4;

    private static final int ID_LENGTH = 12;

    public static DatabaseManager load(
            String basePath,
            LibrarySynchEvents librarySynchEvents,
            IntegrationEvents integrationEvents,
            PeerEngineClient peerEngineClient
    ) throws IOException, VersionedSerializationException {
        return new DatabaseManager(
                new Databases(basePath),
                librarySynchEvents,
                integrationEvents,
                peerEngineClient);
    }

    public static void save(String basePath, DatabaseManager databaseManager) throws IOException {
        saveItemRelations(basePath, databaseManager.getDatabases().getItemRelations());
    }

    private static void saveItemRelations(String basePath, ItemRelations itemRelations) throws IOException {
        VersionedObjectSerializer.serialize(itemRelations, CRCBytes, Paths.itemRelationsPath(basePath), Paths.itemRelationsBackupPath(basePath));
    }

    public static void createNewDatabaseFileStructure(String basePath) throws IOException {
        DatabaseMediator.dropAndCreate(Paths.integratedDBPath(basePath), RandomStringUtils.randomAlphanumeric(ID_LENGTH));
        DatabaseMediator.dropAndCreate(Paths.localDBPath(basePath), RandomStringUtils.randomAlphanumeric(ID_LENGTH));
        DatabaseMediator.dropAndCreate(Paths.sharedDBPath(basePath), RandomStringUtils.randomAlphanumeric(ID_LENGTH));
        DatabaseMediator.dropAndCreate(Paths.deletedDBPath(basePath), RandomStringUtils.randomAlphanumeric(ID_LENGTH));
        ItemRelations itemRelations = new ItemRelations();
        saveItemRelations(basePath, itemRelations);
    }

    static String createNewRemoteDatabase(String basePath, PeerID peerID) throws IOException {
        String dbPath = Paths.remoteDBPath(basePath, peerID);
        DatabaseMediator.dropAndCreate(dbPath, RandomStringUtils.randomAlphanumeric(ID_LENGTH));
        return dbPath;
    }

    public static void removeRemoteDatabase(String basePath, PeerID peerID) {
        //noinspection ResultOfMethodCallIgnored
        new File(Paths.remoteDBPath(basePath, peerID)).delete();
    }

}
