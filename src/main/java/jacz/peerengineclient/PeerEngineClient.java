package jacz.peerengineclient;

import jacz.peerengineclient.file_system.FileIO;
import jacz.peerengineclient.file_system.Paths;
import jacz.peerengineclient.file_system.PathsOld;
import jacz.peerengineclient.file_system.PeerIDInfo;
import jacz.peerengineclient.libraries.LibraryManager;
import jacz.peerengineclient.libraries.LibraryManagerIO;
import jacz.peerengineservice.NotAliveException;
import jacz.peerengineservice.PeerEncryption;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.UnavailablePeerException;
import jacz.peerengineservice.client.*;
import jacz.peerengineservice.client.connection.ConnectionEvents;
import jacz.peerengineservice.client.connection.NetworkConfiguration;
import jacz.peerengineservice.client.connection.State;
import jacz.peerengineservice.util.ForeignStoreShare;
import jacz.peerengineservice.util.data_synchronization.AccessorNotFoundException;
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
import jacz.store.database.DatabaseMediator;
import jacz.util.hash.hashdb.FileHashDatabase;
import jacz.util.io.object_serialization.VersionedSerializationException;
import jacz.util.lists.Duple;
import jacz.util.notification.ProgressNotificationWithError;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * PeerEngine client adapted for Jacuzzi
 * <p>
 * todo check synch
 * <p>
 * todo recover temp files in constructor and put them in paused mode
 * <p>
 * todo si no tiene conexion, tarda mucho en dar un unable to connect to server (unos 25 segs)
 */
public class PeerEngineClient {

    public static final String DEFAULT_STORE = "@J_PEER_ENGINE_CLIENT_DEFAULT_RESOURCE_STORE";

    public static final String MEDIA_STORE = "@PEER_ENGINE_CLIENT_MEDIA_RESOURCE_STORE";

    public static final String IMAGE_STORE = "@PEER_ENGINE_CLIENT_IMAGE_RESOURCE_STORE";

    private static final String DEFAULT_TEMPORARY_FILE_NAME = "downloadedFile";

    private static final boolean ALLOW_SYNCH_BETWEEN_NON_FRIEND_PEERS = false;

    static final String USER_GENERIC_DATA_FIELD_GROUP = "@USER_GENERIC_DATA_FIELD_GROUP";

    static final String OWN_GENERIC_DATA_FIELD_GROUP = "@OWN_GENERIC_DATA_FIELD_GROUP";

    static final String FINAL_PATH_GENERIC_DATA_FIELD = "@FINAL_PATH_GENERIC_DATA_FIELD";

//    private final BridgePeerClientActionOLD bridgePeerClientActionOLD;

    private final DownloadsManager downloadsManager;

//    private final PeerClient peerClient;


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
//    private final LibraryManager libraryManager;
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
//            libraryManager = LibraryManagerIO.load(PathsOld.getDatabasesPath(configPath), new LibraryManagerNotificationsImpl(this, jacuzziPeerClientAction));
//        } catch (DBException | CorruptDataException e) {
//            throw new IOException("Could not access databases");
//        }
//        ListContainer listContainer = new ListContainerImpl(this, libraryManager);
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

    private final PeerClient peerClient;

    private final String libraryManagerBasePath;

    private LibraryManager libraryManager;

    private final TempFileManager tempFileManager;

    private final FileHashDatabase fileHashDatabase;

    public PeerEngineClient(
            String basePath,
            PeerID ownPeerID,
            PeerEncryption peerEncryption,
            NetworkConfiguration networkConfiguration,
            PeersPersonalData peersPersonalData,
            TransferStatistics transferStatistics,
            PeerRelations peerRelations,
            String libraryManagerBasePath,
            String tempDownloadsPath,
            String baseDataPath,
            GeneralEvents generalEvents,
            ConnectionEvents connectionEvents,
            ResourceTransferEvents resourceTransferEvents,
            TempFileManagerEvents tempFileManagerEvents) throws IOException, VersionedSerializationException {
        this.basePath = basePath;
        this.peersPersonalData = peersPersonalData;
        this.libraryManagerBasePath = libraryManagerBasePath;
        libraryManager = LibraryManagerIO.load(libraryManagerBasePath, librarySynchEvents, integrationEvents, this);
        DataAccessorContainerImpl dataAccessorContainer = new DataAccessorContainerImpl(libraryManager);
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
                dataAccessorContainer);

