package jacz.peerengineclient.data;

import jacz.peerengineservice.PeerId;
import org.aanguita.jacuzzi.io.serialization.localstorage.Updater;
import org.aanguita.jacuzzi.io.serialization.localstorage.VersionedLocalStorage;
import org.aanguita.jacuzzi.maps.DoubleMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Stores the share of a remote peer. Includes id and maxStoredTimestamp to facilitate synchronization
 */
public class RemotePeerShare implements Updater {

    private final static Logger logger = LoggerFactory.getLogger(RemotePeerShare.class);

    private static final String VERSION_0_1 = "VERSION_0.1";

    private static final String CURRENT_VERSION = VERSION_0_1;

    private static final String HASH_CATEGORY = "@@@hash@@@";

    private static final String ID_KEY = "id";

    private static final String MAX_STORED_TIMESTAMP_KEY = "maxStoredTimestamp";

    private static final String REMOTE_PEER_ID_KEY = "remotePeerId";

    private final DoubleMap<Long, String> activeHashes;

    private final ForeignShares foreignShares;

    private final VersionedLocalStorage localStorage;


    public RemotePeerShare(PeerId remotePeerId, ForeignShares foreignShares, String path) throws IOException {
        activeHashes = new DoubleMap<>();
        this.foreignShares = foreignShares;
        localStorage = VersionedLocalStorage.createNew(path, CURRENT_VERSION);
        init("", remotePeerId, -1L);
    }

    /**
     * Attempts to load an existing remote share
     *
     * @param foreignShares storage of foreign shares. Stored shares are reported here
     * @param path          expected path of the local storage containing the saved share
     * @throws IOException the remote share does not exist, or could not be loaded
     */
    public RemotePeerShare(ForeignShares foreignShares, String path) throws IOException {
        activeHashes = new DoubleMap<>();
        this.foreignShares = foreignShares;
        localStorage = loadLocalStorage(path);
        for (String resourceId : activeHashes.values()) {
            foreignShares.addResourceProvider(resourceId, new PeerId(localStorage.getString(REMOTE_PEER_ID_KEY)));
        }
    }

    private VersionedLocalStorage loadLocalStorage(String localStoragePath) throws IOException {
        if (Files.exists(Paths.get(localStoragePath))) {
            VersionedLocalStorage localStorage = new VersionedLocalStorage(localStoragePath, this, CURRENT_VERSION);
            for (String key : localStorage.keys(HASH_CATEGORY)) {
                // this is a file hash -> extract the hash and the path and load
                activeHashes.put(Long.parseLong(key), localStorage.getString(key, HASH_CATEGORY));
            }
            return localStorage;
        } else {
            throw new IOException("Could not load local storage at " + localStoragePath);
        }
    }

    private void init(String id, PeerId remotePeerId, long timestamp) {
        activeHashes.clear();
        localStorage.setString(ID_KEY, id);
        localStorage.setString(REMOTE_PEER_ID_KEY, remotePeerId.toString());
        localStorage.setLong(MAX_STORED_TIMESTAMP_KEY, timestamp);
    }

    public void notifyPeerDisconnected() {
        foreignShares.removeResourceProvider(new PeerId(localStorage.getString(REMOTE_PEER_ID_KEY)));
    }

    public String getId() {
        return localStorage.getString(ID_KEY);
    }

    public void setId(String id) {
        // reset all the peer share
        PeerId remotePeerId = new PeerId(localStorage.getString(REMOTE_PEER_ID_KEY));
        localStorage.clear();
        foreignShares.removeResourceProvider(remotePeerId);
        init(id, remotePeerId, -1L);
    }

    public Long getMaxStoredTimestamp() {
        return localStorage.getLong(MAX_STORED_TIMESTAMP_KEY);
    }

    public ForeignShares getForeignShares() {
        return foreignShares;
    }

    public void addHash(long timestamp, String hash) {
        logger.info("Added shared file for peer " + localStorage.getString(REMOTE_PEER_ID_KEY) + ": " + hash);
        activeHashes.put(timestamp, hash);
        localStorage.setString(Long.toString(timestamp), hash, HASH_CATEGORY);
        updateTimestamp(timestamp);
        foreignShares.addResourceProvider(hash, new PeerId(localStorage.getString(REMOTE_PEER_ID_KEY)));
    }

    public void removeHash(long timestamp, String hash) {
        logger.info("Removed shared file for peer " + localStorage.getString(REMOTE_PEER_ID_KEY) + ": " + hash);
        Long oldTimestamp = activeHashes.removeReverse(hash);
        if (oldTimestamp != null) {
            localStorage.removeItem(Long.toString(oldTimestamp), HASH_CATEGORY);
        }
        updateTimestamp(timestamp);
        foreignShares.removeResourceProvider(hash, new PeerId(localStorage.getString(REMOTE_PEER_ID_KEY)));
    }

    void updateTimestamp(long timestamp) {
        localStorage.setLong(MAX_STORED_TIMESTAMP_KEY, timestamp);
    }

    @Override
    public String update(VersionedLocalStorage versionedLocalStorage, String storedVersion) {
        throw new RuntimeException();
    }
}
