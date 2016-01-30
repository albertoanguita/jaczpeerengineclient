package jacz.peerengineclient;

import jacz.database.Chapter;
import jacz.database.DatabaseMediator;
import jacz.database.Movie;
import jacz.database.TVSeries;
import jacz.database.util.ImageHash;
import jacz.peerengineclient.data.MoveFileAction;
import jacz.peerengineclient.data.PeerShareIO;
import jacz.peerengineclient.data.PeerShareManager;
import jacz.peerengineclient.databases.DatabaseIO;
import jacz.peerengineclient.databases.DatabaseManager;
import jacz.peerengineclient.databases.Databases;
import jacz.peerengineclient.databases.integration.IntegrationEvents;
import jacz.peerengineclient.databases.synch.DatabaseSynchEvents;
import jacz.peerengineclient.file_system.Paths;
import jacz.peerengineclient.util.synch.RemoteSynchReminder;
import jacz.peerengineservice.NotAliveException;
import jacz.peerengineservice.PeerEncryption;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.UnavailablePeerException;
import jacz.peerengineservice.client.GeneralEvents;
import jacz.peerengineservice.client.PeerClient;
import jacz.peerengineservice.client.PeerRelations;
import jacz.peerengineservice.client.PeersPersonalData;
import jacz.peerengineservice.client.connection.ConnectionEvents;
import jacz.peerengineservice.client.connection.NetworkConfiguration;
import jacz.peerengineservice.client.connection.State;
import jacz.peerengineservice.util.data_synchronization.DataAccessor;
import jacz.peerengineservice.util.data_synchronization.SynchError;
import jacz.peerengineservice.util.datatransfer.ResourceTransferEvents;
import jacz.peerengineservice.util.datatransfer.TransferStatistics;
import jacz.peerengineservice.util.datatransfer.master.DownloadManager;
import jacz.peerengineservice.util.datatransfer.resource_accession.BasicFileWriter;
import jacz.peerengineservice.util.datatransfer.resource_accession.ResourceWriter;
import jacz.peerengineservice.util.datatransfer.resource_accession.TempFileWriter;
import jacz.peerengineservice.util.datatransfer.slave.UploadManager;
import jacz.peerengineservice.util.tempfile_api.TempFileManager;
import jacz.peerengineservice.util.tempfile_api.TempFileManagerEvents;
import jacz.util.files.FileUtil;
import jacz.util.hash.HashFunction;
import jacz.util.hash.MD5;
import jacz.util.hash.hashdb.FileHashDatabase;
import jacz.util.io.serialization.VersionedSerializationException;
import jacz.util.lists.tuple.Triple;
import jacz.util.log.ErrorHandler;
import jacz.util.notification.ProgressNotificationWithError;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * PeerEngine client adapted for Jacuzzi
 * <p>
 * todo recover temp files in constructor and put them in paused mode
 * <p>
 * todo si no tiene conexion, no debería sacar la ip pública
 */
public class PeerEngineClient {

    public static final String DEFAULT_STORE = "@J_PEER_ENGINE_CLIENT_DEFAULT_RESOURCE_STORE";

    public static final String MEDIA_STORE = "@PEER_ENGINE_CLIENT_MEDIA_RESOURCE_STORE";

    public static final String IMAGE_STORE = "@PEER_ENGINE_CLIENT_IMAGE_RESOURCE_STORE";

    private static final String HASH_ALGORITHM = "MD5";

    private static final String DEFAULT_TEMPORARY_FILE_NAME = "downloadedFile";

    private static final boolean ALLOW_SYNCH_BETWEEN_NON_FRIEND_PEERS = false;

    static final String USER_GENERIC_DATA_FIELD_GROUP = "@USER_GENERIC_DATA_FIELD_GROUP";

    static final String OWN_GENERIC_DATA_FIELD_GROUP = "@OWN_GENERIC_DATA_FIELD_GROUP";

