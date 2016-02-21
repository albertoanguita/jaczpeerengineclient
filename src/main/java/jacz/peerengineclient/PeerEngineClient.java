package jacz.peerengineclient;

import jacz.database.*;
import jacz.database.util.ImageHash;
import jacz.peerengineclient.data.MoveFileAction;
import jacz.peerengineclient.data.PeerShareIO;
import jacz.peerengineclient.data.PeerShareManager;
import jacz.peerengineclient.databases.DatabaseIO;
import jacz.peerengineclient.databases.DatabaseManager;
import jacz.peerengineclient.databases.Databases;
import jacz.peerengineclient.databases.integration.IntegrationEvents;
import jacz.peerengineclient.databases.integration.SharedDatabaseGenerator;
import jacz.peerengineclient.databases.synch.DatabaseSynchEvents;
import jacz.peerengineclient.file_system.Paths;
import jacz.peerengineclient.images.ImageDownloader;
import jacz.peerengineclient.util.FileAPI;
import jacz.peerengineclient.util.PeriodicTaskReminder;
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
import jacz.peerengineservice.util.datatransfer.master.MasterResourceStreamer;
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

import javax.xml.stream.XMLStreamException;
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
 * todo si no tiene conexion, no deberia sacar la ip publica
 */
public class PeerEngineClient {

    public static final String DEFAULT_STORE = "@J_PEER_ENGINE_CLIENT_DEFAULT_RESOURCE_STORE";

    public static final String MEDIA_STORE = "@PEER_ENGINE_CLIENT_MEDIA_RESOURCE_STORE";

    public static final String IMAGE_STORE = "@PEER_ENGINE_CLIENT_IMAGE_RESOURCE_STORE";

    private static final double DEFAULT_STREAMING_NEED = 0d;

    private static final String HASH_ALGORITHM = "MD5";


    private final String basePath;

    private final PeersPersonalData peersPersonalData;

    private final TransferStatistics transferStatistics;

    private final PeerClient peerClient;

    private final FileAPI fileAPI;

    private final PeerShareManager peerShareManager;

    private final ImageDownloader imageDownloader;

    private final DatabaseManager databaseManager;

    private final PeriodicTaskReminder periodicTaskReminder;

    private final DownloadEvents downloadEvents;

    private final TempFileManager tempFileManager;

    private final String tempDownloadsPath;

    private final String baseMediaPath;


    public PeerEngineClient(
            String basePath,
            PeerID ownPeerID,
            PeerEncryption peerEncryption,
            NetworkConfiguration networkConfiguration,
            PeersPersonalData peersPersonalData,
            TransferStatistics transferStatistics,
            PeerRelations peerRelations,
            String tempDownloadsPath,
            String baseMediaPath,
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
        peerShareManager = PeerShareIO.load(basePath, this);
        databaseManager = DatabaseIO.load(basePath, databaseSynchEvents, integrationEvents, this, peerRelations.getFriendPeers());
        ErrorHandlerBridge errorHandlerBridge = new ErrorHandlerBridge(this, errorHandler);
        DataAccessorContainerImpl dataAccessorContainer = new DataAccessorContainerImpl(databaseManager, peerShareManager);
        synchronized (this) {
            // other threads will try to get the peerClient while it has not yet been created. We avoid concurrency
            // issues by synchronizing its creation and its getter
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
                    errorHandlerBridge);
        }
        fileAPI = new FileAPI(peerShareManager.getFileHash(), peerClient);
        imageDownloader = new ImageDownloader(this, databaseManager.getDatabases().getIntegratedDB(), fileAPI);
        peerShareManager.setPeerClient(peerClient);
        periodicTaskReminder = new PeriodicTaskReminder(this, databaseManager.getDatabaseSynchManager(), peerShareManager, imageDownloader);
        tempFileManager = new TempFileManager(tempDownloadsPath, tempFileManagerEvents);
        this.downloadEvents = downloadEvents;
        this.tempDownloadsPath = tempDownloadsPath;
        this.baseMediaPath = baseMediaPath;

        peerClient.setLocalGeneralResourceStore(new GeneralResourceStoreImpl(peerShareManager.getFileHash(), tempFileManager));

