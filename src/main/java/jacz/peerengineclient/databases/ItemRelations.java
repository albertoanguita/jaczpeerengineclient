package jacz.peerengineclient.databases;

import jacz.database.DatabaseMediator;
import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineclient.file_system.PathConstants;
import jacz.peerengineservice.PeerId;
import org.aanguita.jacuzzi.io.serialization.VersionedSerializationException;
import org.aanguita.jacuzzi.io.serialization.localstorage.Updater;
import org.aanguita.jacuzzi.io.serialization.localstorage.VersionedLocalStorage;
import org.aanguita.jacuzzi.lists.tuple.Duple;
import org.aanguita.jacuzzi.maps.DoubleMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 */
public class ItemRelations {

    public static class ItemRelationsMap implements Updater {

        private static final String VERSION_0_1 = "0.1";

        private static final String CURRENT_VERSION = VERSION_0_1;

        private final Map<DatabaseMediator.ItemType, DoubleMap<Integer, Integer>> itemRelations;

        private final VersionedLocalStorage vls;

        public ItemRelationsMap(String path) {
            itemRelations = new HashMap<>();
            vls = new VersionedLocalStorage(path, this, CURRENT_VERSION);
            for (String typeString : vls.categories()) {
                DatabaseMediator.ItemType type = DatabaseMediator.ItemType.valueOf(typeString);
                for (String fromKey : vls.keys(typeString)) {
                    int from = Integer.parseInt(fromKey);
                    int to = vls.getInteger(fromKey, typeString);
                    put(type, from, to);
                }
            }
        }

        public static void createNew(String path) throws IOException {
            VersionedLocalStorage.createNew(path, CURRENT_VERSION);
        }

//        public ItemRelationsMap(byte[] data) throws VersionedSerializationException {
//            VersionedObjectSerializer.deserialize(this, data);
//        }

        public synchronized boolean contains(DatabaseMediator.ItemType type, int from) {
            return itemRelations.containsKey(type) && itemRelations.get(type).containsKey(from);
        }

        public synchronized boolean containsReverse(DatabaseMediator.ItemType type, int to) {
            return itemRelations.containsKey(type) && itemRelations.get(type).containsValue(to);
        }

        // todo the to parameter was an Integer before. Switch back if needed
        public synchronized void put(DatabaseMediator.ItemType type, int from, int to) {
            if (!itemRelations.containsKey(type)) {
                itemRelations.put(type, new DoubleMap<>());
            }
            itemRelations.get(type).put(from, to);
            vls.setInteger(Integer.toString(from), to, type.toString());
        }

        public synchronized Integer get(DatabaseMediator.ItemType type, int from) {
            if (!itemRelations.containsKey(type)) {
                return null;
            }
            return itemRelations.get(type).get(from);
        }

        public synchronized Integer getReverse(DatabaseMediator.ItemType type, int to) {
            if (!itemRelations.containsKey(type)) {
                return null;
            }
            return itemRelations.get(type).getReverse(to);
        }

        public synchronized void remove(DatabaseMediator.ItemType type, int from) {
            if (itemRelations.containsKey(type)) {
                itemRelations.get(type).remove(from);
                vls.removeItem(Integer.toString(from), type.toString());
            }
        }

        public synchronized void removeReverse(DatabaseMediator.ItemType type, int to) {
            if (containsReverse(type, to)) {
                remove(type, getReverse(type, to));
            }
        }

        public Map<DatabaseMediator.ItemType, Map<Integer, Integer>> getTypeMappings() {
            Map<DatabaseMediator.ItemType, Map<Integer, Integer>> map = new HashMap<>();
            for (Map.Entry<DatabaseMediator.ItemType, DoubleMap<Integer, Integer>> entry : itemRelations.entrySet()) {
                map.put(entry.getKey(), entry.getValue().getDirectMap());
            }
            return map;
        }

