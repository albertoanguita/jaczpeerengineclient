package jacz.peerengineclient.data;

import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineclient.PeersEventsBridge;
import jacz.peerengineclient.common.Client;
import jacz.peerengineclient.file_system.PathConstants;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.PeerClient;
import jacz.util.files.FileReaderWriter;
import jacz.util.hash.hashdb.FileHashDatabase;
import jacz.util.lists.tuple.Duple;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by Alberto on 15/05/2016.
 */
public class RemotePeerShareTest {

    private static class ForeignSharesFake extends ForeignShares {

        List<String> activeResources;

        public ForeignSharesFake(PeerClient peerClient) {
            super(peerClient);
            activeResources = new ArrayList<>();
        }

        @Override
        public synchronized void addResourceProvider(String resourceID, PeerId peerID) {
            activeResources.add(resourceID);
        }

        @Override
        public synchronized void removeResourceProvider(String resourceID, PeerId peerID) {
            activeResources.remove(resourceID);
        }

        @Override
        public synchronized void reportVolatileResources(PeerId peerID, Set<String> resources) {

        }

        @Override
        public synchronized void removeResourceProvider(PeerId peerID) {
            activeResources.clear();
        }
    }

    private static final String dir = "./etc/test-rps/";

    private static final String bd = dir + "rps.bd";

    @Test
    public void test() throws IOException {

        FileUtils.forceMkdir(new File(dir));
        FileUtils.cleanDirectory(new File(dir));
        List<Duple<String, String>> pathAndHash = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            FileReaderWriter.writeTextFile(path(i), "file " + i);
            pathAndHash.add(new Duple<>(path(i), FileHashDatabase.getHash(new File(path(i)))));
        }

        String userPath = "./etc/user_0";
        PeerEngineClient peerEngineClient = Client.loadClient(userPath);
        PeerClient peerClient = new PeerClient(
                PeerId.buildTestPeerId("1"),
                "https://jaczserver.appspot.com/_ah/api/server/v1/",
                PathConstants.connectionConfigPath(userPath),
                PathConstants.peerKBPath(userPath),
                null,
                PathConstants.networkConfigPath(userPath),
                null,
                null,
                new PeersEventsBridge(null, null),
                null,
                PathConstants.personalDataPath(userPath),
                PathConstants.statisticsPath(userPath),
                new HashMap<>(),
                null,
                null);

        ForeignSharesFake foreignShares = new ForeignSharesFake(peerClient);
        RemotePeerShare remotePeerShare = new RemotePeerShare(PeerId.buildTestPeerId("2"), foreignShares, bd);
        Assert.assertEquals("", remotePeerShare.getId());
        Assert.assertEquals(-1L, remotePeerShare.getMaxStoredTimestamp());

        remotePeerShare.addHash(3L, "hash1");
        remotePeerShare.addHash(4L, "hash2");
        remotePeerShare.addHash(6L, "hash3");
        Assert.assertEquals(6L, remotePeerShare.getMaxStoredTimestamp());
        Assert.assertEquals(Arrays.asList("hash1", "hash2", "hash3"), foreignShares.activeResources);

        remotePeerShare.removeHash(8L, "hash2");
        Assert.assertEquals(8L, remotePeerShare.getMaxStoredTimestamp());
        Assert.assertEquals(Arrays.asList("hash1", "hash3"), foreignShares.activeResources);

        // reload
        foreignShares = new ForeignSharesFake(peerClient);
        remotePeerShare = new RemotePeerShare(foreignShares, bd);
        Assert.assertEquals(8L, remotePeerShare.getMaxStoredTimestamp());
        Assert.assertEquals(Arrays.asList("hash1", "hash3"), foreignShares.activeResources);

        // set id
        remotePeerShare.setId("asdf");
        Assert.assertEquals("asdf", remotePeerShare.getId());
        Assert.assertEquals(-1L, remotePeerShare.getMaxStoredTimestamp());
        Assert.assertEquals(new ArrayList<String>(), foreignShares.activeResources);

        // reload
        foreignShares = new ForeignSharesFake(peerClient);
        remotePeerShare = new RemotePeerShare(foreignShares, bd);
        Assert.assertEquals("asdf", remotePeerShare.getId());
        Assert.assertEquals(-1L, remotePeerShare.getMaxStoredTimestamp());
        Assert.assertEquals(new ArrayList<String>(), foreignShares.activeResources);


        FileUtils.cleanDirectory(new File(dir));
    }

    private String path(int index) {
        return dir + index;
    }
}