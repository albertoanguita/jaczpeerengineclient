package jacz.peerengineclient;

import com.neovisionaries.i18n.CountryCode;
import jacz.peerengineclient.data.PeerShareIO;
import jacz.peerengineclient.databases.DatabaseIO;
import jacz.peerengineclient.databases.integration.IntegrationEvents;
import jacz.peerengineclient.databases.synch.DatabaseSynchEvents;
import jacz.peerengineclient.file_system.MediaPaths;
import jacz.peerengineclient.file_system.PathConstants;
import jacz.peerengineclient.file_system.PeerIdConfig;
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
import jacz.util.io.serialization.localstorage.LocalStorage;
import jacz.util.lists.tuple.Duple;
import org.apache.commons.io.FileUtils;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
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

    // @FUTURE@ todo allow changing temp and downloads dir. Store old downloads dir to be able to automatically detect files requiring move in old dirs
    // store a list of existing media dirs, one of which is the "active" dir. Provide operations to move files from one to another, remove one
    // no need to store old dirs. We only care about current one. We can make a GUI to handle downloaded files (see a tree with location of files, sizes, etc)

    public static synchronized Duple<String, PeerId> createUserConfig(String basePath, byte[] randomBytes, String nick, CountryCode mainCountry) throws IOException {
        // creates a new user account
        // first find a free user directory, then create all initial config files in it and return it
        // base path is created if needed

        if (!new File(basePath).isDirectory()) {
            FileUtils.forceMkdir(new File(basePath));
        }
        try {
            String userPath = FileGenerator.createDirectoryWithIndex(basePath, USER_BASE_PATH, "", "", false);
            String mediaPath = PathConstants.getDefaultMediaDir(userPath).getPath();
            String tempPath = PathConstants.getDefaultTempDir(userPath).getAbsolutePath();

            // create sub-directories
            for (File dir : PathConstants.getOrderedDirectories(userPath)) {
                FileUtils.forceMkdir(dir);
            }

            // id and encryption files
            Duple<PeerId, PeerEncryption> peerIDAndEncryption = PeerId.generateIdAndEncryptionKeys(randomBytes);
            PeerIdConfig.writePeerIdConfig(userPath, peerIDAndEncryption.element1);
            VersionedObjectSerializer.serialize(peerIDAndEncryption.element2, CRC_LENGTH, PathConstants.encryptionPath(userPath), PathConstants.encryptionBackupPath(userPath));

            // config files
            new PeerConnectionConfig(PathConstants.connectionConfigPath(userPath), mainCountry);
            new NetworkConfiguration(PathConstants.networkConfigPath(userPath), DEFAULT_LOCAL_PORT, DEFAULT_EXTERNAL_PORT);

            // database files
            DatabaseIO.createNewDatabaseFileStructure(userPath);

            // peer knowledge base
            PeerKnowledgeBase.createNew(PathConstants.peerKBPath(userPath));

            // personal data file
            new PeersPersonalData(PathConstants.personalDataPath(userPath), DEFAULT_NICK, nick);

            // user media paths
            new MediaPaths(PathConstants.mediaPathsConfigPath(userPath), mediaPath, tempPath);

            // statistics file
            TransferStatistics.createNew(PathConstants.statisticsPath(userPath)).stop();

            PeerShareIO.createNewFileStructure(userPath);
//            PeerShareIO.saveLocalHash(userPath, new FileHashDatabaseWithTimestamp(RandomStringUtils.randomAlphanumeric(ID_LENGTH)));

            return new Duple<>(userPath, peerIDAndEncryption.element1);
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

    public static synchronized Date profileCreationDate(String userPath) throws IOException {
        return new LocalStorage(PathConstants.connectionConfigPath(userPath)).getCreationDate();
    }

    public static synchronized Duple<PeerEngineClient, List<String>> load(
            String userPath,
            GeneralEvents generalEvents,
            ConnectionEvents connectionEvents,
            PeersEvents peersEvents,
            ResourceTransferEvents resourceTransferEvents,
            TempFileManagerEvents tempFileManagerEvents,
            DatabaseSynchEvents databaseSynchEvents,
            DownloadEvents downloadEvents,
            IntegrationEvents integrationEvents,
            ErrorEvents errorEvents) throws IOException {

        try {
            List<String> repairedFiles = new ArrayList<>();
            Duple<PeerId, List<String>> peerIdAndRepairedFiles = PeerIdConfig.readPeerId(userPath);
            PeerId ownPeerId = peerIdAndRepairedFiles.element1;
            repairedFiles.addAll(peerIdAndRepairedFiles.element2);
            PeerEncryption peerEncryption = new PeerEncryption(PathConstants.encryptionPath(userPath), true, PathConstants.encryptionBackupPath(userPath));
            repairedFiles.addAll(peerEncryption.getRepairedFiles());

            PeerEngineClient peerEngineClient = new PeerEngineClient(
                    userPath,
                    ownPeerId,
                    peerEncryption,
                    new MediaPaths(PathConstants.mediaPathsConfigPath(userPath)),
                    generalEvents,
                    connectionEvents,
                    peersEvents,
                    resourceTransferEvents,
                    tempFileManagerEvents,
                    databaseSynchEvents,
                    downloadEvents,
                    integrationEvents,
                    errorEvents);

            repairedFiles.addAll(peerEngineClient.getRepairedFiles());

            return new Duple<>(peerEngineClient, repairedFiles);
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }
}
