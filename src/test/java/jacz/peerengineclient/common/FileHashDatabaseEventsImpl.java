package jacz.peerengineclient.common;

import jacz.peerengineclient.data.FileHashDatabaseEvents;

/**
 * Created by alberto on 7/4/16.
 */
public class FileHashDatabaseEventsImpl implements FileHashDatabaseEvents {

    @Override
    public void fileAdded(String hash, String path) {
        System.out.println("File added. Hash: " + hash + ". Path: " + path);
    }

    @Override
    public void fileRemoved(String hash, String path) {
        System.out.println("File removed. Hash: " + hash + ". Path: " + path);
    }
}
