package jacz.peerengineclient.util;

import jacz.peerengineclient.file_system.PathConstants;
import org.aanguita.jacuzzi.io.serialization.localstorage.Updater;
import org.aanguita.jacuzzi.io.serialization.localstorage.VersionedLocalStorage;
import org.aanguita.jacuzzi.string.AlphanumericString;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Represents a persistent id factory that allows generating unique ids across different sessions
 */
public class PersistentIdFactory implements Updater {

    private static final String VERSION_0_1 = "version_0_1";

    private static final String CURRENT_VERSION = VERSION_0_1;

    private static final String ID_KEY = "next_id";

    private static final AlphanumericString.CharTypeSequence charTypeSequence =
            new AlphanumericString.CharTypeSequence(
                    AlphanumericString.CharType.NUMERIC,
                    AlphanumericString.CharType.LOWERCASE,
                    AlphanumericString.CharType.UPPERCASE);

    private final VersionedLocalStorage vls;

    public PersistentIdFactory(String basePath) throws IOException {
        String idFactoryPath = PathConstants.persistentIDFactoryPath(basePath);
        vls = new VersionedLocalStorage(idFactoryPath, this, CURRENT_VERSION);
//        if (!Files.isRegularFile(Paths.get(idFactoryPath))) {
//            // create id factory file
//            vls = VersionedLocalStorage.createNew(idFactoryPath, CURRENT_VERSION);
//            vls.setString(ID_KEY, AlphanumericString.nextAlphanumericString("", charTypeSequence));
//        } else {
//            vls = new VersionedLocalStorage(idFactoryPath, this, CURRENT_VERSION);
//        }
    }

    public static void createNew(String basePath) throws IOException {
        String idFactoryPath = PathConstants.persistentIDFactoryPath(basePath);
        VersionedLocalStorage vls = VersionedLocalStorage.createNew(idFactoryPath, CURRENT_VERSION);
        vls.setString(ID_KEY, AlphanumericString.nextAlphanumericString("", charTypeSequence));
    }

    public String generateId() {
        String id = vls.getString(ID_KEY);
        String nextId = AlphanumericString.nextAlphanumericString(id, charTypeSequence);
        vls.setString(ID_KEY, nextId);
        return id;
    }

    @Override
    public String update(VersionedLocalStorage versionedLocalStorage, String storedVersion) {
        // no versions yet, cannot be invoked
        return null;
    }
}
