package jacz.peerengineclient.file_system;

import jacz.peerengineclient.SessionManager;
import jacz.util.hash.CRCMismatchException;
import jacz.util.io.serialization.StrCast;
import jacz.util.io.xml.XMLReader;
import jacz.util.io.xml.XMLWriter;
import jacz.util.lists.tuple.Duple;

import javax.xml.stream.XMLStreamException;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by Alberto on 09/03/2016.
 */
public class PathsConfig {

    public static Duple<String, String> readPathsConfig(String basePath) throws FileNotFoundException, XMLStreamException, CRCMismatchException {
        XMLReader xmlReader = new XMLReader(Paths.pathsConfigPath(basePath), true, Paths.pathsConfigBackupPath(basePath));
        String tempDownloadsPath = xmlReader.getFieldValue("temp-downloads-path");
        String baseMediaPath = xmlReader.getFieldValue("base-media-path");
        return new Duple<>(tempDownloadsPath, baseMediaPath);
    }

    public static void writePathsConfig(String basePath, String tempDownloadsPath, String baseMediaPath) throws IOException, XMLStreamException {
        XMLWriter xmlWriter = new XMLWriter("config");
        xmlWriter.addField("temp-downloads-path", tempDownloadsPath);
        xmlWriter.addField("base-media-path", baseMediaPath);
        xmlWriter.write(Paths.pathsConfigPath(basePath), SessionManager.CRC_LENGTH, Paths.pathsConfigBackupPath(basePath));
    }
}
