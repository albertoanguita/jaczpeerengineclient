package jacz.peerengineclient.data;

import jacz.peerengineclient.PeerEngineClient;
import org.aanguita.jacuzzi.hash.hashdb.FileHashDatabaseLS;
import org.aanguita.jacuzzi.maps.DoubleMap;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A FileHashDatabase enlarged with objects that allow tracking the timestamp of existing and deleted hashes.
 * <p>
 * This class is used to model the hashes stored locally and shared with the rest of the peers. The put and remove
 * methods are overridden to maintain track of active and deleted items.
 * <p>
 * All stored paths are automatically converted to absolute
 */
public class FileHashDatabaseWithTimestamp extends FileHashDatabaseLS {

    private static final String VERSION_0_1 = "VERSION_0.1";

    private static final String CURRENT_VERSION = VERSION_0_1;

    private static final String VERSION_KEY = FileHashDatabaseWithTimestamp.class.getName() + "Version";

    private static final String ID_KEY = "id";

    private static final String TIMESTAMP_KEY = "timestamp";

    private static final String ACTIVE_TIMESTAMP_CATEGORY = "@@@act@@@";

    private static final String DELETED_TIMESTAMP_CATEGORY = "@@@del@@@";

    private DoubleMap<Long, String> activeHashes;

    private HashMap<Long, String> deletedHashes;

    private final FileHashDatabaseEventsBridge fileHashDatabaseEvents;

    private Long getNextTimestamp() {
        long timestamp = getLocalStorage().getLong(TIMESTAMP_KEY);
        getLocalStorage().setLong(TIMESTAMP_KEY, timestamp + 1);
        return timestamp;
    }

    public FileHashDatabaseWithTimestamp(String path, String id) throws IOException {
        super(path, PeerEngineClient.getHashFunction(), true);
        getLocalStorage().setString(VERSION_KEY, CURRENT_VERSION);
        getLocalStorage().setString(ID_KEY, id);
        init();
        this.fileHashDatabaseEvents = null;
    }

    private void init() {
        activeHashes = new DoubleMap<>();
        deletedHashes = new HashMap<>();
        getLocalStorage().setLong(TIMESTAMP_KEY, 1L);
    }

    public FileHashDatabaseWithTimestamp(String path, FileHashDatabaseEvents fileHashDatabaseEvents) throws IOException {
        super(path);
        updateFileHashDatabaseWithTimestamp(getLocalStorage().getString(VERSION_KEY));
        activeHashes = new DoubleMap<>();
        deletedHashes = new HashMap<>();
        for (String activeHashesKey : getLocalStorage().keys(ACTIVE_TIMESTAMP_CATEGORY)) {
            activeHashes.put(Long.parseLong(activeHashesKey), getLocalStorage().getString(activeHashesKey, ACTIVE_TIMESTAMP_CATEGORY));
        }
        for (String deletedHashesKey : getLocalStorage().keys(DELETED_TIMESTAMP_CATEGORY)) {
            deletedHashes.put(Long.parseLong(deletedHashesKey), getLocalStorage().getString(deletedHashesKey, DELETED_TIMESTAMP_CATEGORY));
        }
        this.fileHashDatabaseEvents = new FileHashDatabaseEventsBridge(fileHashDatabaseEvents);
    }

    @Override
    public synchronized void clear() {
        String id = getLocalStorage().getString(ID_KEY);
        super.clear();
        getLocalStorage().setString(VERSION_KEY, CURRENT_VERSION);
        getLocalStorage().setString(ID_KEY, id);
        init();
    }

    public String getId() {
        return getLocalStorage().getString(ID_KEY);
    }

    @Override
    public synchronized String put(String path) throws IOException {
        String hash = super.put(path);
        Long timeStamp = getNextTimestamp();
        activeHashes.put(timeStamp, hash);
        getLocalStorage().setString(timeStamp.toString(), hash, ACTIVE_TIMESTAMP_CATEGORY);
        fileHashDatabaseEvents.fileAdded(hash, path);
        return hash;
    }

    @Override
    public synchronized String remove(String hash) {
        String path = super.remove(hash);
        moveFromActiveToDeleted(hash);
        fileHashDatabaseEvents.fileRemoved(hash, path);
        return path;
    }

    @Override
    public synchronized String removeValue(String path) throws IOException {
        String hash = super.removeValue(path);
        moveFromActiveToDeleted(hash);
        fileHashDatabaseEvents.fileRemoved(hash, path);
        return hash;
    }

    private void moveFromActiveToDeleted(String hash) {
        Long timestamp = activeHashes.removeReverse(hash);
        if (timestamp != null) {
            // the item did exist in activeHashes -> add to deleted hashes
            getLocalStorage().removeItem(timestamp.toString(), ACTIVE_TIMESTAMP_CATEGORY);
            Long newTimestamp = getNextTimestamp();
            deletedHashes.put(newTimestamp, hash);
            getLocalStorage().setString(newTimestamp.toString(), hash, DELETED_TIMESTAMP_CATEGORY);
        }
    }

    public synchronized Set<String> getActiveHashesSetCopy() {
        return new HashSet<>(activeHashes.values());
    }

    public synchronized List<SerializedHashItem> getHashesFrom(long fromTimestamp) {
        List<SerializedHashItem> items =
                activeHashes
                        .entrySet()
                        .stream()
                        .filter(entry -> entry.getKey() >= fromTimestamp)
                        .map(entry -> new SerializedHashItem(entry.getKey(), entry.getValue()))
                        .collect(Collectors.toList());
        items.addAll(
                deletedHashes
                        .entrySet()
                        .stream()
                        .filter(entry -> entry.getKey() >= fromTimestamp)
                        .map(entry -> new SerializedHashItem(entry.getKey(), entry.getValue(), false))
                        .collect(Collectors.toList()));
        return items;
    }


    private void updateFileHashDatabaseWithTimestamp(String storedVersion) {
        if (!storedVersion.equals(CURRENT_VERSION)) {
            throw new RuntimeException();
        }
    }

    public void stop() {
        fileHashDatabaseEvents.stop();
    }
}
