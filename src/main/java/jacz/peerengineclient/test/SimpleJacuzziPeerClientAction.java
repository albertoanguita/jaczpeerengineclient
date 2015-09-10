package jacz.peerengineclient.test;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.client.PeerRelations;
import jacz.peerengineservice.client.PeerServerData;
import jacz.peerengineservice.client.connection.ClientConnectionToServerFSM;
import jacz.peerengineservice.client.connection.State;
import jacz.peerengineservice.util.ConnectionStatus;
import jacz.peerengineservice.util.data_synchronization.old.SynchronizeError;
import jacz.peerengineservice.util.datatransfer.UploadsManager;
import jacz.commengine.communication.CommError;
import jacz.peerengineclient.DownloadsManager;
import jacz.peerengineclient.JacuzziPeerClientAction;
import jacz.peerengineclient.ResourceRequestResult;
import jacz.util.identifier.UniqueIdentifier;
import jacz.util.network.IP4Port;

import java.util.List;

/**
 * Test
 *
 * todo!!! did not compile 9/9/2015
 */
public class SimpleJacuzziPeerClientAction implements JacuzziPeerClientAction {

    private String initMessage;

    public SimpleJacuzziPeerClientAction(String initMessage) {
        this.initMessage = initMessage;
    }

    @Override
    public void newPeerNick(PeerID peerID, String nick) {
        System.out.println("Peer " + peerID + " changed his nick to " + nick);
    }

    @Override
    public void peerAddedAsFriend(PeerID peerID, PeerRelations peerRelations) {
        System.out.println(initMessage + "New friend peer: " + peerID);
    }

    @Override
    public void peerRemovedAsFriend(PeerID peerID, PeerRelations peerRelations) {
        System.out.println(initMessage + "Peer no longer friend: " + peerID);
    }

    @Override
    public void peerAddedAsBlocked(PeerID peerID, PeerRelations peerRelations) {
        System.out.println(initMessage + "New blocked peer: " + peerID);
    }

    @Override
    public void peerRemovedAsBlocked(PeerID peerID, PeerRelations peerRelations) {
        System.out.println(initMessage + "Peer no longer blocked: " + peerID);
    }

    @Override
    public void newPeerConnected(PeerID peerID, ConnectionStatus status) {
        System.out.println(initMessage + "New peer connected: " + peerID + ", " + status);
    }

//    @Override
//    public Map<String, ListAccessor> getTransmittingListsForNewPeer(PeerID peerID) {
//        return new HashMap<>();
//    }
//
//    @Override
//    public Map<String, ListAccessor> getReceivingListsForNewPeer(PeerID peerID) {
//        return new HashMap<>();
//    }

    @Override
    public void newObjectMessage(PeerID peerID, Object message) {
        System.out.println(initMessage + "New object message from " + peerID + ": " + message);
    }

    @Override
    public void newChatMessage(PeerID peerID, String message) {
        System.out.println(initMessage + "New chat message from " + peerID + ": " + message);
    }

    @Override
    public void peerValidatedUs(PeerID peerID) {
        System.out.println("Peer " + peerID + " has validated us");
    }

    @Override
    public void peerDisconnected(PeerID peerID, CommError error) {
        System.out.println(initMessage + "Peer disconnected (" + peerID + "). Error = " + error);
    }

    @Override
    public void listeningPortModified(int port) {
        System.out.println(initMessage + "Listening port modified: " + port);
    }

    @Override
    public void tryingToConnectToServer(PeerServerData peerServerData, State state) {
        System.out.println(initMessage + "Trying to connect to server");
    }

    @Override
    public void connectionToServerEstablished(PeerServerData peerServerData, State state) {
        System.out.println(initMessage + "Connected to server");
    }

    @Override
    public void unableToConnectToServer(PeerServerData peerServerData, State state) {
        System.out.println(initMessage + "Unable to connect to server");
    }

    @Override
    public void serverTookToMuchTimeToAnswerConnectionRequest(PeerServerData peerServerData, State state) {
        System.out.println(initMessage + "Server took too much time to answer");
    }

    @Override
    public void connectionToServerTimedOut(PeerServerData peerServerData, State state) {
        System.out.println(initMessage + "Connection to server timed out");
    }

    @Override
    public void localServerOpen(int port, State state) {
        System.out.println(initMessage + "Local server open");
    }

    @Override
    public void localServerClosed(int port, State state) {
        System.out.println(initMessage + "Local server closed");
    }

    @Override
    public void connectionToServerDenied(PeerServerData peerServerData, ClientConnectionToServerFSM.ConnectionFailureReason reason, State state) {
        System.out.println(initMessage + "Connection to server denied. " + reason);
    }

    @Override
    public void disconnectedFromServer(boolean expected, PeerServerData peerServerData, State state) {
        System.out.println(initMessage + "Disconnected from server. Expected=" + expected);
    }

    @Override
    public void undefinedOwnInetAddress() {
        System.out.println(initMessage + "Inet address not defined");
    }

    @Override
    public void peerCouldNotConnectToUs(Exception e, IP4Port ip4Port) {
        System.out.println(initMessage + "Peer failed to connect to us from " + ip4Port.toString() + ". " + e.getMessage());
    }

