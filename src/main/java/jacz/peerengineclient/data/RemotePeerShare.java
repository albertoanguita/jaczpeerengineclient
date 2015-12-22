package jacz.peerengineclient.data;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.client.PeerClient;
import jacz.util.io.object_serialization.*;
import jacz.util.maps.DoubleMap;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Stores the share of a remote peer. Includes id and maxStoredTimestamp to facilitate synchronization
 */
public class RemotePeerShare implements VersionedObject {

    private static final String VERSION_0_1 = "VERSION_0.1";

    private static final String CURRENT_VERSION = VERSION_0_1;

    private PeerID remotePeerID;

    private String id;

    private DoubleMap<Long, String> activeHashes;

    private long maxStoredTimestamp;

    private final ForeignShares foreignShares;

    public RemotePeerShare(PeerClient peerClient, PeerID remotePeerID) {
        this.remotePeerID = remotePeerID;
        this.id = "";
        activeHashes = new DoubleMap<>();
        maxStoredTimestamp = -1;
        foreignShares = new ForeignShares(peerClient);
    }

    public RemotePeerShare(PeerClient peerClient, String path, String... backupPaths) throws VersionedSerializationException, IOException {
        VersionedObjectSerializer.deserialize(this, path, backupPaths);
        foreignShares = new ForeignShares(peerClient);
        for (String resourceId : activeHashes.values()) {
            foreignShares.addResourceProvider(resourceId, remotePeerID);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getMaxStoredTimestamp() {
        return maxStoredTimestamp;
    }

    public ForeignShares getForeignShares() {
        return foreignShares;
    }

    public void addHash(long timestamp, String hash) {
        activeHashes.put(timestamp, hash);
        updateTimestamp(timestamp);
    }

    public void removeHash(long timestamp, String hash) {
        activeHashes.removeReverse(hash);
        updateTimestamp(timestamp);
    }

    void updateTimestamp(long timestamp) {
        maxStoredTimestamp = timestamp;
    }

    @Override
    public VersionStack getCurrentVersion() {
        return new VersionStack(CURRENT_VERSION);
    }

    @Override
    public Map<String, Serializable> serialize() {
        Map<String, Serializable> attributes = new HashMap<>();
        attributes.put("remotePeerID", remotePeerID);
        attributes.put("id", id);
        attributes.put("activeHashes", activeHashes);
        attributes.put("maxStoredTimestamp", maxStoredTimestamp);
        return attributes;
    }

    @Override
    public void deserialize(String version, Map<String, Object> attributes, VersionStack parentVersions) throws UnrecognizedVersionException {
        if (version.equals(CURRENT_VERSION)) {
            remotePeerID = (PeerID) attributes.get("remotePeerID");
            id = (String) attributes.get("id");
            activeHashes = (DoubleMap<Long, String>) attributes.get("activeHashes");
            maxStoredTimestamp = (Integer) attributes.get("maxStoredTimestamp");
        } else {
            throw new UnrecognizedVersionException();
        }
    }
}
