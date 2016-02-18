package jacz.peerengineclient;

import com.neovisionaries.i18n.CountryCode;
import com.neovisionaries.i18n.LanguageCode;
import jacz.database.*;
import jacz.database.util.GenreCode;
import jacz.database.util.ImageHash;
import jacz.database.util.QualityCode;
import jacz.peerengineclient.databases.DatabaseIO;
import jacz.peerengineclient.test.Client;
import jacz.peerengineclient.test.IntegrationTest;
import jacz.peerengineclient.test.TestUtil;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.UnavailablePeerException;
import jacz.util.concurrency.ThreadUtil;
import junitx.framework.ListAssert;
import org.junit.Assert;
import org.junit.experimental.categories.Category;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This test performs a synch with two other peers and integrates their data to create the integrated database.
 * After that, two files are downloaded, so the shared database is  built. Finally, one of the files is deleted.
 */
@Category(IntegrationTest.class)
public class SynchAndIntegrateIT {

    // for this test we setup timed cycles that allow dbs to synch and to be tested
    // each cycle lasts 15 seconds, and all users have a 10 second warm up
    // the cycles divide the events. Event 0 takes place right after the warm up. Event 1, 15 seconds later...
    // user 1 will test his data at even events. Users 2 and 3 will modify their data at odd events.

    private static final long WARM_UP = 10000;

    private static final long CYCLE_LENGTH = 20000;

    @org.junit.Test
    public void synchAndIntegrate1() throws IOException, XMLStreamException, UnavailablePeerException {

        String userPath = "./etc/user_0";

        // clear dbs
        DatabaseIO.createNewDatabaseFileStructure(userPath);

        PeerEngineClient peerEngineClient = Client.loadClient(userPath);
        peerEngineClient.addFriendPeer(PeerID.buildTestPeerID("2"));
        peerEngineClient.addFriendPeer(PeerID.buildTestPeerID("3"));
        String localDB = peerEngineClient.getDatabases().getLocalDB();
        String integratedDB = peerEngineClient.getDatabases().getIntegratedDB();
        System.out.println("Client started for peer " + TestUtil.formatPeer(peerEngineClient.getPeerClient().getOwnPeerID()));

        setupLocal(localDB, peerEngineClient);
        // connect and warm up
        peerEngineClient.connect();
        ThreadUtil.safeSleep(WARM_UP);

        announceEvent(0);
//        setupLocal(localDB, peerEngineClient);
        assertLocal(localDB);
        assertIntegrated(integratedDB, 0);

        // wait for dbs to synch, test their contents (remote-2 and integrated)
        // wait 2 cycles
        ThreadUtil.safeSleep(2 * CYCLE_LENGTH);

        announceEvent(2);
        assertDB2(peerEngineClient.getDatabases().getRemoteDB(PeerID.buildTestPeerID("2")), 0);
        assertIntegrated(integratedDB, 1);

        // wait 2 cycles
        ThreadUtil.safeSleep(2 * CYCLE_LENGTH);

        announceEvent(4);
        assertDB2(peerEngineClient.getDatabases().getRemoteDB(PeerID.buildTestPeerID("2")), 1);
        assertIntegrated(integratedDB, 2);

        // wait 2 cycles
        ThreadUtil.safeSleep(2 * CYCLE_LENGTH);

        announceEvent(6);
        assertDB3(peerEngineClient.getDatabases().getRemoteDB(PeerID.buildTestPeerID("3")), 0);
        assertIntegrated(integratedDB, 3);

        // wait 2 cycles
        ThreadUtil.safeSleep(2 * CYCLE_LENGTH);

        announceEvent(8);
        assertDB3(peerEngineClient.getDatabases().getRemoteDB(PeerID.buildTestPeerID("3")), 1);
        assertIntegrated(integratedDB, 4);

        ThreadUtil.safeSleep(5000);


        peerEngineClient.removeFriendPeer(PeerID.buildTestPeerID("2"));
        peerEngineClient.removeFriendPeer(PeerID.buildTestPeerID("3"));
        peerEngineClient.stop();
    }

