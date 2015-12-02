package jacz.peerengineclient.dbs_old;

import jacz.store.Database;
import jacz.peerengineservice.PeerID;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * The integrated database. This is what the user visualizes in the interface, and is the result of merging the local database and the remote
 * databases
 */
public class IntegratedDatabase {

    public static class PeerAndId implements Serializable {

        public final PeerID peerID;

        public final String id;

        public PeerAndId(PeerID peerID, String id) {
            this.peerID = peerID;
            this.id = id;
        }

        @Override
        public String toString() {
            return "PeerAndId{" +
                    "peerID=" + peerID +
                    ", id='" + id + '\'' +
                    '}';
        }
    }

    private static final Object ITEMS_TO_LOCAL_ITEMS_LOCK = new Object();

    private static final Object ITEMS_TO_REMOTE_ITEMS_LOCK = new Object();

    private final Database database;

    private final Date dateOfLastIntegration;

    private final HashMap<String, String> itemsToLocalItems;

    private final HashMap<String, List<PeerAndId>> itemsToRemoteItems;

    public IntegratedDatabase(Database database) {
        this(database, new Date(), new HashMap<String, String>(), new HashMap<String, List<PeerAndId>>());
    }

    public IntegratedDatabase(Database database, Date dateOfLastIntegration, HashMap<String, String> itemsToLocalItems, HashMap<String, List<PeerAndId>> itemsToRemoteItems) {
        this.database = database;
        this.dateOfLastIntegration = dateOfLastIntegration;
        this.itemsToLocalItems = itemsToLocalItems;
        this.itemsToRemoteItems = itemsToRemoteItems;
    }

    public Database getDatabase() {
        return database;
    }

    public Date getDateOfLastIntegration() {
        return dateOfLastIntegration;
    }

    public HashMap<String, String> getItemsToLocalItems() {
        // only for saving object state!!!
        return itemsToLocalItems;
    }

    public boolean containsKeyToLocalItem(String integratedId) {
        synchronized (ITEMS_TO_LOCAL_ITEMS_LOCK) {
            return itemsToLocalItems.containsKey(integratedId);
        }
    }

    public String getItemToLocalItem(String integratedId) {
        synchronized (ITEMS_TO_LOCAL_ITEMS_LOCK) {
            return itemsToLocalItems.get(integratedId);
        }
    }

    public void putItemToLocalItem(String integratedId, String localId) {
        synchronized (ITEMS_TO_LOCAL_ITEMS_LOCK) {
            itemsToLocalItems.put(integratedId, localId);
        }
    }

    public HashMap<String, List<PeerAndId>> getItemsToRemoteItems() {
        return itemsToRemoteItems;
    }

    public void addRemoteLink(String integratedID, PeerID remotePeer, String remoteID) {
        synchronized (ITEMS_TO_REMOTE_ITEMS_LOCK) {
            if (!itemsToRemoteItems.containsKey(integratedID)) {
                itemsToRemoteItems.put(integratedID, new ArrayList<PeerAndId>());
            }
            itemsToRemoteItems.get(integratedID).add(new PeerAndId(remotePeer, remoteID));
        }
    }

    public List<PeerAndId> getRemotePeerAndID(String integratedID) {
        synchronized (ITEMS_TO_REMOTE_ITEMS_LOCK) {
            if (itemsToRemoteItems.containsKey(integratedID)) {
                return itemsToRemoteItems.get(integratedID);
            } else {
                return new ArrayList<>();
            }
        }
    }
}
