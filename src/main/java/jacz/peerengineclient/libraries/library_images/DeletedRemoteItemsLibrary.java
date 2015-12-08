package jacz.peerengineclient.libraries.library_images;

/**
 * Stores items deleted from remote databases. One item per integrated item at most
 */
public class DeletedRemoteItemsLibrary extends GenericDatabase {

    public DeletedRemoteItemsLibrary(String databasePath) {
        super(databasePath);
    }
}