    static final String FINAL_PATH_GENERIC_DATA_FIELD = "@FINAL_PATH_GENERIC_DATA_FIELD";


//    /**
//     * Path to read config information for the user
//     */
//    private final String configPath;
//
//    /**
//     * Actions invoked upon different events
//     */
//    private JacuzziPeerClientAction jacuzziPeerClientAction;
//
////    private final SimplePersonalData ownData;
//
////    private final PeerClientData peerClientData;
//
//    private final PeerIDInfo peerIDInfo;
//
////    private final PeerRelations peerRelations;
//
//    /**
//     * Personal data (our and other peers nicks)
//     */
//    private final PeersPersonalData peersPersonalData;
////    private final Map<PeerID, SimplePersonalData> peersSimplePersonalData;
//
//    private final String tempDownloadsDirectory;
//
//    private final TempFileManager tempFileManager;
//
//    private final DatabaseManager databaseManager;
//
//    private final FileHashDatabase fileHashDatabase;
//
//    private String baseDataDir;
//
//
//    public PeerEngineClient(String configPath, JacuzziPeerClientAction jacuzziPeerClientAction, PeerIDInfo peerIDInfo, int port, String serverIP, int serverPort, String ownNick, Map<PeerID, String> peerNicks, PeerRelations peerRelations, String tempDownloadsDirectory, FileHashDatabase fileHashDatabase, String baseDataDir) throws IOException {
//        this.configPath = configPath;
//        this.jacuzziPeerClientAction = jacuzziPeerClientAction;
//        ownData = new SimplePersonalData(peerIDInfo.peerID, ownNick, null);
//        this.peerIDInfo = peerIDInfo;
//        PeerClientData peerClientData = new PeerClientData(peerIDInfo.peerID, port, new PeerServerData(new IP4Port(serverIP, serverPort)));
//        peersSimplePersonalData = new HashMap<>();
//        for (PeerID otherPeerID : peerNicks.keySet()) {
//            peersSimplePersonalData.put(peerIDInfo.peerID, new SimplePersonalData(otherPeerID, peerNicks.get(peerIDInfo.peerID), jacuzziPeerClientAction));
//        }
//        this.tempDownloadsDirectory = tempDownloadsDirectory;
//        tempFileManager = new TempFileManager(tempDownloadsDirectory);
//
//        this.fileHashDatabase = fileHashDatabase;
//        this.baseDataDir = baseDataDir;
//
//        try {
//            databaseManager = DatabaseIO.load(PathsOld.getDatabasesPath(configPath), new LibraryManagerNotificationsImpl(this, jacuzziPeerClientAction));
//        } catch (DBException | CorruptDataException e) {
//            throw new IOException("Could not access databases");
//        }
//        ListContainer listContainer = new ListContainerImpl(this, databaseManager);
//
////        Map<String, ListAccessor > basicReadingLists = new HashMap<>();
////        basicReadingLists.put(SimplePersonalData.getListName(), new NonIndexedListAccessorBridge(ownData));
////        BasicListContainer basicListContainer = new BasicListContainer(basicReadingLists);
//        downloadsManager = new DownloadsManager();
//        bridgePeerClientActionOLD = new BridgePeerClientActionOLD(this, jacuzziPeerClientAction, downloadsManager);
//        GlobalDownloadStatistics globalDownloadStatistics = new GlobalDownloadStatistics();
//        GlobalUploadStatistics globalUploadStatistics = new GlobalUploadStatistics();
//        PeerStatistics peerStatistics = new PeerStatistics();
//        peerClient = new PeerClient(peerClientData, bridgePeerClientActionOLD, globalDownloadStatistics, globalUploadStatistics, peerStatistics, peerRelations, new HashMap<String, PeerFSMFactory>(), listContainer, ALLOW_SYNCH_BETWEEN_NON_FRIEND_PEERS);
//        downloadsManager.setPeerClient(peerClient);
//
//    }

    private final String basePath;

    private final PeersPersonalData peersPersonalData;

    private final TransferStatistics transferStatistics;

    private final PeerClient peerClient;

//    private final String libraryManagerBasePath;

    private final DatabaseManager databaseManager;

    private final PeerShareManager peerShareManager;

    private final RemoteSynchReminder remoteSynchReminder;

    private final DownloadEvents downloadEvents;

    private final ErrorHandlerBridge errorHandler;

    private final TempFileManager tempFileManager;

    // todo use??
    private final DownloadsManager downloadsManager;

    private final String tempDownloadsPath;

    private final String downloadsPath;


