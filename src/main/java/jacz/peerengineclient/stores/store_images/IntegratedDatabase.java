package jacz.peerengineclient.stores.store_images;

import jacz.peerengineservice.PeerID;
import jacz.store.Database;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * The integrated database. This is what the user visualizes in the interface, and is the result of merging the local database and the remote
 * databases
 */
public class IntegratedDatabase extends GenericDatabase {

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

    /**
     * Date at which the last item integration took place
     */
    private final Date dateOfLastIntegration;

    /**
     * Links integrated ids to local ids (for faster item lookup)
     */
    private final HashMap<String, String> itemsToLocalItems;

    /**
     * Links integrated ids to remote ids (for faster item lookup)
     */
    private final HashMap<String, List<PeerAndId>> itemsToRemoteItems;

    /**
     * Sets up a fresh store. The store itself must be created before
     *
     * @param database path to the store
     */
    public IntegratedDatabase(Database database) {
        this(database, new Date(), new HashMap<String, String>(), new HashMap<String, List<PeerAndId>>());
    }

    /**
     * Recovers an existing store
     *
     * @param database              path to the store
     * @param dateOfLastIntegration date of last item integration
     * @param itemsToLocalItems     pointers of integrated ids to local ids
     * @param itemsToRemoteItems    pointers of integrated ids to remote ids
     */
    public IntegratedDatabase(Database database, Date dateOfLastIntegration, HashMap<String, String> itemsToLocalItems, HashMap<String, List<PeerAndId>> itemsToRemoteItems) {
        super(database);
        this.dateOfLastIntegration = dateOfLastIntegration;
        this.itemsToLocalItems = itemsToLocalItems;
        this.itemsToRemoteItems = itemsToRemoteItems;
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
