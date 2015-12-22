package jacz.peerengineclient;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.client.PeerClientAction;
import jacz.peerengineservice.client.PeerRelations;
import jacz.peerengineservice.client.PeerServerData;
import jacz.peerengineservice.client.connection.ClientConnectionToServerFSM;
import jacz.peerengineservice.client.connection.State;
import jacz.peerengineservice.util.ConnectionStatus;
import jacz.peerengineservice.util.datatransfer.UploadsManager;
import jacz.commengine.communication.CommError;
import jacz.util.identifier.UniqueIdentifier;
import jacz.util.network.IP4Port;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class BridgePeerClientActionOLD implements PeerClientAction {

    private PeerEngineClient peerEngineClient;

    private final JacuzziPeerClientAction jacuzziPeerClientAction;

    private final Map<UniqueIdentifier, DownloadManagerOLD> visibleDownloads;

    private final DownloadsManager downloadsManager;

    public BridgePeerClientActionOLD(PeerEngineClient peerEngineClient, JacuzziPeerClientAction jacuzziPeerClientAction, DownloadsManager downloadsManager) {
        this.peerEngineClient = peerEngineClient;
        this.jacuzziPeerClientAction = jacuzziPeerClientAction;
        this.downloadsManager = downloadsManager;
        visibleDownloads = new HashMap<>();
    }

    public synchronized void addVisibleDownload(DownloadManagerOLD downloadManager) {
        visibleDownloads.put(downloadManager.getId(), downloadManager);
    }

    @Override
    public void peerAddedAsFriend(PeerID peerID, PeerRelations peerRelations) {
        peerEngineClient.savePeerRelations(peerRelations);
        peerEngineClient.peerIsNowFriend(peerID);
        jacuzziPeerClientAction.peerAddedAsFriend(peerID, peerRelations);
    }

    @Override
    public void peerRemovedAsFriend(PeerID peerID, PeerRelations peerRelations) {
        peerEngineClient.savePeerRelations(peerRelations);
        peerEngineClient.peerIsNoLongerFriend(peerID);
        jacuzziPeerClientAction.peerRemovedAsFriend(peerID, peerRelations);
    }

    @Override
    public void peerAddedAsBlocked(PeerID peerID, PeerRelations peerRelations) {
        peerEngineClient.savePeerRelations(peerRelations);
        jacuzziPeerClientAction.peerAddedAsBlocked(peerID, peerRelations);
    }

    @Override
    public void peerRemovedAsBlocked(PeerID peerID, PeerRelations peerRelations) {
        peerEngineClient.savePeerRelations(peerRelations);
        jacuzziPeerClientAction.peerRemovedAsBlocked(peerID, peerRelations);
    }

    @Override
    public void newPeerConnected(PeerID peerID, ConnectionStatus status) {
        peerEngineClient.newPeerConnected(peerID);
        jacuzziPeerClientAction.newPeerConnected(peerID, status);
    }

    @Override
    public void newObjectMessage(PeerID peerID, Object message) {
        if (message instanceof ModifiedPersonalDataNotification) {
            peerEngineClient.synchPersonalData(peerID);
        } else if (message instanceof ModifiedSharedLibrariesMessage) {
            // remote databases were modified -> report client
            ModifiedSharedLibrariesMessage modifiedSharedLibrariesMessage = (ModifiedSharedLibrariesMessage) message;
            peerEngineClient.remoteLibrariesNeedSynchronizing(peerID, modifiedSharedLibrariesMessage.modifiedLibraries);
        } else {
            jacuzziPeerClientAction.newObjectMessage(peerID, message);
        }
    }

    @Override
    public void peerValidatedUs(PeerID peerID) {
        peerEngineClient.synchPersonalData(peerID);
        jacuzziPeerClientAction.peerValidatedUs(peerID);
    }

    @Override
    public void peerDisconnected(PeerID peerID, CommError error) {
        jacuzziPeerClientAction.peerDisconnected(peerID, error);
    }

    @Override
    public void listeningPortModified(int port) {
        jacuzziPeerClientAction.listeningPortModified(port);
    }

    @Override
    public void tryingToConnectToServer(PeerServerData peerServerData, State state) {
        jacuzziPeerClientAction.tryingToConnectToServer(peerServerData, state);
    }

    @Override
    public void connectionToServerEstablished(PeerServerData peerServerData, State state) {
        jacuzziPeerClientAction.connectionToServerEstablished(peerServerData, state);
    }

    @Override
    public void unableToConnectToServer(PeerServerData peerServerData, State state) {
        jacuzziPeerClientAction.unableToConnectToServer(peerServerData, state);
    }

    @Override
    public void serverTookToMuchTimeToAnswerConnectionRequest(PeerServerData peerServerData, State state) {
        jacuzziPeerClientAction.serverTookToMuchTimeToAnswerConnectionRequest(peerServerData, state);
    }

    @Override
    public void connectionToServerDenied(PeerServerData peerServerData, ClientConnectionToServerFSM.ConnectionFailureReason reason, State state) {
        jacuzziPeerClientAction.connectionToServerDenied(peerServerData, reason, state);
    }

    @Override
    public void connectionToServerTimedOut(PeerServerData peerServerData, State state) {
        jacuzziPeerClientAction.connectionToServerTimedOut(peerServerData, state);
    }

    @Override
    public void localServerOpen(int port, State state) {
        jacuzziPeerClientAction.localServerOpen(port, state);
    }

    @Override
    public void localServerClosed(int port, State state) {
        jacuzziPeerClientAction.localServerClosed(port, state);
    }

    @Override
    public void disconnectedFromServer(boolean expected, PeerServerData peerServerData, State state) {
        jacuzziPeerClientAction.disconnectedFromServer(expected, peerServerData, state);
    }

    @Override
    public void undefinedOwnInetAddress() {
        jacuzziPeerClientAction.undefinedOwnInetAddress();
    }

    @Override
    public void peerCouldNotConnectToUs(Exception e, IP4Port ip4Port) {
        jacuzziPeerClientAction.peerCouldNotConnectToUs(e, ip4Port);
    }

    @Override
    public void localServerError(Exception e) {
        jacuzziPeerClientAction.peerConnectionsListenerError(e);
    }

    @Override
    public void periodicDownloadsNotification(jacz.peerengineservice.util.datatransfer.DownloadsManager downloadsManager) {
        jacuzziPeerClientAction.periodicDownloadsNotification(this.downloadsManager);
    }

    @Override
    public void periodicUploadsNotification(UploadsManager uploadsManager) {
        jacuzziPeerClientAction.periodicUploadsNotification(uploadsManager);
    }
}