        public Map<DatabaseMediator.ItemType, Map<Integer, Integer>> getTypeMappingsReverse() {
            Map<DatabaseMediator.ItemType, Map<Integer, Integer>> map = new HashMap<>();
            for (Map.Entry<DatabaseMediator.ItemType, DoubleMap<Integer, Integer>> entry : itemRelations.entrySet()) {
                map.put(entry.getKey(), entry.getValue().getReverseMap());
            }
            return map;
        }

//        @Override
//        public VersionStack getCurrentVersion() {
//            return new VersionStack(CURRENT_VERSION);
//        }

//        @Override
//        public Map<String, Serializable> serialize() {
//            Map<String, Serializable> attributes = new HashMap<>();
//            attributes.put("itemRelations", itemRelations);
//            return attributes;
//        }
//
//        @Override
//        public void deserialize(String version, Map<String, Object> attributes, VersionStack parentVersions) throws UnrecognizedVersionException {
//            if (version.equals(CURRENT_VERSION)) {
//                itemRelations = (HashMap<DatabaseMediator.ItemType, Map<Integer, Integer>>) attributes.get("itemRelations");
//            } else {
//                throw new UnrecognizedVersionException();
//            }
//        }


        @Override
        public String update(VersionedLocalStorage versionedLocalStorage, String storedVersion) {
            // ignore, cannot happen yet
            return null;
        }
    }

    public static class PeerItemRelationsMap implements Updater {

        private static final String VERSION_0_1 = "0.1";

        private static final String CURRENT_VERSION = VERSION_0_1;

        private final Map<PeerId, Map<DatabaseMediator.ItemType, DoubleMap<Integer, Integer>>> peerItemRelations;

        private final VersionedLocalStorage vls;

        public PeerItemRelationsMap(String path) {
            peerItemRelations = new HashMap<>();
            vls = new VersionedLocalStorage(path, this, CURRENT_VERSION);
            for (String peerIdString : vls.categories()) {
                PeerId peerId = new PeerId(peerIdString);
                for (String typeString : vls.categories(peerIdString)) {
                    DatabaseMediator.ItemType type = DatabaseMediator.ItemType.valueOf(typeString);
                    for (String fromKey : vls.keys(peerIdString, typeString)) {
                        int from = Integer.parseInt(fromKey);
                        int to = vls.getInteger(fromKey, peerIdString, typeString);
                        put(peerId, type, from, to);
                    }
                }
            }
        }

        public static void createNew(String path) throws IOException {
            VersionedLocalStorage.createNew(path, CURRENT_VERSION);
        }

//        public ItemRelationsMap(byte[] data) throws VersionedSerializationException {
//            VersionedObjectSerializer.deserialize(this, data);
//        }

        public synchronized boolean contains(PeerId peerId, DatabaseMediator.ItemType type, int from) {
            return peerItemRelations.containsKey(peerId) &&
                    peerItemRelations.get(peerId).containsKey(type) &&
                    peerItemRelations.get(peerId).get(type).containsKey(from);
        }

        public synchronized boolean containsReverse(PeerId peerId, DatabaseMediator.ItemType type, int to) {
            return peerItemRelations.containsKey(peerId) &&
                    peerItemRelations.get(peerId).containsKey(type) &&
                    peerItemRelations.get(peerId).get(type).containsValue(to);
        }

        // todo the to parameter was an Integer before. Switch back if needed
        public synchronized void put(PeerId peerId, DatabaseMediator.ItemType type, int from, int to) {
            if (!peerItemRelations.containsKey(peerId)) {
                peerItemRelations.put(peerId, new HashMap<>());
            }
            if (!peerItemRelations.get(peerId).containsKey(type)) {
                peerItemRelations.get(peerId).put(type, new DoubleMap<>());
            }
            peerItemRelations.get(peerId).get(type).put(from, to);
            vls.setInteger(Integer.toString(from), to, peerId.toString(), type.toString());
        }

        public synchronized Integer get(PeerId peerId, DatabaseMediator.ItemType type, int from) {
            if (!peerItemRelations.containsKey(peerId) || !peerItemRelations.get(peerId).containsKey(type)) {
                return null;
            }
            return peerItemRelations.get(peerId).get(type).get(from);
        }

        public synchronized Integer getReverse(PeerId peerId, DatabaseMediator.ItemType type, int to) {
            if (!peerItemRelations.containsKey(peerId) || !peerItemRelations.get(peerId).containsKey(type)) {
                return null;
            }
            return peerItemRelations.get(peerId).get(type).getReverse(to);
        }

        public synchronized void remove(PeerId peerId, DatabaseMediator.ItemType type, int from) {
            if (peerItemRelations.containsKey(peerId) && peerItemRelations.get(peerId).containsKey(type)) {
                peerItemRelations.get(peerId).get(type).remove(from);
                vls.removeItem(Integer.toString(from), peerId.toString(), type.toString());
            }
        }

