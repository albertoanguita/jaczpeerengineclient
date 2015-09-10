package jacz.peerengineclient;

import jacz.peerengineservice.util.datatransfer.resource_accession.ResourceWriter;
import jacz.util.identifier.UniqueIdentifier;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

/**
 * This class allows controlling one resource download. One object of this class is returned when issuing a
 * resource download so the user can control it. It allows pausing, resuming, stopping and cancelling the download,
 * or even modifying its streaming need level.
 */
public class DownloadManager  {

    private final jacz.peerengineservice.util.datatransfer.master.DownloadManager peerEngineDownloadManager;

    private final DownloadEvents downloadEvents;

    private final ResourceWriter resourceWriter;
    
    private final String currentPath;

    private boolean errorFlag;
    
    private String finalPath;
    
    /**
     * We store this field to avoid possible IOExceptions when reading it from the ResourceWriter. It cannot change anyway
     */
    private Map<String, Serializable> userGenericData;

    DownloadManager(jacz.peerengineservice.util.datatransfer.master.DownloadManager peerEngineDownloadManager, DownloadEvents downloadEvents, ResourceWriter resourceWriter, String currentPath, String finalPath, Map<String, Serializable> userGenericData) {
        this.peerEngineDownloadManager = peerEngineDownloadManager;
        this.downloadEvents = downloadEvents;
        this.resourceWriter = resourceWriter;
        this.currentPath = currentPath;
        errorFlag = false;
        setFinalPath(finalPath);
        this.userGenericData = userGenericData;
    }

    public synchronized void pause() {
        peerEngineDownloadManager.pause();
    }

    public synchronized void resume() {
        peerEngineDownloadManager.resume();
    }

    public synchronized void stop() {
        peerEngineDownloadManager.stop();
    }

    public synchronized void cancel() {
        peerEngineDownloadManager.cancel();
    }

    public synchronized double getStreamingNeed() {
        return peerEngineDownloadManager.getStreamingNeed();
    }

    public synchronized void setStreamingNeed(double streamingNeed) {
        peerEngineDownloadManager.setStreamingNeed(streamingNeed);
    }

    public UniqueIdentifier getId() {
        return peerEngineDownloadManager.getId();
    }

    public String getResourceID() {
        return peerEngineDownloadManager.getResourceID();
    }

    public String getStoreName() {
        return peerEngineDownloadManager.getStoreName().equals(JPeerEngineClient.DEFAULT_STORE) ? null : peerEngineDownloadManager.getStoreName();
    }

    public DownloadEvents getDownloadEvents() {
        return downloadEvents;
    }

    public Long getLength() {
        return peerEngineDownloadManager.getLength();
    }

    public String getCurrentPath() {
        return currentPath;
    }

    public boolean isErrorFlag() {
        return errorFlag;
    }

    public synchronized String getFinalPath() {
        return finalPath;
    }

    public synchronized void setFinalPath(String finalPath) {
        this.finalPath = finalPath;
        try {
            resourceWriter.setUserGenericDataField(JPeerEngineClient.OWN_GENERIC_DATA_FIELD_GROUP, JPeerEngineClient.FINAL_PATH_GENERIC_DATA_FIELD, finalPath);
        } catch (IOException e) {
            // writing procedure failed, download must be cancelled
            // we set a flag indicating that the cancellation reason is an error instead of user-issued
            errorFlag = true;
            peerEngineDownloadManager.cancel();
        }
    }

    public jacz.peerengineservice.util.datatransfer.master.Statistics getStatistics() {
        return peerEngineDownloadManager.getStatistics();
    }

    ResourceWriter getResourceWriter() {
        return resourceWriter;
    }

    public synchronized Map<String, Serializable> getUserGenericData() {
        return userGenericData;
    }
}
