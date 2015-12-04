package jacz.peerengineclient.test.synch;

import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineservice.PeerID;
import jacz.peerengineclient.SessionManager;
import jacz.peerengineclient.file_system.FileIO;
import jacz.peerengineclient.file_system.ServersInfo;
import jacz.peerengineclient.test.SimpleJacuzziPeerClientAction;
import jacz.peerengineclient.test.dbs.Samples;
import jacz.util.files.FileReaderWriter;
import jacz.util.files.FileUtil;
import jacz.util.hash.hashdb.FileHashDatabase;
import jacz.util.lists.Duple;

import java.util.HashMap;

/**
 * build data for the synch test. Creates three users with different databases
 */
public class BuildData {

    public static void main(String[] args) throws Exception {

        String path = SessionManager.createUserConfig("./examples/configs", new PeerID("pid{0000000000000000000000000000000000000000001}"), "alb", 1024, 100, 100, 0.5d, new ServersInfo.ServerInfo("138.100.11.51", 50000), 10000, "./examples/storage/user1/temp", "./examples/storage/user1");
        System.out.println(path);
        Duple<FileHashDatabase, HashMap<String, String>> data = Samples.buildPersonCreatorDB1(path + "/databases/integrated", path + "/databases/local");
        FileIO.writeFileHashDatabase(path, data.element1);
        FileReaderWriter.writeObject(FileUtil.joinPaths(path + "/databases/integrated", "itemsToLocalItems.bin"), data.element2);
        PeerEngineClient peerEngineClient = SessionManager.load(path, new SimpleJacuzziPeerClientAction(""));
        peerEngineClient.addFriendPeer(new PeerID("pid{0000000000000000000000000000000000000000002}"));
        peerEngineClient.addFriendPeer(new PeerID("pid{0000000000000000000000000000000000000000003}"));
        peerEngineClient.stop();
        SessionManager.save(peerEngineClient);

        path = SessionManager.createUserConfig("./examples/configs", new PeerID("pid{0000000000000000000000000000000000000000002}"), "alex", 1024, 100, 100, 0.5d, new ServersInfo.ServerInfo("138.100.11.51", 50000), 10001, "./examples/storage/user2/temp", "./examples/storage/user2");
        System.out.println(path);
        data = Samples.buildPersonCreatorDB2(path + "/databases/integrated", path + "/databases/local");
        FileIO.writeFileHashDatabase(path, data.element1);
        FileReaderWriter.writeObject(FileUtil.joinPaths(path + "/databases/integrated", "itemsToLocalItems.bin"), data.element2);
        peerEngineClient = SessionManager.load(path, new SimpleJacuzziPeerClientAction(""));
        peerEngineClient.addFriendPeer(new PeerID("pid{0000000000000000000000000000000000000000001}"));
        peerEngineClient.stop();
        SessionManager.save(peerEngineClient);

        path = SessionManager.createUserConfig("./examples/configs", new PeerID("pid{0000000000000000000000000000000000000000003}"), "andres", 1024, 100, 100, 0.5d, new ServersInfo.ServerInfo("138.100.11.51", 50000), 10002, "./examples/storage/user3/temp", "./examples/storage/user3");
        System.out.println(path);
        data = Samples.buildPersonCreatorDB3(path + "/databases/integrated", path + "/databases/local");
        FileIO.writeFileHashDatabase(path, data.element1);
        FileReaderWriter.writeObject(FileUtil.joinPaths(path + "/databases/integrated", "itemsToLocalItems.bin"), data.element2);
        peerEngineClient = SessionManager.load(path, new SimpleJacuzziPeerClientAction(""));
        peerEngineClient.addFriendPeer(new PeerID("pid{0000000000000000000000000000000000000000001}"));
        peerEngineClient.stop();
        SessionManager.save(peerEngineClient);
    }

}
