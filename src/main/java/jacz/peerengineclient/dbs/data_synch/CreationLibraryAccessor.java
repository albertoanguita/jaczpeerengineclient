package jacz.peerengineclient.dbs.data_synch;

import jacz.store.Database;
import jacz.store.common.Creation;
import jacz.store.common.LibraryItem;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.data_synchronization.old.DataAccessException;
import jacz.peerengineservice.util.data_synchronization.old.ElementNotFoundException;
import jacz.peerengineclient.dbs.IntegratedDatabase;
import jacz.peerengineclient.dbs.ItemLockManager;
import jacz.peerengineclient.dbs.LibraryManager;
import jacz.util.hash.SHA_1;
import jacz.util.hash.hashdb.FileHashDatabase;
import jacz.util.io.object_serialization.FragmentedByteArray;
import jacz.util.io.object_serialization.MutableOffset;
import jacz.util.io.object_serialization.Serializer;

import java.util.List;

/**
 * Accessor that handles the synchronization of "creation" items
 * <p/>
 * All the creation self fields are encapsulated in the same level, and transmitted as byte arrays
 * The accessor makes use of the ItemImages accessor to handle the synchronization of images
 */
public abstract class CreationLibraryAccessor extends ItemsWithImagesAccessor {

    private final int levelForCreationData;

    protected CreationLibraryAccessor(Database database, String container, FileHashDatabase fileHashDatabase, LibraryManager libraryManager, ItemLockManager itemLockManager, int levelForCreationData, int levelForReducedImages, PeerID remotePeerID, IntegratedDatabase integratedDatabase, String baseDir) {
        super(database, container, fileHashDatabase, libraryManager, itemLockManager, levelForReducedImages, remotePeerID, integratedDatabase, baseDir);
        this.levelForCreationData = levelForCreationData;
    }

    @Override
    public String getItemHash(int level, LibraryItem item) {
        if (level == levelForCreationData) {
            Creation creation = (Creation) item;
            return new SHA_1().update(creation.getTitle()).update(creation.getOriginalTitle()).update(creation.getYear()).update(creation.getCreators()).update(creation.getOwnerCompanies()).update(creation.getWriters()).update(creation.getProducers()).digestAsHex();
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
        if (level == levelForCreationData) {
            return false;
        } else {
            return super.hashEqualsElement(level);
        }
    }

    @Override
    public TransmissionType getTransmissionType(int level) {
        if (level == levelForCreationData) {
            return TransmissionType.BYTE_ARRAY;
        } else {
            return super.getTransmissionType(level);
        }
    }

    @Override
    public boolean mustRequestElement(String index, int level, String hash) throws DataAccessException {
        if (level == levelForCreationData) {
            return true;
        } else {
            return super.mustRequestElement(index, level, hash);
        }
    }

    @Override
    public byte[] getElementByteArray(String index, int level) throws ElementNotFoundException, DataAccessException {
        // we send all the creation data
        synchronized (itemLockManager.getLock(container)) {
            if (level == levelForCreationData) {
                Creation creation = (Creation) getItem(index);
                FragmentedByteArray fragmentedByteArray = new FragmentedByteArray();
                fragmentedByteArray.addArrays(
                        Serializer.serialize(creation.getTitle()),
                        Serializer.serialize(creation.getOriginalTitle()),
                        Serializer.serialize(creation.getYear()),
                        Serializer.serializeListToByteArray(creation.getCreators(), ','),
                        Serializer.serializeListToByteArray(creation.getOwnerCompanies(), ','),
                        Serializer.serializeListToByteArray(creation.getWriters(), ','),
                        Serializer.serializeListToByteArray(creation.getProducers(), ',')
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
            if (level == levelForCreationData) {
                return getElementByteArray(index, level).length;
            } else {
                return super.getElementByteArrayLength(index, level);
            }
        }
    }

    @Override
    public void addElementAsByteArray(String index, int level, byte[] data) throws DataAccessException {
        if (level == levelForCreationData) {
            try {
                Creation creation = (Creation) getItemCreateIfNeeded(index);
                MutableOffset offset = new MutableOffset();
                String title = Serializer.deserializeString(data, offset);
                String originalTitle = Serializer.deserializeString(data, offset);
                Integer year = Serializer.deserializeInt(data, offset);
                List<String> creators = Serializer.deserializeListFromByteArray(data, offset, ',');
                List<String> ownerCompanies = Serializer.deserializeListFromByteArray(data, offset, ',');
                List<String> writers = Serializer.deserializeListFromByteArray(data, offset, ',');
                List<String> producers = Serializer.deserializeListFromByteArray(data, offset, ',');
                creation.setTitle(title);
                creation.setOriginalTitle(originalTitle);
                creation.setYear(year);
                creation.setCreators(creators);
                creation.setOwnerCompanies(ownerCompanies);
                creation.setWriters(writers);
                creation.setProducers(producers);
                reportRemoteModifiedItem(index);
            } catch (Exception e) {
                throw new DataAccessException();
            }
        } else {
            super.addElementAsByteArray(index, level, data);
        }
    }
}
