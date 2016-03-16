package jacz.peerengineclient.databases.synch;

import jacz.database.*;

/**
 * Created by Alberto on 26/12/2015.
 */
public class ItemSerializer {

    static SerializedItem serializeMovie(Movie movie) {
        SerializedItem item = setupSerializeItem(movie, true);
        serializeProducedCreationItem(item, movie);
        item.addInteger(DatabaseMediator.Field.MINUTES, movie.getMinutes());
        item.addIntegerList(DatabaseMediator.Field.VIDEO_FILE_LIST, movie.getVideoFilesIds());
        return item;
    }

    static SerializedItem serializeTVSeries(TVSeries tvSeries) {
        SerializedItem item = setupSerializeItem(tvSeries, true);
        serializeProducedCreationItem(item, tvSeries);
        item.addIntegerList(DatabaseMediator.Field.CHAPTER_LIST, tvSeries.getChaptersIds());
        return item;
    }

    static SerializedItem serializeChapter(Chapter chapter) {
        SerializedItem item = setupSerializeItem(chapter, true);
        serializeCreationItem(item, chapter);
        item.addString(DatabaseMediator.Field.SEASON, chapter.getSeason());
        item.addInteger(DatabaseMediator.Field.MINUTES, chapter.getMinutes());
        item.addIntegerList(DatabaseMediator.Field.VIDEO_FILE_LIST, chapter.getVideoFilesIds());
        return item;
    }

//    static SerializedItem serializePerson(Person person) {
//        SerializedItem item = setupSerializeItem(person, true);
//        serializeNamedItem(item, person);
//        return item;
//    }
//
//    static SerializedItem serializeCompany(Company company) {
//        SerializedItem item = setupSerializeItem(company, true);
//        serializeNamedItem(item, company);
//        return item;
//    }

    static SerializedItem serializeVideoFile(VideoFile videoFile) {
        SerializedItem item = setupSerializeItem(videoFile, true);
        serializeFileWithLanguages(item, videoFile);
        item.addInteger(DatabaseMediator.Field.MINUTES, videoFile.getMinutes());
        item.addInteger(DatabaseMediator.Field.RESOLUTION, videoFile.getResolution());
        item.addQuality(DatabaseMediator.Field.QUALITY_CODE, videoFile.getQuality());
        item.addIntegerList(DatabaseMediator.Field.SUBTITLE_FILE_LIST, videoFile.getSubtitleFilesIds());
        return item;
    }

    static SerializedItem serializeSubtitleFile(SubtitleFile subtitleFile) {
        SerializedItem item = setupSerializeItem(subtitleFile, true);
        serializeFileWithLanguages(item, subtitleFile);
        return item;
    }

    static SerializedItem serializeDeletedItem(DeletedItem deletedItem) {
        return setupSerializeItem(deletedItem.getDeletedItemType(), deletedItem.getDeletedItemId(), deletedItem, false);
    }

    static void serializeProducedCreationItem(SerializedItem item, ProducedCreationItem producedCreationItem) {
        serializeCreationItem(item, producedCreationItem);
        item.addStringList(DatabaseMediator.Field.COMPANY_LIST, producedCreationItem.getProductionCompanies());
//        item.addIntegerList(DatabaseMediator.Field.COMPANY_LIST, producedCreationItem.getProductionCompaniesIds());
        item.addGenreList(DatabaseMediator.Field.GENRES, producedCreationItem.getGenres());
        item.addImageHash(DatabaseMediator.Field.IMAGE_HASH, producedCreationItem.getImageHash());
    }

