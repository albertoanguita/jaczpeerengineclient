package jacz.peerengineclient.databases;

import jacz.database.DatabaseMediator;
import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineservice.PeerId;
import org.aanguita.jacuzzi.io.serialization.*;
import org.aanguita.jacuzzi.lists.tuple.Duple;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * todo refactor to local storage
 */
public class ItemRelations implements VersionedObject {

    public static class ItemRelationsMap implements VersionedObject {

        private static final String VERSION_0_1 = "0.1";

        private static final String CURRENT_VERSION = VERSION_0_1;

        private HashMap<DatabaseMediator.ItemType, Map<Integer, Integer>> itemRelations;

        public ItemRelationsMap() {
            itemRelations = new HashMap<>();
        }

        public ItemRelationsMap(byte[] data) throws VersionedSerializationException {
            VersionedObjectSerializer.deserialize(this, data);
        }

        public synchronized boolean contains(DatabaseMediator.ItemType type, int from) {
            return itemRelations.containsKey(type) && itemRelations.get(type).containsKey(from);
        }

        public synchronized void put(DatabaseMediator.ItemType type, int from, Integer to) {
            if (!itemRelations.containsKey(type)) {
                itemRelations.put(type, new HashMap<>());
            }
            itemRelations.get(type).put(from, to);
        }

        public synchronized Integer get(DatabaseMediator.ItemType type, int from) {
            if (!itemRelations.containsKey(type)) {
                return null;
            }
            return itemRelations.get(type).get(from);
        }

        public synchronized void remove(DatabaseMediator.ItemType type, int from) {
            if (itemRelations.containsKey(type)) {
                itemRelations.get(type).remove(from);
            }
        }

        public Map<DatabaseMediator.ItemType, Map<Integer, Integer>> getTypeMappings() {
            return itemRelations;
        }

        @Override
        public VersionStack getCurrentVersion() {
            return new VersionStack(CURRENT_VERSION);
        }

        @Override
        public Map<String, Serializable> serialize() {
            Map<String, Serializable> attributes = new HashMap<>();
            attributes.put("itemRelations", itemRelations);
            return attributes;
        }

        @Override
        public void deserialize(String version, Map<String, Object> attributes, VersionStack parentVersions) throws UnrecognizedVersionException {
            if (version.equals(CURRENT_VERSION)) {
                itemRelations = (HashMap<DatabaseMediator.ItemType, Map<Integer, Integer>>) attributes.get("itemRelations");
            } else {
                throw new UnrecognizedVersionException();
            }
        }
    }

    public static class ItemToPeerListRelationsMap implements VersionedObject {

        private static final String VERSION_0_1 = "0.1";

        private static final String CURRENT_VERSION = VERSION_0_1;

        private HashMap<DatabaseMediator.ItemType, Map<Integer, List<Duple<PeerId, Integer>>>> itemRelations;

        public ItemToPeerListRelationsMap() {
            itemRelations = new HashMap<>();
        }

        public ItemToPeerListRelationsMap(byte[] data) throws VersionedSerializationException {
            VersionedObjectSerializer.deserialize(this, data);
        }

        public synchronized void add(DatabaseMediator.ItemType type, int from, PeerId peerID, int to) {
            if (!itemRelations.containsKey(type)) {
                itemRelations.put(type, new HashMap<>());
            }
            if (!itemRelations.get(type).containsKey(from)) {
                itemRelations.get(type).put(from, new ArrayList<>());
            }
            itemRelations.get(type).get(from).add(new Duple<>(peerID, to));
        }

        public synchronized List<Duple<PeerId, Integer>> get(DatabaseMediator.ItemType type, int from) {
            if (!itemRelations.containsKey(type) || !itemRelations.get(type).containsKey(from)) {
                return new ArrayList<>();
            }
            return itemRelations.get(type).get(from);
        }

