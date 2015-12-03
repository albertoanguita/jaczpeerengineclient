package jacz.peerengineclient.libraries.library_images;

import jacz.store.database.DatabaseMediator;
import jacz.util.io.object_serialization.*;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Alberto on 02/12/2015.
 */
public abstract class GenericDatabase {

    public static class LibraryId implements VersionedObject {

        private static final String VERSION_0_1 = "0.1";

        private static final String CURRENT_VERSION = VERSION_0_1;

        public DatabaseMediator.ITEM_TYPE type;

        public int id;

        public LibraryId() {
        }

        public LibraryId(DatabaseMediator.ITEM_TYPE type, int id) {
            this.type = type;
            this.id = id;
        }

        public static LibraryId deserialize(byte[] data, MutableOffset offset) throws VersionedSerializationException {
            LibraryId libraryId = new LibraryId();
            VersionedObjectSerializer.deserialize(libraryId, data, offset);
            return libraryId;
        }

        @Override
        public String getCurrentVersion() {
            return CURRENT_VERSION;
        }

        @Override
        public Map<String, Serializable> serialize() {
            Map<String, Serializable> attributes = new HashMap<>();
            attributes.put("type", type.name());
            attributes.put("id", id);
            return attributes;
        }

        @Override
        public void deserialize(Map<String, Object> attributes) {
            type = DatabaseMediator.ITEM_TYPE.valueOf((String) attributes.get("type"));
            id = (Integer) attributes.get("id");
        }

        @Override
        public void deserializeOldVersion(String version, Map<String, Object> attributes) throws UnrecognizedVersionException {
            throw new UnrecognizedVersionException();
        }
    }

    /**
     * Path to the integrated data library
     */
    private final String databasePath;

    public GenericDatabase(String databasePath) {
        this.databasePath = databasePath;
    }

    public String getDatabase() {
        return databasePath;
    }
}