    static void serializeCreationItem(SerializedItem item, CreationItem creationItem) {
        item.addString(DatabaseMediator.Field.TITLE, creationItem.getTitle());
        item.addString(DatabaseMediator.Field.ORIGINAL_TITLE, creationItem.getOriginalTitle());
        item.addInteger(DatabaseMediator.Field.YEAR, creationItem.getYear());
        item.addString(DatabaseMediator.Field.SYNOPSIS, creationItem.getSynopsis());
        item.addCountryList(DatabaseMediator.Field.COUNTRIES, creationItem.getCountries());
        item.addStringList(DatabaseMediator.Field.EXTERNAL_URLS, creationItem.getExternalURLs());
        item.addStringList(DatabaseMediator.Field.CREATOR_LIST, creationItem.getCreators());
        item.addStringList(DatabaseMediator.Field.ACTOR_LIST, creationItem.getActors());
//        item.addIntegerList(DatabaseMediator.Field.CREATOR_LIST, creationItem.getCreatorsIds());
//        item.addIntegerList(DatabaseMediator.Field.ACTOR_LIST, creationItem.getActorsIds());
    }

    static void serializeNamedItem(SerializedItem item, NamedItem namedItem) {
        item.addString(DatabaseMediator.Field.NAME, namedItem.getName());
        item.addStringList(DatabaseMediator.Field.ALIASES, namedItem.getAliases());
    }

    static void serializeFileWithLanguages(SerializedItem item, FileWithLanguages fileWithLanguages) {
        serializeFile(item, fileWithLanguages);
        item.addLanguageList(DatabaseMediator.Field.LANGUAGES, fileWithLanguages.getLanguages());
    }

    static void serializeFile(SerializedItem item, File file) {
        item.addString(DatabaseMediator.Field.HASH, file.getHash());
        item.addLong(DatabaseMediator.Field.LENGTH, file.getLength());
        item.addString(DatabaseMediator.Field.NAME, file.getName());
        item.addStringList(DatabaseMediator.Field.ADDITIONAL_SOURCES, file.getAdditionalSources());
    }

    private static SerializedItem setupSerializeItem(DatabaseItem databaseItem, boolean alive) {
        return setupSerializeItem(databaseItem.getItemType(), databaseItem.getId(), databaseItem, alive);
    }

    private static SerializedItem setupSerializeItem(DatabaseMediator.ItemType type, int id, DatabaseItem databaseItem, boolean alive) {
        return new SerializedItem(type, id, databaseItem.getTimestamp(), alive);
    }

    static void deserializeMovie(SerializedItem item, Movie movie) {
        deserializeProducedCreationItem(item, movie);
        movie.setMinutesPostponed(item.getInteger(DatabaseMediator.Field.MINUTES));
        movie.setVideoFilesIdsPostponed(item.getIntegerList(DatabaseMediator.Field.VIDEO_FILE_LIST));
        finishDeserialization(movie);
    }

    static void deserializeTVSeries(SerializedItem item, TVSeries tvSeries) {
        deserializeProducedCreationItem(item, tvSeries);
        tvSeries.setChaptersIdsPostponed(item.getIntegerList(DatabaseMediator.Field.CHAPTER_LIST));
        finishDeserialization(tvSeries);
    }

    static void deserializeChapter(SerializedItem item, Chapter chapter) {
        deserializeCreationItem(item, chapter);
        chapter.setSeasonPostponed(item.getString(DatabaseMediator.Field.SEASON));
        chapter.setMinutesPostponed(item.getInteger(DatabaseMediator.Field.MINUTES));
        chapter.setVideoFilesIdsPostponed(item.getIntegerList(DatabaseMediator.Field.VIDEO_FILE_LIST));
        finishDeserialization(chapter);
    }

//    static void deserializePerson(SerializedItem item, Person person) {
//        deserializeNamedItem(item, person);
//        finishDeserialization(person);
//    }
//
//    static void deserializeCompany(SerializedItem item, Company company) {
//        deserializeNamedItem(item, company);
//        finishDeserialization(company);
//    }

