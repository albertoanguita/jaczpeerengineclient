package jacz.peerengineclient;

import jacz.database.Movie;
import jacz.database.VideoFile;
import jacz.peerengineclient.databases.DatabaseIO;
import jacz.peerengineclient.test.Client;
import jacz.peerengineclient.test.TestUtil;
import jacz.util.concurrency.ThreadUtil;
import jacz.util.files.FileUtil;
import org.junit.Assert;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;

/**
 * Created by Alberto on 15/02/2016.
 */
public class SharedGenerationTest {

    private static String fileName() {
        return "file1.wmv";
    }

    private static String filePath() {
        return "./etc/test-files/file1.wmv";
    }

    private static String fileHash() {
        return "f9a7c648b0c0f240c19ff1e92db11128".toUpperCase();
    }

    private static final long WARM_UP = 5000;

    private static final long CYCLE_LENGTH = 15000;

    @org.junit.Test
    public void test() throws IOException, XMLStreamException {
        String userPath = "./etc/user_0";

        // clear dbs
        DatabaseIO.createNewDatabaseFileStructure(userPath);

        PeerEngineClient peerEngineClient = Client.loadClient(userPath);
        peerEngineClient.getFileHashDatabase().clear();
        FileUtil.clearDirectory(peerEngineClient.getMediaPath());
        System.out.println("Client started for peer " + TestUtil.formatPeer(peerEngineClient.getPeerClient().getOwnPeerId()));
//        peerEngineClient.addFriendPeer(PeerId.buildTestPeerId("2"));
        String localDB = peerEngineClient.getDatabases().getLocalDB();
        String sharedDB = peerEngineClient.getDatabases().getSharedDB();


        // connect and warm up
//        peerEngineClient.connect();
        ThreadUtil.safeSleep(WARM_UP);

        setupDB3(localDB, peerEngineClient);
        announceEvent(0);

        // generate the shared database, twice
        peerEngineClient.getSharedDatabaseGenerator().updateSharedDatabase();

        ThreadUtil.safeSleep(CYCLE_LENGTH);
        announceEvent(1);

        assertShared(sharedDB);

        peerEngineClient.stop();
    }

    private static void announceEvent(int event) {
        System.out.println("-------------");
        System.out.println("EVENT " + event + "!!!");
        System.out.println("-------------");
    }

    private static void startDBAssert(String db) {
        System.out.println("Checking " + db + "...");
    }

    private static void endDBAssert(String db) {
        System.out.println(db + " checked!!!");
    }

    private static void setupDB3(String db, PeerEngineClient peerEngineClient) throws IOException {
        // named movie, shared with other peers
        Movie movie = new Movie(db, "Alien");
        VideoFile videoFile = new VideoFile(db, fileHash());
        videoFile.setName(fileName());
        movie.addVideoFile(videoFile);
        peerEngineClient.localItemModified(videoFile);
        peerEngineClient.localItemModified(movie);

        peerEngineClient.addLocalFileFixedPath(filePath());
    }


    private static void assertShared(String db) {
        startDBAssert(db);
        Assert.assertEquals(1, Movie.getMovies(db).size());
        Movie movie = Movie.getMovies(db).get(0);

        Assert.assertEquals("Alien", movie.getTitle());
        Assert.assertEquals(1, movie.getVideoFiles().size());
        VideoFile videoFile = movie.getVideoFiles().get(0);
        Assert.assertEquals(fileName(), videoFile.getName());
        Assert.assertEquals(fileHash(), videoFile.getHash());
        Assert.assertEquals(0, videoFile.getSubtitleFiles().size());

        endDBAssert(db);
    }

}
