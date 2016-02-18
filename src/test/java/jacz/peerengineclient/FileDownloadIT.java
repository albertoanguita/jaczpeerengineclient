package jacz.peerengineclient;

import jacz.database.*;
import jacz.peerengineclient.databases.DatabaseIO;
import jacz.peerengineclient.file_system.Paths;
import jacz.peerengineclient.test.Client;
import jacz.peerengineclient.test.IntegrationTest;
import jacz.peerengineclient.test.TestUtil;
import jacz.peerengineservice.NotAliveException;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.UnavailablePeerException;
import jacz.peerengineservice.util.datatransfer.master.DownloadManager;
import jacz.peerengineservice.util.datatransfer.master.DownloadState;
import jacz.util.concurrency.ThreadUtil;
import jacz.util.files.FileUtil;
import jacz.util.lists.tuple.Triple;
import org.junit.Assert;
import org.junit.experimental.categories.Category;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;

/**
 * Created by Alberto on 12/02/2016.
 * todo if connection is lost before connecting, there is a nasty fast loop, instead of waiting a few seconds for
 * trying to fetch the external address. Same must happen with local address....
 */
@Category(IntegrationTest.class)
public class FileDownloadIT {

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
        SUB_1,
        VIDEO_2,
        SUB_2,
        VIDEO_3,
        SUB_3,
        VIDEO_4,
        VIDEO_5,
        VIDEO_6
    }

    // paths and MD5 hashes of the files above:
    private static Triple<String, String, String> namePathAndHash(File file) {
        String name;
        String path = "./etc/test-files/";
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

            default:
                throw new RuntimeException();
        }
        path += name;
        return new Triple<>(name, path, hash);
    }


    private static final long WARM_UP = 10000;

    private static final long CYCLE_LENGTH = 20000;


    @org.junit.Test
    public void fileDownload1() throws IOException, XMLStreamException, UnavailablePeerException, NotAliveException {

        String userPath = "./etc/user_0";

        // clear dbs
        DatabaseIO.createNewDatabaseFileStructure(userPath);

        PeerEngineClient peerEngineClient = Client.loadClient(userPath);
        peerEngineClient.getFileHashDatabase().clear();
        FileUtil.clearDirectory(peerEngineClient.getMediaPath());
        FileUtil.clearDirectory(peerEngineClient.getTempDownloadsPath());
        // download at 25 kB/s
        peerEngineClient.setMaxDesiredDownloadSpeed(250);
        peerEngineClient.addFriendPeer(PeerID.buildTestPeerID("2"));
        peerEngineClient.addFriendPeer(PeerID.buildTestPeerID("3"));
        String integratedDB = peerEngineClient.getDatabases().getIntegratedDB();
        System.out.println("Client started for peer " + TestUtil.formatPeer(peerEngineClient.getPeerClient().getOwnPeerID()));

        // connect and warm up
        peerEngineClient.connect();
        ThreadUtil.safeSleep(WARM_UP);

        // wait for dbs to synch, test integrated content
        // wait 2 cycles
        ThreadUtil.safeSleep(2 * CYCLE_LENGTH);

        announceEvent(2);
        assertIntegrated(integratedDB, 0);

        // start downloading the first file
        Movie movie0 = Movie.getMovies(integratedDB).get(0);
        VideoFile videoFile = movie0.getVideoFiles().get(0);
        SubtitleFile subtitleFile = videoFile.getSubtitleFiles().get(0);
        DownloadManager vfDownloadManager = peerEngineClient.downloadMediaFile(DownloadInfo.Type.VIDEO_FILE, DatabaseMediator.ItemType.MOVIE, movie0.getId(), null, videoFile.getId(), videoFile.getHash(), videoFile.getName());
        DownloadManager sbDownloadManager = peerEngineClient.downloadMediaFile(DownloadInfo.Type.VIDEO_FILE, DatabaseMediator.ItemType.MOVIE, movie0.getId(), null, subtitleFile.getId(), subtitleFile.getHash(), subtitleFile.getName());

        Assert.assertEquals(DownloadState.RUNNING, vfDownloadManager.getState());
        Assert.assertEquals(DownloadState.RUNNING, sbDownloadManager.getState());

        // start downloading the second file
        TVSeries tvSeries = TVSeries.getTVSeries(integratedDB).get(0);
        Chapter chapter = tvSeries.getChapters().get(0);
        VideoFile videoFile2 = chapter.getVideoFiles().get(0);
        SubtitleFile subtitleFile2 = videoFile2.getSubtitleFiles().get(0);
        DownloadManager vfDownloadManager2 = peerEngineClient.downloadMediaFile(DownloadInfo.Type.VIDEO_FILE, DatabaseMediator.ItemType.CHAPTER, chapter.getId(), tvSeries.getId(), videoFile2.getId(), videoFile2.getHash(), videoFile2.getName());
        DownloadManager sbDownloadManager2 = peerEngineClient.downloadMediaFile(DownloadInfo.Type.VIDEO_FILE, DatabaseMediator.ItemType.CHAPTER, chapter.getId(), tvSeries.getId(), subtitleFile2.getId(), subtitleFile2.getHash(), subtitleFile2.getName());

        Assert.assertEquals(DownloadState.RUNNING, vfDownloadManager2.getState());
        Assert.assertEquals(DownloadState.RUNNING, sbDownloadManager2.getState());

        // file 4
        Movie movie1 = Movie.getMovies(integratedDB).get(1);
        VideoFile videoFile4 = movie1.getVideoFiles().get(0);
        DownloadManager vfDownloadManager3 = peerEngineClient.downloadMediaFile(DownloadInfo.Type.VIDEO_FILE, DatabaseMediator.ItemType.MOVIE, movie1.getId(), null, videoFile4.getId(), videoFile4.getHash(), videoFile4.getName());

        Assert.assertEquals(DownloadState.RUNNING, vfDownloadManager3.getState());

        // file 5, shared
        Movie movie2 = Movie.getMovies(integratedDB).get(2);
        VideoFile videoFile5 = movie2.getVideoFiles().get(0);
        DownloadManager vfDownloadManager5 = peerEngineClient.downloadMediaFile(DownloadInfo.Type.VIDEO_FILE, DatabaseMediator.ItemType.MOVIE, movie2.getId(), null, videoFile5.getId(), videoFile5.getHash(), videoFile5.getName());

        Assert.assertEquals(DownloadState.RUNNING, vfDownloadManager5.getState());
        ThreadUtil.safeSleep(2000);
        Assert.assertEquals(2, vfDownloadManager5.getStatistics().getProviders().size());


        // wait 2 cycles for files to download
        ThreadUtil.safeSleep(2 * CYCLE_LENGTH - 2000);
        announceEvent(4);

//        // files should now be downloaded
//        // check that the file hash has got them, and that they are in the expected place
        Assert.assertTrue(peerEngineClient.getFileHashDatabase().containsKey(namePathAndHash(File.VIDEO_1).element3));
        Assert.assertTrue(peerEngineClient.getFileHashDatabase().containsKey(namePathAndHash(File.SUB_1).element3));
        Assert.assertTrue(peerEngineClient.getFileHashDatabase().containsKey(namePathAndHash(File.VIDEO_2).element3));
        Assert.assertTrue(peerEngineClient.getFileHashDatabase().containsKey(namePathAndHash(File.SUB_2).element3));
        Assert.assertTrue(peerEngineClient.getFileHashDatabase().containsKey(namePathAndHash(File.VIDEO_4).element3));
        Assert.assertTrue(peerEngineClient.getFileHashDatabase().containsKey(namePathAndHash(File.VIDEO_5).element3));

        Assert.assertTrue(FileUtil.isFile(FileUtil.joinPaths(Paths.moviesDir(peerEngineClient.getMediaPath()), movie0.getTitle() + "_" + movie0.getId(), videoFile.getName())));
        Assert.assertTrue(FileUtil.isFile(FileUtil.joinPaths(Paths.moviesDir(peerEngineClient.getMediaPath()), movie0.getTitle() + "_" + movie0.getId(), subtitleFile.getName())));
        Assert.assertTrue(FileUtil.isFile(FileUtil.joinPaths(Paths.seriesDir(peerEngineClient.getMediaPath()), tvSeries.getTitle() + "_" + tvSeries.getId(), chapter.getTitle() + "_" + chapter.getId(), videoFile2.getName())));
        Assert.assertTrue(FileUtil.isFile(FileUtil.joinPaths(Paths.seriesDir(peerEngineClient.getMediaPath()), tvSeries.getTitle() + "_" + tvSeries.getId(), chapter.getTitle() + "_" + chapter.getId(), subtitleFile2.getName())));
        Assert.assertTrue(FileUtil.isFile(FileUtil.joinPaths(Paths.moviesDir(peerEngineClient.getMediaPath()), "untitled_item" + "_" + movie1.getId(), videoFile4.getName())));
        Assert.assertTrue(FileUtil.isFile(FileUtil.joinPaths(Paths.moviesDir(peerEngineClient.getMediaPath()), movie2.getTitle() + "_" + movie2.getId(), videoFile5.getName())));



        ThreadUtil.safeSleep(5000);

        peerEngineClient.removeFriendPeer(PeerID.buildTestPeerID("2"));
        peerEngineClient.removeFriendPeer(PeerID.buildTestPeerID("3"));
        peerEngineClient.stop();
    }


    @org.junit.Test
    public void fileDownload2() throws IOException, XMLStreamException {
        String userPath = "./etc/user_1";

        // clear dbs
        DatabaseIO.createNewDatabaseFileStructure(userPath);

        PeerEngineClient peerEngineClient = Client.loadClient(userPath);
        peerEngineClient.getFileHashDatabase().clear();
        FileUtil.clearDirectory(peerEngineClient.getMediaPath());
        System.out.println("Client started for peer " + TestUtil.formatPeer(peerEngineClient.getPeerClient().getOwnPeerID()));
        peerEngineClient.addFriendPeer(PeerID.buildTestPeerID("1"));
        String localDB = peerEngineClient.getDatabases().getLocalDB();

        setupDB2(localDB, peerEngineClient);

        // warm up
        ThreadUtil.safeSleep(WARM_UP);

        // generate the shared database
        peerEngineClient.getSharedDatabaseGenerator().updateSharedDatabase();

        // connect and wait for synch and downloads
        announceEvent(0);
        peerEngineClient.connect();

        // wait 4 cycles
        ThreadUtil.safeSleep(4 * CYCLE_LENGTH);

        announceEvent(4);

        peerEngineClient.removeFriendPeer(PeerID.buildTestPeerID("1"));
        peerEngineClient.stop();
    }

    @org.junit.Test
    public void fileDownload3() throws IOException, XMLStreamException {
        String userPath = "./etc/user_2";

        // clear dbs
        DatabaseIO.createNewDatabaseFileStructure(userPath);

        PeerEngineClient peerEngineClient = Client.loadClient(userPath);
        peerEngineClient.getFileHashDatabase().clear();
        FileUtil.clearDirectory(peerEngineClient.getMediaPath());
        System.out.println("Client started for peer " + TestUtil.formatPeer(peerEngineClient.getPeerClient().getOwnPeerID()));
        peerEngineClient.addFriendPeer(PeerID.buildTestPeerID("1"));
        String localDB = peerEngineClient.getDatabases().getLocalDB();

        setupDB3(localDB, peerEngineClient);

        // warm up
        ThreadUtil.safeSleep(WARM_UP);

        // generate the shared database
        peerEngineClient.getSharedDatabaseGenerator().updateSharedDatabase();

        // wait 1 cycle
        ThreadUtil.safeSleep(CYCLE_LENGTH);

        // connect and wait for synch and downloads
        announceEvent(1);
        peerEngineClient.connect();

        // wait 3 cycles
        ThreadUtil.safeSleep(3 * CYCLE_LENGTH);

        announceEvent(4);

        peerEngineClient.removeFriendPeer(PeerID.buildTestPeerID("1"));
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
        SubtitleFile subtitleFile = new SubtitleFile(db, namePathAndHash(File.SUB_1).element3);
        subtitleFile.setName(namePathAndHash(File.SUB_1).element1);
        videoFile.addSubtitleFile(subtitleFile);
        movie.addVideoFile(videoFile);
        peerEngineClient.localItemModified(subtitleFile);
        peerEngineClient.localItemModified(videoFile);
        peerEngineClient.localItemModified(movie);

        // named chapter with tv series
        Chapter chapter1 = new Chapter(db, "Friends 1");
        videoFile = new VideoFile(db, namePathAndHash(File.VIDEO_2).element3);
        videoFile.setName(namePathAndHash(File.VIDEO_2).element1);
        subtitleFile = new SubtitleFile(db, namePathAndHash(File.SUB_2).element3);
        subtitleFile.setName(namePathAndHash(File.SUB_2).element1);
        videoFile.addSubtitleFile(subtitleFile);
        chapter1.addVideoFile(videoFile);
        TVSeries tvSeries = new TVSeries(db, "Friends");
        tvSeries.addChapter(chapter1);
        peerEngineClient.localItemModified(subtitleFile);
        peerEngineClient.localItemModified(videoFile);
        peerEngineClient.localItemModified(chapter1);
        peerEngineClient.localItemModified(tvSeries);

        // named chapter without tv series
        Chapter chapter2 = new Chapter(db, "Breaking bad 1");
        videoFile = new VideoFile(db, namePathAndHash(File.VIDEO_3).element3);
        videoFile.setName(namePathAndHash(File.VIDEO_3).element1);
        subtitleFile = new SubtitleFile(db, namePathAndHash(File.SUB_3).element3);
        subtitleFile.setName(namePathAndHash(File.SUB_3).element1);
        videoFile.addSubtitleFile(subtitleFile);
        chapter2.addVideoFile(videoFile);
        peerEngineClient.localItemModified(subtitleFile);
        peerEngineClient.localItemModified(videoFile);
        peerEngineClient.localItemModified(chapter2);

        // unnamed movie (blank title!!)
        movie = new Movie(db, "");
        videoFile = new VideoFile(db, namePathAndHash(File.VIDEO_4).element3);
        videoFile.setName(namePathAndHash(File.VIDEO_4).element1);
        movie.addVideoFile(videoFile);
        peerEngineClient.localItemModified(videoFile);
        peerEngineClient.localItemModified(movie);

        // named movie, shared with other peers
        movie = new Movie(db, "Alien");
        movie.setOriginalTitle("Alien");
        movie.setMinutes(100);
        movie.setYear(1990);
        videoFile = new VideoFile(db, namePathAndHash(File.VIDEO_5).element3);
        videoFile.setName(namePathAndHash(File.VIDEO_5).element1);
        movie.addVideoFile(videoFile);
        peerEngineClient.localItemModified(videoFile);
        peerEngineClient.localItemModified(movie);

        addFiles2(peerEngineClient);
    }

    private static void addFiles2(PeerEngineClient peerEngineClient) throws IOException {
        peerEngineClient.addLocalFileFixedPath(namePathAndHash(File.VIDEO_1).element2);
        peerEngineClient.addLocalFileFixedPath(namePathAndHash(File.SUB_1).element2);
        peerEngineClient.addLocalFileFixedPath(namePathAndHash(File.VIDEO_2).element2);
        peerEngineClient.addLocalFileFixedPath(namePathAndHash(File.SUB_2).element2);
        peerEngineClient.addLocalFileFixedPath(namePathAndHash(File.VIDEO_3).element2);
        peerEngineClient.addLocalFileFixedPath(namePathAndHash(File.SUB_3).element2);
        peerEngineClient.addLocalFileFixedPath(namePathAndHash(File.VIDEO_4).element2);
        peerEngineClient.addLocalFileFixedPath(namePathAndHash(File.VIDEO_5).element2);
    }

    private static void setupDB3(String db, PeerEngineClient peerEngineClient) throws IOException {
        // named movie, shared with other peers
        Movie movie = new Movie(db, "Alien");
        movie.setOriginalTitle("Alien");
        movie.setMinutes(100);
        movie.setYear(1990);
        VideoFile videoFile = new VideoFile(db, namePathAndHash(File.VIDEO_5).element3);
        videoFile.setName(namePathAndHash(File.VIDEO_5).element1);
        movie.addVideoFile(videoFile);
        peerEngineClient.localItemModified(videoFile);
        peerEngineClient.localItemModified(movie);

        addFiles3(peerEngineClient);
    }

    private static void addFiles3(PeerEngineClient peerEngineClient) throws IOException {
        peerEngineClient.addLocalFileFixedPath(namePathAndHash(File.VIDEO_5).element2);
    }

    private static void assertIntegrated(String db, int phase) {
        startDBAssert(db, phase);
        Assert.assertEquals(3, Movie.getMovies(db).size());
        Movie movie1 = Movie.getMovies(db).get(0);
        Movie movie2 = Movie.getMovies(db).get(1);
        Movie movie3 = Movie.getMovies(db).get(2);
        Assert.assertEquals(1, Chapter.getChapters(db).size());
        Chapter chapter1 = Chapter.getChapters(db).get(0);
        Assert.assertEquals(1, TVSeries.getTVSeries(db).size());
        TVSeries tvSeries = TVSeries.getTVSeries(db).get(0);

        Assert.assertEquals("Rocky", movie1.getTitle());
        Assert.assertEquals("Rocky", movie1.getOriginalTitle());
        Assert.assertEquals(new Integer(150), movie1.getMinutes());
        Assert.assertEquals(new Integer(2000), movie1.getYear());
        Assert.assertEquals(1, movie1.getVideoFiles().size());
        VideoFile videoFile = movie1.getVideoFiles().get(0);
        Assert.assertEquals(namePathAndHash(File.VIDEO_1).element1, videoFile.getName());
        Assert.assertEquals(namePathAndHash(File.VIDEO_1).element3, videoFile.getHash());
        Assert.assertEquals(1, videoFile.getSubtitleFiles().size());
        SubtitleFile subtitleFile = videoFile.getSubtitleFiles().get(0);
        Assert.assertEquals(namePathAndHash(File.SUB_1).element1, subtitleFile.getName());
        Assert.assertEquals(namePathAndHash(File.SUB_1).element3, subtitleFile.getHash());


        Assert.assertEquals("Friends 1", chapter1.getTitle());
        Assert.assertEquals(1, chapter1.getVideoFiles().size());
        videoFile = chapter1.getVideoFiles().get(0);
        Assert.assertEquals(namePathAndHash(File.VIDEO_2).element1, videoFile.getName());
        Assert.assertEquals(namePathAndHash(File.VIDEO_2).element3, videoFile.getHash());
        Assert.assertEquals(1, videoFile.getSubtitleFiles().size());
        subtitleFile = videoFile.getSubtitleFiles().get(0);
        Assert.assertEquals(namePathAndHash(File.SUB_2).element1, subtitleFile.getName());
        Assert.assertEquals(namePathAndHash(File.SUB_2).element3, subtitleFile.getHash());
        Assert.assertEquals("Friends", tvSeries.getTitle());
        Assert.assertEquals(1, tvSeries.getChapters(db).size());
        Assert.assertEquals("Friends 1", tvSeries.getChapters(db).get(0).getTitle());
        Assert.assertEquals(1, chapter1.getTVSeries().size());
        Assert.assertEquals("Friends", chapter1.getTVSeries().get(0).getTitle());

        Assert.assertEquals("", movie2.getTitle());
        Assert.assertEquals(null, movie2.getOriginalTitle());
        Assert.assertEquals(1, movie2.getVideoFiles().size());
        videoFile = movie2.getVideoFiles().get(0);
        Assert.assertEquals(namePathAndHash(File.VIDEO_4).element1, videoFile.getName());
        Assert.assertEquals(namePathAndHash(File.VIDEO_4).element3, videoFile.getHash());
        Assert.assertEquals(0, videoFile.getSubtitleFiles().size());

        Assert.assertEquals("Alien", movie3.getTitle());
        Assert.assertEquals("Alien", movie3.getOriginalTitle());
        Assert.assertEquals(1, movie3.getVideoFiles().size());
        videoFile = movie3.getVideoFiles().get(0);
        Assert.assertEquals(namePathAndHash(File.VIDEO_5).element1, videoFile.getName());
        Assert.assertEquals(namePathAndHash(File.VIDEO_5).element3, videoFile.getHash());
        Assert.assertEquals(0, videoFile.getSubtitleFiles().size());

        endDBAssert(db, phase);
    }

}