        public synchronized void removeReverse(PeerId peerId, DatabaseMediator.ItemType type, int to) {
            if (containsReverse(peerId, type, to)) {
                remove(peerId, type, getReverse(peerId, type, to));
            }
        }

        public Map<DatabaseMediator.ItemType, Map<Integer, Integer>> getTypeMappings(PeerId peerId) {
            Map<DatabaseMediator.ItemType, Map<Integer, Integer>> map = new HashMap<>();
            for (Map.Entry<DatabaseMediator.ItemType, DoubleMap<Integer, Integer>> entry : peerItemRelations.get(peerId).entrySet()) {
                map.put(entry.getKey(), entry.getValue().getDirectMap());
            }
            return map;
        }

        public Map<DatabaseMediator.ItemType, Map<Integer, Integer>> getTypeMappingsReverse(PeerId peerId) {
            Map<DatabaseMediator.ItemType, Map<Integer, Integer>> map = new HashMap<>();
            for (Map.Entry<DatabaseMediator.ItemType, DoubleMap<Integer, Integer>> entry : peerItemRelations.get(peerId).entrySet()) {
                map.put(entry.getKey(), entry.getValue().getReverseMap());
            }
            return map;
        }

        @Override
        public String update(VersionedLocalStorage versionedLocalStorage, String storedVersion) {
            // ignore, cannot happen yet
            return null;
        }
    }

    public static class ItemToPeerListRelationsMap implements Updater {

        private static final String VERSION_0_1 = "0.1";

        private static final String CURRENT_VERSION = VERSION_0_1;

        private final Map<DatabaseMediator.ItemType, Map<Integer, List<Duple<PeerId, Integer>>>> itemRelations;

        private final VersionedLocalStorage vls;

        public ItemToPeerListRelationsMap(String path) {
            itemRelations = new HashMap<>();
            vls = new VersionedLocalStorage(path, this, CURRENT_VERSION);
            for (String typeString : vls.categories()) {
                DatabaseMediator.ItemType type = DatabaseMediator.ItemType.valueOf(typeString);
                itemRelations.put(type, new HashMap<>());
                for (String fromKey : vls.keys(typeString)) {
                    int from = Integer.parseInt(fromKey);
                    List<Duple<PeerId, Integer>> peerAndIdList = parseStringList(vls.getStringList(fromKey, typeString));
                    itemRelations.get(type).put(from, peerAndIdList);
                }
            }
        }

        public static void createNew(String path) throws IOException {
            VersionedLocalStorage.createNew(path, CURRENT_VERSION);
        }

//        public ItemToPeerListRelationsMap(byte[] data) throws VersionedSerializationException {
//            VersionedObjectSerializer.deserialize(this, data);
//        }

        public synchronized void add(DatabaseMediator.ItemType type, int from, PeerId peerID, int to) {
            if (!itemRelations.containsKey(type)) {
                itemRelations.put(type, new HashMap<>());
            }
            if (!itemRelations.get(type).containsKey(from)) {
                itemRelations.get(type).put(from, new ArrayList<>());
            }
            itemRelations.get(type).get(from).add(new Duple<>(peerID, to));
            vls.setStringList(Integer.toString(from), toStringList(get(type, from)), type.toString());
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
            if (list.isEmpty()) {
                vls.removeItem(Integer.toString(from), type.toString());
            } else {
                vls.setStringList(Integer.toString(from), toStringList(list), type.toString());
            }
        }

        private static List<String> toStringList(List<Duple<PeerId, Integer>> peerAndIdList) {
            return peerAndIdList.stream().map(ItemToPeerListRelationsMap::toString).collect(Collectors.toList());
        }

        private static String toString(Duple<PeerId, Integer> peerAndId) {
            return peerAndId.element1.toString() + "," + peerAndId.element2.toString();
        }

        private static List<Duple<PeerId, Integer>> parseStringList(List<String> stringList) {
            return stringList.stream().map(ItemToPeerListRelationsMap::parseString).collect(Collectors.toList());
        }

        private static Duple<PeerId, Integer> parseString(String str) {
            String[] split = str.split(",");
            return new Duple<>(new PeerId(split[0]), Integer.parseInt(split[1]));
        }

