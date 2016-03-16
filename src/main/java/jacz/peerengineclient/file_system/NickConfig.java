package jacz.peerengineclient.file_system;

import jacz.peerengineclient.SessionManager;
import jacz.util.hash.CRCMismatchException;
import jacz.util.io.xml.XMLReader;
import jacz.util.io.xml.XMLWriter;

import javax.xml.stream.XMLStreamException;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by Alberto on 09/03/2016.
 */
public class NickConfig {

    public static String readNick(String basePath) throws FileNotFoundException, XMLStreamException, CRCMismatchException {
        XMLReader xmlReader = new XMLReader(Paths.nickConfigPath(basePath), true, Paths.nickConfigBackupPath(basePath));
        return xmlReader.getFieldValue("nick");
    }

    public static void writeNickConfig(String basePath, String ownNick) throws IOException, XMLStreamException {
        XMLWriter xmlWriter = new XMLWriter("config");
        xmlWriter.addField("nick", ownNick);
        xmlWriter.write(Paths.nickConfigPath(basePath), SessionManager.CRC_LENGTH, Paths.nickConfigBackupPath(basePath));
    }
}
