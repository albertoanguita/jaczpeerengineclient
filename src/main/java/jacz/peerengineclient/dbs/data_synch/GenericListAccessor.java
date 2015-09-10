package jacz.peerengineclient.dbs.data_synch;

import jacz.peerengineservice.util.data_synchronization.old.DataAccessException;
import jacz.peerengineservice.util.data_synchronization.old.ElementNotFoundException;
import jacz.peerengineservice.util.data_synchronization.old.IndexAndHash;
import jacz.peerengineservice.util.data_synchronization.old.ServerSynchRequestAnswer;
import jacz.store.Database;
import jacz.store.ResultSet;
import jacz.store.common.LibraryItem;
import jacz.store.db_mediator.CorruptDataException;
import jacz.store.db_mediator.DBException;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.data_synchronization.*;
import jacz.peerengineclient.dbs.IntegratedDatabase;
import jacz.peerengineclient.dbs.ItemLockManager;
import jacz.peerengineclient.dbs.LibraryManager;
import jacz.util.hash.hashdb.FileHashDatabase;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Generic list accessor for item containers
 * <p/>
 * It would also handle the synch of the LibraryItem attributes, but non of them requires it:
 * - identifier: not needed, as it is transferred in the index
 * - creationDate: not needed, as that information has no sense out of local
 * - modificationDate: not needed, same as above
 * <p/>
 * Tags in TaggedLibraryItem do not need to be synched either, as they are local information
 */
public abstract class GenericListAccessor implements ListAccessor {

    protected final Database database;

    protected final String container;

    protected final FileHashDatabase fileHashDatabase;

    protected final LibraryManager libraryManager;

    protected final ItemLockManager itemLockManager;

    /**
     * If this accessor is for a remote database, this is the PeerID of that database
     */
    private final PeerID remotePeerID;

    private final IntegratedDatabase integratedDatabase;

    protected GenericListAccessor(Database database, String container, FileHashDatabase fileHashDatabase, LibraryManager libraryManager, ItemLockManager itemLockManager, PeerID remotePeerID, IntegratedDatabase integratedDatabase) {
        this.database = database;
        this.container = container;
        this.fileHashDatabase = fileHashDatabase;
        this.libraryManager = libraryManager;
        this.itemLockManager = itemLockManager;
        this.remotePeerID = remotePeerID;
        if (remotePeerID == null) {
            this.integratedDatabase = integratedDatabase;
        } else {
            this.integratedDatabase = null;
        }
    }

    public abstract String getItemHash(int level, LibraryItem item);

    protected LibraryItem getItem(String id) throws ElementNotFoundException, DataAccessException {
        try {
            LibraryItem item = database.getItem(container, id);
            if (item != null) {
                return item;
            } else {
                throw new ElementNotFoundException();
            }
        } catch (DBException | CorruptDataException | ParseException | IOException e) {
            throw new DataAccessException();
        }
    }

    protected LibraryItem getItemCreateIfNeeded(String id) throws DataAccessException {
        try {
            return getItem(id);
        } catch (ElementNotFoundException e) {
            // the element did not exist -> create it
            try {
                return database.createNewItem(container, id);
            } catch (Exception e1) {
                throw new DataAccessException();
            }
        }
    }


    @Override
    public Collection<IndexAndHash> getHashList(int level) throws DataAccessException {
        List<IndexAndHash> indexAndHashList = new ArrayList<>();
        try {
            synchronized (itemLockManager.getLock(container)) {
                ResultSet resultSet = database.getAllItems(container);
                while (resultSet.hasNext()) {
                    LibraryItem item = resultSet.next();
                    // todo we should not include images that are not in the file hash database, or that the file is missing. Otherwise -> mess because transfer will always fail
                    if (integratedDatabase == null || integratedDatabase.containsKeyToLocalItem(item.getIdentifier())) {
                        // for the integrated database, only consider items that are shared (have a related local item)
                        indexAndHashList.add(new IndexAndHash(item.getIdentifier(), getItemHash(level, item)));
                    }
                }
            }
            return indexAndHashList;
        } catch (Exception e) {
            throw new DataAccessException();
        }
    }

    @Override
    public String getElementHash(String index, int requestLevel) throws ElementNotFoundException, DataAccessException {
        synchronized (itemLockManager.getLock(container)) {
            return getItemHash(requestLevel, getItem(index));
        }
    }

    @Override
    public boolean mustEraseOldIndexes() {
        return true;
    }

    @Override
    public void eraseElements(Collection<String> indexes) throws DataAccessException {
        for (String index : indexes) {
            try {
                database.removeItem(container, index);
                reportRemoteModifiedItem(index);
            } catch (Exception e) {
                throw new DataAccessException();
            }
        }
    }

    @Override
    public ServerSynchRequestAnswer initiateListSynchronizationAsServer(PeerID clientPeerID, int level, boolean singleElement) {
        if (!singleElement) {
            return libraryManager.requestForSharedLibrarySynchFromRemotePeer(clientPeerID, container, level);
        } else {
            return libraryManager.requestForSharedLibraryItemSynchFromRemotePeer(clientPeerID, container, level);
        }
    }

    protected void reportRemoteModifiedItem(String index) {
        libraryManager.remoteItemModified(remotePeerID, container, index);
    }
}
