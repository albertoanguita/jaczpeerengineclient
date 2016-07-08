package jacz.peerengineclient.file_system;

import jacz.peerengineclient.SessionManager;
import jacz.peerengineservice.PeerId;
import org.aanguita.jacuzzi.hash.CRCMismatchException;
import org.aanguita.jacuzzi.io.xml.XMLReader;
import org.aanguita.jacuzzi.io.xml.XMLWriter;
import org.aanguita.jacuzzi.lists.tuple.Duple;

import javax.xml.stream.XMLStreamException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

/**
 * Created by Alberto on 09/03/2016.
 */
public class PeerIdConfig {

    public static Duple<PeerId, List<String>> readPeerId(String basePath) throws IOException, XMLStreamException, CRCMismatchException {
        XMLReader xmlReader = new XMLReader(PathConstants.peerIdConfigPath(basePath), true, true, PathConstants.peerIdConfigBackupPath(basePath));
        return new Duple<>(new PeerId(xmlReader.getFieldValue("peer-id")), xmlReader.getRepairedFiles());
    }

    public static void writePeerIdConfig(String basePath, PeerId peerId) throws IOException, XMLStreamException {
        XMLWriter xmlWriter = new XMLWriter("config");
        xmlWriter.addField("peer-id", peerId.toString());
        xmlWriter.write(PathConstants.peerIdConfigPath(basePath), SessionManager.CRC_LENGTH, PathConstants.peerIdConfigBackupPath(basePath));
    }
}
