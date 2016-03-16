package jacz.peerengineclient;

import jacz.database.Chapter;
import jacz.database.Movie;
import jacz.database.TVSeries;
import jacz.peerengineclient.databases.DatabaseIO;
import jacz.peerengineclient.test.Client;
import jacz.util.files.FileUtil;
import jacz.util.lists.tuple.Triple;
import org.junit.Assert;

import java.io.IOException;

/**
 * Created by Alberto on 18/02/2016.
 */
public class AddLocalFilesTest {

    public enum File {
        VIDEO_1,
        SUB_1,
        VIDEO_2,
        SUB_2,
        VIDEO_3,
        SUB_3,
        VIDEO_4,
        VIDEO_5,
        VIDEO_6,
        GRAMOS,
        MOON
    }

    // paths and MD5 hashes of the files above:
    private static Triple<String, String, String> namePathAndHash(File file) {
        String name;
        String path = "./etc/test-files-cp/";
        String hash;
        switch (file) {
            case SUB_1:
                name = "file1.srt";
                hash = "572976d9712dc9bafbcdd68f5945b25d".toUpperCase();
                break;

            case VIDEO_1:
                name = "file1.wmv";
                hash = "f9a7c648b0c0f240c19ff1e92db11128".toUpperCase();
                break;

            case SUB_2:
                name = "file2.srt";
                hash = "d9c347f32fe4c6645cd8b1e5871a6b39".toUpperCase();
                break;

            case VIDEO_2:
                name = "file2.wmv";
                hash = "d2ac2bbb088e479b2610743bc67504b7".toUpperCase();
                break;

            case SUB_3:
                name = "file3.srt";
                hash = "9862d8d462bbe428624143549d6f55f1".toUpperCase();
                break;

            case VIDEO_3:
                name = "file3.wmv";
                hash = "b95cd9032f8efb0eb0513e0801cff4c4".toUpperCase();
                break;

            case VIDEO_4:
                name = "file4.wmv";
                hash = "766ecbc1af6de928c6b78410d9034e31".toUpperCase();
                break;

            case VIDEO_5:
                name = "file5.wmv";
                hash = "6c47c9fe10c943912de0f9ed68f53252".toUpperCase();
                break;

            case VIDEO_6:
                name = "file6.wmv";
                hash = "eaf4cc31adeee97a6ad3de3dee298887".toUpperCase();
                break;

            case GRAMOS:
                name = "21_gramos.jpg";
                hash = "c640c3096a9c359af5f76562b9742bed".toUpperCase();
                break;

            case MOON:
                name = "moon.jpeg";
                hash = "97ad31403b2fb78d50a6120c517fe694".toUpperCase();
                break;

            default:
                throw new RuntimeException();
        }
        path += name;
        return new Triple<>(name, path, hash);
    }


    @org.junit.Test
    public void addLocalFiles() throws IOException {

        String userPath = "./etc/user_0";

        FileUtil.clearDirectory("./etc/test-files-cp/");
        for (String file : FileUtil.getDirectoryContents("./etc/test-files/")) {
            FileUtil.copy(FileUtil.joinPaths("./etc/test-files/", file), "./etc/test-files-cp/");
        }

        // clear dbs
        DatabaseIO.createNewDatabaseFileStructure(userPath);

        PeerEngineClient peerEngineClient = Client.loadClient(userPath);
        peerEngineClient.getFileHashDatabase().clear();
        FileUtil.clearDirectory(peerEngineClient.getMediaPath());
        FileUtil.clearDirectory(peerEngineClient.getTempDownloadsPath());
        String db = peerEngineClient.getDatabases().getLocalDB();
        String mediaPath = peerEngineClient.getMediaPath();
        Movie movie = new Movie(db, "Alien");
        TVSeries tvSeries = new TVSeries(db, "Bottom");
        Chapter chapter = new Chapter(db, "Day out");

        peerEngineClient.addLocalFileFixedPath(namePathAndHash(File.VIDEO_1).element2);
        peerEngineClient.addLocalFileFixedPath(namePathAndHash(File.SUB_1).element2);
        assertFile(peerEngineClient, namePathAndHash(File.VIDEO_1).element2, namePathAndHash(File.VIDEO_1).element3);
        assertFile(peerEngineClient, namePathAndHash(File.SUB_1).element2, namePathAndHash(File.SUB_1).element3);

        peerEngineClient.addLocalMovieFile(namePathAndHash(File.VIDEO_2).element2, movie);
        peerEngineClient.addLocalMovieFile(namePathAndHash(File.SUB_2).element2, movie);
        assertFile(peerEngineClient, FileUtil.joinPaths(mediaPath, "movies", movie.getTitle() + "_" + movie.getId(), namePathAndHash(File.VIDEO_2).element1), namePathAndHash(File.VIDEO_2).element3);
        assertFile(peerEngineClient, FileUtil.joinPaths(mediaPath, "movies", movie.getTitle() + "_" + movie.getId(), namePathAndHash(File.SUB_2).element1), namePathAndHash(File.SUB_2).element3);

        peerEngineClient.addLocalChapterFile(namePathAndHash(File.VIDEO_3).element2, tvSeries, chapter);
        peerEngineClient.addLocalChapterFile(namePathAndHash(File.SUB_3).element2, tvSeries, chapter);
        assertFile(peerEngineClient, FileUtil.joinPaths(mediaPath, "series", tvSeries.getTitle() + "_" + tvSeries.getId(), chapter.getTitle() + "_" + chapter.getId(), namePathAndHash(File.VIDEO_3).element1), namePathAndHash(File.VIDEO_3).element3);
        assertFile(peerEngineClient, FileUtil.joinPaths(mediaPath, "series", tvSeries.getTitle() + "_" + tvSeries.getId(), chapter.getTitle() + "_" + chapter.getId(), namePathAndHash(File.SUB_3).element1), namePathAndHash(File.SUB_3).element3);

        peerEngineClient.addLocalImageFile(namePathAndHash(File.GRAMOS).element2);
        peerEngineClient.addLocalImageFile(namePathAndHash(File.MOON).element2);
        assertFile(peerEngineClient, FileUtil.joinPaths(mediaPath, "images", namePathAndHash(File.GRAMOS).element3 + ".jpg"), namePathAndHash(File.GRAMOS).element3);
        assertFile(peerEngineClient, FileUtil.joinPaths(mediaPath, "images", namePathAndHash(File.MOON).element3) + ".jpeg", namePathAndHash(File.MOON).element3);
    }

    private void assertFile(PeerEngineClient peerEngineClient, String path, String hash) {
        System.out.println("Asserting file at " + path + "...");
        Assert.assertTrue(peerEngineClient.getFileHashDatabase().containsKey(hash));
        Assert.assertTrue(FileUtil.isFile(path));
    }
}
