package jacz.peerengineclient.databases;

import jacz.database.DatabaseMediator;
import jacz.peerengineservice.PeerID;
import jacz.util.io.serialization.*;
import jacz.util.lists.tuple.Duple;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Alberto on 21/12/2015.
 */
public class ItemRelations implements VersionedObject {

    public static class ItemRelationsMap implements Serializable {

        private final Map<DatabaseMediator.ItemType, Map<Integer, Integer>> itemRelations;

        public ItemRelationsMap() {
            itemRelations = new HashMap<>();
        }

        public boolean contains(DatabaseMediator.ItemType type, int from) {
            return itemRelations.containsKey(type) && itemRelations.get(type).containsKey(from);
        }

        public void put(DatabaseMediator.ItemType type, int from, Integer to) {
            if (!itemRelations.containsKey(type)) {
                itemRelations.put(type, new HashMap<>());
            }
            itemRelations.get(type).put(from, to);
        }

        public Integer get(DatabaseMediator.ItemType type, int from) {
            if (!itemRelations.containsKey(type)) {
                return null;
            }
            return itemRelations.get(type).get(from);
        }

        public void remove(DatabaseMediator.ItemType type, int from) {
            if (itemRelations.containsKey(type)) {
                itemRelations.get(type).remove(from);
            }
        }
    }

    public static class ItemToPeerListRelationsMap implements Serializable {

        private final Map<DatabaseMediator.ItemType, Map<Integer, List<Duple<PeerID, Integer>>>> itemRelations;

        public ItemToPeerListRelationsMap() {
            itemRelations = new HashMap<>();
        }

        public void add(DatabaseMediator.ItemType type, int from, PeerID peerID, int to) {
            if (!itemRelations.containsKey(type)) {
                itemRelations.put(type, new HashMap<>());
            }
            if (!itemRelations.get(type).containsKey(from)) {
                itemRelations.get(type).put(from, new ArrayList<>());
            }
            itemRelations.get(type).get(from).add(new Duple<>(peerID, to));
        }

        public List<Duple<PeerID, Integer>> get(DatabaseMediator.ItemType type, int from) {
            if (!itemRelations.containsKey(type) || !itemRelations.get(type).containsKey(from)) {
                return new ArrayList<>();
            }
            return itemRelations.get(type).get(from);
        }

        public void remove(DatabaseMediator.ItemType type, int from, PeerID peerID) {
            List<Duple<PeerID, Integer>> list = get(type, from);
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).element1.equals(peerID)) {
                    list.remove(i);
                    break;
                }
            }
        }
    }

    private static final String VERSION_0_1 = "0.1";

    private static final String CURRENT_VERSION = VERSION_0_1;

    private ItemRelationsMap integratedToLocal;

    private ItemToPeerListRelationsMap integratedToRemote;

    private ItemRelationsMap integratedToDeleted;

    private ItemRelationsMap integratedToShared;

    private ItemRelationsMap localToIntegrated;

    private HashMap<PeerID, ItemRelationsMap> remoteToIntegrated;

    public ItemRelations() {
        integratedToLocal = new ItemRelationsMap();
        integratedToRemote = new ItemToPeerListRelationsMap();
        integratedToDeleted = new ItemRelationsMap();
        integratedToShared = new ItemRelationsMap();
        localToIntegrated = new ItemRelationsMap();
        remoteToIntegrated = new HashMap<>();
    }

    public ItemRelations(String path, String... backupPaths) throws IOException, VersionedSerializationException {
        VersionedObjectSerializer.deserialize(this, path, backupPaths);
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

    public ItemRelationsMap getIntegratedToShared() {
        return integratedToShared;
    }

    public ItemRelationsMap getLocalToIntegrated() {
        return localToIntegrated;
    }

    public ItemRelationsMap getRemoteToIntegrated(PeerID peerID) {
        if (!remoteToIntegrated.containsKey(peerID)) {
            remoteToIntegrated.put(peerID, new ItemRelationsMap());
        }
        return remoteToIntegrated.get(peerID);
    }

    @Override
    public VersionStack getCurrentVersion() {
        return new VersionStack(CURRENT_VERSION);
    }

    @Override
    public Map<String, Serializable> serialize() {
        Map<String, Serializable> attributes = new HashMap<>();
        attributes.put("integratedToLocal", integratedToLocal);
        attributes.put("integratedToRemote", integratedToRemote);
        attributes.put("integratedToDeleted", integratedToDeleted);
        attributes.put("integratedToShared", integratedToShared);
        attributes.put("localToIntegrated", localToIntegrated);
        attributes.put("remoteToIntegrated", remoteToIntegrated);
        return attributes;
    }

    @Override
    public void deserialize(String version, Map<String, Object> attributes, VersionStack parentVersions) throws UnrecognizedVersionException {
        if (version.equals(CURRENT_VERSION)) {
            integratedToLocal = (ItemRelationsMap) attributes.get("integratedToLocal");
            integratedToRemote = (ItemToPeerListRelationsMap) attributes.get("integratedToRemote");
            integratedToDeleted = (ItemRelationsMap) attributes.get("integratedToDeleted");
            integratedToShared = (ItemRelationsMap) attributes.get("integratedToShared");
            localToIntegrated = (ItemRelationsMap) attributes.get("localToIntegrated");
            remoteToIntegrated = (HashMap<PeerID, ItemRelationsMap>) attributes.get("remoteToIntegrated");
        } else {
            throw new UnrecognizedVersionException();
        }
    }
}
