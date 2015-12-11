package jacz.peerengineclient.file_system;

import jacz.peerengineservice.PeerEncryption;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.client.PeerClientData;
import jacz.peerengineservice.client.PeerRelations;
import jacz.peerengineservice.client.PeerServerData;
import jacz.peerengineservice.client.PeersPersonalData;
import jacz.peerengineservice.client.connection.NetworkConfiguration;
import jacz.util.files.FileReaderWriter;
import jacz.util.hash.hashdb.FileHashDatabase;
import jacz.util.io.object_serialization.StrCast;
import jacz.util.io.object_serialization.XMLReader;
import jacz.util.io.xml.Element;
import jacz.util.io.xml.XMLDom;
import jacz.util.lists.Four_Tuple;
import jacz.util.lists.tuple.SixTuple;
import jacz.util.network.IP4Port;

import javax.xml.stream.XMLStreamException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

/**
 * Creates, reads and writes the user files. It also handles backup of the files
 * <p>
 * todo meter un formato especifioc para parametros guardados en fich config que sean null (ej: NOT_DEFINED)
 */
public class FileIO {

    public static SixTuple<PeerID, NetworkConfiguration, PeersPersonalData, PeerRelations, Integer, Integer> readConfig(
            String basePath,
            String defaultNick) throws FileNotFoundException, XMLStreamException, IllegalArgumentException {
        XMLReader xmlReader = new XMLReader(Paths.getConfigPath(basePath), Paths.getConfigBackupPath(basePath));

        PeerID ownPeerID = new PeerID(xmlReader.getFieldValue("peer-id"));
        NetworkConfiguration networkConfiguration = new NetworkConfiguration(
                StrCast.asInteger(xmlReader.getFieldValue("port")),
                StrCast.asInteger(xmlReader.getFieldValue("external-port")));
        PeersPersonalData peersPersonalData = new PeersPersonalData(defaultNick, xmlReader.getFieldValue("nick"));

        PeerRelations peerRelations = new PeerRelations();
        xmlReader.getStruct("friend-peers");
        while (xmlReader.hasMoreChildren()) {
            xmlReader.getNextStruct();
            PeerID peerID = new PeerID(xmlReader.getFieldValue("peer-id"));
            String nick = xmlReader.getFieldValue("nick");
            peersPersonalData.setPeersNicks(peerID, nick);
            peerRelations.addFriendPeer(peerID);
            xmlReader.gotoParent();
        }
        xmlReader.getStruct("blocked-peers");
        while (xmlReader.hasMoreChildren()) {
            xmlReader.getNextStruct();
            PeerID peerID = new PeerID(xmlReader.getFieldValue("peer-id"));
            String nick = xmlReader.getFieldValue("nick");
            peersPersonalData.setPeersNicks(peerID, nick);
            peerRelations.addBlockedPeer(peerID);
            xmlReader.gotoParent();
        }

        Integer maxDownloadSpeed = StrCast.asInteger(xmlReader.getFieldValue("max-download-speed"));
        Integer maxUploadSpeed = StrCast.asInteger(xmlReader.getFieldValue("max-upload-speed"));

        return new SixTuple<>(ownPeerID, networkConfiguration, peersPersonalData, peerRelations, maxDownloadSpeed, maxUploadSpeed);
    }


    public static PeerClientData readPeerClientData(String userPath) throws FileNotFoundException, XMLStreamException, ParseException {
        XMLReader xmlReader = new XMLReader(PathsOld.getPeerClientData(userPath));
        xmlReader.getStruct("peer-server-data");
        String ip = xmlReader.getFieldValue("ip");
        int port = StrCast.asInteger(xmlReader.getFieldValue("port"));
        PeerServerData peerServerData = new PeerServerData(new IP4Port(ip, port));
        xmlReader.gotoParent();
        return new PeerClientData(new PeerID(xmlReader.getFieldValue("peer-id")), StrCast.asInteger(xmlReader.getFieldValue("port")), peerServerData);
    }

    public static PeersPersonalData readPeersPersonalData(String userPath) throws FileNotFoundException, XMLStreamException, ParseException {
        XMLReader xmlReader = new XMLReader(path);
        PeersPersonalData peersPersonalData = new PeersPersonalData("UNNAMED_PEER", xmlReader.getFieldValue("nick"));
        xmlReader.getStruct("friend-peers");
        while (xmlReader.hasMoreChildren()) {
            xmlReader.getNextStruct();
            PeerID peerID = new PeerID(xmlReader.getFieldValue("peer-id"));
            String nick = xmlReader.getFieldValue("nick");
            peersPersonalData.setPeersNicks(peerID, nick);
            xmlReader.gotoParent();
        }
        xmlReader.getStruct("blocked-peers");
        while (xmlReader.hasMoreChildren()) {
            xmlReader.getNextStruct();
            PeerID peerID = new PeerID(xmlReader.getFieldValue("peer-id"));
            String nick = xmlReader.getFieldValue("nick");
            peersPersonalData.setPeersNicks(peerID, nick);
            xmlReader.gotoParent();
        }
        return peersPersonalData;
    }

