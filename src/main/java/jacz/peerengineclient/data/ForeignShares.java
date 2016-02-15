package jacz.peerengineclient.data;

import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.client.PeerClient;
import jacz.peerengineservice.util.ForeignStoreShare;

/**
 * This class stores the two foreign shares used and corresponding to the two resource stores defined in the API.
 * One is for video and subtitle files, and the other is for image files.
 * <p>
 * We only use one object of this class, as these shares store the resources share for all peers
 */
public class ForeignShares {

    private final ForeignStoreShare videosShare;

    private final ForeignStoreShare imagesShare;

    public ForeignShares(PeerClient peerClient) {
        this.videosShare = new ForeignStoreShare(peerClient);
        this.imagesShare = new ForeignStoreShare(peerClient);
        peerClient.addForeignResourceStore(PeerEngineClient.MEDIA_STORE, videosShare);
        peerClient.addForeignResourceStore(PeerEngineClient.IMAGE_STORE, imagesShare);
    }

    public ForeignStoreShare getVideosShare() {
        return videosShare;
    }

    public ForeignStoreShare getImagesShare() {
        return imagesShare;
    }

    public synchronized void addResourceProvider(String resourceID, PeerID peerID) {
        videosShare.addResourceProvider(resourceID, peerID);
        imagesShare.addResourceProvider(resourceID, peerID);
    }

    public synchronized void removeResourceProvider(String resourceID, PeerID peerID) {
        videosShare.removeResourceProvider(resourceID, peerID);
        imagesShare.removeResourceProvider(resourceID, peerID);
    }

    public synchronized void removeResourceProvider(PeerID peerID) {
        videosShare.removeResourceProvider(peerID);
        imagesShare.removeResourceProvider(peerID);
    }

}
