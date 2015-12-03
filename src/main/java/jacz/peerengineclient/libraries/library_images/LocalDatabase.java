package jacz.peerengineclient.libraries.library_images;

import jacz.store.Database;

import java.util.HashMap;

/**
 * The local database, with items created by us
 */
public class LocalDatabase extends GenericDatabase {

    private final HashMap<String, String> itemsToIntegratedItems;

    public LocalDatabase(Database database) {
        this(database, new HashMap<>());
    }

    public LocalDatabase(Database database, HashMap<String, String> itemsToIntegratedItems) {
        super(database);
        this.itemsToIntegratedItems = itemsToIntegratedItems;
    }

    public HashMap<String, String> getItemsToIntegratedItems() {
        return itemsToIntegratedItems;
    }
}