    public static PeerRelations readPeerRelations(String userPath) throws FileNotFoundException, XMLStreamException, ParseException {
        XMLReader xmlReader = new XMLReader(path);
        PeerRelations peerRelations = new PeerRelations();
        xmlReader.getStruct("friend-peers");
        while (xmlReader.hasMoreChildren()) {
            xmlReader.getNextStruct();
            PeerID peerID = new PeerID(xmlReader.getFieldValue("peer-id"));
            peerRelations.addFriendPeer(peerID);
            xmlReader.gotoParent();
        }
        xmlReader.getStruct("blocked-peers");
        while (xmlReader.hasMoreChildren()) {
            xmlReader.getNextStruct();
            PeerID peerID = new PeerID(xmlReader.getFieldValue("peer-id"));
            peerRelations.addBlockedPeer(peerID);
            xmlReader.gotoParent();
        }
        return peerRelations;
    }

    public static PeerIDInfo readPeerID(String userPath) throws FileNotFoundException, XMLStreamException, ParseException {
        Element root = XMLDom.parse(PathsOld.getPeerIdFile(userPath));
        Element peerIDElement = root.getChild("peer-id");
        PeerID peerID = new PeerID(peerIDElement.getText());
        Element keySizeForPeerGenerationElement = root.getChild("key-size-for-peer-generation");
        int keySizeForPeerGeneration = Integer.parseInt(keySizeForPeerGenerationElement.getText());
        Element creationDateElement = root.getChild("creation-date");
        Date creationDate = DateFormatting.SIMPLE_DATE_FORMAT.parse(creationDateElement.getText());
        return new PeerIDInfo(peerID, keySizeForPeerGeneration, creationDate);
    }

    public static void writePeerID(String userPath, PeerIDInfo peerIDInfo) throws IOException, XMLStreamException {
        Element root = new Element("peer-info");
        Element peerIDElement = new Element("peer-id");
        peerIDElement.setText(peerIDInfo.peerID.toString());
        root.addChild(peerIDElement);
        Element keySizeForPeerGenerationElement = new Element("key-size-for-peer-generation");
        keySizeForPeerGenerationElement.setText(Integer.toString(peerIDInfo.keySizeForPeerGeneration));
        root.addChild(keySizeForPeerGenerationElement);
        Element creationDateElement = new Element("creation-date");
        creationDateElement.setText(DateFormatting.SIMPLE_DATE_FORMAT.format(peerIDInfo.creationDate));
        root.addChild(creationDateElement);
        XMLDom.write(PathsOld.getPeerIdFile(userPath), root);
    }

    public static PeerEncryption readPeerEncryption(String userPath) throws IOException, ClassNotFoundException {
        // todo store individual keys, so class update is supported
        return (PeerEncryption) FileReaderWriter.readObject(PathsOld.getEncryptionFile(userPath));
    }

    public static void writePeerEncryption(String userPath, PeerEncryption peerEncryption) throws IOException {
        FileReaderWriter.writeObject(PathsOld.getEncryptionFile(userPath), peerEncryption);
    }

    public static PersonalData readPersonalData(String userPath) throws FileNotFoundException, XMLStreamException, ParseException {
        Element root = XMLDom.parse(PathsOld.getPersonalDataFile(userPath));
        Element ownElement = root.getChild("own");
        Element ownNickElement = ownElement.getChild("nick");
        String ownNick = ownNickElement.getText();
        Element restElement = root.getChild("rest");
        Map<PeerID, String> peerNicks = new HashMap<>();
        for (Element peerDataElement : restElement.getChildren()) {
            peerNicks.put(new PeerID(peerDataElement.getChild("peer-id").getText()), peerDataElement.getChild("nick").getText());
        }
        return new PersonalData(ownNick, peerNicks);
    }

    public static void writePersonalData(String userPath, PersonalData personalData) throws IOException, XMLStreamException {
        Element root = new Element("personal-data");
        Element ownElement = new Element("own");
        Element ownNickElement = new Element("nick");
        ownNickElement.setText(personalData.ownNick);
        ownElement.addChild(ownNickElement);
        root.addChild(ownElement);
        Element restElement = new Element("rest");
        for (Map.Entry<PeerID, String> peerData : personalData.peerNicks.entrySet()) {
            Element peerDataElement = new Element("peer-data");
            Element peerIDElement = new Element("peer-id");
            peerIDElement.setText(peerData.getKey().toString());
            Element nickElement = new Element("nick");
            nickElement.setText(peerData.getValue());
            peerDataElement.addChild(peerIDElement);
            peerDataElement.addChild(nickElement);
            restElement.addChild(peerDataElement);
        }
        root.addChild(restElement);
        XMLDom.write(PathsOld.getPersonalDataFile(userPath), root);
    }

