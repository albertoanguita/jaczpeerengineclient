package jacz.peerengineclient;

import com.neovisionaries.i18n.CountryCode;
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
import jacz.peerengineclient.file_system.MediaPaths;
import jacz.peerengineclient.file_system.PathConstants;
import jacz.peerengineclient.images.ImageDownloader;
import jacz.peerengineclient.util.FileAPI;
import jacz.peerengineclient.util.PeriodicTaskReminder;
import jacz.peerengineclient.util.PersistentIdFactory;
import jacz.peerengineservice.NotAliveException;
import jacz.peerengineservice.PeerEncryption;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.UnavailablePeerException;
import jacz.peerengineservice.client.GeneralEvents;
import jacz.peerengineservice.client.PeerClient;
import jacz.peerengineservice.client.connection.ConnectedPeers;
import jacz.peerengineservice.client.connection.ConnectionEvents;
import jacz.peerengineservice.client.connection.ConnectionState;
import jacz.peerengineservice.client.connection.State;
import jacz.peerengineservice.client.connection.peers.PeerInfo;
import jacz.peerengineservice.client.connection.peers.PeersEvents;
import jacz.peerengineservice.util.PeerRelationship;
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
import jacz.util.files.FileGenerator;
import jacz.util.hash.HashFunction;
import jacz.util.hash.MD5;
import jacz.util.io.serialization.VersionedSerializationException;
import jacz.util.io.serialization.localstorage.LocalStorage;
import jacz.util.lists.tuple.Duple;
import jacz.util.lists.tuple.Triple;
import jacz.util.log.ErrorFactory;
import jacz.util.notification.ProgressNotificationWithError;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * PeerEngine client adapted for Jacuzzi
 * @FUTURE@ todo remove all use of File, use Path, in all projects
 */
public class PeerEngineClient {

    private static final String SERVER_URL = "https://jaczserver.appspot.com/_ah/api/server/v1/";

    public static final String DEFAULT_STORE = "@J_PEER_ENGINE_CLIENT_DEFAULT_RESOURCE_STORE";

    public static final String MEDIA_STORE = "@PEER_ENGINE_CLIENT_MEDIA_RESOURCE_STORE";

    public static final String IMAGE_STORE = "@PEER_ENGINE_CLIENT_IMAGE_RESOURCE_STORE";

    private static final double DEFAULT_STREAMING_NEED = 0d;

    private static final String HASH_ALGORITHM = "MD5";


    private final String basePath;

    private final PeerClient peerClient;

    private final FileAPI fileAPI;

    private final PeerShareManager peerShareManager;

    private final PersistentIdFactory persistentIdFactory;

    private final ImageDownloader imageDownloader;

    private final DatabaseManager databaseManager;

    private final ErrorHandlerBridge errorHandlerBridge;

    private final PeriodicTaskReminder periodicTaskReminder;

    private final DownloadEvents downloadEvents;

    private final TempFileManager tempFileManager;

    private final MediaPaths mediaPaths;

    private final List<String> repairedFiles;

    public PeerEngineClient(
            String basePath,
            PeerId ownPeerId,
            PeerEncryption peerEncryption,
            MediaPaths mediaPaths,
            GeneralEvents generalEvents,
            ConnectionEvents connectionEvents,
            PeersEvents peersEvents,
            ResourceTransferEvents resourceTransferEvents,
            TempFileManagerEvents tempFileManagerEvents,
            DatabaseSynchEvents databaseSynchEvents,
            DownloadEvents downloadEvents,
            IntegrationEvents integrationEvents,
            ErrorEvents errorEvents) throws IOException, VersionedSerializationException {
        this.basePath = basePath;
        repairedFiles = new ArrayList<>();
        peerShareManager = PeerShareIO.load(basePath, this);
        repairedFiles.addAll(peerShareManager.getFileHash().getRepairedFiles());
        databaseManager = DatabaseIO.load(basePath, databaseSynchEvents, integrationEvents, this);
        repairedFiles.addAll(databaseManager.getDatabases().getRepairedFiles());
        errorHandlerBridge = new ErrorHandlerBridge(this, errorEvents);
        DataAccessorContainerImpl dataAccessorContainer = new DataAccessorContainerImpl(this, databaseManager, peerShareManager);
        peerClient = new PeerClient(
                ownPeerId,
                SERVER_URL,
                PathConstants.connectionConfigPath(basePath),
                PathConstants.peerKBPath(basePath),
                peerEncryption,
                PathConstants.networkConfigPath(basePath),
                generalEvents,
                connectionEvents,
                new PeersEventsBridge(this, peersEvents),
                resourceTransferEvents,
                PathConstants.personalDataPath(basePath),
                PathConstants.statisticsPath(basePath),
                new HashMap<>(),
                dataAccessorContainer,
                errorHandlerBridge);
        fileAPI = new FileAPI(peerShareManager.getFileHash(), peerClient);
        imageDownloader = new ImageDownloader(this, databaseManager.getDatabases().getIntegratedDB(), fileAPI);
        peerShareManager.setPeerClient(peerClient);
        persistentIdFactory = new PersistentIdFactory(basePath);
        periodicTaskReminder = new PeriodicTaskReminder(this, databaseManager.getDatabaseSynchManager(), peerShareManager, imageDownloader);
        tempFileManager = new TempFileManager(mediaPaths.getTempDownloadsPath(), tempFileManagerEvents);
        this.downloadEvents = downloadEvents;
        this.mediaPaths = mediaPaths;

        peerClient.setLocalGeneralResourceStore(new GeneralResourceStoreImpl(peerShareManager.getFileHash(), tempFileManager));

        loadTempDownloads();
        start();
    }