    public PeerEngineClient(
            String basePath,
            PeerID ownPeerID,
            PeerEncryption peerEncryption,
            NetworkConfiguration networkConfiguration,
            PeersPersonalData peersPersonalData,
            TransferStatistics transferStatistics,
            PeerRelations peerRelations,
            String tempDownloadsPath,
            String downloadsPath,
            GeneralEvents generalEvents,
            ConnectionEvents connectionEvents,
            ResourceTransferEvents resourceTransferEvents,
            TempFileManagerEvents tempFileManagerEvents,
            DatabaseSynchEvents databaseSynchEvents,
            DownloadEvents downloadEvents,
            IntegrationEvents integrationEvents,
            ErrorHandler errorHandler) throws IOException, VersionedSerializationException {
        this.basePath = basePath;
        this.peersPersonalData = peersPersonalData;
        this.transferStatistics = transferStatistics;
        databaseManager = DatabaseIO.load(basePath, databaseSynchEvents, integrationEvents, this, peerRelations.getFriendPeers());
        peerShareManager = PeerShareIO.load(basePath, this);
        this.errorHandler = new ErrorHandlerBridge(this, errorHandler);
        DataAccessorContainerImpl dataAccessorContainer = new DataAccessorContainerImpl(databaseManager, peerShareManager);
        peerClient = new PeerClient(
                ownPeerID,
                peerEncryption,
                networkConfiguration,
                new GeneralEventsBridge(this, generalEvents),
                connectionEvents,
                resourceTransferEvents,
                peersPersonalData,
                transferStatistics,
                peerRelations,
                new HashMap<>(),
                dataAccessorContainer,
                this.errorHandler);

        peerShareManager.setPeerClient(peerClient);
        remoteSynchReminder = new RemoteSynchReminder(this, databaseManager.getDatabaseSynchManager(), peerShareManager);
        remoteSynchReminder.start();
        tempFileManager = new TempFileManager(tempDownloadsPath, tempFileManagerEvents);
        this.downloadEvents = downloadEvents;
        downloadsManager = new DownloadsManager(peerClient);
        this.tempDownloadsPath = tempDownloadsPath;
        this.downloadsPath = downloadsPath;

        peerClient.setLocalGeneralResourceStore(new GeneralResourceStoreImpl(peerShareManager.getFileHash(), tempFileManager));
    }

    public PeerClient getPeerClient() {
        return peerClient;
    }

    public void connect() {
        peerClient.connect();
    }

    public void disconnect() {
        peerClient.disconnect();
    }

    public void stop() throws IOException {
        remoteSynchReminder.stop();
        if (databaseManager != null) {
            databaseManager.stop();
            DatabaseIO.save(basePath, databaseManager);
        }
        if (peerShareManager != null) {
            PeerShareIO.save(basePath, peerShareManager);
        }
        if (transferStatistics != null) {
            transferStatistics.stop();
        }
        if (tempFileManager != null) {
            tempFileManager.stop();
        }
        if (peerClient != null) {
            peerClient.stop();
        }
        // todo save all data
    }

    public Databases getDatabases() {
        return databaseManager.getDatabases();
    }

    public State getConnectionState() {
        return peerClient.getConnectionState();
    }

    public int getListeningPort() {
        return peerClient.getListeningPort();
    }

    public void setListeningPort(int port) {
        peerClient.setListeningPort(port);
    }

    public boolean isFriendPeer(PeerID peerID) {
        return peerClient.isFriendPeer(peerID);
    }

    public boolean isBlockedPeer(PeerID peerID) {
        return peerClient.isBlockedPeer(peerID);
    }

    public boolean isNonRegisteredPeer(PeerID peerID) {
        return peerClient.isNonRegisteredPeer(peerID);
    }

    public Set<PeerID> getFriendPeers() {
        return peerClient.getFriendPeers();
    }

    public void addFriendPeer(PeerID peerID) {
        // add personal data so its data can be saved
        peerClient.addFriendPeer(peerID);
    }

    public void removeFriendPeer(PeerID peerID) {
        peerClient.removeFriendPeer(peerID);
    }

    public Set<PeerID> getBlockedPeers() {
        return peerClient.getBlockedPeers();
    }

    public void addBlockedPeer(PeerID peerID) {
        peerClient.addBlockedPeer(peerID);
    }

    public void removeBlockedPeer(PeerID peerID) {
        peerClient.removeBlockedPeer(peerID);
    }

    synchronized void peerIsNowFriend(PeerID peerID) {
        try {
            databaseManager.addPeer(basePath, peerID);
        } catch (IOException e) {
            // todo handle
            e.printStackTrace();
        }
    }

