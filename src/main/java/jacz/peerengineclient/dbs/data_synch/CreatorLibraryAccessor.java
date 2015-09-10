package jacz.peerengineclient.dbs.data_synch;

import jacz.store.Database;
import jacz.store.common.Creator;
import jacz.store.common.LibraryItem;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.data_synchronization.DataAccessException;
import jacz.peerengineservice.util.data_synchronization.ElementNotFoundException;
import jacz.peerengineclient.dbs.IntegratedDatabase;
import jacz.peerengineclient.dbs.ItemLockManager;
import jacz.peerengineclient.dbs.LibraryManager;
import jacz.util.hash.SHA_1;
import jacz.util.hash.hashdb.FileHashDatabase;
import jacz.util.io.object_serialization.FragmentedByteArray;
import jacz.util.io.object_serialization.MutableOffset;
import jacz.util.io.object_serialization.Serializer;

/**
 * Accessor for creator. Name goes into unique level
 */
public abstract class CreatorLibraryAccessor extends ItemsWithImagesAccessor {

    protected final int levelForCreatorData;

    protected CreatorLibraryAccessor(Database database, String container, FileHashDatabase fileHashDatabase, LibraryManager libraryManager, ItemLockManager itemLockManager, int levelForCreatorData, int levelForReducedImages, PeerID remotePeerID, IntegratedDatabase integratedDatabase, String baseDir) {
        super(database, container, fileHashDatabase, libraryManager, itemLockManager, levelForReducedImages, remotePeerID, integratedDatabase, baseDir);
        this.levelForCreatorData = levelForCreatorData;
    }

    @Override
    public String getItemHash(int level, LibraryItem item) {
        if (level == levelForCreatorData) {
            Creator creator = (Creator) item;
            return new SHA_1().update(creator.getName()).digestAsHex();
        } else {
            return super.getItemHash(level, item);
        }
    }

    @Override
    public int getLevelCount() {
        return 1 + super.getLevelCount();
    }

    @Override
    public boolean hashEqualsElement(int level) {
        if (level == levelForCreatorData) {
            return false;
        } else {
            return super.hashEqualsElement(level);
        }
    }

    @Override
    public TransmissionType getTransmissionType(int level) {
        if (level == levelForCreatorData) {
            return TransmissionType.BYTE_ARRAY;
        } else {
            return super.getTransmissionType(level);
        }
    }

    @Override
    public boolean mustRequestElement(String index, int level, String hash) throws DataAccessException {
        if (level == levelForCreatorData) {
            return true;
        } else {
            return super.mustRequestElement(index, level, hash);
        }
    }

    @Override
    public byte[] getElementByteArray(String index, int level) throws ElementNotFoundException, DataAccessException {
        // we send all the creator data
        synchronized (itemLockManager.getLock(container)) {
            if (level == levelForCreatorData) {
                Creator creator = (Creator) getItem(index);
                FragmentedByteArray fragmentedByteArray = new FragmentedByteArray();
                fragmentedByteArray.addArrays(
                        Serializer.serialize(creator.getName())
                );
                return fragmentedByteArray.generateArray();
            } else {
                return super.getElementByteArray(index, level);
            }
        }
    }

    @Override
    public int getElementByteArrayLength(String index, int level) throws ElementNotFoundException, DataAccessException {
        synchronized (itemLockManager.getLock(container)) {
            if (level == levelForCreatorData) {
                return getElementByteArray(index, level).length;
            } else {
                return super.getElementByteArrayLength(index, level);
            }
        }
    }

    @Override
    public void addElementAsByteArray(String index, int level, byte[] data) throws DataAccessException {
        if (level == levelForCreatorData) {
            try {
                Creator creator = (Creator) getItemCreateIfNeeded(index);
                MutableOffset offset = new MutableOffset();
                String name = Serializer.deserializeString(data, offset);
                creator.setName(name);
                reportRemoteModifiedItem(index);
            } catch (Exception e) {
                throw new DataAccessException();
            }
        } else {
            super.addElementAsByteArray(index, level, data);
        }
    }
}
