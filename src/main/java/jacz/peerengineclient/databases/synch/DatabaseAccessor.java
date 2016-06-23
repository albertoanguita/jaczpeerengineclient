package jacz.peerengineclient.databases.synch;

import jacz.database.*;
import jacz.peerengineclient.databases.DatabaseManager;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.util.data_synchronization.DataAccessException;
import jacz.peerengineservice.util.data_synchronization.DataAccessor;
import jacz.peerengineservice.util.data_synchronization.SynchError;
import jacz.util.notification.ProgressNotificationWithError;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Accessor implementation for databases. Works both in client and server mode
 * - Client: synch the remote database corresponding to the other peer
 * - Server: synch our shared database with the other peer
 */
public class DatabaseAccessor implements DataAccessor {

    public static final String NAME = "LIBRARY_ACCESSOR";

    private static final int ELEMENTS_PER_MESSAGE = 5;

    private static final int CRC_BYTES = 4;

    private final DatabaseManager databaseManager;

    private final PeerId remotePeerId;

    private final String dbPath;

    private final ProgressNotificationWithError<Integer, SynchError> databaseSynchProgress;

    public DatabaseAccessor(DatabaseManager databaseManager, PeerId remotePeerId, String dbPath, ProgressNotificationWithError<Integer, SynchError> databaseSynchProgress) {
        this.databaseManager = databaseManager;
        this.remotePeerId = remotePeerId;
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
        // the existing database is not valid -> remove all items contained in the current database.
        // and then create a new one
        clearLocalDatabase();
        DatabaseMediator.dropAndCreate(dbPath, databaseID);
    }

    private void clearLocalDatabase() {
        // todo test
        Stream<DatabaseItem> itemStream = Stream.empty();
        itemStream = Stream.concat(itemStream, Movie.getMovies(dbPath).stream());
        itemStream = Stream.concat(itemStream, TVSeries.getTVSeries(dbPath).stream());
        itemStream = Stream.concat(itemStream, Chapter.getChapters(dbPath).stream());
        itemStream = Stream.concat(itemStream, VideoFile.getVideoFiles(dbPath).stream());
        itemStream = Stream.concat(itemStream, SubtitleFile.getSubtitleFiles(dbPath).stream());
        itemStream.forEach(item -> databaseManager.remoteItemWillBeRemoved(remotePeerId, item));
    }

    @Override
    public Long getLastTimestamp() throws DataAccessException {
        return DatabaseMediator.getHighestManualTimestamp(dbPath);
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
//        List<Person> persons = Person.getPeopleFromTimestamp(dbPath, fromTimestamp);
//        itemsFromTimestamp.addAll(persons.stream().map(ItemSerializer::serializePerson).collect(Collectors.toList()));
//        List<Company> companies = Company.getCompaniesFromTimestamp(dbPath, fromTimestamp);
//        itemsFromTimestamp.addAll(companies.stream().map(ItemSerializer::serializeCompany).collect(Collectors.toList()));
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
                        movie = new Movie(dbPath, item.getId());
                    }
                    ItemSerializer.deserializeMovie(item, movie);
                    databaseManager.remoteItemModified(remotePeerId, movie);
                } else if (movie != null) {
                    databaseManager.remoteItemWillBeRemoved(remotePeerId, movie);
                    movie.delete();
                }
                break;

            case TV_SERIES:
                TVSeries tvSeries = TVSeries.getTVSeriesById(dbPath, item.getId());
                if (item.isAlive()) {
                    if (tvSeries == null) {
                        tvSeries = new TVSeries(dbPath, item.getId());
                    }
                    ItemSerializer.deserializeTVSeries(item, tvSeries);
                    databaseManager.remoteItemModified(remotePeerId, tvSeries);
                } else if (tvSeries != null) {
                    databaseManager.remoteItemWillBeRemoved(remotePeerId, tvSeries);
                    tvSeries.delete();
                }
                break;

            case CHAPTER:
                Chapter chapter = Chapter.getChapterById(dbPath, item.getId());
                if (item.isAlive()) {
                    if (chapter == null) {
                        chapter = new Chapter(dbPath, item.getId());
                    }
                    ItemSerializer.deserializeChapter(item, chapter);
                    databaseManager.remoteItemModified(remotePeerId, chapter);
                } else if (chapter != null) {
                    databaseManager.remoteItemWillBeRemoved(remotePeerId, chapter);
                    chapter.delete();
                }
                break;

//            case PERSON:
//                Person person = Person.getPersonById(dbPath, item.getId());
//                if (item.isAlive()) {
//                    if (person == null) {
//                        person = new Person(dbPath, item.getId());
//                    }
//                    ItemSerializer.deserializePerson(item, person);
//                    databaseManager.remoteItemModified(remotePeerId, person);
//                } else if (person != null) {
//                    databaseManager.remoteItemWillBeRemoved(remotePeerId, person);
//                    person.delete();
//                }
//                break;
//
//            case COMPANY:
//                Company company = Company.getCompanyById(dbPath, item.getId());
//                if (item.isAlive()) {
//                    if (company == null) {
//                        company = new Company(dbPath, item.getId());
//                    }
//                    ItemSerializer.deserializeCompany(item, company);
//                    databaseManager.remoteItemModified(remotePeerId, company);
//                } else if (company != null) {
//                    databaseManager.remoteItemWillBeRemoved(remotePeerId, company);
//                    company.delete();
//                }
//                break;

            case VIDEO_FILE:
                VideoFile videoFile = VideoFile.getVideoFileById(dbPath, item.getId());
                if (item.isAlive()) {
                    if (videoFile == null) {
                        videoFile = new VideoFile(dbPath, item.getId());
                    }
                    ItemSerializer.deserializeVideoFile(item, videoFile);
                    databaseManager.remoteItemModified(remotePeerId, videoFile);
                } else if (videoFile != null) {
                    databaseManager.remoteItemWillBeRemoved(remotePeerId, videoFile);
                    videoFile.delete();
                }
                break;

            case SUBTITLE_FILE:
                SubtitleFile subtitleFile = SubtitleFile.getSubtitleFileById(dbPath, item.getId());
                if (item.isAlive()) {
                    if (subtitleFile == null) {
                        subtitleFile = new SubtitleFile(dbPath, item.getId());
                    }
                    ItemSerializer.deserializeSubtitleFile(item, subtitleFile);
                    databaseManager.remoteItemModified(remotePeerId, subtitleFile);
                } else if (subtitleFile != null) {
                    databaseManager.remoteItemWillBeRemoved(remotePeerId, subtitleFile);
                    subtitleFile.delete();
                }
                break;
        }
        DatabaseMediator.updateHighestManualTimestamp(dbPath, item.getTimestamp());
    }

    @Override
    public void endSynchProcess(Mode mode, boolean success) {
        // ignore
    }

    @Override
    public ProgressNotificationWithError<Integer, SynchError> getServerSynchProgress(PeerId clientPeerId) {
        return databaseSynchProgress;
    }
}