    public static NetworkConfig readNetworkConfig(String userPath) throws FileNotFoundException, XMLStreamException {
        Element root = XMLDom.parse(PathsOld.getNetworkConfigFile(userPath));
        Element portElement = root.getChild("port");
        return new NetworkConfig(Integer.parseInt(portElement.getText()));
    }

    public static void writeNetworkConfig(String userPath, NetworkConfig networkConfig) throws IOException, XMLStreamException {
        Element root = new Element("network-config");
        Element portElement = new Element("port");
        portElement.setText(Integer.toString(networkConfig.port));
        root.addChild(portElement);
        XMLDom.write(PathsOld.getNetworkConfigFile(userPath), root);
    }

    public static EngineConfig readEngineConfig(String userPath) throws FileNotFoundException, XMLStreamException {
        Element root = XMLDom.parse(PathsOld.getEngineConfigFile(userPath));
        Element tempDownloadsElement = root.getChild("temp-downloads");
        String tempDownloads = tempDownloadsElement.getText();
        Element maxUploadSpeedElement = root.getChild("max-upload-speed");
        int maxUploadSpeed = Integer.parseInt(maxUploadSpeedElement.getText());
        Element maxDownloadSpeedElement = root.getChild("max-download-speed");
        int maxDownloadSpeed = Integer.parseInt(maxDownloadSpeedElement.getText());
        Element precisionElement = root.getChild("precision");
        double precision = Double.parseDouble(precisionElement.getText());
        return new EngineConfig(tempDownloads, maxUploadSpeed, maxDownloadSpeed, precision);
    }

    public static void writeEngineConfig(String userPath, EngineConfig engineConfig) throws IOException, XMLStreamException {
        Element root = new Element("engine-config");
        Element tempDownloadsElement = new Element("temp-downloads");
        tempDownloadsElement.setText(engineConfig.tempDownloads);
        root.addChild(tempDownloadsElement);
        Element maxUploadSpeedElement = new Element("max-upload-speed");
        maxUploadSpeedElement.setText(Integer.toString(engineConfig.maxUploadSpeed));
        root.addChild(maxUploadSpeedElement);
        Element maxDownloadSpeedElement = new Element("max-download-speed");
        maxDownloadSpeedElement.setText(Integer.toString(engineConfig.maxDownloadSpeed));
        root.addChild(maxDownloadSpeedElement);
        Element precisionElement = new Element("precision");
        precisionElement.setText(Double.toString(engineConfig.precision));
        root.addChild(precisionElement);
        XMLDom.write(PathsOld.getEngineConfigFile(userPath), root);
    }

    public static GeneralConfig readGeneralConfig(String userPath) throws FileNotFoundException, XMLStreamException {
        Element root = XMLDom.parse(PathsOld.getGeneralConfigFile(userPath));
        Element baseDataDirElement = root.getChild("base-data-dir");
        String baseDataDir = baseDataDirElement.getText();
        return new GeneralConfig(baseDataDir);
    }

    public static void writeGeneralConfig(String userPath, GeneralConfig generalConfig) throws IOException, XMLStreamException {
        Element root = new Element("general-config");
        Element baseDataDirElement = new Element("base-data-dir");
        baseDataDirElement.setText(generalConfig.baseDataDir);
        root.addChild(baseDataDirElement);
        XMLDom.write(PathsOld.getGeneralConfigFile(userPath), root);
    }

    public static ServersInfo readServers(String userPath) throws FileNotFoundException, XMLStreamException {
        Element root = XMLDom.parse(PathsOld.getServersFile(userPath));
        List<Element> servers = root.getChildren("server");
        List<ServersInfo.ServerInfo> serverInfoList = new ArrayList<>();
        for (Element serverElement : servers) {
            Element ipElement = serverElement.getChild("ip");
            String ip = ipElement.getText();
            Element portElement = serverElement.getChild("port");
            int port = Integer.parseInt(portElement.getText());
            serverInfoList.add(new ServersInfo.ServerInfo(ip, port));
        }
        return new ServersInfo(serverInfoList);
    }

