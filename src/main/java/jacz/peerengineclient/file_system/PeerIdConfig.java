package jacz.peerengineclient.file_system;

import jacz.peerengineclient.SessionManager;
import jacz.peerengineservice.PeerId;
import jacz.util.hash.CRCMismatchException;
import jacz.util.io.xml.XMLReader;
import jacz.util.io.xml.XMLWriter;

import javax.xml.stream.XMLStreamException;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by Alberto on 09/03/2016.
 */
public class PeerIdConfig {

    public static PeerId readPeerId(String basePath) throws FileNotFoundException, XMLStreamException, CRCMismatchException {
        XMLReader xmlReader = new XMLReader(Paths.peerIdConfigPath(basePath), true, Paths.peerIdConfigBackupPath(basePath));
        return new PeerId(xmlReader.getFieldValue("peer-id"));
    }

    public static void writePeerIdConfig(String basePath, PeerId peerId) throws IOException, XMLStreamException {
        XMLWriter xmlWriter = new XMLWriter("config");
        xmlWriter.addField("peer-id", peerId.toString());
        xmlWriter.write(Paths.peerIdConfigPath(basePath), SessionManager.CRC_LENGTH, Paths.peerIdConfigBackupPath(basePath));
    }
}
