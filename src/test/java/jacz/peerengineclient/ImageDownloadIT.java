package jacz.peerengineclient;

import jacz.database.Movie;
import jacz.database.TVSeries;
import jacz.database.util.ImageHash;
import jacz.peerengineclient.databases.DatabaseIO;
import jacz.peerengineclient.test.Client;
import jacz.peerengineclient.test.TestUtil;
import jacz.peerengineservice.NotAliveException;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.UnavailablePeerException;
import jacz.util.concurrency.ThreadUtil;
import jacz.util.lists.tuple.Triple;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;

/**
 * Created by Alberto on 12/02/2016.
 */
public class ImageDownloadIT {

    public enum TestFile {
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
        MOON,
        ALIEN
    }

    // paths and MD5 hashes of the files above:
    private static Triple<String, String, String> namePathAndHash(TestFile testFile) {
        String name;
        String path = "./etc/test-files-cp/";
        String hash;
        switch (testFile) {
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

            case ALIEN:
                name = "alien.jpg";
                hash = "88f0ccc8c682073560da382b77bb85c3".toUpperCase();
                break;

            default:
                throw new RuntimeException();
        }
        path += name;
        return new Triple<>(name, path, hash);
    }


    private static final long WARM_UP = 10000;

    private static final long CYCLE_LENGTH = 15000;


    @org.junit.Test
    public void imageDownload1() throws IOException, XMLStreamException, UnavailablePeerException, NotAliveException {

        String userPath = "./etc/user_0";

        // clear dbs
        DatabaseIO.createNewDatabaseFileStructure(userPath);

        PeerEngineClient peerEngineClient = Client.loadClient(userPath);
        peerEngineClient.getFileHashDatabase().clear();
        FileUtils.cleanDirectory(new File(peerEngineClient.getMediaPath()));
        FileUtils.cleanDirectory(new File(peerEngineClient.getTempDownloadsPath()));
        // download at 25 kB/s
        peerEngineClient.setMaxDownloadSpeed(250);
        peerEngineClient.addFavoritePeer(PeerId.buildTestPeerId("2"));
        String integratedDB = peerEngineClient.getDatabases().getIntegratedDB();
        System.out.println("Client started for peer " + TestUtil.formatPeer(peerEngineClient.getPeerClient().getOwnPeerId()));

        // connect and warm up
        peerEngineClient.connect();
        ThreadUtil.safeSleep(WARM_UP);

        setupDB1(integratedDB, peerEngineClient);

        // wait for dbs to synch, test integrated content
        // wait 1 cycles
        ThreadUtil.safeSleep(CYCLE_LENGTH);

        announceEvent(1);

        assertFile(peerEngineClient, FileUtils.getFile(peerEngineClient.getMediaPath(), "images", namePathAndHash(TestFile.GRAMOS).element3 + ".jpg"), namePathAndHash(TestFile.GRAMOS).element3);
        assertFile(peerEngineClient, FileUtils.getFile(peerEngineClient.getMediaPath(), "images", namePathAndHash(TestFile.MOON).element3 + ".jpeg"), namePathAndHash(TestFile.MOON).element3);
        assertFileNot(peerEngineClient, FileUtils.getFile(peerEngineClient.getMediaPath(), "images", namePathAndHash(TestFile.ALIEN).element3) + ".jpg", namePathAndHash(TestFile.ALIEN).element3);

        // wait 4 cycles
        ThreadUtil.safeSleep(4 * CYCLE_LENGTH);
        announceEvent(5);

        assertFile(peerEngineClient, FileUtils.getFile(peerEngineClient.getMediaPath(), "images", namePathAndHash(TestFile.ALIEN).element3 + ".jpg"), namePathAndHash(TestFile.ALIEN).element3);

        peerEngineClient.removeFavoritePeer(PeerId.buildTestPeerId("2"));
        peerEngineClient.stop();
    }


    @org.junit.Test
    public void imageDownload2() throws IOException, XMLStreamException {
        String userPath = "./etc/user_1";

        FileUtils.cleanDirectory(new File("./etc/test-files-cp/"));
        for (File file : FileUtils.listFiles(new File("./etc/test-files/"), null, false)) {
            FileUtils.copyFile(file, FileUtils.getFile("./etc/test-files-cp/", file.getName()));
        }

        // clear dbs
        DatabaseIO.createNewDatabaseFileStructure(userPath);

        PeerEngineClient peerEngineClient = Client.loadClient(userPath);
        peerEngineClient.getFileHashDatabase().clear();
        FileUtils.cleanDirectory(new File(peerEngineClient.getMediaPath()));
        System.out.println("Client started for peer " + TestUtil.formatPeer(peerEngineClient.getPeerClient().getOwnPeerId()));
        peerEngineClient.addFavoritePeer(PeerId.buildTestPeerId("1"));
        String sharedDB = peerEngineClient.getDatabases().getSharedDB();

        setupDB2(sharedDB, peerEngineClient);

        // warm up
        ThreadUtil.safeSleep(WARM_UP);

        // connect and wait for synch and downloads
        announceEvent(0);
        peerEngineClient.connect();

        // wait 1 cycles
        ThreadUtil.safeSleep(CYCLE_LENGTH);

        announceEvent(1);

        // wait 4 cycles
        ThreadUtil.safeSleep(4 * CYCLE_LENGTH);
        announceEvent(5);

        peerEngineClient.removeFavoritePeer(PeerId.buildTestPeerId("1"));
        peerEngineClient.stop();
    }


    private static void announceEvent(int event) {
        System.out.println("-------------");
        System.out.println("EVENT " + event + "!!!");
        System.out.println("-------------");
    }

    private static void setupDB1(String db, PeerEngineClient peerEngineClient) throws IOException {
        // named movie
        Movie movie = new Movie(db, "Alien");
        movie.setImageHash(new ImageHash(namePathAndHash(TestFile.ALIEN).element3, "jpg"));
//        peerEngineClient.localItemModified(movie);
    }

    private static void setupDB2(String db, PeerEngineClient peerEngineClient) throws IOException {
        // named movie
        Movie movie = new Movie(db, "21 gramos");
        movie.setImageHash(new ImageHash(namePathAndHash(TestFile.GRAMOS).element3, "jpg"));

        TVSeries tvSeries = new TVSeries(db, "Moon");
        tvSeries.setImageHash(new ImageHash(namePathAndHash(TestFile.MOON).element3, "jpeg"));

        addFiles2(peerEngineClient);
    }

    private static void addFiles2(PeerEngineClient peerEngineClient) throws IOException {
        peerEngineClient.addLocalImageFile(namePathAndHash(TestFile.GRAMOS).element2);
        peerEngineClient.addLocalImageFile(namePathAndHash(TestFile.MOON).element2);
        peerEngineClient.addLocalImageFile(namePathAndHash(TestFile.ALIEN).element2);
    }

    private void assertFile(PeerEngineClient peerEngineClient, File path, String hash) {
        System.out.println("Asserting file at " + path + "...");
        Assert.assertTrue(peerEngineClient.getFileHashDatabase().containsKey(hash));
        Assert.assertTrue(path.isFile());
    }

    private void assertFileNot(PeerEngineClient peerEngineClient, String path, String hash) {
        System.out.println("Asserting not file at " + path + "...");
        Assert.assertFalse(peerEngineClient.getFileHashDatabase().containsKey(hash));
        Assert.assertFalse(new File(path).isFile());
    }

}
