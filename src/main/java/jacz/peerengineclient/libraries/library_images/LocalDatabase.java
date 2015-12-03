package jacz.peerengineclient.libraries.library_images;

import jacz.util.io.object_serialization.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The local database, with items created by us
 */
public class LocalDatabase extends GenericDatabase implements VersionedObject {

    private static final String VERSION_0_1 = "0.1";

    private static final String CURRENT_VERSION = VERSION_0_1;

    private final HashMap<LibraryId, LibraryId> itemsToIntegratedItems;

    public LocalDatabase(String databasePath) {
        this(databasePath, new HashMap<>());
    }

    public LocalDatabase(String databasePath, HashMap<LibraryId, LibraryId> itemsToIntegratedItems) {
        super(databasePath);
        this.itemsToIntegratedItems = itemsToIntegratedItems;
    }

    public HashMap<LibraryId, LibraryId> getItemsToIntegratedItems() {
        return itemsToIntegratedItems;
    }

    @Override
    public String getCurrentVersion() {
        return CURRENT_VERSION;
    }

    @Override
    public Map<String, Serializable> serialize() {
        Map<String, Serializable> attributes = new HashMap<>();
        FragmentedByteArray itemsToIntegratedItemsBytes = new FragmentedByteArray(Serializer.serialize(itemsToIntegratedItems.size()));
        for (Map.Entry<LibraryId, LibraryId> itemToLocalItem : itemsToIntegratedItems.entrySet()) {
            itemsToIntegratedItemsBytes.addArrays(
                    VersionedObjectSerializer.serialize(itemToLocalItem.getKey(), 4),
                    VersionedObjectSerializer.serialize(itemToLocalItem.getValue(), 4));
        }
        attributes.put("itemsToIntegratedItems", itemsToIntegratedItemsBytes.generateArray());
        return attributes;
    }

    @Override
    public void deserialize(Map<String, Object> attributes) {
        try {
            byte[] itemsToIntegratedItemsBytes = (byte[]) attributes.get("itemsToIntegratedItems");
            MutableOffset offset = new MutableOffset();
            int entryCount = Serializer.deserializeInt(itemsToIntegratedItemsBytes, offset);
            for (int i = 0; i < entryCount; i++) {
                itemsToIntegratedItems.put(LibraryId.deserialize(itemsToIntegratedItemsBytes, offset), LibraryId.deserialize(itemsToIntegratedItemsBytes, offset));
            }
        } catch (VersionedSerializationException e) {
            throw new RuntimeException("Error deserializing local database");
        }
    }

    @Override
    public void deserializeOldVersion(String version, Map<String, Object> attributes) throws UnrecognizedVersionException {
        throw new UnrecognizedVersionException();
    }
}
