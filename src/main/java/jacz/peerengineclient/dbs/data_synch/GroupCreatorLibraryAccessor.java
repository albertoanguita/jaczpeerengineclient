package jacz.peerengineclient.dbs.data_synch;

import jacz.store.Database;
import jacz.store.Libraries;
import jacz.store.common.GroupCreator;
import jacz.store.common.LibraryItem;
import jacz.store.common.TaggedLibraryItemWithImages;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Accessor for group creators
 */
public class GroupCreatorLibraryAccessor extends CreatorLibraryAccessor {

    public static int LEVEL_FOR_GROUP_CREATOR_DATA = 0;

    public static int LEVEL_FOR_REDUCED_IMAGES = 1;

    public static int LEVEL_FOR_MAIN_IMAGES = 2;

    public GroupCreatorLibraryAccessor(Database database, FileHashDatabase fileHashDatabase, LibraryManager libraryManager, ItemLockManager itemLockManager, PeerID remotePeerID, IntegratedDatabase integratedDatabase, String baseDir) {
        super(database, Libraries.GROUP_CREATOR_LIBRARY, fileHashDatabase, libraryManager, itemLockManager, LEVEL_FOR_GROUP_CREATOR_DATA, LEVEL_FOR_REDUCED_IMAGES, remotePeerID, integratedDatabase, baseDir);
    }

    @Override
    public String getItemHash(int level, LibraryItem item) {
        if (level == levelForCreatorData) {
            GroupCreator groupCreator = (GroupCreator) item;
            return new SHA_1().update(super.getItemHash(level, item)).update(groupCreator.getPersons()).digestAsHex();
        } else {
            return super.getItemHash(level, item);
        }
    }

    @Override
    public byte[] getElementByteArray(String index, int level) throws ElementNotFoundException, DataAccessException {
        // we send all the group creator data
        synchronized (itemLockManager.getLock(container)) {
            if (level == levelForCreatorData) {
                GroupCreator groupCreator = (GroupCreator) getItem(index);
                FragmentedByteArray fragmentedByteArray = new FragmentedByteArray();
                fragmentedByteArray.addArrays(
                        Serializer.serializeListToByteArray(groupCreator.getPersons(), ','),
                        super.getElementByteArray(index, level)
                );
                return fragmentedByteArray.generateArray();
            } else {
                return super.getElementByteArray(index, level);
            }
        }
    }

    @Override
    public void addElementAsByteArray(String index, int level, byte[] data) throws DataAccessException {
        if (level == levelForCreatorData) {
            try {
                GroupCreator groupCreator = (GroupCreator) getItemCreateIfNeeded(index);
                MutableOffset offset = new MutableOffset();
                List<String> aliases = Serializer.deserializeListFromByteArray(data, offset, ',');
                groupCreator.setPersons(aliases);
                super.addElementAsByteArray(index, level, Serializer.deserializeRest(data, offset));
            } catch (Exception e) {
                throw new DataAccessException();
            }
        } else {
            super.addElementAsByteArray(index, level, data);
        }
    }

    public static int getFieldLevel(String field) {
        switch (field) {
            case TaggedLibraryItemWithImages.MAIN_IMAGE:
                return LEVEL_FOR_MAIN_IMAGES;

            case TaggedLibraryItemWithImages.MAIN_IMAGE_REDUCED:
                return LEVEL_FOR_REDUCED_IMAGES;

            default:
                return LEVEL_FOR_GROUP_CREATOR_DATA;
        }
    }

    public static List<Integer> getAllLevels() {
        List<Integer> levels = new ArrayList<>();
        levels.add(LEVEL_FOR_GROUP_CREATOR_DATA);
        levels.add(LEVEL_FOR_REDUCED_IMAGES);
        levels.add(LEVEL_FOR_MAIN_IMAGES);
        return levels;
    }
}
