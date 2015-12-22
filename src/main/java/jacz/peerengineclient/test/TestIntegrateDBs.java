package jacz.peerengineclient.test;

/**
 * test
 */
public class TestIntegrateDBs {

    public static void main(String[] args) throws Exception {

//        IntegratedDatabase integratedDatabase = new IntegratedDatabase(Samples.buildEmptyDatabase("./examples/dbsamples/integrated"));
//        LocalDatabase localDatabase = new LocalDatabase(Samples.buildEmptyDatabase("./examples/dbsamples/local"));
//
//        PeerID peerID1 = new PeerID("pid{0000000000000000000000000000000000000000001}");
//        PeerID peerID2 = new PeerID("pid{0000000000000000000000000000000000000000002}");
//        PeerID peerID3 = new PeerID("pid{0000000000000000000000000000000000000000003}");
//        Duple<Database, FileHashDatabase> db1 = Samples.buildPersonCreatorDB1("./examples/dbsamples/db1");
//        Duple<Database, FileHashDatabase> db2 = Samples.buildPersonCreatorDB1("./examples/dbsamples/db2");
//        Duple<Database, FileHashDatabase> db3 = Samples.buildPersonCreatorDB1("./examples/dbsamples/db3");
//        RemoteDatabase remoteDatabase1 = new RemoteDatabase(db1.element1, peerID1);
//        RemoteDatabase remoteDatabase2 = new RemoteDatabase(db2.element1, peerID2);
//        RemoteDatabase remoteDatabase3 = new RemoteDatabase(db3.element1, peerID3);
//        Map<PeerID, RemoteDatabase> peerIDRemoteDatabaseMap = new HashMap<>();
//        peerIDRemoteDatabaseMap.put(peerID1, remoteDatabase1);
//        peerIDRemoteDatabaseMap.put(peerID2, remoteDatabase2);
//        peerIDRemoteDatabaseMap.put(peerID3, remoteDatabase3);
//
//
//        DatabaseManager libraryManager = new DatabaseManager(integratedDatabase, localDatabase, peerIDRemoteDatabaseMap, new LibraryManagerNotificationsImpl());
////        ConcurrencyController cc = libraryManager.concurrencyController;
//        ConcurrencyController cc = new ConcurrencyControllerReadWriteBasic();
//
//        cc.beginActivity(LibraryManagerConcurrencyController.SYNCH_REMOTE_LIBRARY);
//        libraryManager.remoteItemModified(peerID1, Libraries.PERSON_LIBRARY, "0000000000");
//        libraryManager.remoteItemModified(peerID1, Libraries.PERSON_LIBRARY, "0000000001");
//        libraryManager.remoteItemModified(peerID1, Libraries.PERSON_LIBRARY, "0000000002");
//        libraryManager.remoteItemModified(peerID1, Libraries.PERSON_LIBRARY, "0000000003");
//
//        libraryManager.remoteItemModified(peerID2, Libraries.PERSON_LIBRARY, "0000000000");
//        libraryManager.remoteItemModified(peerID2, Libraries.PERSON_LIBRARY, "0000000001");
//        libraryManager.remoteItemModified(peerID2, Libraries.PERSON_LIBRARY, "0000000002");
//
//        libraryManager.remoteItemModified(peerID3, Libraries.PERSON_LIBRARY, "0000000000");
//        libraryManager.remoteItemModified(peerID3, Libraries.PERSON_LIBRARY, "0000000001");
//        libraryManager.remoteItemModified(peerID3, Libraries.PERSON_LIBRARY, "0000000002");
//        cc.endActivity(LibraryManagerConcurrencyController.SYNCH_REMOTE_LIBRARY);
//
//
//        ThreadUtil.safeSleep(5000);
//
//
//        libraryManager.stop();
    }
}