    public List<String> getRepairedFiles() {
        return repairedFiles;
    }

    private void loadTempDownloads() throws IOException {
        for (String tempFile : tempFileManager.getExistingTempFiles()) {
            try {
                TempFileWriter tempFileWriter = new TempFileWriter(tempFileManager, tempFile);
                String storeName = (String) tempFileWriter.getSystemDictionary().get(MasterResourceStreamer.RESOURCE_WRITER_STORE_NAME_FIELD);
                String totalHash = (String) tempFileWriter.getSystemDictionary().get(MasterResourceStreamer.RESOURCE_WRITER_TOTAL_HASH_FIELD);
                String hashAlgorithm = (String) tempFileWriter.getSystemDictionary().get(MasterResourceStreamer.RESOURCE_WRITER_HASH_ALGORITHM_FIELD);
                downloadFile(storeName, totalHash, tempFileWriter, DEFAULT_STREAMING_NEED, hashAlgorithm);
            } catch (IOException e) {
                // error loading the temp file
                errorHandlerBridge.temporaryDownloadFileCouldNotBeRecovered(FileUtils.getFile(mediaPaths.getTempDownloadsPath(), tempFile).getAbsolutePath());
            } catch (NotAliveException e) {
                // ignore, cannot happen
            }
        }

    }

