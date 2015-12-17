package jacz.peerengineclient.libraries.library_images;

/**
 * The library that we share to others. This library is a subset of the integrated library, composed by items
 * that have physical files attached. It is calculated from the integrated database, by copying part of its elements.
 */
public class SharedLibrary extends GenericDatabase {

    public SharedLibrary(String databasePath) {
        super(databasePath);
    }
}
