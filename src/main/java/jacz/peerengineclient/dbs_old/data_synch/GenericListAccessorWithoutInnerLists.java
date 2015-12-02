package jacz.peerengineclient.dbs_old.data_synch;

import jacz.store.Database;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.data_synchronization.DataAccessException;
import jacz.peerengineservice.util.data_synchronization.ElementNotFoundException;
import jacz.peerengineservice.util.data_synchronization.ListAccessor;
import jacz.peerengineclient.dbs_old.IntegratedDatabase;
import jacz.peerengineclient.dbs_old.ItemLockManager;
import jacz.peerengineclient.dbs_old.LibraryManager;
import jacz.util.hash.hashdb.FileHashDatabase;

import java.util.List;

/**
 * Generic list accessor which does not make use of inner lists
 */
public abstract class GenericListAccessorWithoutInnerLists extends GenericListAccessor {

    protected GenericListAccessorWithoutInnerLists(Database database, String container, FileHashDatabase fileHashDatabase, LibraryManager libraryManager, ItemLockManager itemLockManager, PeerID remotePeerID, IntegratedDatabase integratedDatabase) {
        super(database, container, fileHashDatabase, libraryManager, itemLockManager, remotePeerID, integratedDatabase);
    }

    @Override
    public List<Integer> getInnerListLevels(int level) {
        return null;
    }

    @Override
    public ListAccessor getInnerList(String index, int level, boolean buildElementIfNeeded) throws ElementNotFoundException, DataAccessException {
        return null;
    }
}