    public static void writeServers(String userPath, ServersInfo serversInfo) throws IOException, XMLStreamException {
        Element root = new Element("servers");
        for (ServersInfo.ServerInfo serverInfo : serversInfo.servers) {
            Element serverElement = new Element("server");
            Element ipElement = new Element("ip");
            ipElement.setText(serverInfo.ip);
            serverElement.addChild(ipElement);
            Element portElement = new Element("port");
            portElement.setText(Integer.toString(serverInfo.port));
            serverElement.addChild(portElement);
            root.addChild(serverElement);
        }
        XMLDom.write(PathsOld.getServersFile(userPath), root);
    }

//    public static PeerRelations readPeerRelations(String userPath) throws FileNotFoundException, XMLStreamException {
//        Element root = XMLDom.parse(PathsOld.getPeerRelationsFile(userPath));
//        Set<PeerID> friendPeers = buildPeerSet(root.getChild("friend-peers").getChildren());
//        Set<PeerID> blockedPeers = buildPeerSet(root.getChild("blocked-peers").getChildren());
//        return new PeerRelations(friendPeers, blockedPeers);
//    }

    private static Set<PeerID> buildPeerSet(List<Element> elements) {
        Set<PeerID> peerIDs = new HashSet<>();
        for (Element element : elements) {
            peerIDs.add(new PeerID(element.getText()));
        }
        return peerIDs;
    }

    public static void writePeerRelations(String userPath, PeerRelations peerRelations) throws IOException, XMLStreamException {
        Element root = new Element("peer-relations");
        Element friendPeersElement = new Element("friend-peers");
        populatePeersElement(friendPeersElement, peerRelations.getFriendPeers());
        root.addChild(friendPeersElement);
        Element blockedPeersElement = new Element("blocked-peers");
        populatePeersElement(blockedPeersElement, peerRelations.getBlockedPeers());
        root.addChild(blockedPeersElement);
        XMLDom.write(PathsOld.getPeerRelationsFile(userPath), root);
    }

    private static void populatePeersElement(Element peersElement, Set<PeerID> peerIDs) {
        for (PeerID peerID : peerIDs) {
            Element peerElement = new Element("peer-id");
            peerElement.setText(peerID.toString());
            peersElement.addChild(peerElement);
        }
    }

//    public static PeerConfig readConfig(String userPath) {
//        return null;
//    }
//
//    public static void writePeerConfig(String userPath, PeerConfig peerConfig) {
//
//    }

    public static FileHashDatabase readFileHashDatabase(String userPath) throws IOException {
        try {
            return (FileHashDatabase) FileReaderWriter.readObject(PathsOld.getFileHashDatabaseFile(userPath));
        } catch (ClassNotFoundException e) {
            throw new IOException();
        }
    }

    public static void writeFileHashDatabase(String userPath, FileHashDatabase fileHashDatabase) throws IOException {
        FileReaderWriter.writeObject(PathsOld.getFileHashDatabaseFile(userPath), fileHashDatabase);
    }

//    public static Triple<IntegratedDatabase, LocalDatabase, Map<PeerID, RemoteDatabase>> readDatabases(String userPath) throws IOException {
//        try {
//            String integratedDatabasePath = PathsOld.getIntegratedDatabasePath(userPath);
//            Database databaseForIntegrated = new Database(new CSVDBMediator(integratedDatabasePath), true);
//            Date dateOfLastIntegration = (Date) FileReaderWriter.readObject(integratedDatabasePath + "dateOfLastIntegration.bin");
//            //noinspection unchecked
//            HashMap<String, String> itemsToLocalItems = (HashMap<String, String>) FileReaderWriter.readObject(integratedDatabasePath + "itemsToLocalItems.bin");
//            //noinspection unchecked
//            HashMap<String, List<IntegratedDatabase.PeerAndId>> itemsToRemoteItems = (HashMap<String, List<IntegratedDatabase.PeerAndId>>) FileReaderWriter.readObject(integratedDatabasePath + "itemsToRemoteItems.bin");
//            IntegratedDatabase integratedDatabase = new IntegratedDatabase(databaseForIntegrated, dateOfLastIntegration, itemsToLocalItems, itemsToRemoteItems);
//
//            String localDatabasePath = PathsOld.getLocalDatabasePath(userPath);
//            Database databaseForLocal = new Database(new CSVDBMediator(localDatabasePath), true);
//            //noinspection unchecked
//            HashMap<String, String> itemsToIntegratedItems = (HashMap<String, String>) FileReaderWriter.readObject(integratedDatabasePath + "itemsToIntegratedItems.bin");
//            LocalDatabase localDatabase = new LocalDatabase(databaseForLocal, itemsToIntegratedItems);
//
//
//
//        } catch (ClassNotFoundException | DBException | CorruptDataException e) {
//            throw new IOException("Corrupt data!");
//        }
//    }
}