        public synchronized void remove(DatabaseMediator.ItemType type, int from, PeerId peerID) {
            List<Duple<PeerId, Integer>> list = get(type, from);
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).element1.equals(peerID)) {
                    list.remove(i);
                    break;
                }
            }
        }

        @Override
        public VersionStack getCurrentVersion() {
            return new VersionStack(CURRENT_VERSION);
        }

        @Override
        public Map<String, Serializable> serialize() {
            Map<String, Serializable> attributes = new HashMap<>();
            attributes.put("itemRelations", itemRelations);
            return attributes;
        }

        @Override
        public void deserialize(String version, Map<String, Object> attributes, VersionStack parentVersions) throws UnrecognizedVersionException {
            if (version.equals(CURRENT_VERSION)) {
                itemRelations = (HashMap<DatabaseMediator.ItemType, Map<Integer, List<Duple<PeerId, Integer>>>>) attributes.get("itemRelations");
            } else {
                throw new UnrecognizedVersionException();
            }
        }
    }

    private static final String VERSION_0_1 = "0.1";

    private static final String CURRENT_VERSION = VERSION_0_1;

    private final PeerEngineClient peerEngineClient;

    private ItemRelationsMap integratedToLocal;

    private ItemToPeerListRelationsMap integratedToRemote;

    private ItemRelationsMap integratedToDeleted;

    private ItemRelationsMap deletedToIntegrated;

    private ItemRelationsMap integratedToShared;

    private ItemRelationsMap localToIntegrated;

    private Map<PeerId, ItemRelationsMap> remoteToIntegrated;

    private final List<String> repairedFiles;

    public ItemRelations() {
        peerEngineClient = null;
        integratedToLocal = new ItemRelationsMap();
        integratedToRemote = new ItemToPeerListRelationsMap();
        integratedToDeleted = new ItemRelationsMap();
        deletedToIntegrated = new ItemRelationsMap();
        integratedToShared = new ItemRelationsMap();
        localToIntegrated = new ItemRelationsMap();
        remoteToIntegrated = new HashMap<>();
        repairedFiles = new ArrayList<>();
    }

    public ItemRelations(PeerEngineClient peerEngineClient, String path, String... backupPaths) throws IOException, VersionedSerializationException {
        this.peerEngineClient = peerEngineClient;
        repairedFiles = VersionedObjectSerializer.deserialize(this, path, true, backupPaths);
    }

    public ItemRelationsMap getIntegratedToLocal() {
        return integratedToLocal;
    }

    public ItemToPeerListRelationsMap getIntegratedToRemote() {
        return integratedToRemote;
    }

    public ItemRelationsMap getIntegratedToDeleted() {
        return integratedToDeleted;
    }

    public ItemRelationsMap getDeletedToIntegrated() {
        return deletedToIntegrated;
    }

    public ItemRelationsMap getIntegratedToShared() {
        return integratedToShared;
    }

    public ItemRelationsMap getLocalToIntegrated() {
        return localToIntegrated;
    }

    public ItemRelationsMap getRemoteToIntegrated(PeerId peerID) {
        if (!remoteToIntegrated.containsKey(peerID)) {
            remoteToIntegrated.put(peerID, new ItemRelationsMap());
        }
        return remoteToIntegrated.get(peerID);
    }

    public List<String> getRepairedFiles() {
        return repairedFiles;
    }

    @Override
    public VersionStack getCurrentVersion() {
        return new VersionStack(CURRENT_VERSION);
    }

    @Override
    public Map<String, Serializable> serialize() {
        Map<String, Serializable> attributes = new HashMap<>();
        try {
            attributes.put("integratedToLocal", VersionedObjectSerializer.serialize(integratedToLocal));
            attributes.put("integratedToRemote", VersionedObjectSerializer.serialize(integratedToRemote));
            attributes.put("integratedToDeleted", VersionedObjectSerializer.serialize(integratedToDeleted));
            attributes.put("deletedToIntegrated", VersionedObjectSerializer.serialize(deletedToIntegrated));
            attributes.put("integratedToShared", VersionedObjectSerializer.serialize(integratedToShared));
            attributes.put("localToIntegrated", VersionedObjectSerializer.serialize(localToIntegrated));
            attributes.put("remoteToIntegrated", serializeRemoteToIntegrated(peerEngineClient, remoteToIntegrated));
            return attributes;
        } catch (NotSerializableException e) {
            peerEngineClient.reportFatalError("Item relation is not serializable", e);
            return new HashMap<>();
        }
    }

    @Override
    public void deserialize(String version, Map<String, Object> attributes, VersionStack parentVersions) throws UnrecognizedVersionException {
        if (version.equals(CURRENT_VERSION)) {
            try {
                integratedToLocal = new ItemRelationsMap((byte[]) attributes.get("integratedToLocal"));
                integratedToRemote = new ItemToPeerListRelationsMap((byte[]) attributes.get("integratedToRemote"));
                integratedToDeleted = new ItemRelationsMap((byte[]) attributes.get("integratedToDeleted"));
                deletedToIntegrated = new ItemRelationsMap((byte[]) attributes.get("deletedToIntegrated"));
                integratedToShared = new ItemRelationsMap((byte[]) attributes.get("integratedToShared"));
                localToIntegrated = new ItemRelationsMap((byte[]) attributes.get("localToIntegrated"));
                remoteToIntegrated = deserializeRemoteToIntegrated(peerEngineClient, (byte[]) attributes.get("remoteToIntegrated"));
            } catch (VersionedSerializationException e) {
                throw new RuntimeException(e.getMessage());
            }
        } else {
            throw new UnrecognizedVersionException();
        }
    }

    private static byte[] serializeRemoteToIntegrated(PeerEngineClient peerEngineClient, Map<PeerId, ItemRelationsMap> remoteToIntegrated) {
        FragmentedByteArray data = new FragmentedByteArray(Serializer.serialize(remoteToIntegrated.size()));
        try {
            for (Map.Entry<PeerId, ItemRelationsMap> entry : remoteToIntegrated.entrySet()) {
                PeerId peerID = entry.getKey();
                ItemRelationsMap itemRelationsMap = entry.getValue();
                data.add(Serializer.serialize(peerID.toByteArray()), Serializer.serialize(VersionedObjectSerializer.serialize(itemRelationsMap)));
            }
            return data.generateArray();
        } catch (NotSerializableException e) {
            peerEngineClient.reportFatalError("Item relation is not serializable", e);
            return new byte[0];
        }
    }

    private static Map<PeerId, ItemRelationsMap> deserializeRemoteToIntegrated(PeerEngineClient peerEngineClient, byte[] data) {
        MutableOffset offset = new MutableOffset();
        int mapSize = Serializer.deserializeIntValue(data, offset);
        Map<PeerId, ItemRelationsMap> remoteToIntegrated = new HashMap<>();
        try {
            for (int i = 0; i < mapSize; i++) {
                PeerId peerID = new PeerId(Serializer.deserializeBytes(data, offset));
                ItemRelationsMap itemRelationsMap = new ItemRelationsMap(Serializer.deserializeBytes(data, offset));
                remoteToIntegrated.put(peerID, itemRelationsMap);
            }
            return remoteToIntegrated;
        } catch (VersionedSerializationException e) {
            peerEngineClient.reportFatalError("Item relation is not serializable", e);
            return new HashMap<>();
        }
    }
}