        @Override
        public String update(VersionedLocalStorage versionedLocalStorage, String storedVersion) {
            // ignore, cannot happen yet
            return null;
        }

//        @Override
//        public VersionStack getCurrentVersion() {
//            return new VersionStack(CURRENT_VERSION);
//        }
//
//        @Override
//        public Map<String, Serializable> serialize() {
//            FragmentedByteArray data = new FragmentedByteArray(Serializer.serialize(itemRelations.size()));
//            for (Map.Entry<DatabaseMediator.ItemType, Map<Integer, List<Duple<PeerId, Integer>>>> entry : itemRelations.entrySet()) {
//                data.add(Serializer.serialize(entry.getKey()));
//                data.add(serializeItemRelationsValue(entry.getValue()));
//            }
//            Map<String, Serializable> attributes = new HashMap<>();
//            attributes.put("itemRelations", data.generateArray());
//            return attributes;
//        }
//
//        private static byte[] serializeItemRelationsValue(Map<Integer, List<Duple<PeerId, Integer>>> map) {
//            FragmentedByteArray data = new FragmentedByteArray(Serializer.serialize(map.size()));
//            for (Map.Entry<Integer, List<Duple<PeerId, Integer>>> entry : map.entrySet()) {
//                data.add(Serializer.serialize(entry.getKey()));
//                data.add(serializePeerList(entry.getValue()));
//            }
//            return data.generateArray();
//        }
//
//        private static byte[] serializePeerList(List<Duple<PeerId, Integer>> list) {
//            FragmentedByteArray data = new FragmentedByteArray(Serializer.serialize(list.size()));
//            for (Duple<PeerId, Integer> peerAndId : list) {
//                data.add(Serializer.serialize(peerAndId.element1.toByteArray()));
//                data.add(Serializer.serialize(peerAndId.element2));
//            }
//            return data.generateArray();
//        }
//
////        private static HashMap<DatabaseMediator.ItemType, Map<Integer, List<Duple<byte[], Integer>>>> serializeItemRelations(
////                Map<DatabaseMediator.ItemType, Map<Integer, List<Duple<PeerId, Integer>>>> itemRelations) {
////
////            itemRelations.entrySet().stream()
////                    .map(entry -> new Duple<>(
////                                    entry.getKey(),
////                                    entry.getValue().entrySet().stream()
////                                            .map(typeEntry -> new Duple<>(
////                                                    typeEntry.getKey(), typeEntry.getValue().stream()
////                                                    .map(duple -> new Duple<>(duple.element1.toByteArray(), duple.element2))
////                                                    .collect(Collectors.toList()))))
////                    );
////
////            HashMap<DatabaseMediator.ItemType, Map<Integer, List<Duple<byte[], Integer>>>> serializedItemRelations = new HashMap<>();
////            for (Map.Entry<DatabaseMediator.ItemType, Map<Integer, List<Duple<PeerId, Integer>>>> entry : itemRelations.entrySet()) {
////                Map<Integer, List<Duple<byte[], Integer>>> value = new HashMap<>();
////                for (Map.Entry<Integer, List<Duple<PeerId, Integer>>> typeEntry : entry.getValue().entrySet()) {
////                    List<Duple<byte[], Integer>> list =
////                            typeEntry.getValue().stream()
////                                    .map(duple -> new Duple<>(duple.element1.toByteArray(), duple.element2))
////                                    .collect(Collectors.toList());
////                    value.put(typeEntry.getKey(), list);
////                }
////                serializedItemRelations.put(entry.getKey(), value);
////            }
////            return serializedItemRelations;
////        }
//
//        @Override
//        public void deserialize(String version, Map<String, Object> attributes, VersionStack parentVersions) throws UnrecognizedVersionException {
//            if (version.equals(CURRENT_VERSION)) {
//                byte[] data = (byte[]) attributes.get("itemRelations");
//                MutableOffset offset = new MutableOffset();
//                int mapSize = Serializer.deserializeIntValue(data, offset);
//                itemRelations = new HashMap<>();
//                for (int i = 0; i < mapSize; i++) {
//                    DatabaseMediator.ItemType itemType = Serializer.deserializeEnum(DatabaseMediator.ItemType.class, data, offset);
//                    Map<Integer, List<Duple<PeerId, Integer>>> itemRelationsValue = deserializeItemRelationsValue(data, offset);
//                    itemRelations.put(itemType, itemRelationsValue);
//                }
//            } else {
//                throw new UnrecognizedVersionException();
//            }
//        }
//
//        private static Map<Integer, List<Duple<PeerId, Integer>>> deserializeItemRelationsValue(byte[] data, MutableOffset offset) {
//            int mapSize = Serializer.deserializeIntValue(data, offset);
//            Map<Integer, List<Duple<PeerId, Integer>>> itemRelationsValue = new HashMap<>();
//            for (int i = 0; i < mapSize; i++) {
//                Integer id = Serializer.deserializeInt(data, offset);
//                List<Duple<PeerId, Integer>> peerList = deserializePeerList(data, offset);
//                itemRelationsValue.put(id, peerList);
//            }
//            return itemRelationsValue;
//        }
//
//        private static List<Duple<PeerId, Integer>> deserializePeerList(byte[] data, MutableOffset offset) {
//            int listSize = Serializer.deserializeIntValue(data, offset);
//            List<Duple<PeerId, Integer>> peerList = new ArrayList<>();
//            for (int i = 0; i < listSize; i++) {
//                PeerId peerId = new PeerId(Serializer.deserializeBytes(data, offset));
//                Integer id = Serializer.deserializeInt(data, offset);
//                peerList.add(new Duple<>(peerId, id));
//            }
//            return peerList;
//        }

//        private Map<DatabaseMediator.ItemType, Map<Integer, List<Duple<PeerId, Integer>>>> deserializeItemRelations(
//                HashMap<DatabaseMediator.ItemType, Map<Integer, List<Duple<byte[], Integer>>>> serializedItemRelations) {
//            Map<DatabaseMediator.ItemType, Map<Integer, List<Duple<PeerId, Integer>>>> itemRelations = new HashMap<>();
//            for (Map.Entry<DatabaseMediator.ItemType, Map<Integer, List<Duple<byte[], Integer>>>> entry : serializedItemRelations.entrySet()) {
//                Map<Integer, List<Duple<PeerId, Integer>>> value = new HashMap<>();
//                for (Map.Entry<Integer, List<Duple<byte[], Integer>>> typeEntry : entry.getValue().entrySet()) {
//                    List<Duple<PeerId, Integer>> list =
//                            typeEntry.getValue().stream()
//                                    .map(duple -> new Duple<>(new PeerId(duple.element1), duple.element2))
//                                    .collect(Collectors.toList());
//                    value.put(typeEntry.getKey(), list);
//                }
//                itemRelations.put(entry.getKey(), value);
//            }
//            return itemRelations;
//        }
    }