        fileHashDatabase = new FileHashDatabase(Paths.getHashPath(basePath), Paths.getHashBackupPath(basePath));
        tempFileManager = new TempFileManager(tempDownloadsPath, tempFileManagerEvents);

        peerClient.setLocalGeneralResourceStore(new GeneralResourceStoreImpl(fileHashDatabase, tempFileManager));
    }

    public void connect() {
        peerClient.connect();
    }

    public void disconnect() {
        peerClient.disconnect();
    }

    public void stop() throws IOException {
        if (libraryManager != null) {
            libraryManager.stop();
            LibraryManagerIO.save(libraryManagerBasePath, libraryManager);
        }
        if (peerClient != null) {
            peerClient.stop();
        }
        // todo save all data
    }

    public String getConfigPath() {
        return configPath;
    }


    synchronized void savePeerRelations(PeerRelations peerRelations) {
        try {
            FileIO.writePeerRelations(configPath, peerRelations);
        } catch (IOException e) {
            // todo
        } catch (XMLStreamException e) {
            // todo
        }
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
            libraryManager.addPeer(Paths.getLibrariesPath(basePath), peerID);
        } catch (IOException e) {
            // todo handle
            e.printStackTrace();
        }
    }

    synchronized void peerIsNoLongerFriend(PeerID peerID) {
        libraryManager.removePeer(Paths.getLibrariesPath(basePath), peerID);
    }

    synchronized void newPeerConnected(PeerID peerID) {
//        synchPersonalData(peerID);
//        synchAllPeerLibraries(peerID);
    }

//    synchronized void synchPersonalData(PeerID peerID) {
//        // todo check these values, are they good?
//        synchronizeList(peerID, SimplePersonalData.getListName(), 0, 10000);
//    }

    public synchronized void setNick(String nick) {
        peerClient.setNick(nick);
    }

//    void reportModifiedSharedLibraries(Map<String, List<Integer>> modifiedLibraries) {
//        broadcastObjectMessage(new ModifiedSharedLibrariesMessage(modifiedLibraries));
//    }

//    synchronized void remoteLibrariesNeedSynchronizing(PeerID peerID, Map<String, List<Integer>> remoteLibrariesModified) {
//        for (String library : remoteLibrariesModified.keySet()) {
//            libraryManager.remoteLibrariesMustBeSynched(peerID, library, remoteLibrariesModified.get(library));
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

//    /**
//     * Sends an object message to a connected peer. If the given peer is not among the list of connected peers, the
//     * message will be ignored
//     *
//     * @param peerID  ID of the peer to which the message is to be sent
//     * @param message string message to send
//     */
//    public void sendObjectMessage(PeerID peerID, Serializable message) {
//        peerClient.sendObjectMessage(peerID, message);
//    }
//
//    /**
//     * Sends an object message to all connected peers
//     *
//     * @param message string message to send to all connected peers
//     */
//    public void broadcastObjectMessage(Serializable message) {
//        peerClient.broadcastObjectMessage(message);
//    }

