package jacz.peerengineclient.libraries.library_images;

import jacz.peerengineservice.PeerID;
import jacz.store.database.DatabaseMediator;
import jacz.util.io.object_serialization.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The integrated database. This is what the user visualizes in the interface, and is the result of merging the local database and the remote
 * databases
 */
public class IntegratedDatabase extends GenericDatabase implements VersionedObject {

    public static class PeerAndLibraryId extends LibraryId {

        public PeerID peerID;

        public PeerAndLibraryId(DatabaseMediator.ItemType type, int id, PeerID peerID) {
            super(type, id);
            this.peerID = peerID;
        }

        public byte[] serialize() {
            return Serializer.addArrays(super.serialize(), Serializer.serialize(peerID.toString()));
        }

        public static PeerAndLibraryId deserialize(byte[] data, MutableOffset offset) {
            return new PeerAndLibraryId(Serializer.deserializeEnum(DatabaseMediator.ItemType.class, data, offset), Serializer.deserializeInt(data, offset), new PeerID(Serializer.deserializeString(data, offset)));
        }
    }

    private static final String VERSION_0_1 = "0.1";

    private static final String CURRENT_VERSION = VERSION_0_1;

    private static final Object ITEMS_TO_LOCAL_ITEMS_LOCK = new Object();

    private static final Object ITEMS_TO_REMOTE_ITEMS_LOCK = new Object();

    /**
     * Links integrated ids to shared ids (for faster item lookup)
     */
    private final Map<LibraryId, LibraryId> itemsToSharedItems;

    /**
     * Links integrated ids to local ids (for faster item lookup)
     */
    private final Map<LibraryId, LibraryId> itemsToLocalItems;

    /**
     * Links integrated ids to remote ids (for faster item lookup)
     */
    private final Map<LibraryId, List<PeerAndLibraryId>> itemsToRemoteItems;

    /**
     * Links integrated ids to shared ids (for faster item lookup)
     */
    private final Map<LibraryId, LibraryId> itemsToDeletedRemoteItems;
    
    /**
     * Sets up a fresh store. The store itself must be created before
     *
     * @param databasePath path to the library
     */
    public IntegratedDatabase(String databasePath) {
        this(databasePath, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>());
    }

    /**
     * Recovers an existing store
     *
     * @param databasePath       path to the library
     * @param itemsToLocalItems  pointers of integrated ids to local ids
     * @param itemsToRemoteItems pointers of integrated ids to remote ids
     */
    public IntegratedDatabase(
            String databasePath,
            Map<LibraryId, LibraryId> itemsToSharedItems,
            Map<LibraryId, LibraryId> itemsToLocalItems,
            Map<LibraryId, List<PeerAndLibraryId>> itemsToRemoteItems,
            Map<LibraryId, LibraryId> itemsToDeletedRemoteItems) {
        super(databasePath);
        this.itemsToSharedItems = itemsToSharedItems;
        this.itemsToLocalItems = itemsToLocalItems;
        this.itemsToRemoteItems = itemsToRemoteItems;
        this.itemsToDeletedRemoteItems = itemsToDeletedRemoteItems;
    }

    public Map<LibraryId, LibraryId> getItemsToSharedItems() {
        return itemsToSharedItems;
    }

    public Map<LibraryId, LibraryId> getItemsToLocalItems() {
        return itemsToLocalItems;
    }

    public Map<LibraryId, List<PeerAndLibraryId>> getItemsToRemoteItems() {
        return itemsToRemoteItems;
    }

    public Map<LibraryId, LibraryId> getItemsToDeletedRemoteItems() {
        return itemsToDeletedRemoteItems;
    }

    public boolean containsKeyToLocalItem(LibraryId integratedId) {
        synchronized (ITEMS_TO_LOCAL_ITEMS_LOCK) {
            return itemsToLocalItems.containsKey(integratedId);
        }
    }

    public LibraryId getItemToLocalItem(LibraryId integratedId) {
        synchronized (ITEMS_TO_LOCAL_ITEMS_LOCK) {
            return itemsToLocalItems.get(integratedId);
        }
    }

