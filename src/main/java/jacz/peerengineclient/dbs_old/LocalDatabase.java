package jacz.peerengineclient.dbs_old;

import jacz.store.Database;

import java.util.HashMap;

/**
 * The local database, with items created by us
 */
public class LocalDatabase {

    private final Database database;

    private final HashMap<String, String> itemsToIntegratedItems;

    public LocalDatabase(Database database) {
        this.database = database;
        itemsToIntegratedItems = new HashMap<>();
    }

    public LocalDatabase(Database database, HashMap<String, String> itemsToIntegratedItems) {
        this.database = database;
        this.itemsToIntegratedItems = itemsToIntegratedItems;
    }

    public Database getDatabase() {
        return database;
    }

    public HashMap<String, String> getItemsToIntegratedItems() {
        return itemsToIntegratedItems;
    }
}
