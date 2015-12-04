package jacz.peerengineclient.libraries.library_images;

import jacz.util.io.object_serialization.*;

import java.io.Serializable;
import java.util.HashMap;
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
        attributes.put("itemsToIntegratedItems", serializeLibraryIdMap(itemsToIntegratedItems));
        return attributes;
    }

    @Override
    public void deserialize(Map<String, Object> attributes) {
        deserializeLibraryIdMap(itemsToIntegratedItems, (byte[]) attributes.get("itemsToIntegratedItems"));
    }

    @Override
    public void deserializeOldVersion(String version, Map<String, Object> attributes) throws UnrecognizedVersionException {
        throw new UnrecognizedVersionException();
    }
}
