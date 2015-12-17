package jacz.peerengineclient.libraries.library_images;

import jacz.store.database.DatabaseMediator;
import jacz.util.io.object_serialization.FragmentedByteArray;
import jacz.util.io.object_serialization.MutableOffset;
import jacz.util.io.object_serialization.Serializer;

import java.io.Serializable;
import java.util.Map;

/**
 * Created by Alberto on 02/12/2015.
 */
public abstract class GenericDatabase {

    public static class LibraryId {

        public final DatabaseMediator.ItemType type;

        public final int id;

        public LibraryId(DatabaseMediator.ItemType type, int id) {
            this.type = type;
            this.id = id;
        }

        public byte[] serialize() {
            return Serializer.addArrays(Serializer.serialize(type), Serializer.serialize(id));
        }

        public static LibraryId deserialize(byte[] data, MutableOffset offset) {
            return new LibraryId(Serializer.deserializeEnum(DatabaseMediator.ItemType.class, data, offset), Serializer.deserializeInt(data, offset));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            LibraryId libraryId = (LibraryId) o;

            if (id != libraryId.id) return false;
            return type == libraryId.type;

        }

        @Override
        public int hashCode() {
            int result = type.hashCode();
            result = 31 * result + id;
            return result;
        }
    }

    /**
     * Path to the integrated data library
     */
    private final String libraryPath;

    public GenericDatabase(String libraryPath) {
        this.libraryPath = libraryPath;
    }

    public String getDatabase() {
        return libraryPath;
    }

    static byte[] serializeLibraryIdMap(Map<LibraryId, LibraryId> map) {
        FragmentedByteArray mapBytes = new FragmentedByteArray(Serializer.serialize(map.size()));
        for (Map.Entry<LibraryId, LibraryId> itemToLocalItem : map.entrySet()) {
            mapBytes.addArrays(
                    itemToLocalItem.getKey().serialize(),
                    itemToLocalItem.getValue().serialize());
        }
        return mapBytes.generateArray();
    }

    static void deserializeLibraryIdMap(Map<LibraryId, LibraryId> map, byte[] data) {
        MutableOffset offset = new MutableOffset();
        int entryCount = Serializer.deserializeInt(data, offset);
        for (int i = 0; i < entryCount; i++) {
            map.put(LibraryId.deserialize(data, offset), LibraryId.deserialize(data, offset));
        }
    }
}
