package jacz.peerengineclient.test_old.dbs;

import jacz.store.Database;
import jacz.store.Libraries;
import jacz.store.common.Person;
import jacz.store.db_mediator.CSVDBMediator;
import jacz.store.db_mediator.CorruptDataException;
import jacz.store.db_mediator.DBException;
import jacz.store.db_mediator.DBMediator;
import jacz.util.hash.hashdb.FileHashDatabase;
import jacz.util.lists.Duple;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;

/**
 * Samples
 */
public class Samples {

    public static Database buildEmptyDatabase(String baseDir) throws DBException, IOException, CorruptDataException {
        DBMediator dbMediator = new CSVDBMediator(baseDir);
        Database.createEmpty(dbMediator);
        return new Database(dbMediator);
    }

    public static Duple<FileHashDatabase, HashMap<String, String>> buildPersonCreatorDB1(String baseDir, String baseDir2) throws DBException, IOException, CorruptDataException, ParseException {
        DBMediator dbMediator = new CSVDBMediator(baseDir);
        FileHashDatabase fileHashDatabase = new FileHashDatabase();
        Database.createEmpty(dbMediator);
        Database database = new Database(dbMediator);
        Person p1 = (Person) database.createNewItem(Libraries.PERSON_LIBRARY);
        Person p2 = (Person) database.createNewItem(Libraries.PERSON_LIBRARY);
        Person p3 = (Person) database.createNewItem(Libraries.PERSON_LIBRARY);
        Person p4 = (Person) database.createNewItem(Libraries.PERSON_LIBRARY);
        p1.setName("woody allen");
        p1.addAlias(0, "w. allen");
        p2.setName("mozart");
        p3.setName("cindy lauper");
        p4.setName("freddie mercury");
        p4.addAlias(0, "Queen singer");
        String woodyLarge = fileHashDatabase.put("./examples/storage/user1/Woody_Allen2.jpg");
        String woodySmall = fileHashDatabase.put("./examples/storage/user1/Woody_Allen2_small.jpg");
        p1.setMainImage(woodyLarge);
        p1.setMainImageReduced(woodySmall);
        database.close();

        dbMediator = new CSVDBMediator(baseDir2);
        Database.createEmpty(dbMediator);
        database = new Database(dbMediator);
        p4 = (Person) database.createNewItem(Libraries.PERSON_LIBRARY);
        p4.setName("freddie mercury");
        p4.addAlias(0, "Queen singer");
        database.close();

        HashMap<String, String> integratedToLocalItems = new HashMap<>();
        integratedToLocalItems.put("0000000003", "0000000000");

        return new Duple<>(fileHashDatabase, integratedToLocalItems);
    }

    public static Duple<FileHashDatabase, HashMap<String, String>> buildPersonCreatorDB2(String baseDir, String baseDir2) throws DBException, IOException, CorruptDataException, ParseException {
        DBMediator dbMediator = new CSVDBMediator(baseDir);
        FileHashDatabase fileHashDatabase = new FileHashDatabase();
        Database.createEmpty(dbMediator);
        Database database = new Database(dbMediator);
        Person p1 = (Person) database.createNewItem(Libraries.PERSON_LIBRARY);
        Person p2 = (Person) database.createNewItem(Libraries.PERSON_LIBRARY);
        Person p3 = (Person) database.createNewItem(Libraries.PERSON_LIBRARY);
        p1.setName("freddie mercury");
        p1.addAlias(0, "farrokh bulsara");
        p2.setName("norah jones");
        p3.setName("leonard cohen");
        String mercuryLarge = fileHashDatabase.put("./examples/storage/user2/fMercury.jpg");
        String mercurySmall = fileHashDatabase.put("./examples/storage/user2/fMercury_small.jpg");
        p1.setMainImage(mercuryLarge);
        p1.setMainImageReduced(mercurySmall);
        database.close();

        dbMediator = new CSVDBMediator(baseDir2);
        Database.createEmpty(dbMediator);
        database = new Database(dbMediator);
        p1 = (Person) database.createNewItem(Libraries.PERSON_LIBRARY);
        p1.setName("freddie mercury");
        p1.addAlias(0, "farrokh bulsara");
        p2 = (Person) database.createNewItem(Libraries.PERSON_LIBRARY);
        p2.setName("norah jones");
        database.close();

        HashMap<String, String> integratedToLocalItems = new HashMap<>();
        integratedToLocalItems.put("0000000000", "0000000000");
        integratedToLocalItems.put("0000000001", "0000000001");

        return new Duple<>(fileHashDatabase, integratedToLocalItems);
    }

    public static Duple<FileHashDatabase, HashMap<String, String>> buildPersonCreatorDB3(String baseDir, String baseDir2) throws DBException, IOException, CorruptDataException, ParseException {
        DBMediator dbMediator = new CSVDBMediator(baseDir);
        FileHashDatabase fileHashDatabase = new FileHashDatabase();
        Database.createEmpty(dbMediator);
        Database database = new Database(dbMediator);
        Person p1 = (Person) database.createNewItem(Libraries.PERSON_LIBRARY);
        Person p2 = (Person) database.createNewItem(Libraries.PERSON_LIBRARY);
        Person p3 = (Person) database.createNewItem(Libraries.PERSON_LIBRARY);
        p1.setName("mozart");
        p2.setName("eric clapton");
        p3.setName("leonard cohen");
        String claptonLarge = fileHashDatabase.put("./examples/storage/user3/Eric-Clapton.jpg");
        String claptonSmall = fileHashDatabase.put("./examples/storage/user3/Eric-Clapton_small.jpg");
        p2.setMainImage(claptonLarge);
        p2.setMainImageReduced(claptonSmall);
        database.close();

        dbMediator = new CSVDBMediator(baseDir2);
        Database.createEmpty(dbMediator);
        database = new Database(dbMediator);
        p1 = (Person) database.createNewItem(Libraries.PERSON_LIBRARY);
        p2 = (Person) database.createNewItem(Libraries.PERSON_LIBRARY);
        p3 = (Person) database.createNewItem(Libraries.PERSON_LIBRARY);
        database.close();

        HashMap<String, String> integratedToLocalItems = new HashMap<>();
        integratedToLocalItems.put("0000000000", "0000000000");
        integratedToLocalItems.put("0000000001", "0000000001");
        integratedToLocalItems.put("0000000002", "0000000002");

        return new Duple<>(fileHashDatabase, integratedToLocalItems);
    }
}
