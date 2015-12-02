package jacz.peerengineclient.dbs_old;

import jacz.util.files.FileUtil;

/**
 * This class stores the paths that each element in the database must be placed in
 */
public class LibraryPaths {

    private static final String METADATA_PATH = "metadata";

    private static final String MAIN_IMAGES_PATH = "main_images";

    private static final String REDUCED_IMAGES_PATH = "reduced_images";

    private static final String AUDIO_PATH = "audio";

    private static final String ALBUMS_PATH = "albums";

    private static final String SONGS_PATH = "songs";

    private static final String VIDEO_PATH = "video";


    public static String getMetadataPath(String baseDir) {
        return FileUtil.joinPaths(baseDir, METADATA_PATH);
    }

    public static String getMainImagesPath(String baseDir) {
        return FileUtil.joinPaths(getMetadataPath(baseDir), MAIN_IMAGES_PATH);
    }

    public static String getReducedImagesPath(String baseDir) {
        return FileUtil.joinPaths(getMetadataPath(baseDir), REDUCED_IMAGES_PATH);
    }

    public static String getAudioPath(String baseDir) {
        return FileUtil.joinPaths(baseDir, AUDIO_PATH);
    }

    public static String getAlbumsPath(String baseDir) {
        return FileUtil.joinPaths(getAudioPath(baseDir), ALBUMS_PATH);
    }

    public static String getSongsPath(String baseDir) {
        return FileUtil.joinPaths(getAudioPath(baseDir), SONGS_PATH);
    }

    public static String getVideoPath(String baseDir) {
        return FileUtil.joinPaths(baseDir, VIDEO_PATH);
    }
}
