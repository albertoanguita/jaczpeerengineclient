package jacz.peerengineclient.stores.store_images;

import jacz.store.Database;

/**
 * Created by Alberto on 02/12/2015.
 */
public abstract class GenericDatabase {

    /**
     * Path to the integrated data store
     */
    private final Database database;

    public GenericDatabase(Database database) {
        this.database = database;
    }

    public Database getDatabase() {
        return database;
    }
}
