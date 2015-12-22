package jacz.peerengineclient.test_old.transfer;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.ConnectionStatus;
import jacz.peerengineclient.ResourceRequestResult;
import jacz.peerengineclient.test_old.SimpleJacuzziPeerClientAction;

/**
 * Class description
 * <p/>
 * User: Alberto<br>
 * Date: 19/07/12<br>
 * Last Modified: 19/07/12
 */
public class SimpleJacuzziPeerClientActionTransfer extends SimpleJacuzziPeerClientAction {

    public SimpleJacuzziPeerClientActionTransfer(String initMessage) {
        super(initMessage);
    }

    @Override
    public void newPeerConnected(PeerID peerID, ConnectionStatus status) {
        super.newPeerConnected(peerID, status);

        //client.getPeerClient().getResourceStreamingManager().getVisibleDownloadsManager().setTimer(5000);
        //client.getPeerClient().getResourceStreamingManager().downloadResource("files", "aaa", new BasicFileWriter("aaa_transfer.txt"), true, new DownloadProgressNotificationHandlerBridge(peerID), 0.1f);

    }

    @Override
    public ResourceRequestResult requestResourceDefaultStore(PeerID peerID, String resourceID) {
        if (resourceID.equals("aaa")) {
            return ResourceRequestResult.requestApproved("Breaking Bad 5x01 Live Free or Die.avi");
            //return ResourceRequestResult.requestApproved(true, "file.txt");
        } else {
            return ResourceRequestResult.resourceNotFound();
        }
    }
}
