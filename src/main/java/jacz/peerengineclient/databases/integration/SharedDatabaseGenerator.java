package jacz.peerengineclient.databases.integration;

import jacz.database.*;
import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineclient.data.FileHashDatabaseWithTimestamp;
import jacz.peerengineclient.databases.Databases;
import jacz.peerengineclient.databases.ItemRelations;
import jacz.peerengineservice.client.PeerClient;
import jacz.peerengineservice.util.datatransfer.master.DownloadManager;
import jacz.util.concurrency.concurrency_controller.ConcurrencyController;
import jacz.util.concurrency.task_executor.SequentialTaskExecutor;
import jacz.util.concurrency.timer.SimpleTimerAction;
import jacz.util.concurrency.timer.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Handles the shared database, and its population with items from the integrated database
 * <p>
 * The shared database is updated periodically, without external intervention. This is better because there are
 * many places that should produce this update (integrated db changes, new downloads, download cancelled...). It would
 * be hard to track all this places, so we just do it automatically and periodically
 */
public class SharedDatabaseGenerator implements SimpleTimerAction {

    private final static Logger logger = LoggerFactory.getLogger(SharedDatabaseGenerator.class);

    /**
     * The shared database is updated every minute
     */
    private static final long UPDATE_DELAY = 10000;

    private final String integratedPath;

    private final String sharedPath;

    private final ItemRelations.ItemRelationsMap integratedToShared;

    /**
     * The hashes of files we are currently offering (both downloaded and in temp downloads)
     * <p>
     * Before re-calculating the shared db, we must load this variable with the available hashes
     */
    private final Set<String> availableHashes;

    /**
     * This object contains the files that we currently offer
     */
    private final FileHashDatabaseWithTimestamp fileHashDatabaseWithTimestamp;

    /**
     * Through the peer engine client, we can gather the files that we are currently downloading
     */
    private final PeerEngineClient peerEngineClient;

    private final SequentialTaskExecutor sequentialTaskExecutor;

    private final ConcurrencyController concurrencyController;

    /**
     * This timer tells us when to trigger the shared db update process. Changes in the integrated db will reset this
     * timer, so the shared db will only be generated when we have a stable integrated db for a specific
     * period of time
     */
    private final Timer timer;

    public SharedDatabaseGenerator(Databases databases, FileHashDatabaseWithTimestamp fileHashDatabaseWithTimestamp, PeerEngineClient peerEngineClient, ConcurrencyController concurrencyController) {
        this.integratedPath = databases.getIntegratedDB();
        this.sharedPath = databases.getSharedDB();
        this.integratedToShared = databases.getItemRelations().getIntegratedToShared();
        this.availableHashes = new HashSet<>();
        this.fileHashDatabaseWithTimestamp = fileHashDatabaseWithTimestamp;
        this.peerEngineClient = peerEngineClient;
        sequentialTaskExecutor = new SequentialTaskExecutor();
        this.concurrencyController = concurrencyController;
        timer = new Timer(UPDATE_DELAY, this, true, this.getClass().getName());
        // perform an initial update
        updateSharedDatabase();
    }

    public synchronized void requestUpdate() {
        timer.reset();
    }

    @Override
    public Long wakeUp(Timer timer) {
        updateSharedDatabase();
        return null;
    }

    public void updateSharedDatabase() {

        // we use the sequential task executor so there cannot be concurrent processes
        sequentialTaskExecutor.executeTask(() -> {
            updateAvailableHashes();
            // go through all movies and series
            logger.info("Starting shared database generation...");
            logger.info("listing movies in integrated...");
            List<Movie> movies = Movie.getMovies(integratedPath);
            logger.info("listed movies in integrated...");
            for (Movie movie : movies) {
                if (checkFiles(movie.getVideoFiles())) {
                    // this movie must be included in the shared db
                    logger.info("adding movie " + movie.getId() + " to shared...");
                    addProducedCreationItem(movie);
                } else {
                    removeProducedCreationItem(movie);
                }
            }
            logger.info("listing tvSeries in integrated...");
            List<TVSeries> tvSeries = TVSeries.getTVSeries(integratedPath);
            logger.info("listed tvSeries in integrated...");
            for (TVSeries aTvSeries : tvSeries) {
                if (checkChapters(aTvSeries.getChapters())) {
                    // this tv series must be included in the shared db
                    logger.info("adding tv series " + aTvSeries.getId() + " to shared...");
                    addProducedCreationItem(aTvSeries);
                } else {
                    removeProducedCreationItem(aTvSeries);
                }
            }
            logger.info("Shared database generation complete!");
        });
    }

    private void updateAvailableHashes() {
        availableHashes.clear();
        availableHashes.addAll(fileHashDatabaseWithTimestamp.getActiveHashesSetCopy());
        PeerClient peerClient = peerEngineClient.getPeerClient();
        if (peerClient != null) {
            for (DownloadManager downloadManager : peerEngineClient.getPeerClient().getAllDownloads()) {
                availableHashes.add(downloadManager.getResourceID());
            }
        }
    }

