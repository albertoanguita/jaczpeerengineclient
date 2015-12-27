package jacz.peerengineclient.databases.synch;

import jacz.database.*;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.data_synchronization.DataAccessException;
import jacz.peerengineservice.util.data_synchronization.DataAccessor;
import jacz.peerengineservice.util.data_synchronization.SynchError;
import jacz.util.notification.ProgressNotificationWithError;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Accessor implementation for databases
 */
public class DatabaseAccessor implements DataAccessor {

    public static final String NAME = "LIBRARY_ACCESSOR";

    private static final int ELEMENTS_PER_MESSAGE = 5;

    private static final int CRC_BYTES = 4;

    private final DatabaseSynchManager databaseSynchManager;

    private final String dbPath;

    private final DatabaseSynchProgress databaseSynchProgress;

    public DatabaseAccessor(DatabaseSynchManager databaseSynchManager, String dbPath, DatabaseSynchProgress databaseSynchProgress) {
        this.databaseSynchManager = databaseSynchManager;
        this.dbPath = dbPath;
        this.databaseSynchProgress = databaseSynchProgress;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void beginSynchProcess(Mode mode) {
        // ignore
    }

    @Override
    public String getDatabaseID() {
        return DatabaseMediator.getDatabaseIdentifier(dbPath);
    }

    @Override
    public void setDatabaseID(String databaseID) {
        DatabaseMediator.setDatabaseIdentifier(dbPath, databaseID);
    }

    @Override
    public Long getLastTimestamp() throws DataAccessException {
        // todo wrong
        return DatabaseMediator.getLastTimestamp(dbPath);
    }

    @Override
    public List<? extends Serializable> getElementsFrom(long fromTimestamp) throws DataAccessException {
        List<SerializedItem> itemsFromTimestamp = new ArrayList<>();
        List<Movie> movies = Movie.getMoviesFromTimestamp(dbPath, fromTimestamp);
        itemsFromTimestamp.addAll(movies.stream().map(ItemSerializer::serializeMovie).collect(Collectors.toList()));
        List<TVSeries> tvSeries = TVSeries.getTVSeriesFromTimestamp(dbPath, fromTimestamp);
        itemsFromTimestamp.addAll(tvSeries.stream().map(ItemSerializer::serializeTVSeries).collect(Collectors.toList()));
        List<Chapter> chapters = Chapter.getChaptersFromTimestamp(dbPath, fromTimestamp);
        itemsFromTimestamp.addAll(chapters.stream().map(ItemSerializer::serializeChapter).collect(Collectors.toList()));
        List<Person> persons = Person.getPeopleFromTimestamp(dbPath, fromTimestamp);
        itemsFromTimestamp.addAll(persons.stream().map(ItemSerializer::serializePerson).collect(Collectors.toList()));
        List<VideoFile> videoFiles = VideoFile.getVideoFilesFromTimestamp(dbPath, fromTimestamp);
        itemsFromTimestamp.addAll(videoFiles.stream().map(ItemSerializer::serializeVideoFile).collect(Collectors.toList()));
        List<SubtitleFile> subtitleFiles = SubtitleFile.getSubtitleFilesFromTimestamp(dbPath, fromTimestamp);
        itemsFromTimestamp.addAll(subtitleFiles.stream().map(ItemSerializer::serializeSubtitleFile).collect(Collectors.toList()));
        List<DeletedItem> deletedItems = DeletedItem.getDeletedItemsFromTimestamp(dbPath, fromTimestamp);
        itemsFromTimestamp.addAll(deletedItems.stream().map(ItemSerializer::serializeDeletedItem).collect(Collectors.toList()));
        Collections.sort(itemsFromTimestamp);
        return itemsFromTimestamp;
    }

    @Override
    public int elementsPerMessage() {
        return ELEMENTS_PER_MESSAGE;
    }

    @Override
    public int CRCBytes() {
        return CRC_BYTES;
    }

    @Override
    public void setElement(Object element) throws DataAccessException {
        SerializedItem item = (SerializedItem) element;
        switch (item.getType()) {
            case MOVIE:
                Movie movie = Movie.getMovieById(dbPath, item.getId());
                if (item.isAlive()) {
                    if (movie == null) {
                        movie = new Movie(dbPath);
                    }
                    ItemSerializer.deserializeMovie(item, movie);
                    // todo notify change in this item
                } else if (movie != null) {
                    // todo notify about to delete
                    movie.delete();
                }
                break;

            case TV_SERIES:
                TVSeries tvSeries = TVSeries.getTVSeriesById(dbPath, item.getId());
                if (item.isAlive()) {
                    if (tvSeries == null) {
                        tvSeries = new TVSeries(dbPath);
                    }
                    ItemSerializer.deserializeTVSeries(item, tvSeries);
                    // todo notify change in this item
                } else if (tvSeries != null) {
                    tvSeries.delete();
                }
                break;

            case CHAPTER:
                Chapter chapter = Chapter.getChapterById(dbPath, item.getId());
                if (item.isAlive()) {
                    if (chapter == null) {
                        chapter = new Chapter(dbPath);
                    }
                    ItemSerializer.deserializeChapter(item, chapter);
                    // todo notify change in this item
                } else if (chapter != null) {
                    chapter.delete();
                }
                break;

            case PERSON:
                Person person = Person.getPersonById(dbPath, item.getId());
                if (item.isAlive()) {
                    if (person == null) {
                        person = new Person(dbPath);
                    }
                    ItemSerializer.deserializePerson(item, person);
                    // todo notify change in this item
                } else if (person != null) {
                    person.delete();
                }
                break;

            case COMPANY:
                Company company = Company.getCompanyById(dbPath, item.getId());
                if (item.isAlive()) {
                    if (company == null) {
                        company = new Company(dbPath);
                    }
                    ItemSerializer.deserializeCompany(item, company);
                    // todo notify change in this item
                } else if (company != null) {
                    company.delete();
                }
                break;

            case VIDEO_FILE:
                VideoFile videoFile = VideoFile.getVideoFileById(dbPath, item.getId());
                if (item.isAlive()) {
                    if (videoFile == null) {
                        videoFile = new VideoFile(dbPath);
                    }
                    ItemSerializer.deserializeVideoFile(item, videoFile);
                    // todo notify change in this item
                } else if (videoFile != null) {
                    videoFile.delete();
                }
                break;

            case SUBTITLE_FILE:
                SubtitleFile subtitleFile = SubtitleFile.getSubtitleFileById(dbPath, item.getId());
                if (item.isAlive()) {
                    if (subtitleFile == null) {
                        subtitleFile = new SubtitleFile(dbPath);
                    }
                    ItemSerializer.deserializeSubtitleFile(item, subtitleFile);
                    // todo notify change in this item
                } else if (subtitleFile != null) {
                    subtitleFile.delete();
                }
                break;
        }
    }

    @Override
    public void endSynchProcess(Mode mode, boolean success) {
        // ignore
    }

    @Override
    public ProgressNotificationWithError<Integer, SynchError> getServerSynchProgress(PeerID clientPeerID) {
        return databaseSynchProgress;
    }
}
