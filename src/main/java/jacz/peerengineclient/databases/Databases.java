package jacz.peerengineclient.databases;

import jacz.peerengineclient.file_system.Paths;
import jacz.peerengineservice.PeerID;
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

    private final Map<PeerID, String> remoteDBs;

    private final String sharedDB;

    private final String deletedDB;

    private final ItemRelations itemRelations;

    public Databases(String basePath) throws IOException, VersionedSerializationException {
        integratedDB = Paths.integratedDBPath(basePath);
        localDB = Paths.localDBPath(basePath);
        remoteDBs = new HashMap<>();
        for (PeerID peerID : Paths.listRemoteDBPeers(basePath)) {
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

    public Map<PeerID, String> getRemoteDBs() {
        return remoteDBs;
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
