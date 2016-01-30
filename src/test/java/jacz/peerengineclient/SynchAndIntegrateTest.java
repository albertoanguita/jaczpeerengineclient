package jacz.peerengineclient;

import jacz.database.Chapter;
import jacz.database.Movie;
import jacz.database.TVSeries;
import jacz.database.util.GenreCode;
import jacz.peerengineclient.test.Client;
import jacz.peerengineservice.PeerID;
import jacz.util.concurrency.ThreadUtil;

import java.io.IOException;

/**
 * This test performs a synch with two other peers and integrates their data to create the integrated database.
 * After that, two files are downloaded, so the shared database is  built. Finally, one of the files is deleted.
 */
public class SynchAndIntegrateTest {

    @org.junit.Test
    public void synchAndIntegrate() throws IOException {

        PeerEngineClient peerEngineClient = Client.loadClient("./etc/user_0");

        // todo clear dbs

        peerEngineClient.connect();

        ThreadUtil.safeSleep(20000);

        peerEngineClient.addFriendPeer(PeerID.buildTestPeerID("2"));

        // wait for dbs to synch, test their contents (remote-2 and integrated)
        ThreadUtil.safeSleep(40000);

        String integratedDB = peerEngineClient.getDatabases().getIntegratedDB();
        String remote2DB = peerEngineClient.getDatabases().getRemoteDBs().get(PeerID.buildTestPeerID("2"));


    }

    public static void setupDB2(String db) {
        Movie movie = new Movie(db);
        movie.setTitle("Avatar");
        movie.setMinutes(150);
        movie = new Movie(db);
        movie.setTitle("Star wars");
        movie.addGenre(GenreCode.ADVENTURE);
        movie = new Movie(db);
        movie.setTitle("The goonies");
        movie.setMinutes(150);
        movie.setOriginalTitle("The goonies orig");
        movie.setYear(1996);

        Chapter chapter1 = new Chapter(db);
        chapter1.setTitle("Friends 1");
        chapter1.setYear(1989);
        Chapter chapter2 = new Chapter(db);
        chapter2.setTitle("Friends 2");
        chapter2.setMinutes(28);
        Chapter chapter3 = new Chapter(db);
        chapter3.setTitle("Breaking bad 1");
        Chapter chapter4 = new Chapter(db);
        chapter4.setTitle("Breaking bad 2");

        TVSeries tvSeries = new TVSeries(db);
        tvSeries.setTitle("Friends");
        tvSeries.addChapter(chapter1);
        tvSeries.addChapter(chapter2);
        tvSeries = new TVSeries(db);
        tvSeries.setTitle("Breaking bad");
        tvSeries.addChapter(chapter3);
        tvSeries.addChapter(chapter4);


    }
}