    synchronized void peerIsNoLongerFriend(PeerID peerID) {
        databaseManager.removePeer(basePath, peerID);
        peerShareManager.removeRemotePeer(basePath, peerID);
    }

    synchronized void peerConnected(PeerID peerID) {
        peerShareManager.peerConnected(basePath, peerID);
    }

    synchronized void peerDisconnected(PeerID peerID) {
        peerShareManager.peerDisconnected(basePath, peerID);
    }

    public synchronized void setNick(String nick) {
        peerClient.setNick(nick);
    }

//    void reportModifiedSharedLibraries(Map<String, List<Integer>> modifiedLibraries) {
//        broadcastObjectMessage(new ModifiedSharedLibrariesMessage(modifiedLibraries));
//    }

//    synchronized void remoteLibrariesNeedSynchronizing(PeerID peerID, Map<String, List<Integer>> remoteLibrariesModified) {
//        for (String library : remoteLibrariesModified.keySet()) {
//            databaseManager.remoteLibrariesMustBeSynched(peerID, library, remoteLibrariesModified.get(library));
//        }
//    }

//    public synchronized void synchAllPeerLibraries(PeerID peerID) {
//        remoteLibrariesNeedSynchronizing(peerID, ListAccessorManager.getLibrariesAndLevels());
//    }

//    public PeerIDInfo getPeerIDInfo() {
//        return peerIDInfo;
//    }

    public String getOwnNick() {
        return peersPersonalData.getOwnNick();
    }

    public String getNick(PeerID peerID) {
        return peersPersonalData.getPeerNick(peerID);
    }

    /**
     * This method forces the peer engine to search for connected friends. In principle, it is not necessary to use
     * this method, since this search is automatically performed when the PeerClient connects to a PeerServer. Still,
     * there might be cases in which it is recommendable (to search for a friend peer who has not listed us as friend,
     * since he will not try to connect to us, etc)
     */
    public void searchFriends() {
        peerClient.searchFriends();
    }

    public synchronized String addLocalFile(String path) throws IOException {
        return peerShareManager.getFileHash().put(path);
    }

    public synchronized String addLocalFile(String path, MoveFileAction moveFileAction, Movie movie) throws IOException {
        return addLocalFile(path, moveFileAction, movie, null, null);
    }

    public synchronized String addLocalFile(String path, MoveFileAction moveFileAction, TVSeries tvSeries, Chapter chapter) throws IOException {
        return addLocalFile(path, moveFileAction, null, tvSeries, chapter);
    }

    private synchronized String addLocalFile(
            String path,
            MoveFileAction moveFileAction,
            Movie movie,
            TVSeries tvSeries,
            Chapter chapter) throws IOException {
        if (!peerShareManager.getFileHash().containsValue(path)) {
            String newPath;
            Triple<String, String, String> location = null;
            if (moveFileAction == MoveFileAction.MOVE_TO_MEDIA_REPO) {
                if (movie != null) {
                    location = Paths.movieFilePath(downloadsPath, movie.getId(), movie.getTitle(), FileUtil.getFileName(path));
                } else {
                    location = Paths.seriesFilePath(downloadsPath, tvSeries.getId(), tvSeries.getTitle(), chapter.getId(), chapter.getTitle(), FileUtil.getFileName(path));
                }
            } else {
                newPath = Paths.imageFilePath(downloadsPath, path);
                String fileName = FileUtil.getFileName(newPath);
                location = new Triple<>(FileUtil.getFileDirectory(path), FileUtil.getFileNameWithoutExtension(fileName), FileUtil.getFileExtension(fileName));
            }
            newPath = FileUtil.createFile(location.element1, location.element2, location.element3, "(", ")", true).element1;
            FileUtil.move(path, newPath);
            return addLocalFile(newPath);
        } else {
            return addLocalFile(path);
        }
    }

