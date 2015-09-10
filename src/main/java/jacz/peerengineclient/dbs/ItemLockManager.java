package jacz.peerengineclient.dbs;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides locks for accessing the integrated database. One lock is given per library. The goal is to avoid simultaneous read and write accessions
 * <p/>
 * Integrated items require several operations when being build or modified. During this time, their lock will be acquired. Any client trying to
 * read any data from the integrated database should ALWAYS first acquire the corresponding lock (the lock of the library of the item)
 */
public class ItemLockManager {

    /**
     * One lock per library for the integrated database
     */
    private final Map<String, Object> itemLocks;

    public ItemLockManager() {
        itemLocks = new HashMap<>();
    }

    public synchronized Object getLock(String library) {
        if (!itemLocks.containsKey(library)) {
            itemLocks.put(library, new Object());
        }
        return itemLocks.get(library);
    }
}
