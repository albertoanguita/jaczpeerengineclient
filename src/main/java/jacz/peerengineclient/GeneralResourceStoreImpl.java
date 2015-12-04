package jacz.peerengineclient;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.datatransfer.GeneralResourceStore;
import jacz.peerengineservice.util.datatransfer.ResourceStoreResponse;
import jacz.peerengineservice.util.datatransfer.resource_accession.BasicFileReader;

import java.io.FileNotFoundException;

/**
 * Class description
 * <p/>
 * User: Alberto<br>
 * Date: 20/07/12<br>
 * Last Modified: 20/07/12
 */
public class GeneralResourceStoreImpl implements GeneralResourceStore {

    private final JacuzziPeerClientAction jacuzziPeerClientAction;

    public GeneralResourceStoreImpl(JacuzziPeerClientAction jacuzziPeerClientAction) {
        this.jacuzziPeerClientAction = jacuzziPeerClientAction;
    }

    @Override
    public ResourceStoreResponse requestResource(String resourceStore, PeerID peerID, String resourceID) {
        ResourceRequestResult resourceRequestResult;
        if (resourceStore.equals(PeerEngineClient.DEFAULT_STORE)) {
            resourceRequestResult = jacuzziPeerClientAction.requestResourceDefaultStore(peerID, resourceID);
        } else {
            resourceRequestResult = jacuzziPeerClientAction.requestResource(peerID, resourceID, resourceStore);
        }
        switch (resourceRequestResult.getResponse()) {

            case RESOURCE_NOT_FOUND:
                return ResourceStoreResponse.resourceNotFound();
            case REQUEST_DENIED:
                return ResourceStoreResponse.requestDenied();
            case REQUEST_APPROVED:
                try {
                    // todo check if its normal file or temp file!!!!!!
                    return ResourceStoreResponse.resourceApproved(new BasicFileReader(resourceRequestResult.getResourcePath()));
                    /*
                    if (resourceRequestResult.isCompletedFile()) {
                        return new ResourceStoreResponse(new BasicFileReader(resourceRequestResult.getResourcePath()));
                    } else {
                        // todo the file path given by the user is not the ini, but the data file. Change the manager so it expects this
                        return new ResourceStoreResponse(new TempFileReader(resourceRequestResult.getResourcePath()));
                    }*/
                } catch (FileNotFoundException e) {
                    return ResourceStoreResponse.resourceNotFound();
                }
            default:
                return null;
        }
    }
}