//    /**
//     * Adds a store of resources shared to us by other peers. It it used to handle downloads from other peers
//     *
//     * @param resourceStore    name of the resource store
//     * @param foreignPeerShare peers share for letting us know the share of resources of each peer
//     */
//    public synchronized void addForeignResourceStore(String resourceStore, ForeignStoreShare foreignPeerShare) {
//        peerClient.addForeignResourceStore(resourceStore, foreignPeerShare);
//    }
//
//    /**
//     * Removes an already defined foreign store
//     *
//     * @param resourceStore name of the store to remove
//     */
//    public synchronized void removeForeignResourceStore(String resourceStore) {
//        peerClient.removeForeignResourceStore(resourceStore);
//    }

    public synchronized DownloadManager downloadMediaFile(
            DatabaseMediator.ItemType containerType,
            Integer containedId,
            DatabaseMediator.ItemType itemType,
            String itemHash,
            String itemName,
            DownloadEvents downloadEvents) {

    }

    public synchronized DownloadManager downloadImage(
            DatabaseMediator.ItemType type,
            Integer id) {

    }

    private ResourceWriter generateTempFileWriter(
            DatabaseMediator.ItemType containerType,
            Integer containedId,
            DatabaseMediator.ItemType itemType,
            String itemHash,
            String itemName) throws IOException {
        return new TempFileWriter(tempFileManager, buildUserDictionary(containerType, containedId, itemType, itemHash, itemName));
    }

    private ResourceWriter generateBasicFileWriter(
            String downloadDir,
            String expectedFileName,
            DatabaseMediator.ItemType containerType,
            Integer containedId,
            DatabaseMediator.ItemType itemType,
            String itemHash,
            String itemName) throws IOException {
        return new BasicFileWriter(downloadDir, expectedFileName, buildUserDictionary(containerType, containedId, itemType, itemHash, itemName));
    }

    static HashMap<String, Serializable> buildUserDictionary(
            DatabaseMediator.ItemType containerType,
            Integer containedId,
            DatabaseMediator.ItemType itemType,
            String itemHash,
            String itemName) {
        HashMap<String, Serializable> userDictionary = new HashMap<>();
        userDictionary.put("containerType", containerType);
        userDictionary.put("containedId", containedId);
        userDictionary.put("itemType", itemType);
        userDictionary.put("itemHash", itemHash);
        userDictionary.put("itemName", itemName);
        return userDictionary;
    }


    private DownloadManager downloadFile(
            String resourceStore,
            String fileHash,
            ResourceWriter resourceWriter,
            DownloadEvents downloadEvents,
            double streamingNeed) throws IOException, NotAliveException {
        DownloadProgressNotificationHandlerBridge downloadProgressNotificationHandler = new DownloadProgressNotificationHandlerBridge(downloadEvents);
        return peerClient.downloadResource(resourceStore, fileHash, resourceWriter, downloadProgressNotificationHandler, streamingNeed, fileHash, "MD5");
    }


    /**
     * Initiates the process for downloading a file (group download). No store is specified so the default store is used. Providers of the file
     * will be obtained from the defaultForeignPeerShare.
     *
     * @param resourceID      ID of the resource
     * @param finalPath       path were the file will be stored
     * @param visible         whether this download should appear in the VisibleDownloadsManager (true) or not (false)
     * @param stoppable       whether this download must be persistent (can be resumed after stop or program exit)
     * @param downloadEvents  handler for receiving notifications concerning this download
     * @param streamingNeed   the need for streaming this file (0: no need, 1: max need). The higher the need,
     *                        the greater efforts that the scheduler will do for downloading the first parts
     *                        of the resource before the last parts. Can hamper total download efficiency
     * @param userGenericData a map of generic data which will be stored in this download and reported back to the user when the download is complete
     * @return a DownloadManager object for controlling this download
     * @throws IOException the download could not be initiated due to problems generating the target file
     */
    public synchronized jacz.peerengineservice.util.datatransfer.master.DownloadManager downloadFile(
            String resourceID,
            String finalPath,
            boolean visible,
            boolean stoppable,
            DownloadEvents downloadEvents,
            double streamingNeed,
            Map<String, Serializable> userGenericData,
            String totalHash,
            String totalHashAlgorithm) throws IOException {
        return downloadFile(resourceID, finalPath, visible, stoppable, downloadEvents, streamingNeed, userGenericData, DEFAULT_STORE, totalHash, totalHashAlgorithm);
    }

    /**
     * Initiates the process for downloading a file (group download). The store is specified by the user. Providers of the file
     * will be obtained from the peers share associated to the specified store.
     *
     * @param resourceID      ID of the resource
     * @param finalPath       path were the file will be stored
     * @param visible         whether this download should appear in the VisibleDownloadsManager (true) or not (false)
     * @param stoppable       whether this download must be persistent (can be resumed after stop or program exit)
     * @param downloadEvents  handler for receiving notifications concerning this download
     * @param streamingNeed   the need for streaming this file (0: no need, 1: max need). The higher the need,
     *                        the greater efforts that the scheduler will do for downloading the first parts
     *                        of the resource before the last parts. Can hamper total download efficiency
     * @param userGenericData a map of generic data which will be stored in this download and reported back to the user when the download is complete
     * @param resourceStore   name of the store allocating the resource
     * @return a DownloadManager object for controlling this download
     * @throws IOException the download could not be initiated due to problems generating the target file
     */
    public synchronized jacz.peerengineservice.util.datatransfer.master.DownloadManager downloadFile(
            String resourceID,
            String finalPath,
            boolean visible,
            boolean stoppable,
            DownloadEvents downloadEvents,
            double streamingNeed,
            Map<String, Serializable> userGenericData,
            String resourceStore,
            String totalHash,
            String totalHashAlgorithm) throws IOException {
        return downloadResource(null, resourceID, finalPath, visible, stoppable, downloadEvents, streamingNeed, userGenericData, resourceStore, totalHash, totalHashAlgorithm);
    }

    /**
     * Initiates the process for downloading a file (specific download). No store is specified so the default store is used. Providers of the file
     * will be obtained from the defaultForeignPeerShare.
     *
     * @param serverPeerID    only peer from which we will download the file
     * @param resourceID      ID of the resource
     * @param finalPath       path were the file will be stored
     * @param visible         whether this download should appear in the VisibleDownloadsManager (true) or not (false)
     * @param stoppable       whether this download must be persistent (can be resumed after stop or program exit)
     * @param downloadEvents  handler for receiving notifications concerning this download
     * @param streamingNeed   the need for streaming this file (0: no need, 1: max need). The higher the need,
     *                        the greater efforts that the scheduler will do for downloading the first parts
     *                        of the resource before the last parts. Can hamper total download efficiency
     * @param userGenericData a map of generic data which will be stored in this download and reported back to the user when the download is complete
     * @return a DownloadManager object for controlling this download
     * @throws IOException the download could not be initiated due to problems generating the target file
     */
    public synchronized jacz.peerengineservice.util.datatransfer.master.DownloadManager downloadFile(
            PeerID serverPeerID,
            String resourceID,
            String finalPath,
            boolean visible,
            boolean stoppable,
            DownloadEvents downloadEvents,
            double streamingNeed,
            Map<String, Serializable> userGenericData,
            String totalHash,
            String totalHashAlgorithm) throws IOException {
        return downloadFile(serverPeerID, resourceID, finalPath, visible, stoppable, downloadEvents, streamingNeed, userGenericData, DEFAULT_STORE, totalHash, totalHashAlgorithm);
    }

    /**
     * Initiates the process for downloading a file (specific download). The store is specified by the user. Providers of the file
     * will be obtained from the peers share associated to the specified store.
     *
     * @param serverPeerID    only peer from which we will download the file
     * @param resourceID      ID of the resource
     * @param finalPath       path were the file will be stored
     * @param visible         whether this download should appear in the VisibleDownloadsManager (true) or not (false)
     * @param stoppable       whether this download must be persistent (can be resumed after stop or program exit)
     * @param downloadEvents  handler for receiving notifications concerning this download
     * @param streamingNeed   the need for streaming this file (0: no need, 1: max need). The higher the need,
     *                        the greater efforts that the scheduler will do for downloading the first parts
     *                        of the resource before the last parts. Can hamper total download efficiency
     * @param userGenericData a map of generic data which will be stored in this download and reported back to the user when the download is complete
     * @param resourceStore   name of the store allocating the resource
     * @return a DownloadManager object for controlling this download
     * @throws IOException the download could not be initiated due to problems generating the target file
     */
    public synchronized jacz.peerengineservice.util.datatransfer.master.DownloadManager downloadFile(
            PeerID serverPeerID,
            String resourceID,
            String finalPath,
            boolean visible,
            boolean stoppable,
            DownloadEvents downloadEvents,
            double streamingNeed,
            Map<String, Serializable> userGenericData,
            String resourceStore,
            String totalHash,
            String totalHashAlgorithm) throws IOException {
        return downloadResource(serverPeerID, resourceID, finalPath, visible, stoppable, downloadEvents, streamingNeed, userGenericData, resourceStore, totalHash, totalHashAlgorithm);
    }

    private jacz.peerengineservice.util.datatransfer.master.DownloadManager downloadResource(
            PeerID serverPeerID,
            String resourceID,
            String finalPath,
            boolean visible,
            boolean stoppable,
            DownloadEvents downloadEvents,
            double streamingNeed,
            Map<String, Serializable> userGenericData,
            String resourceStore,
            String totalHash,
            String totalHashAlgorithm) throws IOException {
        Duple<ResourceWriter, String> resourceWriterAndCurrentPath = generateResourceWriter(finalPath, stoppable, tempDownloadsDirectory, userGenericData);
        ResourceWriter resourceWriter = resourceWriterAndCurrentPath.element1;
        String currentPath = resourceWriterAndCurrentPath.element2;
        DownloadProgressNotificationHandlerBridge downloadProgressNotificationHandler = new DownloadProgressNotificationHandlerBridge(downloadEvents);
        jacz.peerengineservice.util.datatransfer.master.DownloadManager peerEngineDownloadManager;
        if (serverPeerID != null) {
            peerEngineDownloadManager = peerClient.downloadResource(serverPeerID, resourceStore, resourceID, resourceWriter, downloadProgressNotificationHandler, streamingNeed, totalHash, totalHashAlgorithm);
        } else {
            peerEngineDownloadManager = peerClient.downloadResource(resourceStore, resourceID, resourceWriter, downloadProgressNotificationHandler, streamingNeed, totalHash, totalHashAlgorithm);
        }
        DownloadManagerOLD downloadManager = new DownloadManagerOLD(peerEngineDownloadManager, downloadEvents, resourceWriter, currentPath, finalPath, userGenericData);
        downloadProgressNotificationHandler.setDownloadManager(downloadManager);
//        if (visible) {
//            bridgePeerClientActionOLD.addVisibleDownload(downloadManager);
//        }
        return downloadManager;
    }

    private Duple<ResourceWriter, String> generateResourceWriter(String finalPath, boolean stoppable, String tempFileDir, Map<String, Serializable> userGenericData) throws IOException {
        ResourceWriter resourceWriter;
        String currentFilePath;
        if (stoppable) {
            // a temp resource writer is needed
            HashMap<String, Serializable> userDictionary = new HashMap<>();
            // todo store download info
            TempFileWriter tempFileWriter = new TempFileWriter(tempFileManager);
            currentFilePath = tempFileWriter.getTempDataFilePath();
            resourceWriter = tempFileWriter;
        } else {
            // a basic resource writer is enough. If no finalPath is specified, we must select one (use the same approach than the temp file manager)
            BasicFileWriter basicFileWriter;
            if (finalPath == null) {
                finalPath = DEFAULT_TEMPORARY_FILE_NAME;
                basicFileWriter = new BasicFileWriter(tempFileDir, finalPath);
            } else {
                basicFileWriter = new BasicFileWriter(finalPath);
            }
            currentFilePath = basicFileWriter.getPath();
            resourceWriter = basicFileWriter;
        }
        resourceWriter.setUserGenericData(USER_GENERIC_DATA_FIELD_GROUP, userGenericData);
        resourceWriter.setUserGenericDataField(OWN_GENERIC_DATA_FIELD_GROUP, FINAL_PATH_GENERIC_DATA_FIELD, finalPath);
        return new Duple<>(resourceWriter, currentFilePath);
    }

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


    public synchronized void setVisibleUploadsManagerTimer(long millis) {
        peerClient.setVisibleUploadsTimer(millis);
    }

    public synchronized void stopVisibleUploadsManager() {
        peerClient.stopVisibleUploadsTimer();
    }

    public synchronized String getTempDownloadsDirectory() {
        return tempFileManager.;
    }


    // todo remove?
    public boolean synchronizeList(PeerID peerID, String dataAccessorName, long timeout, ProgressNotificationWithError<Integer, SynchError> progress) throws AccessorNotFoundException, UnavailablePeerException {
        return peerClient.getDataSynchronizer().synchronizeData(peerID, dataAccessorName, timeout, progress);
    }


    public synchronized void localItemModified(String library, String elementIndex) {
        libraryManager.localItemModified(library, elementIndex);
    }

    FileHashDatabase getFileHashDatabase() {
        return fileHashDatabase;
    }

    public synchronized String getBaseDataDir() {
        return baseDataPath;
    }

    public synchronized void setBaseDataDir(String baseDataPath) {
        this.baseDataPath = baseDataPath;
    }
}