    private void start() {
        databaseManager.start();
        periodicTaskReminder.start();
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

    public synchronized PeerId getOwnPeerId() {
        return peerClient.getOwnPeerId();
    }

    public synchronized Date profileCreationDate() throws IOException {
        return SessionManager.profileCreationDate(basePath);
    }

    public void stop() {
        try {
            periodicTaskReminder.stop();
            if (databaseManager != null) {
                databaseManager.stop();
                DatabaseIO.save(basePath, databaseManager);
            }
            if (peerShareManager != null) {
                peerShareManager.stop();
//                PeerShareIO.save(basePath, peerShareManager);
            }
            if (tempFileManager != null) {
                tempFileManager.stop();
            }
            if (peerClient != null) {
                peerClient.stop();
            }
        } catch (IOException e) {
            errorHandlerBridge.sessionDataCouldNotBeSaved();
        }
    }


    void downloadedFileCouldNotBeLoaded(String path, String expectedFileName) {
        errorHandlerBridge.downloadedFileCouldNotBeLoaded(path, expectedFileName);
    }

    public void reportFatalError(String message, Object... data) {
        ErrorFactory.reportError(errorHandlerBridge, message, data);
    }

    public Databases getDatabases() {
        return databaseManager.getDatabases();
    }

    public SharedDatabaseGenerator getSharedDatabaseGenerator() {
        return databaseManager.getSharedDatabaseGenerator();
    }

    /**
     * A local item has been modified, and needs to be re-integrated in the integrated database
     *
     * @param item modified item
     * @throws IllegalStateException if the client has been previously stopped
     */
    public void localItemModified(DatabaseItem item) throws IllegalStateException {
        databaseManager.localItemModified(item);
    }

    public void removeLocalItem(DatabaseItem item) {
        databaseManager.removeLocalItem(item);
    }

    public int getLocalPort() {
        return peerClient.getLocalPort();
    }

    public void setLocalPort(int port) {
        peerClient.setLocalPort(port);
    }

    public int getExternalPort() {
        return peerClient.getExternalPort();
    }

    public void setExternalPort(int port) {
        peerClient.setExternalPort(port);
    }

    public ConnectionState getConnectionState() {
        return peerClient.getConnectionState();
    }

    public synchronized boolean isConnectedPeer(PeerId peerId) {
        return peerClient.isConnectedPeer(peerId);
    }

    public synchronized Set<PeerId> getConnectedPeers() {
        return peerClient.getConnectedPeers();
    }

    public synchronized ArrayList<ConnectedPeers.PeerConnectionData> getConnectedPeersData() {
        return peerClient.getConnectedPeersData();
    }

    public String getOwnNick() {
        return peerClient.getOwnNick();
    }

    public synchronized void setOwnNick(String nick) {
        peerClient.setOwnNick(nick);
    }

    public PeerInfo getPeerInfo(PeerId peerId) {
        return peerClient.getPeerInfo(peerId);
    }

    public String getPeerNick(PeerId peerId) {
        return peerClient.getPeerNick(peerId);
    }

    public int getPeerAffinity(PeerId peerId) {
        return peerClient.getPeerAffinity(peerId);
    }

    public void updatePeerAffinity(PeerId peerId, int affinity) {
        peerClient.updatePeerAffinity(peerId, affinity);
    }

    public synchronized PeerRelationship getPeerRelationship(PeerId peerId) {
        return peerClient.getPeerRelationship(peerId);
    }

    public synchronized boolean isFavoritePeer(PeerId peerId) {
        return peerClient.isFavoritePeer(peerId);
    }

    public synchronized boolean isBlockedPeer(PeerId peerId) {
        return peerClient.isBlockedPeer(peerId);
    }

    public synchronized Set<PeerId> getFavoritePeers() {
        return peerClient.getFavoritePeers();
    }

    public void addFavoritePeer(final PeerId peerId) {
        peerClient.addFavoritePeer(peerId);
    }

    public synchronized void removeFavoritePeer(final PeerId peerId) {
        peerClient.removeFavoritePeer(peerId);
    }

    public synchronized Set<PeerId> getBlockedPeers() {
        return peerClient.getBlockedPeers();
    }

    public synchronized void addBlockedPeer(final PeerId peerId) {
        peerClient.addBlockedPeer(peerId);
    }

    public synchronized void removeBlockedPeer(final PeerId peerId) {
        peerClient.removeBlockedPeer(peerId);
    }

    public PeerId getNextConnectedPeer(PeerId peerId) {
        return peerClient.getNextConnectedPeer(peerId);
    }

    public boolean isWishForRegularConnections() {
        return peerClient.isWishForRegularConnections();
    }

    public void setWishForRegularsConnections(boolean enabled) {
        peerClient.setWishForRegularsConnections(enabled);
    }

    public int getMaxRegularConnections() {
        return peerClient.getMaxRegularConnections();
    }

    public void setMaxRegularConnections(int maxRegularConnections) {
        peerClient.setMaxRegularConnections(maxRegularConnections);
    }

    public int getMaxRegularConnectionsForAdditionalCountries() {
        return peerClient.getMaxRegularConnectionsForAdditionalCountries();
    }

    public void setMaxRegularConnectionsForAdditionalCountries(int maxRegularConnections) {
        peerClient.setMaxRegularConnectionsForAdditionalCountries(maxRegularConnections);
    }

    public int getMaxRegularConnectionsForOtherCountries() {
        return peerClient.getMaxRegularConnectionsForOtherCountries();
    }

    public CountryCode getMainCountry() {
        return peerClient.getMainCountry();
    }

    public void setMainCountry(CountryCode mainCountry) {
        peerClient.setMainCountry(mainCountry);
    }

    public List<CountryCode> getAdditionalCountries() {
        return peerClient.getAdditionalCountries();
    }

    public boolean isAdditionalCountry(CountryCode country) {
        return peerClient.isAdditionalCountry(country);
    }

    public void setAdditionalCountries(List<CountryCode> additionalCountries) {
        peerClient.setAdditionalCountries(additionalCountries);
    }

    synchronized void peerConnected(PeerId peerId) {
        peerShareManager.peerConnected(basePath, peerId);
    }

    synchronized void peerDisconnected(PeerId peerId) {
        peerShareManager.peerDisconnected(basePath, peerId);
    }

    /**
     * This method forces the peer engine to search for connected friends. In principle, it is not necessary to use
     * this method, since this search is automatically performed when the PeerClient connects to a PeerServer. Still,
     * there might be cases in which it is recommendable (to search for a friend peer who has not listed us as friend,
     * since he will not try to connect to us, etc)
     */
    public void searchFavorites() {
        peerClient.searchFavorites();
    }

    public void clearAllPeerAddresses() {
        peerClient.clearAllPeerAddresses();
    }

    public void clearAllData() {
        peerClient.clearAllData();
    }

    public synchronized String addLocalFileFixedPath(String path) throws IOException {
        return peerShareManager.getFileHash().put(path);
    }

    public synchronized Duple<String, String> addLocalMovieFile(String path, Movie movie) throws IOException {
        return addLocalMovieFile(path, Paths.get(path).getFileName().toString(), movie);
    }

    public synchronized Duple<String, String> addLocalMovieFile(String path, String expectedFileName, Movie movie) throws IOException {
        return addLocalFile(path, expectedFileName, MoveFileAction.MOVE_TO_MEDIA_REPO, movie, null, null);
    }

    public synchronized Duple<String, String> addLocalChapterFile(String path, TVSeries tvSeries, Chapter chapter) throws IOException {
        return addLocalChapterFile(path, Paths.get(path).getFileName().toString(), tvSeries, chapter);
    }

    public synchronized Duple<String, String> addLocalChapterFile(String path, String expectedFileName, TVSeries tvSeries, Chapter chapter) throws IOException {
        return addLocalFile(path, expectedFileName, MoveFileAction.MOVE_TO_MEDIA_REPO, null, tvSeries, chapter);
    }

    public synchronized Duple<String, String> addLocalImageFile(String path) throws IOException {
        return addLocalImageFile(path, Paths.get(path).getFileName().toString());
    }

    public synchronized Duple<String, String> addLocalImageFile(String path, String expectedFileName) throws IOException {
        return addLocalFile(path, Paths.get(path).getFileName().toString(), MoveFileAction.MOVE_TO_IMAGE_REPO, null, null, null);
    }

    private synchronized Duple<String, String> addLocalFile(
            String path,
            String expectedFileName,
            MoveFileAction moveFileAction,
            Movie movie,
            TVSeries tvSeries,
            Chapter chapter) throws IOException {
        Triple<String, String, String> location;
        if (moveFileAction == MoveFileAction.MOVE_TO_MEDIA_REPO) {
            if (movie != null) {
                location = PathConstants.movieFilePath(mediaPaths.getBaseMediaPath(), movie.getId(), movie.getTitle(), expectedFileName);
            } else {
                location = PathConstants.seriesFilePath(mediaPaths.getBaseMediaPath(), tvSeries.getId(), tvSeries.getTitle(), chapter.getId(), chapter.getTitle(), expectedFileName);
            }
        } else {
            // to images repo
            location = PathConstants.imageFilePath(mediaPaths.getBaseMediaPath(), path);
        }
        // this is the path in the media library where this file should go (file is created in the process)
        String finalPath = FileGenerator.createFile(location.element1, location.element2, location.element3, "(", ")", true);

        if (!Paths.get(finalPath).getParent().equals(Paths.get(path))) {
            // the given path is not in the expected place in the media library -> needs to be moved
            Files.move(Paths.get(path), Paths.get(finalPath), StandardCopyOption.REPLACE_EXISTING);
        } else {
            // the given path is already in its expected place in the media library -> use it as final path
            finalPath = path;
        }
        return new Duple<>(finalPath, addLocalFileFixedPath(finalPath));
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
                new DownloadProgressNotificationHandlerBridge(this, downloadEvents, databaseManager.getDatabases().getIntegratedDB(), mediaPaths.getBaseMediaPath()),
                streamingNeed,
                fileHash,
                hashAlgorithm);
    }

