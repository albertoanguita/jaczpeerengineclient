package jacz.peerengineclient.file_system;

import jacz.peerengineclient.SessionManager;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.PeerRelations;
import jacz.peerengineservice.client.PeersPersonalData;
import jacz.peerengineservice.client.connection.NetworkConfiguration;
import jacz.util.hash.CRCMismatchException;
import jacz.util.io.serialization.StrCast;
import jacz.util.io.xml.XMLReader;
import jacz.util.io.xml.XMLWriter;
import jacz.util.lists.tuple.EightTuple;

import javax.xml.stream.XMLStreamException;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Creates, reads and writes the user files. It also handles backup of the files
 */
public class FileIO {

    public static EightTuple<PeerId, NetworkConfiguration, PeersPersonalData, PeerRelations, Integer, Integer, String, String> readConfig(
            String basePath,
            String defaultNick) throws FileNotFoundException, XMLStreamException, IllegalArgumentException, CRCMismatchException {
        XMLReader xmlReader = new XMLReader(Paths.configPath(basePath), true, Paths.configBackupPath(basePath));

        PeerId ownPeerId = new PeerId(xmlReader.getFieldValue("peer-id"));
        NetworkConfiguration networkConfiguration = new NetworkConfiguration(
                StrCast.asInteger(xmlReader.getFieldValue("port")),
                StrCast.asInteger(xmlReader.getFieldValue("external-port")));
        PeersPersonalData peersPersonalData = new PeersPersonalData(defaultNick, xmlReader.getFieldValue("nick"));

        PeerRelations peerRelations = new PeerRelations();
        xmlReader.getStruct("friend-peers");
        while (xmlReader.hasMoreChildren()) {
            xmlReader.getNextStruct();
            PeerId peerID = new PeerId(xmlReader.getFieldValue("peer-id"));
            String nick = xmlReader.getFieldValue("nick");
            peersPersonalData.setPeersNicks(peerID, nick);
            peerRelations.addFriendPeer(peerID);
            xmlReader.gotoParent();
        }
        xmlReader.getStruct("blocked-peers");
        while (xmlReader.hasMoreChildren()) {
            xmlReader.getNextStruct();
            PeerId peerID = new PeerId(xmlReader.getFieldValue("peer-id"));
            String nick = xmlReader.getFieldValue("nick");
            peersPersonalData.setPeersNicks(peerID, nick);
            peerRelations.addBlockedPeer(peerID);
            xmlReader.gotoParent();
        }

        Integer maxDownloadSpeed = StrCast.asInteger(xmlReader.getFieldValue("max-download-speed"));
        Integer maxUploadSpeed = StrCast.asInteger(xmlReader.getFieldValue("max-upload-speed"));

        String tempDownloadsPath = xmlReader.getFieldValue("temp-downloads-path");
        String baseDataPath = xmlReader.getFieldValue("base-data-path");

        return new EightTuple<>(
                ownPeerId,
                networkConfiguration,
                peersPersonalData,
                peerRelations,
                maxDownloadSpeed,
                maxUploadSpeed,
                tempDownloadsPath,
                baseDataPath);
    }

    public static void writeConfig(
            String basePath,
            PeerId peerID,
            NetworkConfiguration networkConfiguration,
            PeersPersonalData peersPersonalData,
            PeerRelations peerRelations,
            Integer maxDownloadSpeed,
            Integer maxUploadSpeed,
            String tempDownloadsPath,
            String baseDataPath) throws IOException, XMLStreamException {
        XMLWriter xmlWriter = new XMLWriter("config");
        xmlWriter.addField("peer-id", peerID.toString());
        xmlWriter.addField("port", networkConfiguration.getLocalPort());
        xmlWriter.addField("external-port", networkConfiguration.getExternalPort());
        xmlWriter.addField("nick", peersPersonalData.getOwnNick());

        xmlWriter.beginStruct("friend-peers");
        for (PeerId friendPeerId : peerRelations.getFriendPeers()) {
            xmlWriter.beginStruct();
            xmlWriter.addField("peer-id", friendPeerId.toString());
            xmlWriter.addField("nick", peersPersonalData.getPeerNick(friendPeerId));
            xmlWriter.endStruct();
        }
        xmlWriter.endStruct();
        xmlWriter.beginStruct("blocked-peers");
        for (PeerId blockedPeerId : peerRelations.getBlockedPeers()) {
            xmlWriter.beginStruct();
            xmlWriter.addField("peer-id", blockedPeerId.toString());
            xmlWriter.addField("nick", peersPersonalData.getPeerNick(blockedPeerId));
            xmlWriter.endStruct();
        }
        xmlWriter.endStruct();

        xmlWriter.addField("max-download-speed", maxDownloadSpeed);
        xmlWriter.addField("max-upload-speed", maxUploadSpeed);

        xmlWriter.addField("temp-downloads-path", tempDownloadsPath);
        xmlWriter.addField("base-data-path", baseDataPath);

        xmlWriter.write(Paths.configPath(basePath), SessionManager.CRC_LENGTH, Paths.configBackupPath(basePath));
    }
}