    private static final String VERSION_0_1 = "0.1";

    private static final String CURRENT_VERSION = VERSION_0_1;

    private ItemToPeerListRelationsMap integratedToRemote;

    private ItemRelationsMap deletedToIntegrated;

    private ItemRelationsMap integratedToShared;

    private ItemRelationsMap localToIntegrated;

    private PeerItemRelationsMap remoteToIntegrated;

    public ItemRelations(String basePath) {
        integratedToRemote = new ItemToPeerListRelationsMap(PathConstants.integratedToRemotePath(basePath));
        deletedToIntegrated = new ItemRelationsMap(PathConstants.deletedToIntegratedPath(basePath));
        integratedToShared = new ItemRelationsMap(PathConstants.integratedToSharedPath(basePath));
        localToIntegrated = new ItemRelationsMap(PathConstants.localToIntegratedPath(basePath));
        remoteToIntegrated = new PeerItemRelationsMap(PathConstants.remoteToIntegratedPath(basePath));
    }

    public static void createNewFiles(String basePath) throws IOException {
        ItemToPeerListRelationsMap.createNew(PathConstants.integratedToRemotePath(basePath));
        ItemRelationsMap.createNew(PathConstants.deletedToIntegratedPath(basePath));
        ItemRelationsMap.createNew(PathConstants.integratedToSharedPath(basePath));
        ItemRelationsMap.createNew(PathConstants.localToIntegratedPath(basePath));
        PeerItemRelationsMap.createNew(PathConstants.remoteToIntegratedPath(basePath));
    }

//    public ItemRelations(PeerEngineClient peerEngineClient, String path, String... backupPaths) throws IOException, VersionedSerializationException {
//        this.peerEngineClient = peerEngineClient;
//    }

