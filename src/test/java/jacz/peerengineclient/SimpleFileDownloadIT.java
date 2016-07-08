package jacz.peerengineclient;

import jacz.database.DatabaseMediator;
import jacz.database.Movie;
import jacz.database.VideoFile;
import jacz.peerengineclient.common.Client;
import jacz.peerengineclient.common.TestUtil;
import jacz.peerengineclient.databases.DatabaseIO;
import jacz.peerengineclient.file_system.PathConstants;
import jacz.peerengineclient.test.IntegrationTest;
import jacz.peerengineservice.NotAliveException;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.UnavailablePeerException;
import jacz.peerengineservice.util.datatransfer.master.DownloadManager;
import jacz.peerengineservice.util.datatransfer.master.DownloadState;
import org.aanguita.jacuzzi.concurrency.ThreadUtil;
import org.aanguita.jacuzzi.lists.tuple.Triple;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.experimental.categories.Category;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;

/**
 * Created by Alberto on 06/05/2016.
 */
@Category(IntegrationTest.class)
public class SimpleFileDownloadIT {

    // for this test we setup timed cycles that allow dbs to synch and to be tested
    // each cycle lasts 15 seconds, and all users have a 10 second warm up
    // the cycles divide the events. Event 0 takes place right after the warm up. Event 1, 15 seconds later...
    // user 1 will test his data at even events. Users 2 and 3 will modify their data at odd events.

    // we download a video file and a subtitle for it from a movie (peer 2)
    //   file1.wmv, file1.srt
    // a video file and a subtitle for it from a chapter with tvSeries (peer 2)
    //   file2.wmv, file2.srt
    // a video file and a subtitle for it from a chapter without tvSeries (peer 2)
    //   file3.wmv, file3.srt
    //   this should not be shared, so we will not download it
    // a video file from an unnamed movie (peer 2)
    //   file4.wmv
    // a video file from a movie shared from multiple peers (peers 2 and 3)
    //   file5.wmv
    // a video file partially available from another movie (peer 3, which is getting it from peer 4)
    //   file6.wmv
    //   WE SKIP THIS TEST AND DO IT IN FileDownloadIncompleteIT

    public enum File {
        VIDEO_1,
    }

