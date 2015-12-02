package jacz.peerengineclient.dbs_old.data_synch;

/**
 * Defines the server transmission for the images of a library item with images. Each of this items has a main reduced image (transferred before
 * the actual main image to give a preview), the main image, and a list of optional additional images. Each image is in fact a FileImage item.
 * <p/>
 * Each FileImage item is defined with two levels: one for the metadata (length, comment...) and one for the file itself. The additional images are
 * provided with an inner list of two levels (0 and 1). Hence we define a ListAccessor for a FileImage object, which we reuse three times
 */
public abstract class ItemImages {


//    /**
//     * Generic list accessor for a list of images. Two levels are defined: one for the metadata (position, name, length, comment and resolution)
//     * and one for the image file data itself
//     * <p/>
//     * The underlying implementation must specify how to access the images
//     */
//    private static class ImageFileAccessor implements ListAccessor {
//
//        private Map<String, Integer> identifierListAndPosition;
//
//        private final Database database;
//
////        private final ImageFileLibrary imageFileLibrary;
//
//        private final LibraryManager libraryManager;
//
//        /**
//         * Null if local database
//         */
//        private final PeerID remotePeerID;
//
//        private final int levelForMetadata;
//
//        private final FileHashDatabase fileHashDatabase;
//
//        private final String baseDir;
//
//        protected ImageFileAccessor(Map<String, Integer> identifierListAndPosition, Database database, LibraryManager libraryManager, PeerID remotePeerID, int levelForMetadata, FileHashDatabase fileHashDatabase, String baseDir) {
//            this.identifierListAndPosition = identifierListAndPosition;
//            this.database = database;
////            this.imageFileLibrary = (ImageFileLibrary) database.getLibrary(Database.Library.IMAGE_FILE_LIBRARY);
//            this.libraryManager = libraryManager;
//            this.remotePeerID = remotePeerID;
//            this.levelForMetadata = levelForMetadata;
//            this.fileHashDatabase = fileHashDatabase;
//            this.baseDir = baseDir;
//        }
//
//
//        private synchronized void setIdentifierListAndPosition(Map<String, Integer> identifierListAndPosition) {
//            this.identifierListAndPosition = identifierListAndPosition;
//        }
//
////        public abstract ImageFile getImageFile(String id);
//
////        public abstract void modifiedImageFile(String id);
////
////        public abstract ImageFile addImageFile(String id);
//
////        public abstract void removeItem(String id);
//
//        private synchronized Duple<ImageFile, Integer> findImageFileAndPosition(String id) throws ElementNotFoundException, DataAccessException {
//            if (identifierListAndPosition.containsKey(id)) {
//                try {
////                    ImageFile imageFile = (ImageFile) database.getItem("", id);
////                    if (imageFile != null) {
//                        return new Duple<>(imageFile, identifierListAndPosition.get(id));
//                    } else {
//                        throw new ElementNotFoundException();
//                    }
//                } catch (Exception e) {
//                    throw new DataAccessException();
//                }
//            } else {
//                throw new ElementNotFoundException();
//            }
//        }
//
//        @Override
//        public int getLevelCount() {
//            return 2;
//        }
//
//        @Override
//        public void beginSynchProcess(Mode mode) {
//            //To change body of implemented methods use File | Settings | File Templates.
//        }
//
//        @Override
//        public synchronized Collection<IndexAndHash> getHashList(int level) throws DataAccessException {
//            List<IndexAndHash> indexAndHashList = new ArrayList<>();
//            for (String id : identifierListAndPosition.keySet()) {
//                try {
//                    Duple<ImageFile, Integer> imageFileAndPosition = findImageFileAndPosition(id);
//                    ImageFile imageFile = imageFileAndPosition.element1;
//                    int position = imageFileAndPosition.element2;
//                    String hash;
//                    if (level == levelForMetadata) {
//                        hash = getMetadataHash(imageFile, position);
//                    } else { // level for file
//                        hash = getFileHash(imageFile);
//                    }
//                    indexAndHashList.add(new IndexAndHash(id, hash));
//                } catch (ElementNotFoundException e) {
//                    throw new DataAccessException();
//                }
//            }
//            return indexAndHashList;
//        }
//
//        private String getMetadataHash(ImageFile imageFile, int position) throws DataAccessException {
//            // SHA-1 hash for file length, comment, xSize and ySize
//            // todo xsize and ysize
//            try {
//                SHA_1 sha_1 = new SHA_1();
//                return sha_1.update(position).update(imageFile.getFileName()).update(imageFile.getFileLength()).update(imageFile.getComment()).digestAsHex();
//            } catch (DBException | ParseException e) {
//                throw new DataAccessException();
//            }
//        }
//
//        private String getFileHash(ImageFile imageFile) throws DataAccessException {
//            try {
//                return imageFile.getFileHash();
//            } catch (DBException | ParseException e) {
//                throw new DataAccessException();
//            }
//        }
//
//        @Override
//        public boolean hashEqualsElement(int level) {
//            // none of the hashes equals the elements
//            return false;
//        }
//
//        @Override
//        public TransmissionType getTransmissionType(int level) {
//            return TransmissionType.BYTE_ARRAY;
//        }
//
//        @Override
//        public List<Integer> getInnerListLevels(int level) {
//            // cannot happen
//            return null;
//        }
//
//        @Override
//        public ListAccessor getInnerList(String index, int level, boolean buildElementIfNeeded) throws ElementNotFoundException, DataAccessException {
//            // cannot happen
//            return null;
//        }
//
//        @Override
//        public boolean mustRequestElement(String index, int level, String hash) throws DataAccessException {
//            return true;
//        }
//
//        @Override
//        public String getElementHash(String index, int requestLevel) throws ElementNotFoundException, DataAccessException {
//            Duple<ImageFile, Integer> imageFileAndPosition = findImageFileAndPosition(index);
//            if (requestLevel == levelForMetadata) {
//                return getMetadataHash(imageFileAndPosition.element1, imageFileAndPosition.element2);
//            } else { // level for file
//                return getFileHash(imageFileAndPosition.element1);
//            }
//        }
//
//        @Override
//        public Serializable getElementObject(String index, int level) throws ElementNotFoundException, DataAccessException {
//            // ignore
//            return null;
//        }
//
//        @Override
//        public byte[] getElementByteArray(String index, int level) throws ElementNotFoundException, DataAccessException {
//            Duple<ImageFile, Integer> imageFileAndPosition = findImageFileAndPosition(index);
//            ImageFile imageFile = imageFileAndPosition.element1;
//            int position = imageFileAndPosition.element2;
//            try {
//                if (level == levelForMetadata) {
//                    // concatenate metadata fields in a byte array
//                    return Serializer.addArrays(Serializer.serialize(position), Serializer.serialize(imageFile.getFileName()), Serializer.serialize(imageFile.getFileLength()), Serializer.serialize(imageFile.getComment()), Serializer.serialize(imageFile.getFileLength()), Serializer.serialize(imageFile.getFileLength()));
//                } else { // level for file
//                    // read the file into a byte array and return it
//                    return jacz.util.files.RandomAccess.read(fileHashDatabase.getFile(imageFile.getFileHash()));
//                }
//            } catch (DBException | ParseException | IOException e) {
//                throw new DataAccessException();
//            }
//        }
//
//        @Override
//        public int getElementByteArrayLength(String index, int level) throws ElementNotFoundException, DataAccessException {
//            try {
//                if (level == levelForMetadata) {
//                    // concatenate metadata fields in a byte array
//                    return getElementByteArray(index, level).length;
//                } else { // level for file
//                    // read the file into a byte array and return it
//                    ImageFile imageFile = findImageFileAndPosition(index).element1;
//                    return (int) fileHashDatabase.getFile(imageFile.getFileHash()).length();
//                }
//            } catch (DBException | ParseException | IOException e) {
//                throw new DataAccessException();
//            }
//        }
//
//        @Override
//        public void addElementAsObject(String index, int level, Object element) throws DataAccessException {
//            // ignore
//        }
//
//        @Override
//        public synchronized void addElementAsByteArray(String index, int level, byte[] data) throws DataAccessException {
//            ImageFile imageFile;
//            try {
//                Duple<ImageFile, Integer> imageFileAndPosition = findImageFileAndPosition(index);
//                imageFile = imageFileAndPosition.element1;
//            } catch (ElementNotFoundException e) {
//                // new image
//                imageFile = null;
//            }
//
////            if (identifierListAndPosition.containsKey(index)) {
////                imageFile = getImageFile(index);
////            } else {
////                // new image file
//////                imageFile = addImageFile(index);
////                imageFile = new ImageFile(database, );
////                mustAddImage = true;
////            }
//            try {
//                if (level == levelForMetadata) {
//                    // deserialize metadata fields
//                    MutableOffset offset = new MutableOffset();
//                    int position = Serializer.deserializeInt(data, offset);
//                    String fileName = Serializer.deserializeString(data, offset);
//                    long fileLength = Serializer.deserializeLong(data, offset);
//                    String comment = Serializer.deserializeString(data, offset);
//
//                    if (imageFile != null) {
//                        // existing image
//                        identifierListAndPosition.put(index, position);
//                        imageFile.setFileName(fileName);
//                        imageFile.setFileLength(fileLength);
//                        imageFile.setComment(comment);
////                        modifiedImageFile(index);
//                    } else {
//                        // new image
////                        Map<String, String> values = new HashMap<>();
////                        values.put("fileName", fileName);
////                        values.put("fileLength", Long.toString(fileLength));
////                        values.put("comment", comment);
////                        imageFile = new ImageFile(database, values);
//                        imageFile = (ImageFile) database.createNewItem("");
//                        imageFile.setFileName(fileName);
//                        imageFile.setFileLength(fileLength);
//                        imageFile.setComment(comment);
//                        identifierListAndPosition.put(imageFile.getIdentifier(), position);
////                        addImageFile(imageFile.getIdentifier());
//                        // todo rest of fields
//                    }
//                } else { // level for file
//                    // write hash to hash field and data to FileHashDatabase
//                    String hash = fileHashDatabase.put(baseDir, data);
//                    if (imageFile != null) {
//                        // existing image
//                        imageFile.setFileHash(hash);
////                        modifiedImageFile(index);
//                    } else {
//                        // new image
////                        Map<String, String> values = new HashMap<>();
////                        values.put("fileHash", hash);
////                        imageFile = new ImageFile(database, values);
//                        imageFile = (ImageFile) database.createNewItem("");
//                        imageFile.setFileHash(hash);
//                        identifierListAndPosition.put(imageFile.getIdentifier(), 0);
////                        addImageFile(imageFile.getIdentifier());
//                    }
//                }
//                libraryManager.remoteItemModified(remotePeerID, "", index);
//                // todo same for modified and added?
//            } catch (DBException | ParseException | IOException e) {
//                throw new DataAccessException();
//            }
//        }
//
//        @Override
//        public boolean mustEraseOldIndexes() {
//            return true;
//        }
//
//        @Override
//        public synchronized void eraseElements(Collection<String> indexes) throws DataAccessException {
//            for (String index : indexes) {
//                identifierListAndPosition.remove(index);
////                removeItem(index);
//                libraryManager.remoteItemModified(remotePeerID, "", index);
//            }
//        }
//
//        @Override
//        public void endSynchProcess(Mode mode, boolean success) {
//            //To change body of implemented methods use File | Settings | File Templates.
//        }
//
//        @Override
//        public ServerSynchRequestAnswer initiateListSynchronizationAsServer(PeerID clientPeerID, int level, boolean singleElement) {
//            // todo library manager
//            return null;  //To change body of implemented methods use File | Settings | File Templates.
//        }
//    }
//
//    private final int levelForReducedMainImageMetadata;
//
//    private final int levelForReducedMainImageFile;
//
//    private final int levelForMainImageMetadata;
//
//    private final int levelForMainImageFile;
//
//    /**
//     * Accessor for the reduced image
//     */
//    private ImageFileAccessor reducedMainImageAccessor;
//
//    /**
//     * Accessor for the main image
//     */
//    private ImageFileAccessor mainImageAccessor;
//
//    /**
//     * Accessor for the list of additional images
//     */
//    private ImageFileAccessor extraImagesAccessor;
//
//    protected ItemImages(Database database, String container, FileHashDatabase fileHashDatabase, LibraryManager libraryManager, int levelForReducedMainImageMetadata, int levelForReducedMainImageFile, int levelForMainImageMetadata, int levelForMainImageFile, int levelForExtraImages, PeerID remotePeerID) {
//        super(database, container, fileHashDatabase, libraryManager, remotePeerID);
//        this.levelForReducedMainImageMetadata = levelForReducedMainImageMetadata;
//        this.levelForReducedMainImageFile = levelForReducedMainImageFile;
//        this.levelForMainImageMetadata = levelForMainImageMetadata;
//        this.levelForMainImageFile = levelForMainImageFile;
//    }
//
//    @Override
//    public int getLevelCount() {
//        return 5;
//    }
//
//    @Override
//    public Collection<IndexAndHash> getHashList(int level) throws DataAccessException {
//        if (level == levelForReducedMainImageMetadata || level == levelForReducedMainImageFile) {
//            return reducedMainImageAccessor.getHashList(level);
//        } else if (level == levelForMainImageMetadata || level == levelForMainImageFile) {
//            return mainImageAccessor.getHashList(level);
//        } else {
//            // todo
//            return null;
//        }
//    }
//
//    @Override
//    public boolean hashEqualsElement(int level) {
//        return false;
//    }
//
//    @Override
//    public TransmissionType getTransmissionType(int level) {
//        if (level == levelForReducedMainImageMetadata || level == levelForReducedMainImageFile) {
//            return reducedMainImageAccessor.getTransmissionType(level);
//        } else if (level == levelForMainImageMetadata || level == levelForMainImageFile) {
//            return mainImageAccessor.getTransmissionType(level);
//        } else {
//            return TransmissionType.INNER_LISTS;
//        }
//    }
//
//    @Override
//    public List<Integer> getInnerListLevels(int level) {
//        // for the extra images. Levels are always 0 and 1
//        List<Integer> innerListLevels = new ArrayList<>();
//        innerListLevels.add(0);
//        innerListLevels.add(1);
//        return innerListLevels;
//    }
//
//    @Override
//    public ListAccessor getInnerList(String index, int level, boolean buildElementIfNeeded) throws ElementNotFoundException, DataAccessException {
//        // for the extra images. Levels are always 0 and 1
//        try {
//            TaggedLibraryItemWithImages taggedLibraryItemWithImages = (TaggedLibraryItemWithImages) database.getItem(container, index);
//            if (taggedLibraryItemWithImages == null) {
//                throw new ElementNotFoundException();
//            }
//            // todo must specify position in images!!
//    //        try {
//    //            // todo
//    //            return new ImageFileAccessor(taggedLibraryItemWithImages.getImages(), 0, null, null);
//    //        } catch (DBException | ParseException e) {
//    //            throw new DataAccessException();
//    //        }
//            return null;
//        } catch (Exception e) {
//            throw new DataAccessException();
//        }
//    }
//
//    @Override
//    public String getElementHash(String index, int requestLevel) throws ElementNotFoundException, DataAccessException {
//        return null;  //To change body of implemented methods use File | Settings | File Templates.
//    }
}
