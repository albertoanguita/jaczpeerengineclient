package jacz.peerengineclient.file_system;

import jacz.util.io.object_serialization.VersionedObject;

import java.util.HashMap;
import java.util.Map;

/**
 * This class allows storing and parsing from the file system the encryption information
 */
public class PeerEncryptionIO implements VersionedObject {

//    private final PeerEncryption peerEncryption;


    @Override
    public String getCurrentVersion() {
        return "1.0";
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<String, Object>();
        // todo use list
//        map.put("keyList", peerEncryption.getPrivateKey());
        return map;
    }

    @Override
    public void deserialize(String version, Map<String, Object> attributes) throws RuntimeException {
        // todo recover list
    }

    @Override
    public void errorDeserializing(String version, Map<String, Object> attributes) {
        // todo
    }
}
