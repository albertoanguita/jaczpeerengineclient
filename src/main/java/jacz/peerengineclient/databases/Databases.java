package jacz.peerengineclient.databases;

import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineclient.file_system.PathConstants;
import jacz.peerengineservice.PeerId;
import org.aanguita.jacuzzi.io.serialization.VersionedSerializationException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * This class provides access to all media databases
 */
public class Databases {

    private final String basePath;

    private final String integratedDB;

    private final String localDB;

    private final Map<PeerId, String> remoteDBs;

    private final String sharedDB;

    private final String deletedDB;

    private final ItemRelations itemRelations;

    public Databases(PeerEngineClient peerEngineClient, String basePath) throws IOException, VersionedSerializationException {
        this.basePath = basePath;
        integratedDB = PathConstants.integratedDBPath(basePath);
        localDB = PathConstants.localDBPath(basePath);
        remoteDBs = new HashMap<>();
        for (PeerId peerID : PathConstants.listRemoteDBPeers(basePath)) {
            remoteDBs.put(peerID, PathConstants.remoteDBPath(basePath, peerID));
        }
        sharedDB = PathConstants.sharedDBPath(basePath);
        deletedDB = PathConstants.deletedDBPath(basePath);
        itemRelations = new ItemRelations(basePath);
    }

    public String getIntegratedDB() {
        return integratedDB;
    }

    public String getLocalDB() {
        return localDB;
    }

//    public Map<PeerId, String> getRemoteDBs() {
//        return remoteDBs;
//    }

    public synchronized boolean containsRemoteDB(PeerId peerID) {
        return remoteDBs.containsKey(peerID);
    }

    public synchronized String getRemoteDB(PeerId peerID) throws IOException {
        if (!remoteDBs.containsKey(peerID)) {
            String dbPath = DatabaseIO.createNewRemoteDatabase(basePath, peerID);
            remoteDBs.put(peerID, dbPath);
            return remoteDBs.get(peerID);
        }
        return remoteDBs.get(peerID);
    }

    public synchronized void removeRemoteDB(PeerId peerID) {
        remoteDBs.remove(peerID);
    }

    public String getSharedDB() {
        return sharedDB;
    }

    public String getDeletedDB() {
        return deletedDB;
    }

    public ItemRelations getItemRelations() {
        return itemRelations;
    }
}
