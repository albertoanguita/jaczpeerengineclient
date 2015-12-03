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

        public PeerAndLibraryId() {
        }

        public PeerAndLibraryId(DatabaseMediator.ITEM_TYPE type, int id, PeerID peerID) {
            super(type, id);
            this.peerID = peerID;
        }

        public static PeerAndLibraryId deserialize(byte[] data, MutableOffset offset) throws VersionedSerializationException {
            PeerAndLibraryId peerAndLibraryId = new PeerAndLibraryId();
            VersionedObjectSerializer.deserialize(peerAndLibraryId, data, offset);
            return peerAndLibraryId;
        }


        @Override
        public Map<String, Serializable> serialize() {
            Map<String, Serializable> attributes = super.serialize();
            attributes.put("peerID", peerID.toString());
            return attributes;
        }

        @Override
        public void deserialize(Map<String, Object> attributes) {
            super.deserialize(attributes);
            peerID = new PeerID((String) attributes.get("peerID"));
        }

        @Override
        public String toString() {
            return "PeerAndId{" +
                    "peerID=" + peerID +
                    ", id='" + id + '\'' +
                    '}';
        }
    }

    private static final String VERSION_0_1 = "0.1";

    private static final String CURRENT_VERSION = VERSION_0_1;

    private static final Object ITEMS_TO_LOCAL_ITEMS_LOCK = new Object();

    private static final Object ITEMS_TO_REMOTE_ITEMS_LOCK = new Object();

    /**
     * Links integrated ids to local ids (for faster item lookup)
     */
    private final HashMap<LibraryId, LibraryId> itemsToLocalItems;

    /**
     * Links integrated ids to remote ids (for faster item lookup)
     */
    private final HashMap<LibraryId, List<PeerAndLibraryId>> itemsToRemoteItems;

    /**
     * Sets up a fresh store. The store itself must be created before
     *
     * @param databasePath path to the library
     */
    public IntegratedDatabase(String databasePath) {
        this(databasePath, new HashMap<>(), new HashMap<>());
    }

    /**
     * Recovers an existing store
     *
     * @param databasePath          path to the library
     * @param itemsToLocalItems     pointers of integrated ids to local ids
     * @param itemsToRemoteItems    pointers of integrated ids to remote ids
     */
    public IntegratedDatabase(String databasePath, HashMap<LibraryId, LibraryId> itemsToLocalItems, HashMap<LibraryId, List<PeerAndLibraryId>> itemsToRemoteItems) {
        super(databasePath);
        this.itemsToLocalItems = itemsToLocalItems;
        this.itemsToRemoteItems = itemsToRemoteItems;
    }

    public HashMap<LibraryId, LibraryId> getItemsToLocalItems() {
        // only for saving object state!!!
        return itemsToLocalItems;
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

    public void putItemToLocalItem(LibraryId integratedId, LibraryId localId) {
        synchronized (ITEMS_TO_LOCAL_ITEMS_LOCK) {
            itemsToLocalItems.put(integratedId, localId);
        }
    }

    public HashMap<LibraryId, List<PeerAndLibraryId>> getItemsToRemoteItems() {
        return itemsToRemoteItems;
    }

    public void addRemoteLink(LibraryId integratedID, PeerID remotePeer, LibraryId remoteID) {
        synchronized (ITEMS_TO_REMOTE_ITEMS_LOCK) {
            if (!itemsToRemoteItems.containsKey(integratedID)) {
                itemsToRemoteItems.put(integratedID, new ArrayList<>());
            }
            itemsToRemoteItems.get(integratedID).add(new PeerAndLibraryId(remoteID.type, remoteID.id, remotePeer));
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
        FragmentedByteArray itemsToLocalItemsBytes = new FragmentedByteArray(Serializer.serialize(itemsToLocalItems.size()));
        for (Map.Entry<LibraryId, LibraryId> itemToLocalItem : itemsToLocalItems.entrySet()) {
            itemsToLocalItemsBytes.addArrays(
                    VersionedObjectSerializer.serialize(itemToLocalItem.getKey(), 4),
                    VersionedObjectSerializer.serialize(itemToLocalItem.getValue(), 4));
        }
        attributes.put("itemsToLocalItems", itemsToLocalItemsBytes.generateArray());
        FragmentedByteArray itemsToRemoteItemsBytes = new FragmentedByteArray(Serializer.serialize(itemsToRemoteItems.size()));
        for (Map.Entry<LibraryId, List<PeerAndLibraryId>> itemToRemoteItem : itemsToRemoteItems.entrySet()) {
            itemsToLocalItemsBytes.addArrays(
                    VersionedObjectSerializer.serialize(itemToRemoteItem.getKey(), 4),
                    Serializer.serialize(itemToRemoteItem.getValue().size()),
                    VersionedObjectSerializer.serialize(itemToRemoteItem.getValue(), 4));
        }
        attributes.put("itemsToRemoteItems", itemsToRemoteItemsBytes.generateArray());
        return attributes;
    }

    @Override
    public void deserialize(Map<String, Object> attributes) {
        try {
            byte[] itemsToLocalItemsBytes = (byte[]) attributes.get("itemsToLocalItems");
            MutableOffset offset = new MutableOffset();
            int entryCount = Serializer.deserializeInt(itemsToLocalItemsBytes, offset);
            for (int i = 0; i < entryCount; i++) {
                itemsToLocalItems.put(LibraryId.deserialize(itemsToLocalItemsBytes, offset), LibraryId.deserialize(itemsToLocalItemsBytes, offset));
            }
            byte[] itemsToRemoteItemsBytes = (byte[]) attributes.get("itemsToRemoteItems");
            offset = new MutableOffset();
            entryCount = Serializer.deserializeInt(itemsToRemoteItemsBytes, offset);
            for (int i = 0; i < entryCount; i++) {
                LibraryId key = LibraryId.deserialize(itemsToRemoteItemsBytes, offset);
                int listLength = Serializer.deserializeInt(itemsToRemoteItemsBytes, offset);
                List<PeerAndLibraryId> value = new ArrayList<>();
                for (int j = 0; j < listLength; j++) {
                    value.add(PeerAndLibraryId.deserialize(itemsToRemoteItemsBytes, offset));
                }
                itemsToRemoteItems.put(key, value);
            }
        } catch (VersionedSerializationException e) {
            throw new RuntimeException("Error deserializing integrated database");
        }
    }

    @Override
    public void deserializeOldVersion(String version, Map<String, Object> attributes) throws UnrecognizedVersionException {
        throw new UnrecognizedVersionException();
    }
}