    // paths and MD5 hashes of the files above:
    private static Triple<String, String, String> namePathAndHash(File file) {
        String name;
        String path = "./etc/test-files/";
        String hash;
        switch (file) {
            case VIDEO_1:
                name = "file1.wmv";
                hash = "f9a7c648b0c0f240c19ff1e92db11128".toUpperCase();
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
    public void simpleFileDownload1() throws IOException, XMLStreamException, UnavailablePeerException, NotAliveException {

        String userPath = "./etc/user_0";

        // clear dbs
        DatabaseIO.createNewDatabaseFileStructure(userPath);

        FileUtils.cleanDirectory(new java.io.File("./etc/user_0/media"));
        FileUtils.cleanDirectory(new java.io.File("./etc/user_0/temp"));
        PeerEngineClient peerEngineClient = Client.loadClient(userPath);
        peerEngineClient.clearAllData();
        peerEngineClient.addFavoritePeer(PeerId.buildTestPeerId("2"));
        peerEngineClient.addFavoritePeer(PeerId.buildTestPeerId("3"));
        peerEngineClient.setWishForRegularsConnections(false);
        peerEngineClient.clearFileHashDatabase();
        // download at 250 kB/s
        peerEngineClient.setMaxDownloadSpeed(250);
        peerEngineClient.addFavoritePeer(PeerId.buildTestPeerId("2"));
        String integratedDB = peerEngineClient.getDatabases().getIntegratedDB();
        System.out.println("Client started for peer " + TestUtil.formatPeer(peerEngineClient.getOwnPeerId()));

        // connect and warm up
        peerEngineClient.connect();
        peerEngineClient.setVisibleDownloadsTimer(2000L);
        ThreadUtil.safeSleep(WARM_UP);

        // wait for dbs to synch, test integrated content
        // wait 2 cycles
        ThreadUtil.safeSleep(2 * CYCLE_LENGTH);

        announceEvent(2);
        assertIntegrated(integratedDB, 0);

        // start downloading the first file
        Movie movie0 = Movie.getMovies(integratedDB).get(0);
        VideoFile videoFile = movie0.getVideoFiles().get(0);
        DownloadManager vfDownloadManager = peerEngineClient.downloadMediaFile(DownloadInfo.Type.VIDEO_FILE, DatabaseMediator.ItemType.MOVIE, movie0.getId(), null, videoFile.getId());

        Assert.assertEquals(DownloadState.RUNNING, vfDownloadManager.getState());

        // wait 2 cycles for files to download
        ThreadUtil.safeSleep(2 * CYCLE_LENGTH);
        announceEvent(4);

//        // files should now be downloaded
//        // check that the file hash has got them, and that they are in the expected place
        Assert.assertTrue(peerEngineClient.containsFileByHash(namePathAndHash(File.VIDEO_1).element3));

        //.isFile(FileUtil.joinPaths(PathConstants.moviesDir(peerEngineClient.getMediaPath()), movie0.getTitle() + "_" + movie0.getId(), videoFile.getName())));
        Assert.assertTrue(FileUtils.getFile(PathConstants.moviesDir(peerEngineClient.getMediaPath()), movie0.getTitle() + "_" + movie0.getId(), videoFile.getName()).isFile());

        ThreadUtil.safeSleep(5000);

        peerEngineClient.removeFavoritePeer(PeerId.buildTestPeerId("2"));
        peerEngineClient.removeFavoritePeer(PeerId.buildTestPeerId("3"));
        peerEngineClient.stop();
    }


    @org.junit.Test
    public void simpleFileDownload2() throws IOException, XMLStreamException {
        String userPath = "./etc/user_1";

        // clear dbs
        DatabaseIO.createNewDatabaseFileStructure(userPath);

        FileUtils.cleanDirectory(new java.io.File("./etc/user_1/media"));
        FileUtils.cleanDirectory(new java.io.File("./etc/user_1/temp"));
        PeerEngineClient peerEngineClient = Client.loadClient(userPath);
        peerEngineClient.clearAllData();
        peerEngineClient.addFavoritePeer(PeerId.buildTestPeerId("1"));
        peerEngineClient.setWishForRegularsConnections(false);
        peerEngineClient.clearFileHashDatabase();
        System.out.println("Client started for peer " + TestUtil.formatPeer(peerEngineClient.getOwnPeerId()));
        String localDB = peerEngineClient.getDatabases().getLocalDB();

        setupDB2(localDB, peerEngineClient);

        // warm up
        ThreadUtil.safeSleep(WARM_UP);

        // generate the shared database
        System.out.println("Manually update the shared database");
        peerEngineClient.getSharedDatabaseGenerator().updateSharedDatabase();

        // connect and wait for synch and downloads
        peerEngineClient.connect();
        peerEngineClient.setVisibleUploadsManagerTimer(2000L);

        // wait 4 cycles
        ThreadUtil.safeSleep(4 * CYCLE_LENGTH);

        System.out.println("Stopping...");
        peerEngineClient.stop();
    }


    private static void announceEvent(int event) {
        System.out.println("-------------");
        System.out.println("EVENT " + event + "!!!");
        System.out.println("-------------");
    }

    private static void startDBAssert(String db, int phase) {
        System.out.println("Checking " + db + " with phase " + phase + "...");
    }

    private static void endDBAssert(String db, int phase) {
        System.out.println(db + " with phase " + phase + " checked!!!");
    }


    private static void setupDB2(String db, PeerEngineClient peerEngineClient) throws IOException {
        // named movie
        Movie movie = new Movie(db, "Rocky");
        movie.setOriginalTitle("Rocky");
        movie.setMinutes(150);
        movie.setYear(2000);
        VideoFile videoFile = new VideoFile(db, namePathAndHash(File.VIDEO_1).element3);
        videoFile.setName(namePathAndHash(File.VIDEO_1).element1);
        movie.addVideoFile(videoFile);
        peerEngineClient.localItemModified(videoFile);
        peerEngineClient.localItemModified(movie);

        addFiles2(peerEngineClient);
    }

    private static void addFiles2(PeerEngineClient peerEngineClient) throws IOException {
        peerEngineClient.addLocalFileFixedPath(namePathAndHash(File.VIDEO_1).element2);
    }


    private static void assertIntegrated(String db, int phase) {
        startDBAssert(db, phase);
        Assert.assertEquals(1, Movie.getMovies(db).size());
        Movie movie1 = Movie.getMovies(db).get(0);

        Assert.assertEquals("Rocky", movie1.getTitle());
        Assert.assertEquals("Rocky", movie1.getOriginalTitle());
        Assert.assertEquals(new Integer(150), movie1.getMinutes());
        Assert.assertEquals(new Integer(2000), movie1.getYear());
        Assert.assertEquals(1, movie1.getVideoFiles().size());
        VideoFile videoFile = movie1.getVideoFiles().get(0);
        Assert.assertEquals(namePathAndHash(File.VIDEO_1).element1, videoFile.getName());
        Assert.assertEquals(namePathAndHash(File.VIDEO_1).element3, videoFile.getHash());

        endDBAssert(db, phase);
    }

}