    public TransferStatistics getTransferStatistics() {
        return peerClient.getTransferStatistics();
    }

    /**
     * Retrieves the maximum allowed speed for downloading data from other peers. A null value indicates that no limit has been established
     *
     * @return the maximum allowed speed for receiving data from other peers, in KBytes per second (or null if no limit is established)
     */
    public synchronized Integer getMaxDownloadSpeed() {
        Float speed = peerClient.getMaxDownloadSpeed();
        return speed == null ? null : (int) (Math.ceil(speed / 1024f));
    }

    /**
     * Sets the maximum allows speed for downloading data from other peers. The value is provided in KBytes per second. A null or negative value
     * is considered as no limit
     *
     * @param totalMaxDesiredSpeed the value, in KBytes per second, for limiting download speed of data transfer to other peers
     */
    public synchronized void setMaxDownloadSpeed(Integer totalMaxDesiredSpeed) {
        Float speed = (totalMaxDesiredSpeed == null || totalMaxDesiredSpeed < 0) ? null : (float) (totalMaxDesiredSpeed * 1024);
        peerClient.setMaxDownloadSpeed(speed);
    }

    /**
     * Retrieves the maximum allowed speed for transferring data to other peers. A null value indicates that no limit has been established
     *
     * @return the maximum allowed speed for sending data to other peers, in KBytes per second (or null if no limit is established)
     */
    public synchronized Integer getMaxUploadSpeed() {
        Float speed = peerClient.getMaxUploadSpeed();
        return speed == null ? null : (int) (Math.ceil(speed / 1024f));
    }

