package jacz.peerengineclient;

import jacz.peerengineclient.data.PeerShareIO;
import jacz.peerengineclient.databases.DatabaseIO;
import jacz.peerengineclient.databases.integration.IntegrationEvents;
import jacz.peerengineclient.databases.synch.DatabaseSynchEvents;
import jacz.peerengineclient.file_system.*;
import jacz.peerengineservice.PeerEncryption;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.GeneralEvents;
import jacz.peerengineservice.client.connection.ConnectionEvents;
import jacz.peerengineservice.client.connection.NetworkConfiguration;
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

    private static final int DEFAULT_EXTERNAL_PORT = 37720;

    public static final int CRC_LENGTH = 8;

    // todo allow changing temp and downloads dir. Store old downloads dir to be able to automatically detect files requiring move in old dirs

    public static synchronized String createUserConfig(String basePath, byte[] randomBytes, String nick) throws IOException {
        // creates a new user account
        // first find a free user directory, then create all initial config files in it and return it
        // base path is created if needed

        if (!new File(basePath).isDirectory()) {
            FileUtils.forceMkdir(new File(basePath));
        }
        try {
            Duple<String, String> newUserDirectory = FileGenerator.createDirectoryWithIndex(basePath, USER_BASE_PATH, "", "", false);
            String userPath = newUserDirectory.element1;
            String tempPath = Paths.getDefaultTempDir(userPath).getName();
            String mediaPath = Paths.getDefaultMediaDir(userPath).getName();

            // create sub-directories
            for (File dir : Paths.getOrderedDirectories(userPath)) {
                FileUtils.forceMkdir(dir);
            }

            // database files
            DatabaseIO.createNewDatabaseFileStructure(userPath);

            // peer knowledge base
            // todo drop and create database

            Duple<PeerId, PeerEncryption> peerIDAndEncryption = PeerId.generateIdAndEncryptionKeys(randomBytes);
            stopAndSave(
                    userPath,
                    peerIDAndEncryption.element1,
                    DEFAULT_NICK,
                    new NetworkConfiguration(0, DEFAULT_EXTERNAL_PORT),
//                    new PeersPersonalData(DEFAULT_NICK, nick),
//                    new PeerRelations(),
                    null,
                    null,
                    tempPath,
                    mediaPath,
                    peerIDAndEncryption.element2,
                    new TransferStatistics()
                    );

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
            ResourceTransferEvents resourceTransferEvents,
            TempFileManagerEvents tempFileManagerEvents,
            DatabaseSynchEvents databaseSynchEvents,
            DownloadEvents downloadEvents,
            IntegrationEvents integrationEvents,
            ErrorHandler errorHandler) throws IOException {

        try {
//            EightTuple<PeerId, NetworkConfiguration, PeersPersonalData, PeerRelations, Integer, Integer, String, String> config =
//                    FileIO.readConfig(userPath, DEFAULT_NICK);
//            PeerId ownPeerId = config.element1;
//            NetworkConfiguration networkConfiguration = config.element2;
//            PeersPersonalData peersPersonalData = config.element3;
//            PeerRelations peerRelations = config.element4;
//            Integer maxDownloadSpeed = config.element5;
//            Integer maxUploadSpeed = config.element6;
//            String tempDownloadsPath = config.element7;
//            String baseMediaPath = config.element8;
            PeerId ownPeerId = PeerIdConfig.readPeerId(userPath);
            String ownNick = NickConfig.readNick(userPath);
            NetworkConfiguration networkConfiguration = NetworkConfig.readNetworkConfig(userPath);
            Duple<Integer, Integer> speedLimits = EngineConfig.readSpeedLimitsConfig(userPath);
            Integer maxDownloadSpeed = speedLimits.element1;
            Integer maxUploadSpeed = speedLimits.element2;
            Duple<String, String> paths = PathsConfig.readPathsConfig(userPath);
            String tempDownloadsPath = paths.element1;
            String baseMediaPath = paths.element2;
            PeerEncryption peerEncryption = new PeerEncryption(Paths.encryptionPath(userPath), Paths.encryptionBackupPath(userPath));
            TransferStatistics transferStatistics = new TransferStatistics(Paths.statisticsPath(userPath), Paths.statisticsBackupPath(userPath));

            PeerEngineClient peerEngineClient = new PeerEngineClient(
                    userPath,
                    ownPeerId,
                    peerEncryption,
                    ownNick,
                    networkConfiguration,
//                    peersPersonalData,
                    transferStatistics,
//                    peerRelations,
                    tempDownloadsPath,
                    baseMediaPath,
                    generalEvents,
                    connectionEvents,
                    resourceTransferEvents,
                    tempFileManagerEvents,
                    databaseSynchEvents,
                    downloadEvents,
                    integrationEvents,
                    errorHandler);
            peerEngineClient.setMaxDesiredDownloadSpeed(maxDownloadSpeed);
            peerEngineClient.setMaxDesiredUploadSpeed(maxUploadSpeed);
            return peerEngineClient;
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    public static synchronized void stopAndSave(
            String userPath,
            PeerId ownPeerId,
            String ownNick,
            NetworkConfiguration networkConfiguration,
//            PeersPersonalData peersPersonalData,
//            PeerRelations peerRelations,
            Integer maxDownloadSpeed,
            Integer maxUploadSpeed,
            String tempDownloadsPath,
            String baseMediaPath,
            PeerEncryption peerEncryption,
            TransferStatistics transferStatistics) throws IOException, XMLStreamException {
        transferStatistics.stop();
//        FileIO.writeConfig(userPath, ownPeerId, networkConfiguration, peersPersonalData, peerRelations, maxDownloadSpeed, maxUploadSpeed, tempDownloadsPath, baseMediaPath);
        PeerIdConfig.writePeerIdConfig(userPath, ownPeerId);
        NickConfig.writeNickConfig(userPath, ownNick);
        NetworkConfig.writeNetworkConfig(userPath, networkConfiguration);
        EngineConfig.writeSpeedLimitsConfig(userPath, maxDownloadSpeed, maxUploadSpeed);
        PathsConfig.writePathsConfig(userPath, tempDownloadsPath, baseMediaPath);
        VersionedObjectSerializer.serialize(peerEncryption, CRC_LENGTH, Paths.encryptionPath(userPath), Paths.encryptionBackupPath(userPath));
        VersionedObjectSerializer.serialize(transferStatistics, CRC_LENGTH, Paths.statisticsPath(userPath), Paths.statisticsBackupPath(userPath));
    }
}
