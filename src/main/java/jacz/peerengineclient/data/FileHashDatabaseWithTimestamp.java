package jacz.peerengineclient.data;

import jacz.peerengineclient.PeerEngineClient;
import jacz.util.hash.hashdb.FileHashDatabase;
import jacz.util.io.serialization.UnrecognizedVersionException;
import jacz.util.io.serialization.VersionStack;
import jacz.util.io.serialization.VersionedObjectSerializer;
import jacz.util.io.serialization.VersionedSerializationException;
import jacz.util.maps.DoubleMap;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A FileHashDatabase enlarged with objects that allow tracking the timestamp of existing and deleted hashes.
 * <p>
 * This class is used to model the hashes stored locally and shared with the rest of the peers. The put and remove
 * methods are overridden to maintain track of active and deleted items.
 *
 * todo we must synchronize this class
 */
public class FileHashDatabaseWithTimestamp extends FileHashDatabase {

    private static final String VERSION_0_1 = "VERSION_0.1";

    private static final String CURRENT_VERSION = VERSION_0_1;

    private String id;

    private DoubleMap<Long, String> activeHashes;

    private HashMap<Long, String> deletedHashes;

    private Long nextTimestamp;

    private Long getNextTimestamp() {
        return nextTimestamp++;
    }

    public FileHashDatabaseWithTimestamp(String id) {
        super(PeerEngineClient.getHashFunction());
        this.id = id;
        init();
    }

    private void init() {
        activeHashes = new DoubleMap<>();
        deletedHashes = new HashMap<>();
        nextTimestamp = 1L;
    }

    public FileHashDatabaseWithTimestamp(String path, String... backupPaths) throws IOException, VersionedSerializationException {
        VersionedObjectSerializer.deserialize(this, path, backupPaths);
    }

    @Override
    public void clear() {
        super.clear();
        init();
    }

    public String getId() {
        return id;
    }

    @Override
    public String put(String path) throws IOException {
        String hash = super.put(path);
        activeHashes.put(getNextTimestamp(), hash);
        return hash;
    }

    @Override
    public String put(String folderPath, List<String> fileNames) throws IOException {
        // todo fatal error
        // folders are not used in this API
        return null;
    }

    @Override
    public String remove(String hash) {
        String path = super.remove(hash);
        moveFromActiveToDeleted(hash);
        return path;
    }

    @Override
    public String removeValue(String path) throws IOException {
        String hash = super.removeValue(path);
        moveFromActiveToDeleted(hash);
        return hash;
    }

    private void moveFromActiveToDeleted(String hash) {
        Long timestamp = activeHashes.removeReverse(hash);
        if (timestamp != null) {
            // the item did exist in activeHashes -> add to deleted hashes
            deletedHashes.put(getNextTimestamp(), hash);
        }
    }

    @Override
    public String removeValue(String folderPath, List<String> fileNames) throws IOException {
        // todo fatal error
        // folders are not used in this API
        return null;
    }

    public Set<String> getActiveHashesSetCopy() {
        return new HashSet<>(activeHashes.values());
    }

    public List<SerializedHashItem> getHashesFrom(long fromTimestamp) {
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

    @Override
    public VersionStack getCurrentVersion() {
        return new VersionStack(CURRENT_VERSION, super.getCurrentVersion());
    }

    @Override
    public Map<String, Serializable> serialize() {
        Map<String, Serializable> map = new HashMap<>(super.serialize());
        map.put("id", id);
        map.put("activeHashes", activeHashes);
        map.put("deletedHashes", deletedHashes);
        map.put("nextTimestamp", nextTimestamp);
        return map;
    }

    @Override
    public void deserialize(String version, Map<String, Object> attributes, VersionStack parentVersions) throws UnrecognizedVersionException {
        if (version.equals(CURRENT_VERSION)) {
            id = (String) attributes.get("id");
            activeHashes = (DoubleMap<Long, String>) attributes.get("activeHashes");
            deletedHashes = (HashMap<Long, String>) attributes.get("deletedHashes");
            nextTimestamp = (Long) attributes.get("nextTimestamp");
            super.deserialize(parentVersions.retrieveVersion(), attributes, parentVersions);
        } else {
            throw new UnrecognizedVersionException();
        }
    }
}
