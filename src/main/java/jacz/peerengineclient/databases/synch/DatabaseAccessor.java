package jacz.peerengineclient.databases.synch;

import jacz.database.*;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.data_synchronization.DataAccessException;
import jacz.peerengineservice.util.data_synchronization.DataAccessor;
import jacz.peerengineservice.util.data_synchronization.SynchError;
import jacz.util.notification.ProgressNotificationWithError;

import java.io.Serializable;
import java.util.List;

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
        // todo ask server to collect elements from timestamp, order them by timestamp
        return null;
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
        // todo update timestamp
        if (!item.isAlive()) {
            // delete item
            switch (item.getType()) {
                case MOVIE:
                    Movie movie = Movie.getMovieById(dbPath, item.getId());
                    if (movie != null) {
                        movie.delete();
                    }
                    break;
                case TV_SERIES:
                    TVSeries tvSeries = TVSeries.getTVSeriesById(dbPath, item.getId());
                    if (tvSeries != null) {
                        tvSeries.delete();
                    }
                    break;
                case CHAPTER:
                    Chapter chapter = Chapter.getChapterById(dbPath, item.getId());
                    if (chapter != null) {
                        chapter.delete();
                    }
                    break;
                case PERSON:
                    Person person = Person.getPersonById(dbPath, item.getId());
                    if (person != null) {
                        person.delete();
                    }
                    break;
                case COMPANY:
                    Company company = Company.getCompanyById(dbPath, item.getId());
                    if (company != null) {
                        company.delete();
                    }
                    break;
                case VIDEO_FILE:
                    VideoFile videoFile = VideoFile.getVideoFileById(dbPath, item.getId());
                    if (videoFile != null) {
                        videoFile.delete();
                    }
                    break;
                case SUBTITLE_FILE:
                    SubtitleFile subtitleFile = SubtitleFile.getSubtitleFileById(dbPath, item.getId());
                    if (subtitleFile != null) {
                        subtitleFile.delete();
                    }
                    break;
            }
        } else {
            switch (item.getType()) {
                case MOVIE:
                    // todo use transactions
                    Movie movie = Movie.getMovieById(dbPath, item.getId());
                    if (movie == null) {
                        movie = new Movie(dbPath);
                    }
                    movie.setTitle(item.getString("title"));
                    // todo rest
                    break;
                case TV_SERIES:
                    break;
                case CHAPTER:
                    break;
                case PERSON:
                    break;
                case COMPANY:
                    break;
                case VIDEO_FILE:
                    break;
                case SUBTITLE_FILE:
                    break;
            }
        }
        // todo write item to db
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
