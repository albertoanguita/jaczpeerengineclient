package jacz.peerengineclient.peer_share;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.client.PeerClient;
import jacz.peerengineservice.util.ForeignStoreShare;
import jacz.util.io.object_serialization.UnrecognizedVersionException;
import jacz.util.io.object_serialization.VersionedObject;
import jacz.util.io.object_serialization.VersionedObjectSerializer;
import jacz.util.io.object_serialization.VersionedSerializationException;
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

    private DoubleMap<Integer, String> activeHashes;

    private Integer maxStoredTimestamp;

    private final ForeignShares foreignShares;

    public RemotePeerShare(PeerClient peerClient, PeerID remotePeerID, String id) {
        this.remotePeerID = remotePeerID;
        this.id = id;
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

    public ForeignShares getForeignShares() {
        return foreignShares;
    }

    @Override
    public String getCurrentVersion() {
        return CURRENT_VERSION;
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
    public void deserialize(Map<String, Object> attributes) {
        remotePeerID = (PeerID) attributes.get("remotePeerID");
        id = (String) attributes.get("id");
        activeHashes = (DoubleMap<Integer, String>) attributes.get("activeHashes");
        maxStoredTimestamp = (Integer) attributes.get("maxStoredTimestamp");
    }

    @Override
    public void deserializeOldVersion(String version, Map<String, Object> attributes) throws UnrecognizedVersionException {
        throw new UnrecognizedVersionException();
    }
}
