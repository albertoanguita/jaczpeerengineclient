package jacz.peerengineclient.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Bridge for the file hash database events
 */
class FileHashDatabaseEventsBridge {

    final static Logger logger = LoggerFactory.getLogger(FileHashDatabaseEvents.class);

    private final FileHashDatabaseEvents fileHashDatabaseEvents;

    private final ExecutorService sequentialTaskExecutor;

    public FileHashDatabaseEventsBridge(FileHashDatabaseEvents fileHashDatabaseEvents) {
        this.fileHashDatabaseEvents = fileHashDatabaseEvents;
        sequentialTaskExecutor = Executors.newSingleThreadExecutor();
    }

    public void fileAdded(String hash, String path) {
        logger.info("FILE ADDED. Hash: " + hash + ". Path: " + path);
        sequentialTaskExecutor.submit(() -> fileHashDatabaseEvents.fileAdded(hash, path));
    }

    public void fileRemoved(String hash, String path) {
        logger.info("FILE REMOVED. Hash: " + hash + ". Path: " + path);
        sequentialTaskExecutor.submit(() -> fileHashDatabaseEvents.fileRemoved(hash, path));
    }

    public void stop() {
        sequentialTaskExecutor.shutdown();
    }
}
