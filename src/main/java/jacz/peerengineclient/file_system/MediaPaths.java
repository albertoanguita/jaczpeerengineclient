package jacz.peerengineclient.file_system;

import org.aanguita.jacuzzi.io.serialization.localstorage.Updater;
import org.aanguita.jacuzzi.io.serialization.localstorage.VersionedLocalStorage;

import java.io.IOException;

/**
 * Storage for paths to user files
 */
public class MediaPaths implements Updater {

    private static final String VERSION_0_1_0 = "0.1.0";

    private static final String CURRENT_VERSION = VERSION_0_1_0;

    /**
     * Base path for downloaded files
     */
    private static final String BASE_MEDIA_PATH = "baseMediaPath";

    /**
     * Path for temporary download files
     */
    private static final String TEMP_DOWNLOADS_PATH = "tempDownloadsPath";

    private final VersionedLocalStorage localStorage;

    public MediaPaths(String localStoragePath, String baseMediaPath, String tempDownloadsPath) throws IOException {
        localStorage = VersionedLocalStorage.createNew(localStoragePath, CURRENT_VERSION);
        setBaseMediaPath(baseMediaPath);
        setTempDownloadsPath(tempDownloadsPath);
    }

    public MediaPaths(String localStoragePath) throws IOException {
        localStorage = new VersionedLocalStorage(localStoragePath, this, CURRENT_VERSION);
    }

    public synchronized String getBaseMediaPath() {
        return localStorage.getString(BASE_MEDIA_PATH);
    }

    public synchronized boolean setBaseMediaPath(String baseMediaPath) {
        return localStorage.setString(BASE_MEDIA_PATH, baseMediaPath);
    }

    public synchronized String getTempDownloadsPath() {
        return localStorage.getString(TEMP_DOWNLOADS_PATH);
    }

    public synchronized boolean setTempDownloadsPath(String tempDownloadsPath) {
        return localStorage.setString(TEMP_DOWNLOADS_PATH, tempDownloadsPath);
    }

    @Override
    public String update(VersionedLocalStorage versionedLocalStorage, String storedVersion) {
        // no versions yet, cannot be invoked
        return null;
    }
}
