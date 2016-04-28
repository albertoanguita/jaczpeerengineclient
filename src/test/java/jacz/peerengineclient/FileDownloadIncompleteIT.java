package jacz.peerengineclient;

import jacz.database.DatabaseMediator;
import jacz.database.Movie;
import jacz.database.VideoFile;
import jacz.peerengineclient.databases.DatabaseIO;
import jacz.peerengineclient.file_system.Paths;
import jacz.peerengineclient.test.Client;
import jacz.peerengineclient.test.TestUtil;
import jacz.peerengineservice.NotAliveException;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.UnavailablePeerException;
import jacz.peerengineservice.util.datatransfer.master.DownloadManager;
import jacz.peerengineservice.util.datatransfer.master.DownloadState;
import jacz.util.concurrency.ThreadUtil;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;

/**
 * Created by Alberto on 15/02/2016.
 */
public class FileDownloadIncompleteIT {


    // for this test we setup timed cycles that allow dbs to synch and to be tested
    // each cycle lasts 15 seconds, and all users have a 10 second warm up
    // the cycles divide the events. Event 0 takes place right after the warm up. Event 1, 15 seconds later...
    // user 1 will test his data at even events. Users 2 and 3 will modify their data at odd events.

    // we download a video file from peer 2. Peer 2 is actually downloading it from peer 3.
    // peer 2 starts downloading the file at start, but he will not share it until 60 seconds later.
    // at that moment peer 1 synchs it, and starts downloading it.
    // both peers use a speed of 10kB/s, so they should take about 1:40 mins to download


    private static String fileName() {
        return "file1.wmv";
    }

    private static String filePath() {
        return "./etc/test-files/file1.wmv";
    }

    private static String fileHash() {
        return "f9a7c648b0c0f240c19ff1e92db11128".toUpperCase();
    }



    private static final long WARM_UP = 10000;

    private static final long CYCLE_LENGTH = 15000;


    @org.junit.Test
    public void fileDownloadIncomplete1() throws IOException, XMLStreamException, UnavailablePeerException, NotAliveException {

        String userPath = "./etc/user_0";

        // clear dbs
        DatabaseIO.createNewDatabaseFileStructure(userPath);

        PeerEngineClient peerEngineClient = Client.loadClient(userPath);
        peerEngineClient.getFileHashDatabase().clear();
        FileUtils.cleanDirectory(new File(peerEngineClient.getMediaPath()));
        FileUtils.cleanDirectory(new File(peerEngineClient.getTempDownloadsPath()));
        // download at 40 kB/s
        peerEngineClient.setMaxDownloadSpeed(40);
        peerEngineClient.addFavoritePeer(PeerId.buildTestPeerId("2"));
        String integratedDB = peerEngineClient.getDatabases().getIntegratedDB();
        System.out.println("Client started for peer " + TestUtil.formatPeer(peerEngineClient.getPeerClient().getOwnPeerId()));

        // connect and warm up
        peerEngineClient.connect();
        ThreadUtil.safeSleep(WARM_UP);

        // wait for dbs to synch, test integrated content
        // wait 5 cycles
        ThreadUtil.safeSleep(5 * CYCLE_LENGTH);

        announceEvent(5);
        assertIntegrated(integratedDB);
        peerEngineClient.getPeerClient().setVisibleDownloadsTimer(3000);

        // start downloading the file
        Movie movie = Movie.getMovies(integratedDB).get(0);
        VideoFile videoFile = movie.getVideoFiles().get(0);
        DownloadManager vfDownloadManager = peerEngineClient.downloadMediaFile(DownloadInfo.Type.VIDEO_FILE, DatabaseMediator.ItemType.MOVIE, movie.getId(), null, videoFile.getId(), videoFile.getHash(), videoFile.getName());

        Assert.assertEquals(DownloadState.RUNNING, vfDownloadManager.getState());

        // wait 10 cycles for files to download
        ThreadUtil.safeSleep(10 * CYCLE_LENGTH);
        announceEvent(15);

        Assert.assertTrue(peerEngineClient.getFileHashDatabase().containsKey(fileHash()));
        Assert.assertTrue(FileUtils.getFile(Paths.moviesDir(peerEngineClient.getMediaPath()), movie.getTitle() + "_" + movie.getId(), videoFile.getName()).isFile());

        ThreadUtil.safeSleep(5000);

        peerEngineClient.removeFavoritePeer(PeerId.buildTestPeerId("2"));
        peerEngineClient.removeFavoritePeer(PeerId.buildTestPeerId("3"));
        ThreadUtil.safeSleep(1000);
        peerEngineClient.stop();
    }


