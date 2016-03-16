package jacz.peerengineclient.databases;

import jacz.peerengineclient.file_system.Paths;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.UnavailablePeerException;
import jacz.util.io.serialization.VersionedSerializationException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Alberto on 21/12/2015.
 */
public class Databases {

    private final String integratedDB;

    private final String localDB;

    private final Map<PeerId, String> remoteDBs;

    private final String sharedDB;

    private final String deletedDB;

    private final ItemRelations itemRelations;

    public Databases(String basePath) throws IOException, VersionedSerializationException {
        integratedDB = Paths.integratedDBPath(basePath);
        localDB = Paths.localDBPath(basePath);
        remoteDBs = new HashMap<>();
        for (PeerId peerID : Paths.listRemoteDBPeers(basePath)) {
            remoteDBs.put(peerID, Paths.remoteDBPath(basePath, peerID));
        }
        sharedDB = Paths.sharedDBPath(basePath);
        deletedDB = Paths.deletedDBPath(basePath);
        itemRelations = new ItemRelations(Paths.itemRelationsPath(basePath), Paths.itemRelationsBackupPath(basePath));
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

    public synchronized String getRemoteDB(PeerId peerID) throws UnavailablePeerException {
        if (remoteDBs.containsKey(peerID)) {
            return remoteDBs.get(peerID);
        } else {
            throw new UnavailablePeerException();
        }
    }

    public synchronized void addRemoteDB(PeerId peerID, String dbPath) {
        remoteDBs.put(peerID, dbPath);
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
