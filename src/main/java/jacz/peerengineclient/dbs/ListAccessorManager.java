package jacz.peerengineclient.dbs;

import jacz.store.Database;
import jacz.store.Libraries;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.data_synchronization.ListAccessor;
import jacz.peerengineclient.dbs.data_synch.*;
import jacz.util.hash.hashdb.FileHashDatabase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides list accessor implementations upon request
 */
public class ListAccessorManager {

    private static Map<String, List<Integer>> librariesAndLevels = null;

    public static ListAccessor getListAccessor(Database database, String container, FileHashDatabase fileHashDatabase, LibraryManager libraryManager, PeerID remotePeerID, String baseDir) {
        switch (container) {

            case Libraries.AUDIO_ALBUM_LIBRARY:
                return new AudioAlbumLibraryAccessor(database, fileHashDatabase, libraryManager, libraryManager.getItemLockManager(), remotePeerID, libraryManager.getIntegratedDatabase(), baseDir);

            case Libraries.SONG_LIBRARY:
                return new SongLibraryAccessor(database, fileHashDatabase, libraryManager, libraryManager.getItemLockManager(), remotePeerID, libraryManager.getIntegratedDatabase(), baseDir);

            case Libraries.PERSON_LIBRARY:
                return new PersonLibraryAccessor(database, fileHashDatabase, libraryManager, libraryManager.getItemLockManager(), remotePeerID, libraryManager.getIntegratedDatabase(), baseDir);

            case Libraries.GROUP_CREATOR_LIBRARY:
                return new GroupCreatorLibraryAccessor(database, fileHashDatabase, libraryManager, libraryManager.getItemLockManager(), remotePeerID, libraryManager.getIntegratedDatabase(), baseDir);

            case Libraries.COMPANY_CREATOR_LIBRARY:
                return new CompanyCreatorLibraryAccessor(database, fileHashDatabase, libraryManager, libraryManager.getItemLockManager(), remotePeerID, libraryManager.getIntegratedDatabase(), baseDir);

            default:
                throw new IllegalArgumentException();
        }
    }

    public static int getFieldLevel(String library, String field) {
        switch (library) {

            case Libraries.AUDIO_ALBUM_LIBRARY:
                return AudioAlbumLibraryAccessor.getFieldLevel(field);

            case Libraries.SONG_LIBRARY:
                return SongLibraryAccessor.getFieldLevel(field);

            case Libraries.PERSON_LIBRARY:
                return PersonLibraryAccessor.getFieldLevel(field);

            case Libraries.GROUP_CREATOR_LIBRARY:
                return GroupCreatorLibraryAccessor.getFieldLevel(field);

            case Libraries.COMPANY_CREATOR_LIBRARY:
                return CompanyCreatorLibraryAccessor.getFieldLevel(field);

            default:
                throw new IllegalArgumentException();
        }
    }

    public synchronized static Map<String, List<Integer>> getLibrariesAndLevels() {
        if (librariesAndLevels == null) {
            librariesAndLevels = new HashMap<>();
//            librariesAndLevels.put(Libraries.AUDIO_ALBUM_LIBRARY, AudioAlbumLibraryAccessor.getAllLevels());
//            librariesAndLevels.put(Libraries.SONG_LIBRARY, SongLibraryAccessor.getAllLevels());
            librariesAndLevels.put(Libraries.PERSON_LIBRARY, PersonLibraryAccessor.getAllLevels());
//            librariesAndLevels.put(Libraries.GROUP_CREATOR_LIBRARY, GroupCreatorLibraryAccessor.getAllLevels());
//            librariesAndLevels.put(Libraries.COMPANY_CREATOR_LIBRARY, CompanyCreatorLibraryAccessor.getAllLevels());
        }
        return librariesAndLevels;
    }
}
