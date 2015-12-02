package jacz.peerengineclient.dbs_old.data_synch;

import jacz.store.Database;
import jacz.store.common.LibraryItem;
import jacz.store.common.TaggedLibraryItemWithImages;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.data_synchronization.old.DataAccessException;
import jacz.peerengineservice.util.data_synchronization.old.ElementNotFoundException;
import jacz.peerengineclient.dbs_old.*;
import jacz.util.files.RandomAccess;
import jacz.util.hash.hashdb.FileHashDatabase;
import jacz.util.io.object_serialization.MutableOffset;
import jacz.util.io.object_serialization.Serializer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;

/**
 * Defines an accessor utility for handling the images of any TaggedLibraryItemWithImages item
 * <p/>
 * Defines 2 levels: one for the reduced image and one for the main image. Information in both cases is sent as byte array
 * <p/>
 * Extra images are not considered in this version
 * <p/>
 * Images are placed in a common directory (one for main images, one for reduced)
 */
public class ItemsWithImagesAccessor extends GenericListAccessorWithoutInnerLists {

    private final int levelForReducedImage;

    private final String baseDir;

    protected ItemsWithImagesAccessor(Database database, String container, FileHashDatabase fileHashDatabase, LibraryManager libraryManager, ItemLockManager itemLockManager, int levelForReducedImage, PeerID remotePeerID, IntegratedDatabase integratedDatabase, String baseDir) {
        super(database, container, fileHashDatabase, libraryManager, itemLockManager, remotePeerID, integratedDatabase);
        this.levelForReducedImage = levelForReducedImage;
        this.baseDir = baseDir;
    }

    @Override
    public String getItemHash(int level, LibraryItem item) {
        TaggedLibraryItemWithImages taggedLibraryItemWithImages = (TaggedLibraryItemWithImages) item;
        if (level == levelForReducedImage) {
            return taggedLibraryItemWithImages.getMainImageReduced();
        } else {
            return taggedLibraryItemWithImages.getMainImage();
        }
    }

    @Override
    public int getLevelCount() {
        return 2;
    }

    @Override
    public void beginSynchProcess(Mode mode) {
        // ignore
    }

    @Override
    public boolean hashEqualsElement(int level) {
        return false;
    }

    @Override
    public TransmissionType getTransmissionType(int level) {
        return TransmissionType.BYTE_ARRAY;
    }

    @Override
    public boolean mustRequestElement(String index, int level, String hash) throws DataAccessException {
        try {
            // only set the hash if we have the file. Otherwise, wait until we receive the file
            if (fileHashDatabase.containsKey(hash)) {
                TaggedLibraryItemWithImages item = (TaggedLibraryItemWithImages) getItem(index);
                if (level == levelForReducedImage) {
                    item.setMainImageReduced(hash);
                } else {
                    item.setMainImage(hash);
                }
                return false;
            } else {
                return true;
            }
        } catch (Exception e) {
            throw new DataAccessException();
        }
    }

    @Override
    public Serializable getElementObject(String index, int level) throws ElementNotFoundException, DataAccessException {
        // ignore
        return null;
    }

    @Override
    public byte[] getElementByteArray(String index, int level) throws ElementNotFoundException, DataAccessException {
        // we send the file name (so the client receives a suggested file name and the file extension) followed by the file data
        synchronized (itemLockManager.getLock(container)) {
            TaggedLibraryItemWithImages item = (TaggedLibraryItemWithImages) getItem(index);
            try {
                File file;
                if (level == levelForReducedImage) {
                    file = fileHashDatabase.getFile(item.getMainImageReduced());
                } else {
                    file = fileHashDatabase.getFile(item.getMainImage());
                }
                return Serializer.addArrays(Serializer.serialize(file.getName()), jacz.util.files.RandomAccess.read(file));
            } catch (FileNotFoundException e) {
                throw new ElementNotFoundException();
            } catch (IOException e) {
                throw new DataAccessException();
            }
        }
    }

    @Override
    public int getElementByteArrayLength(String index, int level) throws ElementNotFoundException, DataAccessException {
        synchronized (itemLockManager.getLock(container)) {
            TaggedLibraryItemWithImages item = (TaggedLibraryItemWithImages) getItem(index);
            try {
                File file;
                if (level == levelForReducedImage) {
                    file = fileHashDatabase.getFile(item.getMainImageReduced());
                } else {
                    file = fileHashDatabase.getFile(item.getMainImage());
                }
                return Serializer.serialize(file.getName()).length + (int) file.length();
            } catch (FileNotFoundException e) {
                throw new ElementNotFoundException();
            }
        }
    }

    @Override
    public void addElementAsObject(String index, int level, Object element) throws DataAccessException {
        // ignore
    }

    @Override
    public void addElementAsByteArray(String index, int level, byte[] data) throws DataAccessException {
        try {
            // deserialize the file name and then generate a file with the rest of the array
            TaggedLibraryItemWithImages item = (TaggedLibraryItemWithImages) getItemCreateIfNeeded(index);
            MutableOffset offset = new MutableOffset();
            String receivedFilename = Serializer.deserializeString(data, offset);
            byte[] fileData = Serializer.deserializeRest(data, offset);
            String path;
            if (level == levelForReducedImage) {
                path = FileGenerator.generateEmptyFile(LibraryPaths.getReducedImagesPath(baseDir), receivedFilename);
            } else {
                path = FileGenerator.generateEmptyFile(LibraryPaths.getMainImagesPath(baseDir), receivedFilename);
            }
            RandomAccess.write(path, 0, fileData);
            String hash = fileHashDatabase.put(path);
            if (level == levelForReducedImage) {
                item.setMainImageReduced(hash);
            } else {
                item.setMainImage(hash);
            }
            reportRemoteModifiedItem(index);
        } catch (Exception e) {
            throw new DataAccessException();
        }
    }

    @Override
    public void endSynchProcess(Mode mode, boolean success) {
        // ignore
    }
}
