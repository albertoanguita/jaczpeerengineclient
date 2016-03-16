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
public class EngineConfig {

    public static Duple<Integer, Integer> readSpeedLimitsConfig(String basePath) throws FileNotFoundException, XMLStreamException, CRCMismatchException {
        XMLReader xmlReader = new XMLReader(Paths.engineConfigPath(basePath), true, Paths.engineConfigBackupPath(basePath));
        Integer maxDownloadSpeed = StrCast.asInteger(xmlReader.getFieldValue("max-download-speed"));
        Integer maxUploadSpeed = StrCast.asInteger(xmlReader.getFieldValue("max-upload-speed"));
        return new Duple<>(maxDownloadSpeed, maxUploadSpeed);
    }

    public static void writeSpeedLimitsConfig(String basePath, Integer maxDownloadSpeed, Integer maxUploadSpeed) throws IOException, XMLStreamException {
        XMLWriter xmlWriter = new XMLWriter("config");
        xmlWriter.addField("max-download-speed", maxDownloadSpeed);
        xmlWriter.addField("max-upload-speed", maxUploadSpeed);
        xmlWriter.write(Paths.engineConfigPath(basePath), SessionManager.CRC_LENGTH, Paths.engineConfigBackupPath(basePath));
    }
}