    public void putItemToLocalItem(DatabaseMediator.ItemType type, Integer integratedId, Integer localId) {
        synchronized (ITEMS_TO_LOCAL_ITEMS_LOCK) {
            itemsToLocalItems.put(new LibraryId(type, integratedId), new LibraryId(type, localId));
        }
    }

    public void addRemoteLink(DatabaseMediator.ItemType type, Integer integratedID, PeerID remotePeer, Integer remoteID) {
        synchronized (ITEMS_TO_REMOTE_ITEMS_LOCK) {
            LibraryId integratedLibraryId = new LibraryId(type, integratedID);
            if (!itemsToRemoteItems.containsKey(integratedLibraryId)) {
                itemsToRemoteItems.put(integratedLibraryId, new ArrayList<>());
            }
            itemsToRemoteItems.get(integratedLibraryId).add(new PeerAndLibraryId(type, remoteID, remotePeer));
        }
    }

    public List<PeerAndLibraryId> getRemotePeerAndID(LibraryId integratedID) {
        synchronized (ITEMS_TO_REMOTE_ITEMS_LOCK) {
            if (itemsToRemoteItems.containsKey(integratedID)) {
                return itemsToRemoteItems.get(integratedID);
            } else {
                return new ArrayList<>();
            }
        }
    }

    @Override
    public String getCurrentVersion() {
        return CURRENT_VERSION;
    }

    @Override
    public Map<String, Serializable> serialize() {
        Map<String, Serializable> attributes = new HashMap<>();
        attributes.put("itemsToSharedItems", serializeLibraryIdMap(itemsToSharedItems));
        attributes.put("itemsToLocalItems", serializeLibraryIdMap(itemsToLocalItems));
        attributes.put("itemsToRemoteItems", serializeLibraryIdMapList(itemsToRemoteItems));
        attributes.put("itemsToDeletedRemoteItems", serializeLibraryIdMap(itemsToDeletedRemoteItems));
        return attributes;
    }

    @Override
    public void deserialize(Map<String, Object> attributes) {
        deserializeLibraryIdMap(itemsToSharedItems, (byte[]) attributes.get("itemsToSharedItems"));
        deserializeLibraryIdMap(itemsToLocalItems, (byte[]) attributes.get("itemsToLocalItems"));
        deserializeLibraryIdMapList(itemsToRemoteItems, (byte[]) attributes.get("itemsToRemoteItems"));
        deserializeLibraryIdMap(itemsToDeletedRemoteItems, (byte[]) attributes.get("itemsToDeletedRemoteItems"));
    }

    @Override
    public void deserializeOldVersion(String version, Map<String, Object> attributes) throws UnrecognizedVersionException {
        throw new UnrecognizedVersionException();
    }


    static byte[] serializeLibraryIdMapList(Map<LibraryId, List<PeerAndLibraryId>> mapList) {
        FragmentedByteArray mapListBytes = new FragmentedByteArray(Serializer.serialize(mapList.size()));
        for (Map.Entry<LibraryId, List<PeerAndLibraryId>> itemToRemoteItem : mapList.entrySet()) {
            mapListBytes.addArrays(
                    itemToRemoteItem.getKey().serialize(),
                    Serializer.serialize(itemToRemoteItem.getValue().size()));
            for (PeerAndLibraryId peerAndLibraryId : itemToRemoteItem.getValue()) {
                mapListBytes.addArray(peerAndLibraryId.serialize());
            }
        }
        return mapListBytes.generateArray();
    }

    static void deserializeLibraryIdMapList(Map<LibraryId, List<PeerAndLibraryId>> mapList, byte[] data) {
        MutableOffset offset = new MutableOffset();
        int entryCount = Serializer.deserializeInt(data, offset);
        for (int i = 0; i < entryCount; i++) {
            LibraryId key = LibraryId.deserialize(data, offset);
            int listLength = Serializer.deserializeInt(data, offset);
            List<PeerAndLibraryId> value = new ArrayList<>();
            for (int j = 0; j < listLength; j++) {
                value.add(PeerAndLibraryId.deserialize(data, offset));
            }
            mapList.put(key, value);
        }
    }
}
