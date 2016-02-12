package jacz.peerengineclient.test.synch_db;

import jacz.database.Movie;
import jacz.database.util.GenreCode;
import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineclient.SessionManager;
import jacz.peerengineclient.test.*;
import jacz.peerengineservice.test.ResourceTransferEventsImpl;
import jacz.peerengineservice.test.TempFileManagerEventsImpl;
import jacz.util.concurrency.ThreadUtil;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;

/**
 * Created by Alberto on 27/12/2015.
 */
public class TestSynch_2 {

    public static void main(String[] args) throws IOException, XMLStreamException {

        PeerEngineClient peerEngineClient = Client.loadClient("./etc/user_1");

        String sharedDB = peerEngineClient.getDatabases().getSharedDB();

//        Movie movie = new Movie(sharedDB);
//        movie.setTitle("Avatar");
//        movie.setMinutes(150);
//        movie = new Movie(sharedDB);
//        movie.setTitle("Star wars");
//        movie.addGenre(GenreCode.ADVENTURE);
//        movie = new Movie(sharedDB);
//        movie.setTitle("The goonies");
//        movie.setMinutes(150);
//        movie.setOriginalTitle("The goonies orig");
//        movie.setYear(1996);

        Movie movie = Movie.getMovieById(sharedDB, 2);
        if (movie != null) {
            movie.delete();
        }

        peerEngineClient.connect();

        ThreadUtil.safeSleep(50000);
        peerEngineClient.stop();

        System.out.println("END");
    }
}
