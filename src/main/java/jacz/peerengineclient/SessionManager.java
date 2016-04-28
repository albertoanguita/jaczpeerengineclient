package jacz.peerengineclient;

import com.neovisionaries.i18n.CountryCode;
import jacz.peerengineclient.data.PeerShareIO;
import jacz.peerengineclient.databases.DatabaseIO;
import jacz.peerengineclient.databases.integration.IntegrationEvents;
import jacz.peerengineclient.databases.synch.DatabaseSynchEvents;
import jacz.peerengineclient.file_system.*;
import jacz.peerengineservice.PeerEncryption;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.GeneralEvents;
import jacz.peerengineservice.client.PeersPersonalData;
import jacz.peerengineservice.client.connection.ConnectionEvents;
import jacz.peerengineservice.client.connection.NetworkConfiguration;
import jacz.peerengineservice.client.connection.peers.PeerConnectionConfig;
import jacz.peerengineservice.client.connection.peers.PeersEvents;
import jacz.peerengineservice.client.connection.peers.kb.PeerKnowledgeBase;
import jacz.peerengineservice.util.datatransfer.ResourceTransferEvents;
import jacz.peerengineservice.util.datatransfer.TransferStatistics;
import jacz.peerengineservice.util.tempfile_api.TempFileManagerEvents;
import jacz.util.files.FileGenerator;
import jacz.util.io.serialization.VersionedObjectSerializer;
import jacz.util.lists.tuple.Duple;
import jacz.util.log.ErrorHandler;
import org.apache.commons.io.FileUtils;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles IO access and opens/closes sessions
 */
public class SessionManager {

    private static final int ID_LENGTH = 12;

    private static final String USER_BASE_PATH = "user_";

    private static final String DEFAULT_NICK = "user";

    private static final int DEFAULT_LOCAL_PORT = 0;

    private static final int DEFAULT_EXTERNAL_PORT = 37720;

    public static final int CRC_LENGTH = 8;

    // todo allow changing temp and downloads dir. Store old downloads dir to be able to automatically detect files requiring move in old dirs

    public static synchronized String createUserConfig(String basePath, byte[] randomBytes, String nick, CountryCode mainCountry) throws IOException {
        // creates a new user account
        // first find a free user directory, then create all initial config files in it and return it
        // base path is created if needed

        if (!new File(basePath).isDirectory()) {
            FileUtils.forceMkdir(new File(basePath));
        }
        try {
            Duple<String, String> newUserDirectory = FileGenerator.createDirectoryWithIndex(basePath, USER_BASE_PATH, "", "", false);
            String userPath = newUserDirectory.element1;
            String mediaPath = Paths.getDefaultMediaDir(userPath).getName();
            String tempPath = Paths.getDefaultTempDir(userPath).getName();

            // create sub-directories
            for (File dir : Paths.getOrderedDirectories(userPath)) {
                FileUtils.forceMkdir(dir);
            }

            // id and encryption files
            Duple<PeerId, PeerEncryption> peerIDAndEncryption = PeerId.generateIdAndEncryptionKeys(randomBytes);
            PeerIdConfig.writePeerIdConfig(userPath, peerIDAndEncryption.element1);
            VersionedObjectSerializer.serialize(peerIDAndEncryption.element2, CRC_LENGTH, Paths.encryptionPath(userPath), Paths.encryptionBackupPath(userPath));

            // config files
            new PeerConnectionConfig(Paths.connectionConfigPath(userPath), mainCountry);
            new NetworkConfiguration(Paths.networkConfigPath(userPath), DEFAULT_LOCAL_PORT, DEFAULT_EXTERNAL_PORT);

            // database files
            DatabaseIO.createNewDatabaseFileStructure(userPath);

            // peer knowledge base
            PeerKnowledgeBase.createNew(Paths.peerKBPath(userPath));

            // personal data file
            new PeersPersonalData(Paths.personalDataPath(userPath), DEFAULT_NICK, nick);

            // user media paths
            new MediaPaths(Paths.mediaPathsConfigPath(userPath), mediaPath, tempPath);

            // statistics file
            TransferStatistics.createNew(Paths.statisticsPath(userPath)).stop();

//            stopAndSave(
//                    userPath,
//                    peerIDAndEncryption.element1,
//                    DEFAULT_NICK,
//                    new NetworkConfiguration(0, DEFAULT_EXTERNAL_PORT),
////                    new PeersPersonalData(DEFAULT_NICK, nick),
////                    new PeerRelations(),
//                    null,
//                    null,
//                    tempPath,
//                    mediaPath,
//                    peerIDAndEncryption.element2,
//                    new TransferStatistics()
//            );

            PeerShareIO.createNewFileStructure(userPath);
//            PeerShareIO.saveLocalHash(userPath, new FileHashDatabaseWithTimestamp(RandomStringUtils.randomAlphanumeric(ID_LENGTH)));

            return userPath;
        } catch (XMLStreamException e) {
            throw new IOException("Error creating data");
        }
    }

    public static synchronized List<String> listAvailableConfigs(String basePath) throws IOException {
        List<String> userPaths = new ArrayList<>();
        File[] subFiles = new File(basePath).listFiles();
        if (subFiles != null) {
            for (File userPath : subFiles) {
                if (userPath.isDirectory() && userPath.getName().startsWith(USER_BASE_PATH)) {
                    userPaths.add(userPath.getPath());
                }
            }
        } else {
            throw new IOException(basePath + " is not a valid directory");
        }
        return userPaths;
    }

    public static synchronized PeerEngineClient load(
            String userPath,
            GeneralEvents generalEvents,
            ConnectionEvents connectionEvents,
            PeersEvents peersEvents,
            ResourceTransferEvents resourceTransferEvents,
            TempFileManagerEvents tempFileManagerEvents,
            DatabaseSynchEvents databaseSynchEvents,
            DownloadEvents downloadEvents,
            IntegrationEvents integrationEvents,
            ErrorHandler errorHandler) throws IOException {

        try {
            PeerId ownPeerId = PeerIdConfig.readPeerId(userPath);
            PeerEncryption peerEncryption = new PeerEncryption(Paths.encryptionPath(userPath), Paths.encryptionBackupPath(userPath));

            return new PeerEngineClient(
                    userPath,
                    ownPeerId,
                    peerEncryption,
                    new MediaPaths(Paths.mediaPathsConfigPath(userPath)),
                    generalEvents,
                    connectionEvents,
                    peersEvents,
                    resourceTransferEvents,
                    tempFileManagerEvents,
                    databaseSynchEvents,
                    downloadEvents,
                    integrationEvents,
                    errorHandler);
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }
}