        loadTempDownloads();
        start();
    }

    private void loadTempDownloads() {
        for (String tempFile : tempFileManager.getExistingTempFiles()) {
            try {
                TempFileWriter tempFileWriter = new TempFileWriter(tempFileManager, tempFile);
                String storeName = (String) tempFileWriter.getSystemDictionary().get(MasterResourceStreamer.RESOURCE_WRITER_STORE_NAME_FIELD);
                String totalHash = (String) tempFileWriter.getSystemDictionary().get(MasterResourceStreamer.RESOURCE_WRITER_TOTAL_HASH_FIELD);
                String hashAlgorithm = (String) tempFileWriter.getSystemDictionary().get(MasterResourceStreamer.RESOURCE_WRITER_HASH_ALGORITHM_FIELD);
                downloadFile(storeName, totalHash, tempFileWriter, DEFAULT_STREAMING_NEED, hashAlgorithm);
            } catch (IOException e) {
                // error loading the temp file
                // todo notify?
                e.printStackTrace();
            } catch (NotAliveException e) {
                // ignore, cannot happen
            }
        }

    }

    private void start() {
        databaseManager.start();
        periodicTaskReminder.start();
    }

    public synchronized PeerClient getPeerClient() {
        return peerClient;
    }

    public FileAPI getFileAPI() {
        return fileAPI;
    }

    public ImageDownloader getImageDownloader() {
        return imageDownloader;
    }

    public void connect() {
        peerClient.connect();
    }

    public void disconnect() {
        peerClient.disconnect();
    }

    public void stop() throws IOException, XMLStreamException {
        periodicTaskReminder.stop();
        if (databaseManager != null) {
            databaseManager.stop();
            DatabaseIO.save(basePath, databaseManager);
        }
        if (peerShareManager != null) {
            peerShareManager.stop();
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
            // only if we managed to create a peer client, save all data
            SessionManager.stopAndSave(
                    basePath,
                    peerClient.getOwnPeerID(),
                    peerClient.getNetworkConfiguration(),
                    peerClient.getPeersPersonalData(),
                    peerClient.getPeerRelations(),
                    getMaxDesiredDownloadSpeed(),
                    getMaxDesiredUploadSpeed(),
                    tempDownloadsPath,
                    baseMediaPath,
                    peerClient.getPeerEncryption(),
                    transferStatistics
            );
        }
    }

    public Databases getDatabases() {
        return databaseManager.getDatabases();
    }

    public SharedDatabaseGenerator getSharedDatabaseGenerator() {
        return databaseManager.getSharedDatabaseGenerator();
    }

    public void localItemModified(DatabaseItem item) {
        databaseManager.localItemModified(item);
    }

    public void removeLocalItem(DatabaseItem item) {
        databaseManager.removeLocalItem(item);
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
        System.out.println("Peer is no longer friend: " + peerID);
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

    public synchronized String addLocalFileFixedPath(String path) throws IOException {
        return peerShareManager.getFileHash().put(path);
    }

    public synchronized String addLocalMovieFile(String path, Movie movie) throws IOException {
        return addLocalFile(path, MoveFileAction.MOVE_TO_MEDIA_REPO, movie, null, null);
    }

    public synchronized String addLocalChapterFile(String path, TVSeries tvSeries, Chapter chapter) throws IOException {
        return addLocalFile(path, MoveFileAction.MOVE_TO_MEDIA_REPO, null, tvSeries, chapter);
    }

    public synchronized String addLocalImageFile(String path) throws IOException {
        return addLocalFile(path, MoveFileAction.MOVE_TO_IMAGE_REPO, null, null, null);
    }

    private synchronized String addLocalFile(
            String path,
            MoveFileAction moveFileAction,
            Movie movie,
            TVSeries tvSeries,
            Chapter chapter) throws IOException {
        if (!peerShareManager.getFileHash().containsValue(path)) {
            String newPath;
            Triple<String, String, String> location;
            if (moveFileAction == MoveFileAction.MOVE_TO_MEDIA_REPO) {
                if (movie != null) {
                    location = Paths.movieFilePath(baseMediaPath, movie.getId(), movie.getTitle(), FileUtil.getFileName(path));
                } else {
                    location = Paths.seriesFilePath(baseMediaPath, tvSeries.getId(), tvSeries.getTitle(), chapter.getId(), chapter.getTitle(), FileUtil.getFileName(path));
                }
            } else {
                // to images repo
                location = Paths.imageFilePath(baseMediaPath, path);
            }
            newPath = FileUtil.createFile(location.element1, location.element2, location.element3, "(", ")", true).element1;
            if (!new File(path).getAbsolutePath().equals(new File(newPath).getAbsolutePath())) {
                // if necessary, move or copy the file to its corresponding place in the media directory
                FileUtil.move(path, newPath);
            }
            return addLocalFileFixedPath(newPath);
        } else {
            return addLocalFileFixedPath(path);
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
        return downloadFile(MEDIA_STORE, fileHash, resourceWriter, DEFAULT_STREAMING_NEED, HASH_ALGORITHM);
    }

    public synchronized DownloadManager downloadImage(ImageHash imageHash) throws IOException, NotAliveException {
        HashMap<String, Serializable> userDictionary =
                buildUserDictionary(DownloadInfo.Type.IMAGE, null, null, null, null, imageHash.getHash(), imageHash.serialize());
        // todo change for temp download (move when complete)
//        ResourceWriter resourceWriter = new BasicFileWriter(Paths.imagesDir(baseMediaPath), Paths.imageFileName(baseMediaPath, imageHash), userDictionary);
        ResourceWriter resourceWriter = new TempFileWriter(tempFileManager, userDictionary);
        return downloadFile(IMAGE_STORE, imageHash.getHash(), resourceWriter, DEFAULT_STREAMING_NEED, HASH_ALGORITHM);
    }

    static HashMap<String, Serializable> buildUserDictionary(
            DownloadInfo.Type type,
            DatabaseMediator.ItemType containerType,
            Integer containerId,
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
            double streamingNeed,
            String hashAlgorithm) throws NotAliveException {
        return peerClient.downloadResource(
                resourceStore,
                fileHash,
                resourceWriter,
                new DownloadProgressNotificationHandlerBridge(this, downloadEvents, databaseManager.getDatabases().getIntegratedDB(), baseMediaPath),
                streamingNeed,
                fileHash,
                hashAlgorithm);
    }



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

    FileHashDatabase getFileHashDatabase() {
        return peerShareManager.getFileHash();
    }

    public synchronized String getBaseDataDir() {
        return basePath;
    }

    public String getTempDownloadsPath() {
        return tempDownloadsPath;
    }

    public String getMediaPath() {
        return baseMediaPath;
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
