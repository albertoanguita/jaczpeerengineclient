package jacz.peerengineclient;

import jacz.store.db_mediator.CorruptDataException;
import jacz.store.db_mediator.DBException;
import jacz.peerengineservice.PeerEncryption;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.client.PeerRelations;
import jacz.peerengineservice.util.data_synchronization.premade_lists.old.SimplePersonalData;
import jacz.peerengineclient.dbs_old.LibraryManagerIO;
import jacz.peerengineclient.file_system.*;
import jacz.util.files.FileUtil;
import jacz.util.hash.hashdb.FileHashDatabase;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles IO access and opens/closes sessions
 */
public class SessionManager {

    public static final String USER_BASE_PATH = "user_";

    public static synchronized String createUserConfig(String basePath, PeerID peerID, String nick, int keySizeForPeerGeneration, int maxUploadSpeed, int maxDownloadSpeed, double precision, ServersInfo.ServerInfo serverInfo, int port, String tempDownloads, String baseDataDir) throws IOException {
        // creates a new user account
        // first find a free user directory, then create all initial config files in it and return it
        // base path is created if needed
        if (!FileUtil.isDirectory(basePath)) {
            FileUtil.createDirectory(basePath);
        }
        try {
            String userPath = FileUtil.createNonExistingDirPathWithIndex(basePath, USER_BASE_PATH, "", "", false);

            String databasesPath = Paths.getDatabasesPath(userPath);
            FileUtil.createDirectory(databasesPath);
            LibraryManagerIO.createNewDatabaseFileStructure(Paths.getDatabasesPath(userPath));

            PeerIDInfo peerIDInfo = new PeerIDInfo(peerID, keySizeForPeerGeneration);
            PersonalData personalData = new PersonalData(nick);
            EngineConfig engineConfig = new EngineConfig(tempDownloads, maxUploadSpeed, maxDownloadSpeed, precision);
            GeneralConfig generalConfig = new GeneralConfig(baseDataDir);
            ServersInfo serversInfo = new ServersInfo(serverInfo);
            PeerRelations peerRelations = new PeerRelations();
            FileHashDatabase fileHashDatabase = new FileHashDatabase();
            FileIO.writePeerID(userPath, peerIDInfo);
            FileIO.writePeerEncryption(userPath, new PeerEncryption(null));
            FileIO.writePersonalData(userPath, personalData);
            FileIO.writeNetworkConfig(userPath, new NetworkConfig(port));
            FileIO.writeEngineConfig(userPath, engineConfig);
            FileIO.writeGeneralConfig(userPath, generalConfig);
            FileIO.writeServers(userPath, serversInfo);
            FileIO.writePeerRelations(userPath, peerRelations);
            FileIO.writeFileHashDatabase(userPath, fileHashDatabase);

            return userPath;
        } catch (XMLStreamException | DBException | CorruptDataException e) {
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

    public static synchronized PeerEngineClient load(String userPath, JacuzziPeerClientAction jacuzziPeerClientAction) throws IOException, XMLStreamException, ParseException {
        PeerIDInfo peerIDInfo = FileIO.readPeerID(userPath);
        PersonalData personalData = FileIO.readPersonalData(userPath);
        NetworkConfig networkConfig = FileIO.readNetworkConfig(userPath);
        EngineConfig engineConfig = FileIO.readEngineConfig(userPath);
        GeneralConfig generalConfig = FileIO.readGeneralConfig(userPath);
        ServersInfo serversInfo = FileIO.readServers(userPath);
        PeerRelations peerRelations = FileIO.readPeerRelations(userPath);
        FileHashDatabase fileHashDatabase = FileIO.readFileHashDatabase(userPath);

        PeerEngineClient peerEngineClient = new PeerEngineClient(userPath, jacuzziPeerClientAction, peerIDInfo, networkConfig.port, serversInfo.servers.get(0).ip, serversInfo.servers.get(0).port, personalData.ownNick, personalData.peerNicks, peerRelations, engineConfig.tempDownloads, fileHashDatabase, generalConfig.baseDataDir);
        peerEngineClient.setMaxDesiredDownloadSpeed(engineConfig.maxDownloadSpeed);
        peerEngineClient.setMaxDesiredUploadSpeed(engineConfig.maxUploadSpeed);
        peerEngineClient.setDownloadPartSelectionAccuracy(engineConfig.precision);
        return peerEngineClient;
    }

    public static synchronized void save(PeerEngineClient peerEngineClient) throws IOException, XMLStreamException {
        String userPath = peerEngineClient.getConfigPath();

        PeerIDInfo peerIDInfo = new PeerIDInfo(peerEngineClient.getPeerIDInfo().peerID, peerEngineClient.getPeerIDInfo().keySizeForPeerGeneration);
        Map<PeerID, String> peerNicks = new HashMap<>();
        for (Map.Entry<PeerID, SimplePersonalData> peerPersonalData : peerEngineClient.getAllPeersPersonalData().entrySet()) {
            peerNicks.put(peerPersonalData.getKey(), peerPersonalData.getValue().getNick());
        }
        PersonalData personalData = new PersonalData(peerEngineClient.getOwnNick(), peerNicks);
        NetworkConfig networkConfig = new NetworkConfig(peerEngineClient.getListeningPort());
        EngineConfig engineConfig = new EngineConfig(peerEngineClient.getTempDownloadsDirectory(), peerEngineClient.getMaxDesiredUploadSpeed(), peerEngineClient.getMaxDesiredDownloadSpeed(), peerEngineClient.getDownloadPartSelectionAccuracy());
        GeneralConfig generalConfig = new GeneralConfig(peerEngineClient.getBaseDataDir());
        ServersInfo.ServerInfo serverInfo = new ServersInfo.ServerInfo(peerEngineClient.getPeerServerData().getIp4Port().getIp(), peerEngineClient.getPeerServerData().getIp4Port().getPort());
        ServersInfo serversInfo = new ServersInfo(serverInfo);
        PeerRelations peerRelations = new PeerRelations(peerEngineClient.getFriendPeers(), peerEngineClient.getBlockedPeers());
        FileHashDatabase fileHashDatabase = peerEngineClient.getFileHashDatabase();

        FileIO.writePeerID(userPath, peerIDInfo);
        FileIO.writePersonalData(userPath, personalData);
        FileIO.writeNetworkConfig(userPath, networkConfig);
        FileIO.writeEngineConfig(userPath, engineConfig);
        FileIO.writeGeneralConfig(userPath, generalConfig);
        FileIO.writeServers(userPath, serversInfo);
        FileIO.writePeerRelations(userPath, peerRelations);
        FileIO.writeFileHashDatabase(userPath, fileHashDatabase);
    }
}