    private boolean checkChapters(List<Chapter> chapters) {
        boolean anyTrue = false;
        for (Chapter chapter : chapters) {
            if (checkFiles(chapter.getVideoFiles())) {
                // this chapter must be included in the shared db
                logger.info("adding chapter " + chapter.getId() + " to shared...");
                addCreationItem(chapter);
                anyTrue = true;
            } else {
                removeCreationItem(chapter);
            }
        }
        return anyTrue;
    }

    private boolean checkFiles(List<? extends File> files) {
        boolean anyTrue = false;
        for (File file : files) {
            if (checkVideoFile(file)) {
                // this element must be included in the shared db
                anyTrue = true;
            }
        }
        return anyTrue;
    }

    private boolean checkVideoFile(File file) {

        String hash = file.getHash();
        boolean isAdded;
        if (availableHashes.contains(hash)) {
            // subtitle files must be added before video files, so the subtitle files mappings are set prior adding
            // video files
            if (file instanceof VideoFile) {
                // its subtitles files are subject to be added as well
                VideoFile videoFile = (VideoFile) file;
                checkFiles(videoFile.getSubtitleFiles());
            }
            addItem(file);
            isAdded = true;
        } else {
            if (file instanceof VideoFile) {
                // its subtitles files are all removed before the video file is removed
                VideoFile videoFile = (VideoFile) file;
                for (SubtitleFile subtitleFile : videoFile.getSubtitleFiles()) {
                    removeItem(subtitleFile);
                }
            }
            removeItem(file);
            isAdded = false;
        }
        return isAdded;
    }

    private void addCreationItem(CreationItem item) {
        // add creators and actors, and then the item itself
        for (Person creator : item.getCreators()) {
            addItem(creator);
        }
        for (Person actor : item.getActors()) {
            addItem(actor);
        }
        addItem(item);
    }

    private void removeCreationItem(CreationItem item) {
        // add creators and actors, and then the item itself
        for (Person creator : item.getCreators()) {
            removeItem(creator);
        }
        for (Person actor : item.getActors()) {
            removeItem(actor);
        }
        removeItem(item);
    }

    private void addProducedCreationItem(ProducedCreationItem item) {
        // add production companies, and then the rest of the creation item
        for (Company company : item.getProductionCompanies()) {
            addItem(company);
        }
        addCreationItem(item);
    }

    private void removeProducedCreationItem(ProducedCreationItem item) {
        // add production companies, and then the rest of the creation item
        for (Company company : item.getProductionCompanies()) {
            removeItem(company);
        }
        removeCreationItem(item);
    }

    private void addItem(DatabaseItem integratedItem) {
        concurrencyController.beginActivity(IntegrationConcurrencyController.Activity.INTEGRATED_TO_SHARED.name());

        DatabaseItem sharedItem;
        // retrieve the shared item: either it already exists or it must be created
        if (integratedToShared.contains(integratedItem.getItemType(), integratedItem.getId())) {
            // exists
            int sharedId = integratedToShared.get(integratedItem.getItemType(), integratedItem.getId());
            sharedItem = DatabaseMediator.getItem(sharedPath, integratedItem.getItemType(), sharedId);
        } else {
            // must be created
            sharedItem = DatabaseMediator.createNewItem(sharedPath, integratedItem.getItemType());
            integratedToShared.put(integratedItem.getItemType(), integratedItem.getId(), sharedItem.getId());
        }
        // once we have a shared item, copy the data from the integrated item, if needed
        if (!integratedItem.equals(sharedItem)) {
            // only merge the item if the contents have changed
            DatabaseMediator.ReferencedElements referencedElements = integratedItem.getReferencedElements();
            referencedElements.mapIds(integratedToShared.getTypeMappings());
            sharedItem.merge(integratedItem, referencedElements);
        }

        concurrencyController.endActivity(IntegrationConcurrencyController.Activity.INTEGRATED_TO_SHARED.name());
    }

    private void removeItem(DatabaseItem integratedItem) {
        concurrencyController.beginActivity(IntegrationConcurrencyController.Activity.INTEGRATED_TO_SHARED.name());

        if (integratedToShared.contains(integratedItem.getItemType(), integratedItem.getId())) {
            int sharedId = integratedToShared.get(integratedItem.getItemType(), integratedItem.getId());
            DatabaseItem sharedItem = DatabaseMediator.getItem(sharedPath, integratedItem.getItemType(), sharedId);
            sharedItem.delete();
            integratedToShared.remove(integratedItem.getItemType(), integratedItem.getId());
        }

        concurrencyController.endActivity(IntegrationConcurrencyController.Activity.INTEGRATED_TO_SHARED.name());
    }

    public synchronized void stop() {
        timer.stop();
        sequentialTaskExecutor.stopAndWaitForFinalization();
    }
}
