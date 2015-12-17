package jacz.peerengineclient.libraries.library_images;

import jacz.util.io.object_serialization.UnrecognizedVersionException;
import jacz.util.io.object_serialization.VersionedObject;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Alberto on 13/12/2015.
 */
public class SerializedGenericLibrary extends GenericDatabase implements VersionedObject {

    private static final String VERSION_0_1 = "0.1";

    private static final String CURRENT_VERSION = VERSION_0_1;

    public SerializedGenericLibrary(String databasePath) {
        super(databasePath);
    }

    @Override
    public String getCurrentVersion() {
        return CURRENT_VERSION;
    }

    @Override
    public Map<String, Serializable> serialize() {
        Map<String, Serializable> attributes = new HashMap<>();
        serializeLibraryPath(attributes);
        return attributes;
    }

    @Override
    public void deserialize(Map<String, Object> attributes) {
        deserializeLibraryPath(attributes);
    }

    @Override
    public void deserializeOldVersion(String version, Map<String, Object> attributes) throws UnrecognizedVersionException {
        throw new UnrecognizedVersionException();
    }

}
