package jacz.peerengineclient.test;

import jacz.peerengineservice.PeerID;
import jacz.peerengineclient.SessionManager;
import jacz.peerengineclient.file_system.ServersInfo;

/**
 * test
 */
public class TestBuildConfig {

    public static void main(String args[]) throws Exception {

//        String path = SessionManager.createUserConfig("./examples/", new PeerID("pid{0000000000000000000000000000000000000000001}"), "alb", 1024, 100, 100, 0.5d, new ServersInfo.ServerInfo("138.100.11.51", 55555), 50000, "d:/downloads");
//        System.out.println(path);


//        String path = SessionManager.createUserConfig("./examples/configs", new PeerID("pid{0000000000000000000000000000000000000000001}"), "alb", 1024, 100, 100, 0.5d, new ServersInfo.ServerInfo("138.100.11.51", 55555), 50000, "./examples/storage/user1/temp", "./examples/storage/user1");
//        System.out.println(path);
//        String path = SessionManager.createUserConfig("./examples/configs", new PeerID("pid{0000000000000000000000000000000000000000002}"), "alex", 1024, 100, 100, 0.5d, new ServersInfo.ServerInfo("138.100.11.51", 55555), 50001, "./examples/storage/user2/temp", "./examples/storage/user2");
//        System.out.println(path);
        String path = SessionManager.createUserConfig("./examples/configs", new PeerID("pid{0000000000000000000000000000000000000000003}"), "andres", 1024, 100, 100, 0.5d, new ServersInfo.ServerInfo("138.100.11.51", 55555), 50002, "./examples/storage/user3/temp", "./examples/storage/user3");
        System.out.println(path);
    }
}
