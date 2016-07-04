package jacz.peerengineclient.data;

/**
 * Events related to the addition and removal of files from the file hash database (local repository of files)
 */
public interface FileHashDatabaseEvents {

    void fileAdded(String hash, String path);

    void fileRemoved(String hash, String path);
}
