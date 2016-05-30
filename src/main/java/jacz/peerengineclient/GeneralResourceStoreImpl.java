package jacz.peerengineclient;

import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.util.datatransfer.GeneralResourceStore;
import jacz.peerengineservice.util.datatransfer.ResourceStoreResponse;
import jacz.peerengineservice.util.datatransfer.resource_accession.BasicFileReader;
import jacz.peerengineservice.util.datatransfer.resource_accession.TempFileReader;
import jacz.peerengineservice.util.tempfile_api.TempFileManager;
import jacz.util.hash.hashdb.FileHashDatabase;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;

/**
 * The general resource store that serves all resources
 */
public class GeneralResourceStoreImpl implements GeneralResourceStore {

    private final FileHashDatabase fileHashDatabase;

    private final TempFileManager tempFileManager;

    public GeneralResourceStoreImpl(FileHashDatabase fileHashDatabase, TempFileManager tempFileManager) {
        this.fileHashDatabase = fileHashDatabase;
        this.tempFileManager = tempFileManager;
    }

    @Override
    public ResourceStoreResponse requestResource(String resourceStore, PeerId peerID, String resourceID) {
        try {
            // first check in the file hash database
            if (fileHashDatabase.containsKey(resourceID)) {
                return ResourceStoreResponse.resourceApproved(new BasicFileReader(fileHashDatabase.getFilePath(resourceID)));
            } else {
                throw new FileNotFoundException();
            }
        } catch (FileNotFoundException e) {
            // now check with the temp file manager
            try {
                for (String tempFile : tempFileManager.getExistingTempFiles()) {
                    try {
                        HashMap<String, Serializable> userDictionary = tempFileManager.getUserDictionary(tempFile);
                        DownloadInfo downloadInfo = DownloadInfo.buildDownloadInfo(userDictionary);
                        if (resourceID.equals(downloadInfo.fileHash)) {
                            // resource found!
                            return ResourceStoreResponse.resourceApproved(new TempFileReader(tempFileManager, tempFile));
                        }
                    } catch (IOException e1) {
                        // ignore, check rest of files
                    }
                }
            } catch (IOException e2) {
                // ignore
            }
            // resource not found
            return ResourceStoreResponse.resourceNotFound();
        }
    }
}
