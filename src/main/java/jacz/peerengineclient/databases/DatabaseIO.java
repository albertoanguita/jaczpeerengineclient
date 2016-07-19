package jacz.peerengineclient.databases;

import jacz.database.DatabaseMediator;
import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineclient.databases.integration.IntegrationEvents;
import jacz.peerengineclient.databases.synch.DatabaseSynchEvents;
import jacz.peerengineclient.file_system.PathConstants;
import jacz.peerengineservice.PeerId;
import org.aanguita.jacuzzi.io.serialization.VersionedSerializationException;
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
 */
public class DatabaseIO {

    private static final int CRCBytes = 4;

    private static final int ID_LENGTH = 12;

    public static DatabaseManager load(
            String basePath,
            DatabaseSynchEvents databaseSynchEvents,
            IntegrationEvents integrationEvents,
            PeerEngineClient peerEngineClient
    ) throws IOException, VersionedSerializationException {
        return new DatabaseManager(
                new Databases(peerEngineClient, basePath),
                databaseSynchEvents,
                integrationEvents,
                peerEngineClient);
    }

    public static void createNewDatabaseFileStructure(String basePath) throws IOException {
        DatabaseMediator.dropAndCreate(PathConstants.integratedDBPath(basePath), RandomStringUtils.randomAlphanumeric(ID_LENGTH));
        DatabaseMediator.dropAndCreate(PathConstants.localDBPath(basePath), RandomStringUtils.randomAlphanumeric(ID_LENGTH));
        DatabaseMediator.dropAndCreate(PathConstants.sharedDBPath(basePath), RandomStringUtils.randomAlphanumeric(ID_LENGTH));
        DatabaseMediator.dropAndCreate(PathConstants.deletedDBPath(basePath), RandomStringUtils.randomAlphanumeric(ID_LENGTH));
        ItemRelations.createNewFiles(basePath);
    }

    static String createNewRemoteDatabase(String basePath, PeerId peerID) throws IOException {
        // set up a temporary database with a pre-fixed identifier. This database will be re-created in the first synch
        String dbPath = PathConstants.remoteDBPath(basePath, peerID);
        DatabaseMediator.dropAndCreate(dbPath, "temp-id");
        return dbPath;
    }

    public static void removeRemoteDatabase(String basePath, PeerId peerID) {
        //noinspection ResultOfMethodCallIgnored
        new File(PathConstants.remoteDBPath(basePath, peerID)).delete();
    }

}
