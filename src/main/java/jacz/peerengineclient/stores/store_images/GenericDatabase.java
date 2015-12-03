package jacz.peerengineclient.stores.store_images;

import jacz.peerengineservice.util.data_synchronization.DataAccessor;
import jacz.store.Database;

/**
 * Created by Alberto on 02/12/2015.
 */
public abstract class GenericDatabase {

    /**
     * Path to the integrated data store
     */
    private final Database database;

    private final DataAccessor dataAccessor;

    public GenericDatabase(Database database, DataAccessor dataAccessor) {
        this.database = database;
        this.dataAccessor = dataAccessor;
    }

    public Database getDatabase() {
        return database;
    }

    public DataAccessor getDataAccessor() {
        return dataAccessor;
    }
}