    public synchronized void removeLocalFile(String key, boolean removeFile) {
        String path = peerShareManager.getFileHash().remove(key);
        if (path != null && removeFile) {
            File file = new File(path);
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    public synchronized DownloadManager downloadMediaFile(
            DownloadInfo.Type type,
            DatabaseMediator.ItemType containerType,
            int containerId,
            Integer superContainerId,
            int itemId,
            String fileHash,
            String fileName) throws IOException, NotAliveException {
        HashMap<String, Serializable> userDictionary =
                buildUserDictionary(type, containerType, containerId, superContainerId, itemId, fileHash, fileName);
        ResourceWriter resourceWriter = new TempFileWriter(tempFileManager, userDictionary);
        return downloadFile(MEDIA_STORE, fileHash, resourceWriter, 0d);
    }

    public synchronized DownloadManager downloadImage(
            DatabaseMediator.ItemType containerType,
            int containerId,
            ImageHash imageHash) throws IOException, NotAliveException {
        HashMap<String, Serializable> userDictionary =
                buildUserDictionary(DownloadInfo.Type.IMAGE, containerType, containerId, null, null, imageHash.getHash(), imageHash.serialize());
        ResourceWriter resourceWriter = new BasicFileWriter(Paths.imagesDir(downloadsPath), Paths.imageFileName(downloadsPath, imageHash), userDictionary);
        return downloadFile(IMAGE_STORE, imageHash.getHash(), resourceWriter, 0d);
    }

    static HashMap<String, Serializable> buildUserDictionary(
            DownloadInfo.Type type,
            DatabaseMediator.ItemType containerType,
            int containerId,
            Integer superContainerId,
            Integer itemId,
            String fileHash,
            String fileName) {
        DownloadInfo downloadInfo = new DownloadInfo(type, containerType, containerId, superContainerId, itemId, fileHash, fileName);
        return downloadInfo.buildUserDictionary();
    }


    private DownloadManager downloadFile(
            String resourceStore,
            String fileHash,
            ResourceWriter resourceWriter,
            double streamingNeed) throws IOException, NotAliveException {
        return peerClient.downloadResource(
                resourceStore,
                fileHash,
                resourceWriter,
                new DownloadProgressNotificationHandlerBridge(downloadEvents, databaseManager.getDatabases().getIntegratedDB(), downloadsPath),
                streamingNeed,
                fileHash,
                HASH_ALGORITHM);
    }


//    /**
//     * Initiates the process for downloading a file (group download). No store is specified so the default store is used. Providers of the file
//     * will be obtained from the defaultForeignPeerShare.
//     *
//     * @param resourceID      ID of the resource
//     * @param finalPath       path were the file will be stored
//     * @param visible         whether this download should appear in the VisibleDownloadsManager (true) or not (false)
//     * @param stoppable       whether this download must be persistent (can be resumed after stop or program exit)
//     * @param downloadEvents  handler for receiving notifications concerning this download
//     * @param streamingNeed   the need for streaming this file (0: no need, 1: max need). The higher the need,
//     *                        the greater efforts that the scheduler will do for downloading the first parts
//     *                        of the resource before the last parts. Can hamper total download efficiency
//     * @param userGenericData a map of generic data which will be stored in this download and reported back to the user when the download is complete
//     * @return a DownloadManager object for controlling this download
//     * @throws IOException the download could not be initiated due to problems generating the target file
//     */
//    public synchronized jacz.peerengineservice.util.datatransfer.master.DownloadManager downloadFile(
//            String resourceID,
//            String finalPath,
//            boolean visible,
//            boolean stoppable,
//            DownloadEvents downloadEvents,
//            double streamingNeed,
//            Map<String, Serializable> userGenericData,
//            String totalHash,
//            String totalHashAlgorithm) throws IOException {
//        return downloadFile(resourceID, finalPath, visible, stoppable, downloadEvents, streamingNeed, userGenericData, DEFAULT_STORE, totalHash, totalHashAlgorithm);
//    }
//
//    /**
//     * Initiates the process for downloading a file (group download). The store is specified by the user. Providers of the file
//     * will be obtained from the peers share associated to the specified store.
//     *
//     * @param resourceID      ID of the resource
//     * @param finalPath       path were the file will be stored
//     * @param visible         whether this download should appear in the VisibleDownloadsManager (true) or not (false)
//     * @param stoppable       whether this download must be persistent (can be resumed after stop or program exit)
//     * @param downloadEvents  handler for receiving notifications concerning this download
//     * @param streamingNeed   the need for streaming this file (0: no need, 1: max need). The higher the need,
//     *                        the greater efforts that the scheduler will do for downloading the first parts
//     *                        of the resource before the last parts. Can hamper total download efficiency
//     * @param userGenericData a map of generic data which will be stored in this download and reported back to the user when the download is complete
//     * @param resourceStore   name of the store allocating the resource
//     * @return a DownloadManager object for controlling this download
//     * @throws IOException the download could not be initiated due to problems generating the target file
//     */
//    public synchronized jacz.peerengineservice.util.datatransfer.master.DownloadManager downloadFile(
//            String resourceID,
//            String finalPath,
//            boolean visible,
//            boolean stoppable,
//            DownloadEvents downloadEvents,
//            double streamingNeed,
//            Map<String, Serializable> userGenericData,
//            String resourceStore,
//            String totalHash,
//            String totalHashAlgorithm) throws IOException {
//        return downloadResource(null, resourceID, finalPath, visible, stoppable, downloadEvents, streamingNeed, userGenericData, resourceStore, totalHash, totalHashAlgorithm);
//    }
//
//    /**
//     * Initiates the process for downloading a file (specific download). No store is specified so the default store is used. Providers of the file
//     * will be obtained from the defaultForeignPeerShare.
//     *
//     * @param serverPeerID    only peer from which we will download the file
//     * @param resourceID      ID of the resource
//     * @param finalPath       path were the file will be stored
//     * @param visible         whether this download should appear in the VisibleDownloadsManager (true) or not (false)
//     * @param stoppable       whether this download must be persistent (can be resumed after stop or program exit)
//     * @param downloadEvents  handler for receiving notifications concerning this download
//     * @param streamingNeed   the need for streaming this file (0: no need, 1: max need). The higher the need,
//     *                        the greater efforts that the scheduler will do for downloading the first parts
//     *                        of the resource before the last parts. Can hamper total download efficiency
//     * @param userGenericData a map of generic data which will be stored in this download and reported back to the user when the download is complete
//     * @return a DownloadManager object for controlling this download
//     * @throws IOException the download could not be initiated due to problems generating the target file
//     */
//    public synchronized jacz.peerengineservice.util.datatransfer.master.DownloadManager downloadFile(
//            PeerID serverPeerID,
//            String resourceID,
//            String finalPath,
//            boolean visible,
//            boolean stoppable,
//            DownloadEvents downloadEvents,
//            double streamingNeed,
//            Map<String, Serializable> userGenericData,
//            String totalHash,
//            String totalHashAlgorithm) throws IOException {
//        return downloadFile(serverPeerID, resourceID, finalPath, visible, stoppable, downloadEvents, streamingNeed, userGenericData, DEFAULT_STORE, totalHash, totalHashAlgorithm);
//    }
//
//    /**
//     * Initiates the process for downloading a file (specific download). The store is specified by the user. Providers of the file
//     * will be obtained from the peers share associated to the specified store.
//     *
//     * @param serverPeerID    only peer from which we will download the file
//     * @param resourceID      ID of the resource
//     * @param finalPath       path were the file will be stored
//     * @param visible         whether this download should appear in the VisibleDownloadsManager (true) or not (false)
//     * @param stoppable       whether this download must be persistent (can be resumed after stop or program exit)
//     * @param downloadEvents  handler for receiving notifications concerning this download
//     * @param streamingNeed   the need for streaming this file (0: no need, 1: max need). The higher the need,
//     *                        the greater efforts that the scheduler will do for downloading the first parts
//     *                        of the resource before the last parts. Can hamper total download efficiency
//     * @param userGenericData a map of generic data which will be stored in this download and reported back to the user when the download is complete
//     * @param resourceStore   name of the store allocating the resource
//     * @return a DownloadManager object for controlling this download
//     * @throws IOException the download could not be initiated due to problems generating the target file
//     */
//    public synchronized jacz.peerengineservice.util.datatransfer.master.DownloadManager downloadFile(
//            PeerID serverPeerID,
//            String resourceID,
//            String finalPath,
//            boolean visible,
//            boolean stoppable,
//            DownloadEvents downloadEvents,
//            double streamingNeed,
//            Map<String, Serializable> userGenericData,
//            String resourceStore,
//            String totalHash,
//            String totalHashAlgorithm) throws IOException {
//        return downloadResource(serverPeerID, resourceID, finalPath, visible, stoppable, downloadEvents, streamingNeed, userGenericData, resourceStore, totalHash, totalHashAlgorithm);
//    }
//
//    private jacz.peerengineservice.util.datatransfer.master.DownloadManager downloadResource(
//            PeerID serverPeerID,
//            String resourceID,
//            String finalPath,
//            boolean visible,
//            boolean stoppable,
//            DownloadEvents downloadEvents,
//            double streamingNeed,
//            Map<String, Serializable> userGenericData,
//            String resourceStore,
//            String totalHash,
//            String totalHashAlgorithm) throws IOException {
//        Duple<ResourceWriter, String> resourceWriterAndCurrentPath = generateResourceWriter(finalPath, stoppable, tempDownloadsDirectory, userGenericData);
//        ResourceWriter resourceWriter = resourceWriterAndCurrentPath.element1;
//        String currentPath = resourceWriterAndCurrentPath.element2;
//        DownloadProgressNotificationHandlerBridge downloadProgressNotificationHandler = new DownloadProgressNotificationHandlerBridge(downloadEvents);
//        jacz.peerengineservice.util.datatransfer.master.DownloadManager peerEngineDownloadManager;
//        if (serverPeerID != null) {
//            peerEngineDownloadManager = peerClient.downloadResource(serverPeerID, resourceStore, resourceID, resourceWriter, downloadProgressNotificationHandler, streamingNeed, totalHash, totalHashAlgorithm);
//        } else {
//            peerEngineDownloadManager = peerClient.downloadResource(resourceStore, resourceID, resourceWriter, downloadProgressNotificationHandler, streamingNeed, totalHash, totalHashAlgorithm);
//        }
//        DownloadManagerOLD downloadManager = new DownloadManagerOLD(peerEngineDownloadManager, downloadEvents, resourceWriter, currentPath, finalPath, userGenericData);
//        downloadProgressNotificationHandler.setDownloadManager(downloadManager);
////        if (visible) {
////            bridgePeerClientActionOLD.addVisibleDownload(downloadManager);
////        }
//        return downloadManager;
//    }
//
//    private Duple<ResourceWriter, String> generateResourceWriter(String finalPath, boolean stoppable, String tempFileDir, Map<String, Serializable> userGenericData) throws IOException {
//        ResourceWriter resourceWriter;
//        String currentFilePath;
//        if (stoppable) {
//            // a temp resource writer is needed
//            HashMap<String, Serializable> userDictionary = new HashMap<>();
//            // todo store download info
//            TempFileWriter tempFileWriter = new TempFileWriter(tempFileManager);
//            currentFilePath = tempFileWriter.getTempDataFilePath();
//            resourceWriter = tempFileWriter;
//        } else {
//            // a basic resource writer is enough. If no finalPath is specified, we must select one (use the same approach than the temp file manager)
//            BasicFileWriter basicFileWriter;
//            if (finalPath == null) {
//                finalPath = DEFAULT_TEMPORARY_FILE_NAME;
//                basicFileWriter = new BasicFileWriter(tempFileDir, finalPath);
//            } else {
//                basicFileWriter = new BasicFileWriter(finalPath);
//            }
//            currentFilePath = basicFileWriter.getPath();
//            resourceWriter = basicFileWriter;
//        }
//        resourceWriter.setUserGenericData(USER_GENERIC_DATA_FIELD_GROUP, userGenericData);
//        resourceWriter.setUserGenericDataField(OWN_GENERIC_DATA_FIELD_GROUP, FINAL_PATH_GENERIC_DATA_FIELD, finalPath);
//        return new Duple<>(resourceWriter, currentFilePath);
//    }

    /**
     * Initiates the process for downloading a resource from a specific peer. In this case it is also necessary to
     * specify the target store. However, it is not required that we have this store updated (not even registered) with
     * the resources shared on it
     *
     * @param serverPeerID      ID of the Peer from which the resource is to be downloaded
     * @param resourceStoreName name of the individual store to access
     * @param resourceID        ID of the resource
     * @param visible           whether this download should appear in the VisibleDownloadsManager (true) or not (false)
     * @param downloadProgressNotificationHandler
     *                          handler for receiving notifications concerning this download
     * @param streamingNeed     the need for streaming this file (0: no need, 1: max need). The higher the need,
     *                          the greater efforts that the scheduler will do for downloading the first parts
     *                          of the resource before the last parts. Can hamper total download efficiency
     * @return a DownloadManager object for controlling this download, or null if the download could not be created
     *         (due to the resource store name given not corresponding to any existing resource store)
     */
    /*public synchronized DownloadManager downloadResource(PeerID serverPeerID, String resourceStoreName, String resourceID, String path, boolean visible, DownloadProgressNotificationHandler downloadProgressNotificationHandler, double streamingNeed) {
        return peerClient.getResourceStreamingManager().downloadResource(serverPeerID, resourceStoreName, resourceID, null, visible, downloadProgressNotificationHandler, streamingNeed);
    }*/

    /**
     * Retrieves the maximum allowed speed for downloading data from other peers. A null value indicates that no limit has been established
     *
     * @return the maximum allowed speed for receiving data from other peers, in KBytes per second (or null if no limit is established)
     */
    public synchronized Integer getMaxDesiredDownloadSpeed() {
        Float speed = peerClient.getMaxDesiredDownloadSpeed();
        return speed == null ? null : (int) (Math.ceil(speed / 1024f));
    }

    /**
     * Sets the maximum allows speed for downloading data from other peers. The value is provided in KBytes per second. A null or negative value
     * is considered as no limit
     *
     * @param totalMaxDesiredSpeed the value, in KBytes per second, for limiting download speed of data transfer to other peers
     */
    public synchronized void setMaxDesiredDownloadSpeed(Integer totalMaxDesiredSpeed) {
        Float speed = (totalMaxDesiredSpeed == null || totalMaxDesiredSpeed < 0) ? null : (float) (totalMaxDesiredSpeed * 1024);
        peerClient.setMaxDesiredDownloadSpeed(speed);
    }

    /**
     * Retrieves the maximum allowed speed for transferring data to other peers. A null value indicates that no limit has been established
     *
     * @return the maximum allowed speed for sending data to other peers, in KBytes per second (or null if no limit is established)
     */
    public synchronized Integer getMaxDesiredUploadSpeed() {
        Float speed = peerClient.getMaxDesiredUploadSpeed();
        return speed == null ? null : (int) (Math.ceil(speed / 1024f));
    }

    /**
     * Sets the maximum allows speed for transferring data to other peers. The value is provided in KBytes per second. A null or negative value
     * is considered as no limit
     *
     * @param totalMaxDesiredSpeed the value, in KBytes per second, for limiting upload speed of data transfer to other peers
     */
    public synchronized void setMaxDesiredUploadSpeed(Integer totalMaxDesiredSpeed) {
        Float speed = (totalMaxDesiredSpeed == null || totalMaxDesiredSpeed < 0) ? null : (float) (totalMaxDesiredSpeed * 1024);
        peerClient.setMaxDesiredUploadSpeed(speed);
    }

    /**
     * Retrieves a shallow copy of the active downloads (only visible ones)
     *
     * @return a shallow copy of the active downloads
     */
    public List<DownloadManager> getVisibleDownloads(String resourceStore) {
        return peerClient.getVisibleDownloads(resourceStore);
    }


    public synchronized void setVisibleDownloadsManagerTimer(long millis) {
        peerClient.setVisibleDownloadsTimer(millis);
    }

    public synchronized void stopVisibleDownloadsManager() {
        peerClient.stopVisibleDownloadsTimer();
    }

    /**
     * Retrieves a shallow copy of the active downloads (only visible ones)
     *
     * @return a shallow copy of the active downloads
     */
    public List<UploadManager> getVisibleUploads(String resourceStore) {
        return peerClient.getVisibleUploads(resourceStore);
    }

    public PeerID getNextConnectedPeer(PeerID peerID) {
        return peerClient.getNextConnectedPeer(peerID);
    }

    public synchronized void setVisibleUploadsManagerTimer(long millis) {
        peerClient.setVisibleUploadsTimer(millis);
    }

    public synchronized void stopVisibleUploadsManager() {
        peerClient.stopVisibleUploadsTimer();
    }


    public boolean synchronizeList(PeerID peerID, DataAccessor dataAccessor, long timeout, ProgressNotificationWithError<Integer, SynchError> progress) throws UnavailablePeerException {
        return peerClient.getDataSynchronizer().synchronizeData(peerID, dataAccessor, timeout, progress);
    }

    // todo use?
    public synchronized void localItemModified(String library, String elementIndex) {
        databaseManager.localItemModified(library, elementIndex);
    }

    FileHashDatabase getFileHashDatabase() {
        return peerShareManager.getFileHash();
    }

    public synchronized String getBaseDataDir() {
        return basePath;
    }

    public String getTempDownloadsPath() {
        return tempDownloadsPath;
    }

    public String getDownloadsPath() {
        return downloadsPath;
    }

    public static HashFunction getHashFunction() {
        try {
            return new HashFunction(HASH_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            // ignore, cannot happen
            return new MD5();
        }
    }
}