    @org.junit.Test
    public void fileDownloadIncomplete2() throws IOException, XMLStreamException, NotAliveException {
        String userPath = "./etc/user_1";

        // clear dbs
        DatabaseIO.createNewDatabaseFileStructure(userPath);

        PeerEngineClient peerEngineClient = Client.loadClient(userPath);
        peerEngineClient.getFileHashDatabase().clear();
        FileUtils.cleanDirectory(new File(peerEngineClient.getMediaPath()));
        FileUtils.cleanDirectory(new File(peerEngineClient.getTempDownloadsPath()));
        // download at 10 kB/s
        peerEngineClient.setMaxDownloadSpeed(10);
        System.out.println("Client started for peer " + TestUtil.formatPeer(peerEngineClient.getPeerClient().getOwnPeerId()));
        peerEngineClient.addFavoritePeer(PeerId.buildTestPeerId("1"));
        peerEngineClient.addFavoritePeer(PeerId.buildTestPeerId("3"));
        String integratedDB = peerEngineClient.getDatabases().getIntegratedDB();

        // connect and warm up
        System.out.println("Connecting...");
        peerEngineClient.connect();
        ThreadUtil.safeSleep(WARM_UP);

        // wait for synch and start downloading
        announceEvent(0);

        // wait 1 cycles
        ThreadUtil.safeSleep(CYCLE_LENGTH);

        announceEvent(1);
        assertIntegrated(integratedDB);

        // start downloading the file
        Movie movie = Movie.getMovies(integratedDB).get(0);
        VideoFile videoFile = movie.getVideoFiles().get(0);
        DownloadManager vfDownloadManager = peerEngineClient.downloadMediaFile(DownloadInfo.Type.VIDEO_FILE, DatabaseMediator.ItemType.MOVIE, movie.getId(), null, videoFile.getId(), videoFile.getHash(), videoFile.getName());

        Assert.assertEquals(DownloadState.RUNNING, vfDownloadManager.getState());

        // wait 9 cycle
        ThreadUtil.safeSleep(9 * CYCLE_LENGTH);

        announceEvent(10);

        Assert.assertTrue(peerEngineClient.getFileHashDatabase().containsKey(fileHash()));
        Assert.assertTrue(FileUtils.getFile(Paths.moviesDir(peerEngineClient.getMediaPath()), movie.getTitle() + "_" + movie.getId(), videoFile.getName()).isFile());

        // wait 5 cycle
        ThreadUtil.safeSleep(5 * CYCLE_LENGTH);
        announceEvent(15);

        ThreadUtil.safeSleep(CYCLE_LENGTH);

        peerEngineClient.removeFavoritePeer(PeerId.buildTestPeerId("1"));
        peerEngineClient.removeFavoritePeer(PeerId.buildTestPeerId("3"));
        ThreadUtil.safeSleep(1000);
        peerEngineClient.stop();
    }

    @org.junit.Test
    public void fileDownloadIncomplete3() throws IOException, XMLStreamException {
        String userPath = "./etc/user_2";

        // clear dbs
        DatabaseIO.createNewDatabaseFileStructure(userPath);

        PeerEngineClient peerEngineClient = Client.loadClient(userPath);
        peerEngineClient.getFileHashDatabase().clear();
        FileUtils.cleanDirectory(new File(peerEngineClient.getMediaPath()));
        System.out.println("Client started for peer " + TestUtil.formatPeer(peerEngineClient.getPeerClient().getOwnPeerId()));
        peerEngineClient.addFavoritePeer(PeerId.buildTestPeerId("2"));
        String localDB = peerEngineClient.getDatabases().getLocalDB();

        setupDB3(localDB, peerEngineClient);
        // generate the shared database
        peerEngineClient.getSharedDatabaseGenerator().updateSharedDatabase();

        // connect and warm up
        System.out.println("Connecting...");
        peerEngineClient.connect();
        ThreadUtil.safeSleep(WARM_UP);


        // connect and wait for synch and downloads
        announceEvent(0);

        // wait 10 cycle
        ThreadUtil.safeSleep(10 * CYCLE_LENGTH);

        announceEvent(10);

        peerEngineClient.removeFavoritePeer(PeerId.buildTestPeerId("2"));
        ThreadUtil.safeSleep(1000);
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


    private static void assertIntegrated(String db) {
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