    static void deserializeVideoFile(SerializedItem item, VideoFile videoFile) {
        deserializeFileWithLanguages(item, videoFile);
        videoFile.setMinutesPostponed(item.getInteger(DatabaseMediator.Field.MINUTES));
        videoFile.setResolutionPostponed(item.getInteger(DatabaseMediator.Field.RESOLUTION));
        videoFile.setQualityPostponed(item.getQuality(DatabaseMediator.Field.QUALITY_CODE));
        videoFile.setSubtitleFilesIdsPostponed(item.getIntegerList(DatabaseMediator.Field.SUBTITLE_FILE_LIST));
        finishDeserialization(videoFile);
    }

    static void deserializeSubtitleFile(SerializedItem item, SubtitleFile subtitleFile) {
        deserializeFileWithLanguages(item, subtitleFile);
        finishDeserialization(subtitleFile);
    }

    static void deserializeProducedCreationItem(SerializedItem item, ProducedCreationItem producedCreationItem) {
        deserializeCreationItem(item, producedCreationItem);
        producedCreationItem.setProductionCompaniesPostponed(item.getStringList(DatabaseMediator.Field.COMPANY_LIST));
//        producedCreationItem.setProductionCompaniesIdsPostponed(item.getIntegerList(DatabaseMediator.Field.COMPANY_LIST));
        producedCreationItem.setGenresPostponed(item.getGenreList(DatabaseMediator.Field.GENRES));
        producedCreationItem.setImageHashPostponed(item.getImageHash(DatabaseMediator.Field.IMAGE_HASH));
    }

    static void deserializeCreationItem(SerializedItem item, CreationItem creationItem) {
        deserializeDatabaseItem(item, creationItem);
        creationItem.setTitlePostponed(item.getString(DatabaseMediator.Field.TITLE));
        creationItem.setOriginalTitlePostponed(item.getString(DatabaseMediator.Field.ORIGINAL_TITLE));
        creationItem.setYearPostponed(item.getInteger(DatabaseMediator.Field.YEAR));
        creationItem.setSynopsisPostponed(item.getString(DatabaseMediator.Field.SYNOPSIS));
        creationItem.setCountriesPostponed(item.getCountryList(DatabaseMediator.Field.COUNTRIES));
        creationItem.setExternalURLsPostponed(item.getStringList(DatabaseMediator.Field.EXTERNAL_URLS));
        creationItem.setCreatorsPostponed(item.getStringList(DatabaseMediator.Field.CREATOR_LIST));
        creationItem.setActorsPostponed(item.getStringList(DatabaseMediator.Field.ACTOR_LIST));
//        creationItem.setCreatorsIdsPostponed(item.getIntegerList(DatabaseMediator.Field.CREATOR_LIST));
//        creationItem.setActorsIdsPostponed(item.getIntegerList(DatabaseMediator.Field.ACTOR_LIST));
    }

    static void deserializeNamedItem(SerializedItem item, NamedItem namedItem) {
        deserializeDatabaseItem(item, namedItem);
        namedItem.setNamePostponed(item.getString(DatabaseMediator.Field.NAME));
        namedItem.setAliasesPostponed(item.getStringList(DatabaseMediator.Field.ALIASES));
    }

    static void deserializeFileWithLanguages(SerializedItem item, FileWithLanguages fileWithLanguages) {
        deserializeFile(item, fileWithLanguages);
        fileWithLanguages.setLanguagesPostponed(item.getLanguageList(DatabaseMediator.Field.LANGUAGES));
    }

    static void deserializeFile(SerializedItem item, File file) {
        deserializeDatabaseItem(item, file);
        file.setHashPostponed(item.getString(DatabaseMediator.Field.HASH));
        file.setLengthPostponed(item.getLong(DatabaseMediator.Field.LENGTH));
        file.setNamePostponed(item.getString(DatabaseMediator.Field.NAME));
        file.setAdditionalSourcesPostponed(item.getStringList(DatabaseMediator.Field.ADDITIONAL_SOURCES));
    }

    static void deserializeDatabaseItem(SerializedItem item, DatabaseItem databaseItem) {
        databaseItem.setTimestamp(item.getTimestamp(), false);
    }

    private static void finishDeserialization(DatabaseItem item) {
        item.flushChanges();
    }
}