    public ItemToPeerListRelationsMap getIntegratedToRemote() {
        return integratedToRemote;
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

    public PeerItemRelationsMap getRemoteToIntegrated() {
        return remoteToIntegrated;
    }

//    @Override
//    public VersionStack getCurrentVersion() {
//        return new VersionStack(CURRENT_VERSION);
//    }
//
//    @Override
//    public Map<String, Serializable> serialize() {
//        Map<String, Serializable> attributes = new HashMap<>();
//        try {
//            attributes.put("integratedToLocal", VersionedObjectSerializer.serialize(integratedToLocal));
//            attributes.put("integratedToRemote", VersionedObjectSerializer.serialize(integratedToRemote));
//            attributes.put("integratedToDeleted", VersionedObjectSerializer.serialize(integratedToDeleted));
//            attributes.put("deletedToIntegrated", VersionedObjectSerializer.serialize(deletedToIntegrated));
//            attributes.put("integratedToShared", VersionedObjectSerializer.serialize(integratedToShared));
//            attributes.put("localToIntegrated", VersionedObjectSerializer.serialize(localToIntegrated));
//            attributes.put("remoteToIntegrated", serializeRemoteToIntegrated(peerEngineClient, remoteToIntegrated));
//            return attributes;
//        } catch (NotSerializableException e) {
//            peerEngineClient.reportFatalError("Item relation is not serializable", e);
//            return new HashMap<>();
//        }
//    }
//
//    @Override
//    public void deserialize(String version, Map<String, Object> attributes, VersionStack parentVersions) throws UnrecognizedVersionException {
//        if (version.equals(CURRENT_VERSION)) {
//            try {
//                integratedToLocal = new ItemRelationsMap((byte[]) attributes.get("integratedToLocal"));
//                integratedToRemote = new ItemToPeerListRelationsMap((byte[]) attributes.get("integratedToRemote"));
//                integratedToDeleted = new ItemRelationsMap((byte[]) attributes.get("integratedToDeleted"));
//                deletedToIntegrated = new ItemRelationsMap((byte[]) attributes.get("deletedToIntegrated"));
//                integratedToShared = new ItemRelationsMap((byte[]) attributes.get("integratedToShared"));
//                localToIntegrated = new ItemRelationsMap((byte[]) attributes.get("localToIntegrated"));
//                remoteToIntegrated = deserializeRemoteToIntegrated(peerEngineClient, (byte[]) attributes.get("remoteToIntegrated"));
//            } catch (VersionedSerializationException e) {
//                throw new RuntimeException(e.getMessage());
//            }
//        } else {
//            throw new UnrecognizedVersionException();
//        }
//    }
//
//    private static byte[] serializeRemoteToIntegrated(PeerEngineClient peerEngineClient, Map<PeerId, ItemRelationsMap> remoteToIntegrated) {
//        FragmentedByteArray data = new FragmentedByteArray(Serializer.serialize(remoteToIntegrated.size()));
//        try {
//            for (Map.Entry<PeerId, ItemRelationsMap> entry : remoteToIntegrated.entrySet()) {
//                PeerId peerID = entry.getKey();
//                ItemRelationsMap itemRelationsMap = entry.getValue();
//                data.add(Serializer.serialize(peerID.toByteArray()), Serializer.serialize(VersionedObjectSerializer.serialize(itemRelationsMap)));
//            }
//            return data.generateArray();
//        } catch (NotSerializableException e) {
//            peerEngineClient.reportFatalError("Item relation is not serializable", e);
//            return new byte[0];
//        }
//    }
//
//    private static Map<PeerId, ItemRelationsMap> deserializeRemoteToIntegrated(PeerEngineClient peerEngineClient, byte[] data) {
//        MutableOffset offset = new MutableOffset();
//        int mapSize = Serializer.deserializeIntValue(data, offset);
//        Map<PeerId, ItemRelationsMap> remoteToIntegrated = new HashMap<>();
//        try {
//            for (int i = 0; i < mapSize; i++) {
//                PeerId peerID = new PeerId(Serializer.deserializeBytes(data, offset));
//                ItemRelationsMap itemRelationsMap = new ItemRelationsMap(Serializer.deserializeBytes(data, offset));
//                remoteToIntegrated.put(peerID, itemRelationsMap);
//            }
//            return remoteToIntegrated;
//        } catch (VersionedSerializationException e) {
//            peerEngineClient.reportFatalError("Item relation is not serializable", e);
//            return new HashMap<>();
//        }
//    }
}
