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

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Handles the shared database, and its population with items from the integrated database
 * <p>
 * The shared database is updated periodically, without external intervention. This is better because there are
 * many places that should produce this update (integrated db changes, new downloads, download cancelled...). It would
 * be hard to track all this places, so we just do it automatically and periodically
 */
public class SharedDatabaseGenerator implements TimerAction {

    /**
     * This class stores the relation of existing shared items, each time the shared database
     * is to be updated. It allows finding elements that must be removed
     */
    private static class ExistingSharedItems {

        private final Map<Integer, Movie> movies;

        private final Map<Integer, TVSeries> tvSeries;

        private final Map<Integer, Chapter> chapters;

        private final Map<Integer, VideoFile> videoFiles;

        private final Map<Integer, SubtitleFile> subtitleFiles;

        public ExistingSharedItems(List<Movie> movies, List<TVSeries> tvSeries, List<Chapter> chapters, List<VideoFile> videoFiles, List<SubtitleFile> subtitleFiles) {
            this.movies = buildItemMap(movies);
            this.tvSeries = buildItemMap(tvSeries);
            this.chapters = buildItemMap(chapters);
            this.videoFiles = buildItemMap(videoFiles);
            this.subtitleFiles = buildItemMap(subtitleFiles);
        }

        private static <T extends DatabaseItem> Map<Integer, T> buildItemMap(List<T> items) {
            Map<Integer, T> itemMap = new HashMap<>();
            for (T item : items) {
                itemMap.put(item.getId(), item);
            }
            return itemMap;
        }

        public void checkMovie(Movie movie) {
            movies.remove(movie.getId());
        }

        public void checkTVSeries(TVSeries tvSeries) {
            this.tvSeries.remove(tvSeries.getId());
        }

        public void checkChapter(Chapter chapter) {
            chapters.remove(chapter.getId());
        }

        public void checkVideoFile(VideoFile videoFile) {
            videoFiles.remove(videoFile.getId());
        }

        public void checkSubtitleFile(SubtitleFile subtitleFile) {
            subtitleFiles.remove(subtitleFile.getId());
        }

        public Collection<DatabaseItem> remainingItems() {
            return Stream.concat(
                    Stream.concat(
                            Stream.concat(
                                    Stream.concat(
                                            movies.values().stream(), tvSeries.values().stream()
                                    ), chapters.values().stream()
                            ), videoFiles.values().stream()
                    ), subtitleFiles.values().stream()
            ).collect(Collectors.toList());
        }
    }

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

    private ExistingSharedItems existingSharedItems;

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
            resetExistingSharedItems();
            // go through all movies and series
            logger.info("Starting shared database generation...");
            checkMovies();
            checkTVSeries();
            removeRemainingSharedItems();
            logger.info("Shared database generation complete!");
        });
    }

    private void updateAvailableHashes() {
        availableHashes = fileAPI.getAvailableHashes();
    }

    private void resetExistingSharedItems() {
        existingSharedItems = new ExistingSharedItems(
                Movie.getMovies(sharedPath),
                TVSeries.getTVSeries(sharedPath),
                Chapter.getChapters(sharedPath),
                VideoFile.getVideoFiles(sharedPath),
                SubtitleFile.getSubtitleFiles(sharedPath));
    }

    private void checkMovies() {
        List<Movie> movies = Movie.getMovies(integratedPath);
        for (Movie movie : movies) {
            existingSharedItems.checkMovie(movie);
            if (checkFiles(movie.getVideoFiles())) {
                // this movie must be included in the shared db
                logger.info("adding movie " + movie.getId() + " to shared...");
                addProducedCreationItem(movie);
            } else {
                removeProducedCreationItem(movie);
            }
        }
    }

    private void checkTVSeries() {
        List<TVSeries> tvSeries = TVSeries.getTVSeries(integratedPath);
        for (TVSeries aTvSeries : tvSeries) {
            existingSharedItems.checkTVSeries(aTvSeries);
            if (checkChapters(aTvSeries)) {
                // this tv series must be included in the shared db
                logger.info("adding tv series " + aTvSeries.getId() + " to shared...");
                addProducedCreationItem(aTvSeries);
            } else {
                removeProducedCreationItem(aTvSeries);
            }
        }
    }

    private boolean checkChapters(TVSeries aTvSeries) {
        boolean anyTrue = false;
        for (Chapter chapter : aTvSeries.getChapters()) {
            existingSharedItems.checkChapter(chapter);
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
            existingSharedItems.checkVideoFile(videoFile);
            checkFiles(videoFile.getSubtitleFiles());
        } else {
            existingSharedItems.checkSubtitleFile((SubtitleFile) file);
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
                    logger.info("New shared item with id " + sharedItem.getId() + " created");
                    integratedToShared.put(integratedItem.getItemType(), integratedItem.getId(), sharedItem.getId());
                }
                // once we have a shared item, copy the data from the integrated item, if needed
                if (!integratedItem.equals(sharedItem)) {
                    // only merge the item if the contents have changed
                    logger.info("Shared item with id " + sharedItem.getId() + " modified");
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

    private void removeRemainingSharedItems() {
        if (concurrencyController.beginActivity(IntegrationConcurrencyController.Activity.INTEGRATED_TO_SHARED.name())) {
            try {
                existingSharedItems.remainingItems().stream().forEach(DatabaseItem::delete);
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