    @Override
    public void peerConnectionsListenerError(Exception e) {
        System.out.println(initMessage + "Error in the peer connections listener. All connections closed. Error: " + e.getMessage());
    }

    @Override
    public void periodicDownloadsNotification(DownloadsManager downloadsManager) {
//        if (visibleDownloads.size() == 1) {
//            Double speed = visibleDownloads.get(0).getStatistics().getSpeed();
//            if (speed != null) {
//                speed /= 1024d;
//            }
//            long size = visibleDownloads.get(0).getStatistics().getDownloadedSizeThisResource();
//            Long length = visibleDownloads.get(0).getLength();
//            Double part = null;
//            if (length != null) {
//                part = (double) size / (double) length * 100d;
//            }
//            if (part != null && part > 100d) {
//                System.out.println("AHHH. Length = " + length + " / size = " + size);
//            }
//            System.out.println("Speed: " + speed + "KB, downloaded part: " + part);
//        }
    }

    @Override
    public void periodicUploadsNotification(UploadsManager uploadsManager) {
        // todo
    }

    @Override
    public ResourceRequestResult requestResourceDefaultStore(PeerID peerID, String resourceID) {
        // todo
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResourceRequestResult requestResource(PeerID peerID, String resourceID, String resourceStore) {
        // todo
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void integratedItemModified(String library, String itemId) {
        System.out.println(initMessage + "Integrated item modified: " + library + ", " + itemId);
    }

    @Override
    public void remoteSynchStarted(UniqueIdentifier id, PeerID remotePeerID, String library, List<Integer> levelList) {
        System.out.println(initMessage + "Remote synch started: " + id + ", " + remotePeerID + ", " + library + ", " + levelList);
    }

    @Override
    public void remoteSynchProgress(UniqueIdentifier id, PeerID remotePeerID, String library, List<Integer> levelList, int progress, int peerActiveSynchTasks, int peerAverageProgress) {
//        System.out.println(initMessage + "Remote synch progress: " + remotePeerID + ", " + library + ", " + levelList + ", " + progress + ", " + peerActiveSynchTasks + ", " + peerAverageProgress);
    }

    @Override
    public void remoteSynchError(UniqueIdentifier id, PeerID remotePeerID, String library, List<Integer> levelList, SynchronizeError error, int peerActiveSynchTasks, int peerAverageProgress) {
        System.out.println(initMessage + "Remote synch error: " + id + ", " + remotePeerID + ", " + library + ", " + levelList + ", " + error + ", " + peerActiveSynchTasks + ", " + peerAverageProgress);
    }

    @Override
    public void remoteSynchTimeout(UniqueIdentifier id, PeerID remotePeerID, String library, List<Integer> levelList, int peerActiveSynchTasks, int peerAverageProgress) {
        System.out.println(initMessage + "Remote synch timeout: " + id + ", " + remotePeerID + ", " + library + ", " + levelList + ", " + peerActiveSynchTasks + ", " + peerAverageProgress);
    }

    @Override
    public void remoteSynchCompleted(UniqueIdentifier id, PeerID remotePeerID, String library, List<Integer> levelList, int peerActiveSynchTasks, int peerAverageProgress) {
        System.out.println(initMessage + "Remote synch complete: " + id + ", " + remotePeerID + ", " + library + ", " + levelList + ", " + peerActiveSynchTasks + ", " + peerAverageProgress);
    }

    @Override
    public void sharedSynchStarted(UniqueIdentifier id, PeerID remotePeerID, String library, int level) {
        System.out.println(initMessage + "Shared synch started: " + id + ", " + remotePeerID + ", " + library + ", " + level);
    }

    @Override
    public void sharedSynchProgress(UniqueIdentifier id, PeerID remotePeerID, String library, int level, int progress, int peerActiveSynchTasks, int peerAverageProgress) {
//        System.out.println(initMessage + "Shared synch progress: " + remotePeerID + ", " + library + ", " + level + ", " + progress + ", " + peerActiveSynchTasks + ", " + peerAverageProgress);
    }

    @Override
    public void sharedSynchError(UniqueIdentifier id, PeerID remotePeerID, String library, int level, SynchronizeError error, int peerActiveSynchTasks, int peerAverageProgress) {
        System.out.println(initMessage + "Shared synch error: " + id + ", " + remotePeerID + ", " + library + ", " + level + ", " + error + ", " + peerActiveSynchTasks + ", " + peerAverageProgress);
    }

    @Override
    public void sharedSynchTimeout(UniqueIdentifier id, PeerID remotePeerID, String library, int level, int peerActiveSynchTasks, int peerAverageProgress) {
        System.out.println(initMessage + "Shared synch timeout: " + id + ", " + remotePeerID + ", " + library + ", " + level + ", " + peerActiveSynchTasks + ", " + peerAverageProgress);
    }

    @Override
    public void sharedSynchCompleted(UniqueIdentifier id, PeerID remotePeerID, String library, int level, int peerActiveSynchTasks, int peerAverageProgress) {
        System.out.println(initMessage + "Shared synch complete: " + id + ", " + remotePeerID + ", " + library + ", " + level + ", " + peerActiveSynchTasks + ", " + peerAverageProgress);
    }
}
