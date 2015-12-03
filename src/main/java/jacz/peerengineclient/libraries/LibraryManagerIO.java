package jacz.peerengineclient.libraries;

import jacz.peerengineclient.JPeerEngineClient;
import jacz.peerengineclient.dbs_old.LibraryManagerNotifications;
import jacz.peerengineclient.libraries.library_images.IntegratedDatabase;
import jacz.peerengineclient.libraries.library_images.LocalDatabase;
import jacz.peerengineclient.libraries.library_images.RemoteDatabase;
import jacz.peerengineclient.libraries.synch.LibrarySynchEvents;
import jacz.peerengineservice.PeerID;
import jacz.util.files.FileUtil;
import jacz.util.io.object_serialization.VersionedObjectSerializer;
import jacz.util.io.object_serialization.VersionedSerializationException;

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

    private final static String INTEGRATED_FILENAME = "integrated";

    private final static String LOCAL_FILENAME = "local";

    private final static String REMOTE_DIRECTORY = "remote";

    private final static String DATABASE_EXTENSION = "db";

    private final static String ANNOTATED_DATABASE_EXTENSION = "lib";

    private final static String BACKUP_EXTENSION = "bak";

    public static LibraryManager load(String basePath, LibrarySynchEvents librarySynchEvents, JPeerEngineClient peerEngineClient) throws IOException, VersionedSerializationException {
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
        LibraryManager libraryManager = new LibraryManager(integratedDatabase, localDatabase, remoteDatabases, librarySynchEvents, peerEngineClient);
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
        return generateFilePath(basePath, LOCAL_FILENAME, DATABASE_EXTENSION);
    }

    private static String generateAnnotatedRemoteDatabasePath(String basePath, String peerID) {
        return generateFilePath(basePath, LOCAL_FILENAME, ANNOTATED_DATABASE_EXTENSION);
    }

    private static String generateBackupRemoteDatabasePath(String basePath, String peerID) {
        return generateFilePath(basePath, LOCAL_FILENAME, BACKUP_EXTENSION);
    }

    private static String generateFilePath(String basePath, String filePath, String extension) {
        return FileUtil.joinPaths(basePath, filePath) + "." + DATABASE_EXTENSION;
    }

    private static Set<String> listRemoteDatabasePeers(String basePath) throws FileNotFoundException {
        String[] filesInRemoteDir = FileUtil.getDirectoryContents(FileUtil.joinPaths(basePath, REMOTE_DIRECTORY));
        Set<String> remoteDatabasePeers = new HashSet<>();
        for (String file : filesInRemoteDir) {
            remoteDatabasePeers.add(FileUtil.getFileNameWithoutExtension(file));
        }
        return remoteDatabasePeers;
    }

    public static void removeRemoteDatabase(PeerID peerID) {
        // todo invoke when we are no longer friends of a peer
    }
