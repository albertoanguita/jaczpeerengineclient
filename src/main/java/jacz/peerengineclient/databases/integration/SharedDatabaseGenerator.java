package jacz.peerengineclient.databases.integration;

import jacz.database.*;
import jacz.peerengineclient.databases.Databases;
import jacz.peerengineclient.databases.ItemRelations;
import jacz.peerengineclient.util.FileAPI;
import org.aanguita.jacuzzi.concurrency.concurrency_controller.ConcurrencyController;
import org.aanguita.jacuzzi.concurrency.timer.Timer;
import org.aanguita.jacuzzi.concurrency.timer.TimerAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles the shared database, and its population with items from the integrated database
 * <p>
 * The shared database is updated periodically, without external intervention. This is better because there are
 * many places that should produce this update (integrated db changes, new downloads, download cancelled...). It would
 * be hard to track all this places, so we just do it automatically and periodically
 */
public class SharedDatabaseGenerator implements TimerAction {

    private final static Logger logger = LoggerFactory.getLogger(SharedDatabaseGenerator.class);

    /**
     * The shared database is updated every minute
     */
    private static final long UPDATE_DELAY = 60000;

    private final String integratedPath;

    private final String sharedPath;

    private final ItemRelations.ItemRelationsMap integratedToShared;

    /**
     * The hashes of files we are currently offering (both downloaded and in temp downloads)
     * <p>
     * Before re-calculating the shared db, we must load this variable with the available hashes
     */
    private Set<String> availableHashes;

    /**
     * Through the FileAPI, we can gather the files that we are currently downloading
     */
    private FileAPI fileAPI;

    private final ExecutorService sequentialTaskExecutor;

    private final ConcurrencyController concurrencyController;

    /**
     * This timer tells us when to trigger the shared db update process. Changes in the integrated db will reset this
     * timer, so the shared db will only be generated when we have a stable integrated db for a specific
     * period of time
     */
    private final Timer timer;

    public SharedDatabaseGenerator(Databases databases, ConcurrencyController concurrencyController) {
        this.integratedPath = databases.getIntegratedDB();
        this.sharedPath = databases.getSharedDB();
        this.integratedToShared = databases.getItemRelations().getIntegratedToShared();
        sequentialTaskExecutor = Executors.newSingleThreadExecutor();
        this.concurrencyController = concurrencyController;
        timer = new Timer(UPDATE_DELAY, this, false, this.getClass().getName());
    }

    public void start(FileAPI fileAPI) {
        this.fileAPI = fileAPI;
        timer.reset();
        // perform an initial update
        updateSharedDatabase();
    }

    @Override
    public Long wakeUp(Timer timer) {
        updateSharedDatabase();
        return null;
    }

    public void updateSharedDatabase() {

        // we use the sequential task executor so there cannot be concurrent processes
        sequentialTaskExecutor.submit(() -> {
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
        availableHashes = fileAPI.getAvailableHashes();
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
            if (checkFile(file)) {
                // this element must be included in the shared db
                anyTrue = true;
            }
        }
        return anyTrue;
    }

    private boolean checkFile(File file) {

        if (file instanceof VideoFile) {
            // before anything else, check if we must add or remove subtitle files (if add, we need to do it before
            // adding the actual video file, so this is the best place to do it)
            VideoFile videoFile = (VideoFile) file;
            checkFiles(videoFile.getSubtitleFiles());
        }
        String hash = file.getHash();
        boolean isAdded;
        if (availableHashes.contains(hash)) {
            addItem(file);
            isAdded = true;
        } else {
            removeItem(file);
            isAdded = false;
        }
        return isAdded;
    }

    private void addCreationItem(CreationItem item) {
        // add creators and actors, and then the item itself
//        for (Person creator : item.getCreators()) {
//            addItem(creator);
//        }
//        for (Person actor : item.getActors()) {
//            addItem(actor);
//        }
        addItem(item);
    }

    private void removeCreationItem(CreationItem item) {
        // add creators and actors, and then the item itself
//        for (Person creator : item.getCreators()) {
//            removeItem(creator);
//        }
//        for (Person actor : item.getActors()) {
//            removeItem(actor);
//        }
        removeItem(item);
    }

    private void addProducedCreationItem(ProducedCreationItem item) {
        // add production companies, and then the rest of the creation item
//        for (Company company : item.getProductionCompanies()) {
//            addItem(company);
//        }
        addCreationItem(item);
    }

    private void removeProducedCreationItem(ProducedCreationItem item) {
        // add production companies, and then the rest of the creation item
//        for (Company company : item.getProductionCompanies()) {
//            removeItem(company);
//        }
        removeCreationItem(item);
    }

    private void addItem(DatabaseItem integratedItem) {
        if (concurrencyController.beginActivity(IntegrationConcurrencyController.Activity.INTEGRATED_TO_SHARED.name())) {
            try {
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
            } finally {
                concurrencyController.endActivity(IntegrationConcurrencyController.Activity.INTEGRATED_TO_SHARED.name());
            }
        } else {
            throw new IllegalStateException();
        }
    }

    private void removeItem(DatabaseItem integratedItem) {
        if (concurrencyController.beginActivity(IntegrationConcurrencyController.Activity.INTEGRATED_TO_SHARED.name())) {
            try {
                if (integratedToShared.contains(integratedItem.getItemType(), integratedItem.getId())) {
                    int sharedId = integratedToShared.get(integratedItem.getItemType(), integratedItem.getId());
                    DatabaseItem sharedItem = DatabaseMediator.getItem(sharedPath, integratedItem.getItemType(), sharedId);
                    sharedItem.delete();
                    integratedToShared.remove(integratedItem.getItemType(), integratedItem.getId());
                }
            } finally {
                concurrencyController.endActivity(IntegrationConcurrencyController.Activity.INTEGRATED_TO_SHARED.name());
            }
        } else {
            throw new IllegalStateException();
        }
    }

    public synchronized void stop() {
        timer.stop();
        sequentialTaskExecutor.shutdown();
    }
}