    /**
     * Sets the maximum allows speed for transferring data to other peers. The value is provided in KBytes per second. A null or negative value
     * is considered as no limit
     *
     * @param totalMaxDesiredSpeed the value, in KBytes per second, for limiting upload speed of data transfer to other peers
     */
    public synchronized void setMaxUploadSpeed(Integer totalMaxDesiredSpeed) {
        Float speed = (totalMaxDesiredSpeed == null || totalMaxDesiredSpeed < 0) ? null : (float) (totalMaxDesiredSpeed * 1024);
        peerClient.setMaxUploadSpeed(speed);
    }

    public double getDownloadPartSelectionAccuracy() {
        return peerClient.getDownloadPartSelectionAccuracy();
    }

    public void setDownloadPartSelectionAccuracy(double accuracy) {
        peerClient.setDownloadPartSelectionAccuracy(accuracy);
    }

    /**
     * Retrieves a shallow copy of the active downloads (only visible ones)
     *
     * @return a shallow copy of the active downloads
     */
    public List<DownloadManager> getDownloads(String resourceStore) {
        return peerClient.getDownloads(resourceStore);
    }

    public List<DownloadManager> getAllDownloads() {
        return peerClient.getAllDownloads();
    }


    public synchronized void setVisibleDownloadsTimer(long millis) {
        peerClient.setVisibleDownloadsTimer(millis);
    }

    public synchronized void stopVisibleDownloads() {
        peerClient.stopVisibleDownloadsTimer();
    }

    /**
     * Retrieves a shallow copy of the active downloads (only visible ones)
     *
     * @return a shallow copy of the active downloads
     */
    public List<UploadManager> getUploads(String resourceStore) {
        return peerClient.getUploads(resourceStore);
    }

    public List<UploadManager> getAllUploads() {
        return peerClient.getAllUploads();
    }

    public synchronized void setVisibleUploadsManagerTimer(long millis) {
        peerClient.setVisibleUploadsTimer(millis);
    }

    public synchronized void stopVisibleUploadsManager() {
        peerClient.stopVisibleUploadsTimer();
    }

    /**
     * Sends an object message to a connected peer. If the given peer is not among the list of connected peers, the
     * message will be ignored
     *
     * @param peerId  ID of the peer to which the message is to be sent
     * @param message string message to send
     */
    public void sendObjectMessage(PeerId peerId, Serializable message) {
        peerClient.sendObjectMessage(peerId, message);
    }

    /**
     * Sends an object message to all connected peers
     *
     * @param message string message to send to all connected peers
     */
    public void broadcastObjectMessage(Serializable message) {
        peerClient.broadcastObjectMessage(message);
    }


    public boolean synchronizeList(PeerId peerId, DataAccessor dataAccessor, long timeout, ProgressNotificationWithError<Integer, SynchError> progress) throws UnavailablePeerException {
        return peerClient.getDataSynchronizer().synchronizeData(peerId, dataAccessor, timeout, progress);
    }

    void clearFileHashDatabase() {
        peerShareManager.getFileHash().clear();
    }

    String removeFileByHash(String hash) {
        return peerShareManager.getFileHash().remove(hash);
    }

    String removeFileByPath(String path) throws IOException {
        return peerShareManager.getFileHash().removeValue(path);
    }

    boolean containsFileByHash(String hash) {
        return peerShareManager.getFileHash().containsKey(hash);
    }

    boolean containsFileByPath(String path) throws IOException {
        return peerShareManager.getFileHash().containsValue(path);
    }

    public String getFile(String hash) {
        return peerShareManager.getFileHash().getFilePath(hash);
    }

    public synchronized String getBaseDataDir() {
        return basePath;
    }

    public String getTempDownloadsPath() {
        return mediaPaths.getTempDownloadsPath();
    }

    public String getMediaPath() {
        return mediaPaths.getBaseMediaPath();
    }

    public static HashFunction getHashFunction() {
        try {
            return new HashFunction(HASH_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            // ignore, cannot happen
            return new MD5();
        }
    }

    public String getIntegratedDB() {
        return databaseManager.getDatabases().getIntegratedDB();
    }

    public String generatePersistentId() {
        return persistentIdFactory.generateId();
    }
}