    @org.junit.Test
    public void synchAndIntegrate2() throws IOException, XMLStreamException {
        String userPath = "./etc/user_1";

        // clear dbs
        DatabaseIO.createNewDatabaseFileStructure(userPath);

        PeerEngineClient peerEngineClient = Client.loadClient(userPath);
        System.out.println("Client started for peer " + TestUtil.formatPeer(peerEngineClient.getPeerClient().getOwnPeerID()));
        peerEngineClient.addFriendPeer(PeerID.buildTestPeerID("1"));

        String sharedDB = peerEngineClient.getDatabases().getSharedDB();
        setupDB2(sharedDB);

        // warm up
        ThreadUtil.safeSleep(WARM_UP);

        // wait 1 cycle
        ThreadUtil.safeSleep(CYCLE_LENGTH);

        // connect and wait for synch
        announceEvent(1);
        peerEngineClient.connect();

        // wait 2 cycles
        ThreadUtil.safeSleep(2 * CYCLE_LENGTH);

        // updated db
        announceEvent(3);
        updateDB2(sharedDB);

        // wait 1 cycle
        ThreadUtil.safeSleep(CYCLE_LENGTH);

        announceEvent(4);

        peerEngineClient.removeFriendPeer(PeerID.buildTestPeerID("1"));
        peerEngineClient.stop();
    }

