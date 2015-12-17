package jacz.peerengineclient.peer_share;

import jacz.util.hash.hashdb.FileHashDatabase;
import jacz.util.io.object_serialization.*;
import jacz.util.maps.DoubleMap;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A FileHashDatabase enlarged with objects that allow tracking the timestamp of existing and deleted hashes.
 * <p>
 * This class is used to model the hashes stored locally and shared with the rest of the peers. The put and remove
 * methods are overridden to maintain track of active and deleted items.
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

    public FileHashDatabaseWithTimestamp(String path, String... backupPaths) throws IOException, VersionedSerializationException {
        VersionedObjectSerializer.deserialize(this, path, backupPaths);
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
    public void remove(String key) throws IOException {
        super.remove(key);
        Long timestamp = activeHashes.removeReverse(key);
        deletedHashes.put(timestamp, key);
    }

    @Override
    public String removeValue(String path) throws IOException {
        String hash = super.removeValue(path);
        Long timestamp = activeHashes.removeReverse(hash);
        deletedHashes.put(timestamp, hash);
        return hash;
    }

    @Override
    public String removeValue(String folderPath, List<String> fileNames) throws IOException {
        // todo fatal error
        // folders are not used in this API
        return null;
    }

    @Override
    public VersionStack getCurrentVersion() {
        return new VersionStack(CURRENT_VERSION);
    }

    @Override
    public Map<String, Serializable> serialize() {
        Map<String, Serializable> map = new HashMap<>(super.serialize());
        map.put("activeHashes", activeHashes);
        map.put("deletedHashes", deletedHashes);
        map.put("nextTimestamp", nextTimestamp);
        return map;
    }

    @Override
    public void deserialize(String version, Map<String, Object> attributes, VersionStack parentVersions) throws UnrecognizedVersionException {
        if (version.equals(CURRENT_VERSION)) {
            activeHashes = (DoubleMap<Long, String>) attributes.get("activeHashes");
            deletedHashes = (HashMap<Long, String>) attributes.get("deletedHashes");
            nextTimestamp = (Long) attributes.get("nextTimestamp");
            super.deserialize(parentVersions.retrieveVersion(), attributes, parentVersions);
        } else {
            throw new UnrecognizedVersionException();
        }
    }
}
