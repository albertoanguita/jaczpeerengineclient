package jacz.peerengineclient.file_system;

import jacz.peerengineclient.SessionManager;
import jacz.peerengineservice.client.connection.NetworkConfiguration;
import jacz.util.hash.CRCMismatchException;
import jacz.util.io.serialization.StrCast;
import jacz.util.io.xml.XMLReader;
import jacz.util.io.xml.XMLWriter;

import javax.xml.stream.XMLStreamException;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by Alberto on 09/03/2016.
 */
public class NetworkConfig {

    public static NetworkConfiguration readNetworkConfig(String basePath) throws FileNotFoundException, XMLStreamException, CRCMismatchException {
        XMLReader xmlReader = new XMLReader(Paths.networkConfigPath(basePath), true, Paths.networkConfigBackupPath(basePath));
        return new NetworkConfiguration(
                StrCast.asInteger(xmlReader.getFieldValue("local-port")),
                StrCast.asInteger(xmlReader.getFieldValue("external-port")));
    }

    public static void writeNetworkConfig(String basePath, NetworkConfiguration networkConfiguration) throws IOException, XMLStreamException {
        XMLWriter xmlWriter = new XMLWriter("config");
        xmlWriter.addField("local-port", networkConfiguration.getLocalPort());
        xmlWriter.addField("external-port", networkConfiguration.getExternalPort());
        xmlWriter.write(Paths.networkConfigPath(basePath), SessionManager.CRC_LENGTH, Paths.networkConfigBackupPath(basePath));
    }
}