    @org.junit.Test
    public void synchAndIntegrate3() throws IOException, XMLStreamException {
        String userPath = "./etc/user_2";

        // clear dbs
        DatabaseIO.createNewDatabaseFileStructure(userPath);

        PeerEngineClient peerEngineClient = Client.loadClient(userPath);
        System.out.println("Client started for peer " + TestUtil.formatPeer(peerEngineClient.getPeerClient().getOwnPeerID()));
        peerEngineClient.addFriendPeer(PeerID.buildTestPeerID("1"));

        String sharedDB = peerEngineClient.getDatabases().getSharedDB();
        setupDB3(sharedDB);

        // warm up
        ThreadUtil.safeSleep(WARM_UP);

        // wait 5 cycles
        ThreadUtil.safeSleep(5 * CYCLE_LENGTH);

        // connect and wait for synch
        announceEvent(5);
        peerEngineClient.connect();

        // wait 2 cycles
        ThreadUtil.safeSleep(2 * CYCLE_LENGTH);

        announceEvent(7);
        updateDB3(sharedDB);

        // wait 1 cycle
        ThreadUtil.safeSleep(CYCLE_LENGTH);

        announceEvent(8);

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

    private static void setupLocal(String db, PeerEngineClient peerEngineClient) {
        Movie movie = new Movie(db, "The lord of the rings");
        peerEngineClient.localItemModified(movie);

        TVSeries tvSeries = new TVSeries(db, "Bottom");
        peerEngineClient.localItemModified(tvSeries);

        Chapter chapter = new Chapter(db, "Day out");
        peerEngineClient.localItemModified(chapter);
    }

    private static void assertLocal(String db) {
        startDBAssert(db, 0);
        Assert.assertEquals(1, Movie.getMovies(db).size());
        Assert.assertEquals("The lord of the rings", Movie.getMovies(db).get(0).getTitle());

        Assert.assertEquals(1, TVSeries.getTVSeries(db).size());
        Assert.assertEquals("Bottom", TVSeries.getTVSeries(db).get(0).getTitle());

        Assert.assertEquals(1, Chapter.getChapters(db).size());
        Assert.assertEquals("Day out", Chapter.getChapters(db).get(0).getTitle());
        endDBAssert(db, 0);
    }

    private static void setupDB2(String db) {
        Movie movie = new Movie(db, "Avatar");
        movie.setMinutes(150);
        Person person = new Person(db, "actor 1");
        person.addAlias("actor 1 1");
        movie.addActor(person);
        person = new Person(db, "actor 2");
        movie.addActor(person);
        person = new Person(db, "director 1");
        movie.addCreator(person);
        Company company = new Company(db, "Disney");
        company.addAlias("Disney inc");
        movie.addProductionCompany(company);
        movie.setSynopsis("Marines in a remote planet");
        movie.setYear(2008);
        movie.setOriginalTitle("Avatar orig");
        movie.addCountry(CountryCode.AC);
        movie.setImageHash(new ImageHash("abcd", "jpg"));
        movie.addExternalURL("external URL 1");
        VideoFile videoFile = new VideoFile(db, "abcdef");
        videoFile.setName("video file 1");
        videoFile.setQuality(QualityCode.HD);
        videoFile.setResolution(720);
        videoFile.addAdditionalSource("torrent 1");
        videoFile.setMinutes(155);
        videoFile.setLength(2048L);
        videoFile.addLanguage(LanguageCode.aa);
        SubtitleFile subtitleFile = new SubtitleFile(db, "qqqwww");
        subtitleFile.setLength(1024L);
        subtitleFile.setName("sub 1");
        subtitleFile.addLanguage(LanguageCode.ab);
        subtitleFile.addAdditionalSource("torrent 2");
        videoFile.addSubtitleFile(subtitleFile);
        movie.addVideoFile(videoFile);

        movie = new Movie(db, "Star wars");
        movie.setOriginalTitle("Star wars");
        movie.addGenre(GenreCode.ADVENTURE);
        movie = new Movie(db, "The goonies");
        movie.setOriginalTitle("The goonies orig");
        movie.setYear(1996);

        Chapter chapter1 = new Chapter(db, "Friends 1");
        chapter1.setYear(1989);
        chapter1.setSeason("s01");
        chapter1.setMinutes(45);
        videoFile = new VideoFile(db, "abcdefgh");
        videoFile.setName("vf");
        chapter1.addVideoFile(videoFile);

        Chapter chapter2 = new Chapter(db, "Friends 2");
        chapter2.setMinutes(28);
        Chapter chapter3 = new Chapter(db, "Breaking bad 1");
        Chapter chapter4 = new Chapter(db, "Breaking bad 2");

        TVSeries tvSeries = new TVSeries(db, "Friends");
        tvSeries.addChapter(chapter1);
        tvSeries.addChapter(chapter2);
        tvSeries = new TVSeries(db, "Breaking bad");
        tvSeries.addChapter(chapter3);
        tvSeries.addChapter(chapter4);
    }

    private static void updateDB2(String db) {
        Movie movie = Movie.getMovies(db).get(1);
        movie.addGenre(GenreCode.COMEDY);
        movie.addGenre(GenreCode.DRAMA);
    }

    private static void assertDB2(String db, int phase) {
        startDBAssert(db, phase);
        Assert.assertEquals(3, Movie.getMovies(db).size());
        Movie movie1 = Movie.getMovies(db).get(0);
        Movie movie2 = Movie.getMovies(db).get(1);
        Movie movie3 = Movie.getMovies(db).get(2);
        Assert.assertEquals("Avatar", movie1.getTitle());
        Assert.assertEquals(new Integer(150), movie1.getMinutes());
        Assert.assertEquals(2, movie1.getActors().size());
        Person actor1 = movie1.getActors().get(0);
        Person actor2 = movie1.getActors().get(1);
        Assert.assertEquals("actor 1", actor1.getName());
        Assert.assertEquals(1, actor1.getAliases().size());
        Assert.assertEquals("actor 1 1", actor1.getAliases().get(0));
        Assert.assertEquals("actor 2", actor2.getName());
        Assert.assertEquals(0, actor2.getAliases().size());
        Assert.assertEquals(1, movie1.getCreators().size());
        Person creator = movie1.getCreators().get(0);
        Assert.assertEquals("director 1", creator.getName());
        Assert.assertEquals(0, creator.getAliases().size());
        Assert.assertEquals(1, movie1.getProductionCompanies().size());
        Company company = movie1.getProductionCompanies().get(0);
        Assert.assertEquals("Disney", company.getName());
        Assert.assertEquals(1, company.getAliases().size());
        Assert.assertEquals("Disney inc", company.getAliases().get(0));
        Assert.assertEquals("Marines in a remote planet", movie1.getSynopsis());
        Assert.assertEquals(new Integer(2008), movie1.getYear());
        Assert.assertEquals("Avatar orig", movie1.getOriginalTitle());
        List<CountryCode> countries = new ArrayList<>();
        countries.add(CountryCode.AC);
        ListAssert.assertEquals(countries, movie1.getCountries());
        Assert.assertEquals(new ImageHash("abcd", "jpg"), movie1.getImageHash());
        Assert.assertEquals(1, movie1.getExternalURLs().size());
        Assert.assertEquals("external URL 1", movie1.getExternalURLs().get(0));
        Assert.assertEquals(1, movie1.getVideoFiles().size());
        VideoFile videoFile = movie1.getVideoFiles().get(0);
        Assert.assertEquals("abcdef", videoFile.getHash());
        Assert.assertEquals(QualityCode.HD, videoFile.getQuality());
        Assert.assertEquals(new Integer(720), videoFile.getResolution());
        Assert.assertEquals("video file 1", videoFile.getName());
        Assert.assertEquals(1, videoFile.getAdditionalSources().size());
        Assert.assertEquals("torrent 1", videoFile.getAdditionalSources().get(0));
        Assert.assertEquals(new Integer(155), videoFile.getMinutes());
        Assert.assertEquals(new Long(2048L), videoFile.getLength());
        Assert.assertEquals(1, videoFile.getLanguages().size());
        Assert.assertEquals(LanguageCode.aa, videoFile.getLanguages().get(0));
        Assert.assertEquals(1, videoFile.getSubtitleFiles().size());
        SubtitleFile subtitleFile = videoFile.getSubtitleFiles().get(0);
        Assert.assertEquals(new Long(1024L), subtitleFile.getLength());
        Assert.assertEquals("sub 1", subtitleFile.getName());
        Assert.assertEquals("qqqwww", subtitleFile.getHash());
        Assert.assertEquals(1, subtitleFile.getLanguages().size());
        Assert.assertEquals(LanguageCode.ab, subtitleFile.getLanguages().get(0));
        Assert.assertEquals(1, subtitleFile.getAdditionalSources().size());
        Assert.assertEquals("torrent 2", subtitleFile.getAdditionalSources().get(0));


        Assert.assertEquals("Star wars", movie2.getTitle());
        Assert.assertEquals("Star wars", movie2.getOriginalTitle());
        List<GenreCode> genres = new ArrayList<>();
        if (phase == 0) {
            genres.add(GenreCode.ADVENTURE);
        } else {
            genres.add(GenreCode.ADVENTURE);
            genres.add(GenreCode.COMEDY);
            genres.add(GenreCode.DRAMA);
        }
        ListAssert.assertEquals(genres, movie2.getGenres());
        Assert.assertEquals("The goonies", movie3.getTitle());
        Assert.assertEquals("The goonies orig", movie3.getOriginalTitle());
        Assert.assertEquals(new Integer(1996), movie3.getYear());

        Assert.assertEquals(4, Chapter.getChapters(db).size());
        Chapter chapter1 = Chapter.getChapters(db).get(0);
        Chapter chapter2 = Chapter.getChapters(db).get(1);
        Chapter chapter3 = Chapter.getChapters(db).get(2);
        Chapter chapter4 = Chapter.getChapters(db).get(3);
        Assert.assertEquals("Friends 1", chapter1.getTitle());
        Assert.assertEquals(new Integer(1989), chapter1.getYear());
        Assert.assertEquals("s01", chapter1.getSeason());
        Assert.assertEquals(new Integer(45), chapter1.getMinutes());
        Assert.assertEquals(1, chapter1.getVideoFiles().size());
        videoFile = chapter1.getVideoFiles().get(0);
        Assert.assertEquals("abcdefgh", videoFile.getHash());

        Assert.assertEquals("Friends 2", chapter2.getTitle());
        Assert.assertEquals(new Integer(28), chapter2.getMinutes());
        Assert.assertEquals("Breaking bad 1", chapter3.getTitle());
        Assert.assertEquals("Breaking bad 2", chapter4.getTitle());

        Assert.assertEquals(2, TVSeries.getTVSeries(db).size());
        TVSeries tvSeries1 = TVSeries.getTVSeries(db).get(0);
        TVSeries tvSeries2 = TVSeries.getTVSeries(db).get(1);
        Assert.assertEquals("Friends", tvSeries1.getTitle());
        Assert.assertEquals(2, tvSeries1.getChapters(db).size());
        Assert.assertEquals("Friends 1", tvSeries1.getChapters(db).get(0).getTitle());
        Assert.assertEquals("Friends 2", tvSeries1.getChapters(db).get(1).getTitle());
        Assert.assertEquals("Breaking bad", tvSeries2.getTitle());
        Assert.assertEquals(2, tvSeries2.getChapters(db).size());
        Assert.assertEquals("Breaking bad 1", tvSeries2.getChapters(db).get(0).getTitle());
        Assert.assertEquals("Breaking bad 2", tvSeries2.getChapters(db).get(1).getTitle());
        endDBAssert(db, phase);
    }

    private static void setupDB3(String db) {
        Movie movie = new Movie(db, "Star wars");
        movie.setOriginalTitle("Star wars");
        movie.addGenre(GenreCode.ACTION);
        movie.addGenre(GenreCode.SCI_FI);
        movie.addCountry(CountryCode.ES);
        movie = new Movie(db, "The goonies wrong");
        movie.setSynopsis("The goonies synopsis");
        movie = new Movie(db, "Interestellar");
        movie.addCountry(CountryCode.US);

        Chapter chapter1 = new Chapter(db, "Game of thrones 1");
        chapter1.setOriginalTitle("GOT 1");
        Chapter chapter2 = new Chapter(db, "Game of thrones 2");
        chapter2.setMinutes(32);
        Chapter chapter3 = new Chapter(db, "Breaking bad 2 other");
        chapter3.setSeason("one");
        Chapter chapter4 = new Chapter(db, "Breaking bad 3");
        chapter4.setSeason("one");

        TVSeries tvSeries = new TVSeries(db, "Game of thrones");
        tvSeries.addChapter(chapter1);
        tvSeries.addChapter(chapter2);
        tvSeries = new TVSeries(db, "Breaking bad other");
        tvSeries.addChapter(chapter3);
        tvSeries.addChapter(chapter4);
    }

    private static void updateDB3(String db) {
        Movie movie = Movie.getMovies(db).get(1);
        movie.setTitle("The goonies");
        movie.setOriginalTitle("The goonies orig");
    }

    private static void assertDB3(String db, int phase) {
        startDBAssert(db, phase);
        Assert.assertEquals(3, Movie.getMovies(db).size());
        Movie movie1 = Movie.getMovies(db).get(0);
        Movie movie2 = Movie.getMovies(db).get(1);
        Movie movie3 = Movie.getMovies(db).get(2);
        Assert.assertEquals("Star wars", movie1.getTitle());
        Assert.assertEquals("Star wars", movie1.getOriginalTitle());
        List<GenreCode> genres = new ArrayList<>();
        genres.add(GenreCode.ACTION);
        genres.add(GenreCode.SCI_FI);
        ListAssert.assertEquals(genres, movie1.getGenres());
        List<CountryCode> countries = new ArrayList<>();
        countries.add(CountryCode.ES);
        ListAssert.assertEquals(countries, movie1.getCountries());
        if (phase == 0) {
            Assert.assertEquals("The goonies wrong", movie2.getTitle());
            Assert.assertEquals(null, movie2.getOriginalTitle());
        } else {
            Assert.assertEquals("The goonies", movie2.getTitle());
            Assert.assertEquals("The goonies orig", movie2.getOriginalTitle());
        }
        Assert.assertEquals("The goonies synopsis", movie2.getSynopsis());
        Assert.assertEquals("Interestellar", movie3.getTitle());
        countries = new ArrayList<>();
        countries.add(CountryCode.US);
        ListAssert.assertEquals(countries, movie3.getCountries());

        Assert.assertEquals(4, Chapter.getChapters(db).size());
        Chapter chapter1 = Chapter.getChapters(db).get(0);
        Chapter chapter2 = Chapter.getChapters(db).get(1);
        Chapter chapter3 = Chapter.getChapters(db).get(2);
        Chapter chapter4 = Chapter.getChapters(db).get(3);
        Assert.assertEquals("Game of thrones 1", chapter1.getTitle());
        Assert.assertEquals("GOT 1", chapter1.getOriginalTitle());
        Assert.assertEquals("Game of thrones 2", chapter2.getTitle());
        Assert.assertEquals(new Integer(32), chapter2.getMinutes());
        Assert.assertEquals("Breaking bad 2 other", chapter3.getTitle());
        Assert.assertEquals("one", chapter3.getSeason());
        Assert.assertEquals("Breaking bad 3", chapter4.getTitle());
        Assert.assertEquals("one", chapter4.getSeason());

        Assert.assertEquals(2, TVSeries.getTVSeries(db).size());
        TVSeries tvSeries1 = TVSeries.getTVSeries(db).get(0);
        TVSeries tvSeries2 = TVSeries.getTVSeries(db).get(1);
        Assert.assertEquals("Game of thrones", tvSeries1.getTitle());
        Assert.assertEquals(2, tvSeries1.getChapters(db).size());
        Assert.assertEquals("Game of thrones 1", tvSeries1.getChapters(db).get(0).getTitle());
        Assert.assertEquals("Game of thrones 2", tvSeries1.getChapters(db).get(1).getTitle());
        Assert.assertEquals("Breaking bad other", tvSeries2.getTitle());
        Assert.assertEquals(2, tvSeries2.getChapters(db).size());
        Assert.assertEquals("Breaking bad 2 other", tvSeries2.getChapters(db).get(0).getTitle());
        Assert.assertEquals("Breaking bad 3", tvSeries2.getChapters(db).get(1).getTitle());
        endDBAssert(db, phase);
    }


    private static void assertIntegrated(String db, int phase) {
        startDBAssert(db, phase);
        if (phase == 0) {
            // only local content
            Assert.assertEquals(1, Movie.getMovies(db).size());
            Assert.assertEquals("The lord of the rings", Movie.getMovies(db).get(0).getTitle());

            Assert.assertEquals(1, TVSeries.getTVSeries(db).size());
            Assert.assertEquals("Bottom", TVSeries.getTVSeries(db).get(0).getTitle());

            Assert.assertEquals(1, Chapter.getChapters(db).size());
            Assert.assertEquals("Day out", Chapter.getChapters(db).get(0).getTitle());
        } else if (phase == 1 || phase == 2 || phase == 3 || phase == 4) {
            Movie movie0;
            Movie movie1;
            Movie movie2;
            Movie movie3;
            Movie movie4 = null;
            Movie movie5 = null;
            // local plus first db2
            if (phase == 1 || phase == 2) {
                Assert.assertEquals(4, Movie.getMovies(db).size());
                movie0 = Movie.getMovies(db).get(0);
                movie1 = Movie.getMovies(db).get(1);
                movie2 = Movie.getMovies(db).get(2);
                movie3 = Movie.getMovies(db).get(3);
            } else if (phase == 3) {
                Assert.assertEquals(6, Movie.getMovies(db).size());
                movie0 = Movie.getMovies(db).get(0);
                movie1 = Movie.getMovies(db).get(1);
                movie2 = Movie.getMovies(db).get(2);
                movie3 = Movie.getMovies(db).get(3);
                movie4 = Movie.getMovies(db).get(4);
                movie5 = Movie.getMovies(db).get(5);
            } else {
                Assert.assertEquals(5, Movie.getMovies(db).size());
                movie0 = Movie.getMovies(db).get(0);
                movie1 = Movie.getMovies(db).get(1);
                movie2 = Movie.getMovies(db).get(2);
                movie3 = Movie.getMovies(db).get(3);
                movie4 = Movie.getMovies(db).get(4);
            }
            Assert.assertEquals("The lord of the rings", movie0.getTitle());
            Assert.assertEquals("Avatar", movie1.getTitle());
            Assert.assertEquals(new Integer(150), movie1.getMinutes());
            Assert.assertEquals(2, movie1.getActors().size());
            Person actor1 = movie1.getActors().get(0);
            Person actor2 = movie1.getActors().get(1);
            Assert.assertEquals("actor 1", actor1.getName());
            Assert.assertEquals(1, actor1.getAliases().size());
            Assert.assertEquals("actor 1 1", actor1.getAliases().get(0));
            Assert.assertEquals("actor 2", actor2.getName());
            Assert.assertEquals(0, actor2.getAliases().size());
            Assert.assertEquals(1, movie1.getCreators().size());
            Person creator = movie1.getCreators().get(0);
            Assert.assertEquals("director 1", creator.getName());
            Assert.assertEquals(0, creator.getAliases().size());
            Assert.assertEquals(1, movie1.getProductionCompanies().size());
            Company company = movie1.getProductionCompanies().get(0);
            Assert.assertEquals("Disney", company.getName());
            Assert.assertEquals(1, company.getAliases().size());
            Assert.assertEquals("Disney inc", company.getAliases().get(0));
            Assert.assertEquals("Marines in a remote planet", movie1.getSynopsis());
            Assert.assertEquals(new Integer(2008), movie1.getYear());
            Assert.assertEquals("Avatar orig", movie1.getOriginalTitle());
            List<CountryCode> countries = new ArrayList<>();
            countries.add(CountryCode.AC);
            ListAssert.assertEquals(countries, movie1.getCountries());
            Assert.assertEquals(new ImageHash("abcd", "jpg"), movie1.getImageHash());
            Assert.assertEquals(1, movie1.getExternalURLs().size());
            Assert.assertEquals("external URL 1", movie1.getExternalURLs().get(0));
            Assert.assertEquals(1, movie1.getVideoFiles().size());
            VideoFile videoFile = movie1.getVideoFiles().get(0);
            Assert.assertEquals("abcdef", videoFile.getHash());
            Assert.assertEquals(QualityCode.HD, videoFile.getQuality());
            Assert.assertEquals(new Integer(720), videoFile.getResolution());
            Assert.assertEquals("video file 1", videoFile.getName());
            Assert.assertEquals(1, videoFile.getAdditionalSources().size());
            Assert.assertEquals("torrent 1", videoFile.getAdditionalSources().get(0));
            Assert.assertEquals(new Integer(155), videoFile.getMinutes());
            Assert.assertEquals(new Long(2048L), videoFile.getLength());
            Assert.assertEquals(1, videoFile.getLanguages().size());
            Assert.assertEquals(LanguageCode.aa, videoFile.getLanguages().get(0));
            Assert.assertEquals(1, videoFile.getSubtitleFiles().size());
            SubtitleFile subtitleFile = videoFile.getSubtitleFiles().get(0);
            Assert.assertEquals(new Long(1024L), subtitleFile.getLength());
            Assert.assertEquals("sub 1", subtitleFile.getName());
            Assert.assertEquals("qqqwww", subtitleFile.getHash());
            Assert.assertEquals(1, subtitleFile.getLanguages().size());
            Assert.assertEquals(LanguageCode.ab, subtitleFile.getLanguages().get(0));
            Assert.assertEquals(1, subtitleFile.getAdditionalSources().size());
            Assert.assertEquals("torrent 2", subtitleFile.getAdditionalSources().get(0));


            Assert.assertEquals("Star wars", movie2.getTitle());
            Assert.assertEquals("Star wars", movie2.getOriginalTitle());
            List<GenreCode> genres = new ArrayList<>();
            countries = new ArrayList<>();
            if (phase == 1) {
                genres.add(GenreCode.ADVENTURE);
            } else if (phase == 2) {
                genres.add(GenreCode.ADVENTURE);
                genres.add(GenreCode.COMEDY);
                genres.add(GenreCode.DRAMA);
            } else {
                // merged with db3 -> additional genres
                genres.add(GenreCode.ADVENTURE);
                genres.add(GenreCode.COMEDY);
                genres.add(GenreCode.DRAMA);
                genres.add(GenreCode.ACTION);
                genres.add(GenreCode.SCI_FI);
                countries.add(CountryCode.ES);
            }
            ListAssert.assertEquals(genres, movie2.getGenres());
            ListAssert.assertEquals(countries, movie2.getCountries());
            Assert.assertEquals("The goonies", movie3.getTitle());
            Assert.assertEquals("The goonies orig", movie3.getOriginalTitle());
            if (phase < 4) {
                // the goonies not yet merged
                Assert.assertEquals(null, movie3.getSynopsis());
            }
            Assert.assertEquals(new Integer(1996), movie3.getYear());
            if (phase == 3) {
                // the goonies is not yet merged
                Assert.assertEquals("The goonies wrong", movie4.getTitle());
                Assert.assertEquals("The goonies synopsis", movie4.getSynopsis());
                Assert.assertEquals("Interestellar", movie5.getTitle());
                countries = new ArrayList<>();
                countries.add(CountryCode.US);
                ListAssert.assertEquals(countries, movie5.getCountries());
            } else if (phase == 4) {
                // the goonies is now merged
                Assert.assertEquals("The goonies synopsis", movie3.getSynopsis());
                Assert.assertEquals("Interestellar", movie4.getTitle());
                countries = new ArrayList<>();
                countries.add(CountryCode.US);
                ListAssert.assertEquals(countries, movie4.getCountries());
            }

            Chapter chapter0;
            Chapter chapter1;
            Chapter chapter2;
            Chapter chapter3;
            Chapter chapter4;
            Chapter chapter5 = null;
            Chapter chapter6 = null;
            Chapter chapter7 = null;
            Chapter chapter8 = null;
            if (phase == 1 || phase == 2) {
                Assert.assertEquals(5, Chapter.getChapters(db).size());
                chapter0 = Chapter.getChapters(db).get(0);
                chapter1 = Chapter.getChapters(db).get(1);
                chapter2 = Chapter.getChapters(db).get(2);
                chapter3 = Chapter.getChapters(db).get(3);
                chapter4 = Chapter.getChapters(db).get(4);
            } else {
                Assert.assertEquals(9, Chapter.getChapters(db).size());
                chapter0 = Chapter.getChapters(db).get(0);
                chapter1 = Chapter.getChapters(db).get(1);
                chapter2 = Chapter.getChapters(db).get(2);
                chapter3 = Chapter.getChapters(db).get(3);
                chapter4 = Chapter.getChapters(db).get(4);
                chapter5 = Chapter.getChapters(db).get(5);
                chapter6 = Chapter.getChapters(db).get(6);
                chapter7 = Chapter.getChapters(db).get(7);
                chapter8 = Chapter.getChapters(db).get(8);
            }
            Assert.assertEquals("Day out", chapter0.getTitle());
            Assert.assertEquals("Friends 1", chapter1.getTitle());
            Assert.assertEquals(new Integer(1989), chapter1.getYear());
            Assert.assertEquals("s01", chapter1.getSeason());
            Assert.assertEquals(new Integer(45), chapter1.getMinutes());
            Assert.assertEquals(1, chapter1.getVideoFiles().size());
            videoFile = chapter1.getVideoFiles().get(0);
            Assert.assertEquals("abcdefgh", videoFile.getHash());
            Assert.assertEquals("Friends 2", chapter2.getTitle());
            Assert.assertEquals(new Integer(28), chapter2.getMinutes());
            Assert.assertEquals("Breaking bad 1", chapter3.getTitle());
            Assert.assertEquals("Breaking bad 2", chapter4.getTitle());
            if (phase == 3 || phase == 4) {
                Assert.assertEquals("Game of thrones 1", chapter5.getTitle());
                Assert.assertEquals("GOT 1", chapter5.getOriginalTitle());
                Assert.assertEquals("Game of thrones 2", chapter6.getTitle());
                Assert.assertEquals(new Integer(32), chapter6.getMinutes());
                Assert.assertEquals("Breaking bad 2 other", chapter7.getTitle());
                Assert.assertEquals("one", chapter7.getSeason());
                Assert.assertEquals("Breaking bad 3", chapter8.getTitle());
                Assert.assertEquals("one", chapter8.getSeason());
            }

            TVSeries tvSeries0;
            TVSeries tvSeries1;
            TVSeries tvSeries2;
            TVSeries tvSeries3 = null;
            TVSeries tvSeries4 = null;
            if (phase == 1 || phase == 2) {
                Assert.assertEquals(3, TVSeries.getTVSeries(db).size());
                tvSeries0 = TVSeries.getTVSeries(db).get(0);
                tvSeries1 = TVSeries.getTVSeries(db).get(1);
                tvSeries2 = TVSeries.getTVSeries(db).get(2);
            } else {
                Assert.assertEquals(5, TVSeries.getTVSeries(db).size());
                tvSeries0 = TVSeries.getTVSeries(db).get(0);
                tvSeries1 = TVSeries.getTVSeries(db).get(1);
                tvSeries2 = TVSeries.getTVSeries(db).get(2);
                tvSeries3 = TVSeries.getTVSeries(db).get(3);
                tvSeries4 = TVSeries.getTVSeries(db).get(4);
            }
            Assert.assertEquals("Bottom", tvSeries0.getTitle());
            Assert.assertEquals("Friends", tvSeries1.getTitle());
            Assert.assertEquals(2, tvSeries1.getChapters(db).size());
            Assert.assertEquals("Friends 1", tvSeries1.getChapters(db).get(0).getTitle());
            Assert.assertEquals("Friends 2", tvSeries1.getChapters(db).get(1).getTitle());
            Assert.assertEquals("Breaking bad", tvSeries2.getTitle());
            Assert.assertEquals(2, tvSeries2.getChapters(db).size());
            Assert.assertEquals("Breaking bad 1", tvSeries2.getChapters(db).get(0).getTitle());
            Assert.assertEquals("Breaking bad 2", tvSeries2.getChapters(db).get(1).getTitle());
            if (phase == 3 || phase == 4) {
                Assert.assertEquals("Game of thrones", tvSeries3.getTitle());
                Assert.assertEquals(2, tvSeries3.getChapters(db).size());
                Assert.assertEquals("Game of thrones 1", tvSeries3.getChapters(db).get(0).getTitle());
                Assert.assertEquals("Game of thrones 2", tvSeries3.getChapters(db).get(1).getTitle());
                Assert.assertEquals("Breaking bad other", tvSeries4.getTitle());
                Assert.assertEquals(2, tvSeries4.getChapters(db).size());
                Assert.assertEquals("Breaking bad 2 other", tvSeries4.getChapters(db).get(0).getTitle());
                Assert.assertEquals("Breaking bad 3", tvSeries4.getChapters(db).get(1).getTitle());
            }
        }
        endDBAssert(db, phase);
    }

}
