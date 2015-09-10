package jacz.peerengineclient.dbs.data_synch;

import jacz.store.Database;
import jacz.store.Libraries;
import jacz.store.common.TaggedLibraryItemWithImages;
import jacz.peerengineservice.PeerID;
import jacz.peerengineclient.dbs.IntegratedDatabase;
import jacz.peerengineclient.dbs.ItemLockManager;
import jacz.peerengineclient.dbs.LibraryManager;
import jacz.util.hash.hashdb.FileHashDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * Accessor for company creators
 */
public class CompanyCreatorLibraryAccessor extends CreatorLibraryAccessor {

    public static int LEVEL_FOR_COMPANY_CREATOR_DATA = 0;

    public static int LEVEL_FOR_REDUCED_IMAGES = 1;

    public static int LEVEL_FOR_MAIN_IMAGES = 2;

    public CompanyCreatorLibraryAccessor(Database database, FileHashDatabase fileHashDatabase, LibraryManager libraryManager, ItemLockManager itemLockManager, PeerID remotePeerID, IntegratedDatabase integratedDatabase, String baseDir) {
        super(database, Libraries.COMPANY_CREATOR_LIBRARY, fileHashDatabase, libraryManager, itemLockManager, LEVEL_FOR_COMPANY_CREATOR_DATA, LEVEL_FOR_REDUCED_IMAGES, remotePeerID, integratedDatabase, baseDir);
    }

    public static int getFieldLevel(String field) {
        switch (field) {
            case TaggedLibraryItemWithImages.MAIN_IMAGE:
                return LEVEL_FOR_MAIN_IMAGES;

            case TaggedLibraryItemWithImages.MAIN_IMAGE_REDUCED:
                return LEVEL_FOR_REDUCED_IMAGES;

            default:
                return LEVEL_FOR_COMPANY_CREATOR_DATA;
        }
    }

    public static List<Integer> getAllLevels() {
        List<Integer> levels = new ArrayList<>();
        levels.add(LEVEL_FOR_COMPANY_CREATOR_DATA);
        levels.add(LEVEL_FOR_REDUCED_IMAGES);
        levels.add(LEVEL_FOR_MAIN_IMAGES);
        return levels;
    }
}
