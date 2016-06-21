package jacz.peerengineclient.newconfig;

import com.neovisionaries.i18n.CountryCode;
import jacz.peerengineclient.SessionManager;
import jacz.peerengineclient.common.TestUtil;
import jacz.peerengineclient.file_system.PathConstants;
import jacz.peerengineservice.PeerId;
import jacz.util.lists.tuple.Duple;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * peer1: 0000000000000000000000000000000000000000001
 */
public class NewConfigTest {

    @Test
    public void test() throws IOException {

        Duple<String, PeerId> dirAndPeerId = SessionManager.createUserConfig("./etc", TestUtil.randomBytes(), "alb", CountryCode.ES, "0.1");
        String dir = dirAndPeerId.element1;
        PeerId peerId = dirAndPeerId.element2;

        System.out.println("User config created at " + dir);
        System.out.println("With peer id: " + peerId);

        Assert.assertTrue(Files.exists(Paths.get(PathConstants.connectionConfigPath(dir))));
        Assert.assertTrue(Files.exists(Paths.get(PathConstants.peerIdConfigPath(dir))));
        Assert.assertTrue(Files.exists(Paths.get(PathConstants.peerIdConfigBackupPath(dir))));
        Assert.assertTrue(Files.exists(Paths.get(PathConstants.networkConfigPath(dir))));
        Assert.assertTrue(Files.exists(Paths.get(PathConstants.mediaPathsConfigPath(dir))));

        Assert.assertTrue(Files.exists(Paths.get(PathConstants.integratedDBPath(dir))));
        Assert.assertTrue(Files.exists(Paths.get(PathConstants.deletedDBPath(dir))));
        Assert.assertTrue(Files.exists(Paths.get(PathConstants.localDBPath(dir))));
        Assert.assertTrue(Files.exists(Paths.get(PathConstants.sharedDBPath(dir))));
        Assert.assertTrue(Files.exists(Paths.get(PathConstants.itemRelationsPath(dir))));
        Assert.assertTrue(Files.exists(Paths.get(PathConstants.itemRelationsBackupPath(dir))));
        Assert.assertTrue(PathConstants.getRemoteDatabasesDir(dir).isDirectory());

        Assert.assertTrue(PathConstants.getRemoteSharesDir(dir).isDirectory());

        Assert.assertTrue(Files.exists(Paths.get(PathConstants.fileHashPath(dir))));
        Assert.assertTrue(Files.exists(Paths.get(PathConstants.peerKBPath(dir))));
        Assert.assertTrue(Files.exists(Paths.get(PathConstants.personalDataPath(dir))));

        Assert.assertTrue(Files.exists(Paths.get(PathConstants.encryptionPath(dir))));
        Assert.assertTrue(Files.exists(Paths.get(PathConstants.encryptionBackupPath(dir))));

        Assert.assertTrue(PathConstants.getDefaultMediaDir(dir).isDirectory());
        Assert.assertTrue(PathConstants.getDefaultTempDir(dir).isDirectory());

        Assert.assertTrue(Files.exists(Paths.get(PathConstants.statisticsPath(dir))));
    }
}