//    public static LibraryManager load(String path, LibraryManagerNotifications libraryManagerNotifications) throws IOException {
//        try {
//            String integratedPath = buildIntegratedPath(path);
//            Database database = new Database(new CSVDBMediator(integratedPath));
//            Date dateOfLastIntegration = (Date) FileReaderWriter.readObject(FileUtil.joinPaths(integratedPath, INTEGRATED_DATE_PATH));
//            //noinspection unchecked
//            HashMap<String, String> itemsToLocalItems = (HashMap<String, String>) FileReaderWriter.readObject(FileUtil.joinPaths(integratedPath, INTEGRATED_ITEMS_TO_LOCAL_ITEMS));
//            //noinspection unchecked
//            HashMap<String, List<IntegratedDatabase.PeerAndId>> itemsToRemoteItems = (HashMap<String, List<IntegratedDatabase.PeerAndId>>) FileReaderWriter.readObject(FileUtil.joinPaths(integratedPath, INTEGRATED_ITEMS_TO_REMOTE_ITEMS));
//            IntegratedDatabase integratedDatabase = new IntegratedDatabase(database, dateOfLastIntegration, itemsToLocalItems, itemsToRemoteItems);
//
//            String localPath = buildLocalPath(path);
//            database = new Database(new CSVDBMediator(localPath));
//            //noinspection unchecked
//            HashMap<String, String> itemsToIntegratedItems = (HashMap<String, String>) FileReaderWriter.readObject(FileUtil.joinPaths(localPath, LOCAL_ITEMS_TO_INTEGRATED_ITEMS));
//            LocalDatabase localDatabase = new LocalDatabase(database, itemsToIntegratedItems);
//
//            String remotePath = buildRemotePath(path);
//            File[] subFiles = new File(remotePath).listFiles();
//            Map<PeerID, RemoteDatabase> remoteDatabases = new HashMap<>();
//            if (subFiles != null) {
//                for (File remoteFile : subFiles) {
//                    if (remoteFile.isDirectory() && PeerID.isPeerID(remoteFile.getName())) {
//                        PeerID remotePeerID = new PeerID(remoteFile.getName());
//                        database = new Database(new CSVDBMediator(remoteFile.getAbsolutePath()));
//                        //noinspection unchecked
//                        itemsToIntegratedItems = (HashMap<String, String>) FileReaderWriter.readObject(FileUtil.joinPaths(remoteFile.getPath(), REMOTE_ITEMS_TO_INTEGRATED_ITEMS));
//                        remoteDatabases.put(remotePeerID, new RemoteDatabase(database, itemsToIntegratedItems, remotePeerID));
//                    }
//                }
//            } else {
//                throw new IOException("Wrong path: " + path);
//            }
//            return new LibraryManager(integratedDatabase, localDatabase, remoteDatabases, libraryManagerNotifications);
//        } catch (ClassNotFoundException e) {
//            // errors reading the files
//            throw new IOException("Error reading the files");
//        }
//    }
//
//    public static void save(String basePath, LibraryManager libraryManager) throws IOException {
//        // disconnect all databases, and save additional information
//        IntegratedDatabase integratedDatabase = libraryManager.getIntegratedDatabase();
//        integratedDatabase.getDatabase().close();
//        LocalDatabase localDatabase = libraryManager.getLocalDatabase();
//        localDatabase.getDatabase().close();
//        for (Map.Entry<PeerID, RemoteDatabase> remoteDatabaseEntry : libraryManager.getRemoteDatabases().entrySet()) {
//            remoteDatabaseEntry.getValue().getDatabase().close();
//        }
//        saveDatabasesMetadata(basePath, integratedDatabase, localDatabase, libraryManager.getRemoteDatabases());
//    }
//
//    private static void saveDatabasesMetadata(String basePath, IntegratedDatabase integratedDatabase, LocalDatabase localDatabase, Map<PeerID, RemoteDatabase> remoteDatabases) throws IOException {
//        String integratedPath = buildIntegratedPath(basePath);
//        FileReaderWriter.writeObject(FileUtil.joinPaths(integratedPath, INTEGRATED_DATE_PATH), integratedDatabase.getDateOfLastIntegration());
//        FileReaderWriter.writeObject(FileUtil.joinPaths(integratedPath, INTEGRATED_ITEMS_TO_LOCAL_ITEMS), integratedDatabase.getItemsToLocalItems());
//        FileReaderWriter.writeObject(FileUtil.joinPaths(integratedPath, INTEGRATED_ITEMS_TO_REMOTE_ITEMS), integratedDatabase.getItemsToRemoteItems());
//        String localPath = buildLocalPath(basePath);
//        FileReaderWriter.writeObject(FileUtil.joinPaths(localPath, LOCAL_ITEMS_TO_INTEGRATED_ITEMS), localDatabase.getItemsToIntegratedItems());
//        for (Map.Entry<PeerID, RemoteDatabase> remoteDatabaseEntry : remoteDatabases.entrySet()) {
//            String remotePath = buildRemotePath(basePath, remoteDatabaseEntry.getKey());
//            remoteDatabaseEntry.getValue().getDatabase().close();
//            FileReaderWriter.writeObject(FileUtil.joinPaths(remotePath, REMOTE_ITEMS_TO_INTEGRATED_ITEMS), remoteDatabaseEntry.getValue().getItemsToIntegratedItems());
//        }
//    }
//
//    public static void createNewDatabaseFileStructure(String basePath) throws DBException, IOException, CorruptDataException {
//        // create local and integrated database. Create directories
//        FileUtil.createDirectory(buildIntegratedPath(basePath));
//        FileUtil.createDirectory(buildLocalPath(basePath));
//        FileUtil.createDirectory(buildRemotePath(basePath));
//        DBMediator dbMediatorIntegrated = new CSVDBMediator(buildIntegratedPath(basePath));
//        Database.createEmpty(dbMediatorIntegrated);
//        Database database = new Database(dbMediatorIntegrated);
//        IntegratedDatabase integratedDatabase = new IntegratedDatabase(database);
//        database.close();
//        DBMediator dbMediatorLocal = new CSVDBMediator(buildLocalPath(basePath));
//        Database.createEmpty(dbMediatorLocal);
//        database = new Database(dbMediatorLocal);
//        LocalDatabase localDatabase = new LocalDatabase(database);
//        database.close();
//        saveDatabasesMetadata(basePath, integratedDatabase, localDatabase, new HashMap<PeerID, RemoteDatabase>());
//    }
//
//    static RemoteDatabase createNewRemoteDatabase(String basePath, PeerID peerID) throws DBException, IOException, CorruptDataException {
//        DBMediator dbMediator = new CSVDBMediator(buildRemotePath(basePath, peerID));
//        Database.createEmpty(dbMediator);
//        Database database = new Database(dbMediator);
//        return new RemoteDatabase(database, peerID);
//    }
//
//    public static void removeRemoteDatabase(String path, PeerID peerID) {
//        File file = new File(buildRemotePath(path, peerID));
//        if (file.isDirectory()) {
//            // remote directory and contents
//            //noinspection ResultOfMethodCallIgnored
//            file.delete();
//        }
//    }

    private static String buildIntegratedPath(String basePath) {
        return FileUtil.joinPaths(basePath, INTEGRATED_PATH);
    }

    private static String buildLocalPath(String basePath) {
        return FileUtil.joinPaths(basePath, LOCAL_PATH);
    }

    private static String buildRemotePath(String basePath) {
        return FileUtil.joinPaths(basePath, REMOTE_PATH);
    }

    private static String buildRemotePath(String basePath, PeerID peerID) {
        return FileUtil.joinPaths(buildRemotePath(basePath), peerID.toString());
    }
}
