package jacz.peerengineclient;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.client.PeerRelations;
import jacz.peerengineservice.client.PeerServerData;
import jacz.peerengineservice.client.connection.ClientConnectionToServerFSM;
import jacz.peerengineservice.client.connection.State;
import jacz.peerengineservice.util.ConnectionStatus;
import jacz.peerengineservice.util.data_synchronization.old.SynchronizeError;
import jacz.peerengineservice.util.data_synchronization.premade_lists.old.ForeignPeerDataAction;
import jacz.peerengineservice.util.data_synchronization.premade_lists.old.ForeignPeerDataAction;
import jacz.peerengineservice.util.datatransfer.UploadsManager;
import jacz.commengine.communication.CommError;
import jacz.util.identifier.UniqueIdentifier;
import jacz.util.network.IP4Port;

import java.util.List;

/**
 *
 */
public interface JacuzziPeerClientAction extends ForeignPeerDataAction {

    public void peerAddedAsFriend(PeerID peerID, PeerRelations peerRelations);

    public void peerRemovedAsFriend(PeerID peerID, PeerRelations peerRelations);

    public void peerAddedAsBlocked(PeerID peerID, PeerRelations peerRelations);

    public void peerRemovedAsBlocked(PeerID peerID, PeerRelations peerRelations);

    public void newPeerConnected(PeerID peerID, ConnectionStatus status);

//    public Map<String, ListAccessor> getTransmittingListsForNewPeer(PeerID peerID);
//
//    public Map<String, ListAccessor> getReceivingListsForNewPeer(PeerID peerID);

    public void newObjectMessage(PeerID peerID, Object message);

    public void newChatMessage(PeerID peerID, String message);

    public void peerValidatedUs(PeerID peerID);

    public void peerDisconnected(PeerID peerID, CommError error);

    public void listeningPortModified(int port);

    void tryingToConnectToServer(jacz.peerengineservice.client.PeerServerData peerServerData, State state);

    public void connectionToServerEstablished(PeerServerData peerServerData, State state);

    public void unableToConnectToServer(PeerServerData peerServerData, State state);

    public void serverTookToMuchTimeToAnswerConnectionRequest(PeerServerData peerServerData, State state);

    public void connectionToServerDenied(PeerServerData peerServerData, ClientConnectionToServerFSM.ConnectionFailureReason reason, State state);

    public void connectionToServerTimedOut(PeerServerData peerServerData, State state);

    public void localServerOpen(int port, State state);

    public void localServerClosed(int port, State state);

    public void disconnectedFromServer(boolean expected, PeerServerData peerServerData, State state);

    public void undefinedOwnInetAddress();

    public void peerCouldNotConnectToUs(Exception e, IP4Port ip4Port);

    public void peerConnectionsListenerError(Exception e);

    public void periodicDownloadsNotification(DownloadsManager downloadsManager);

    public void periodicUploadsNotification(UploadsManager uploadsManager);

    public ResourceRequestResult requestResourceDefaultStore(PeerID peerID, String resourceID);

    public ResourceRequestResult requestResource(PeerID peerID, String resourceID, String resourceStore);

    public void integratedItemModified(String library, String itemId);

    /**
     * The synch of a remote library begins
     *
     * @param remotePeerID remote peer
     * @param library      library being synched
     * @param levelList    list of levels being lynched
     */
    public void remoteSynchStarted(UniqueIdentifier id, PeerID remotePeerID, String library, List<Integer> levelList);

    /**
     * Reports the progress in the synch of a remote library
     *
     * @param remotePeerID         remote peer whose library is being synched
     * @param library              reported library
     * @param levelList            list of levels being synched on the library
     * @param progress             progress of the synch task
     * @param peerActiveSynchTasks total number of active synch tasks for this peer
     * @param peerAverageProgress  average progress for the synch tasks of this peer
     */
    public void remoteSynchProgress(UniqueIdentifier id, PeerID remotePeerID, String library, List<Integer> levelList, int progress, int peerActiveSynchTasks, int peerAverageProgress);

    /**
     * Error in a synch task of a remote library
     *
     * @param remotePeerID         remote peer whose library is being synched
     * @param library              reported library
     * @param levelList            list of levels being synched on the library
     * @param error                error
     * @param peerActiveSynchTasks total number of active synch tasks for this peer
     * @param peerAverageProgress  average progress for the synch tasks of this peer
     */
    public void remoteSynchError(UniqueIdentifier id, PeerID remotePeerID, String library, List<Integer> levelList, SynchronizeError error, int peerActiveSynchTasks, int peerAverageProgress);

    /**
     * Timeout in a synch task of a remote library
     *
     * @param remotePeerID         remote peer whose library is being synched
     * @param library              reported library
     * @param levelList            list of levels being synched on the library
     * @param peerActiveSynchTasks total number of active synch tasks for this peer
     * @param peerAverageProgress  average progress for the synch tasks of this peer
     */
    public void remoteSynchTimeout(UniqueIdentifier id, PeerID remotePeerID, String library, List<Integer> levelList, int peerActiveSynchTasks, int peerAverageProgress);

    /**
     * Completion of a synch task of a remote library
     *
     * @param remotePeerID         remote peer whose library is being synched
     * @param library              reported library
     * @param levelList            list of levels being synched on the library
     * @param peerActiveSynchTasks total number of active synch tasks for this peer
     * @param peerAverageProgress  average progress for the synch tasks of this peer
     */
    public void remoteSynchCompleted(UniqueIdentifier id, PeerID remotePeerID, String library, List<Integer> levelList, int peerActiveSynchTasks, int peerAverageProgress);

    /**
     * The synch of a shared library begins
     *
     * @param remotePeerID remote peer
     * @param library      library being synched
     * @param level        level being synched on the library
     */
    public void sharedSynchStarted(UniqueIdentifier id, PeerID remotePeerID, String library, int level);

    /**
     * Reports the progress in the synch of a shared library
     *
     * @param remotePeerID         remote peer who synchronizes our shared library
     * @param library              reported library
     * @param level                level being synched on the library
     * @param progress             progress of the synch task
     * @param peerActiveSynchTasks total number of active synch tasks for this peer
     * @param peerAverageProgress  average progress for the synch tasks of this peer
     */
    public void sharedSynchProgress(UniqueIdentifier id, PeerID remotePeerID, String library, int level, int progress, int peerActiveSynchTasks, int peerAverageProgress);

    /**
     * Error in a synch task of a shared library
     *
     * @param remotePeerID         remote peer who synchronizes our shared library
     * @param library              reported library
     * @param level                level being synched on the library
     * @param error                error
     * @param peerActiveSynchTasks total number of active synch tasks for this peer
     * @param peerAverageProgress  average progress for the synch tasks of this peer
     */
    public void sharedSynchError(UniqueIdentifier id, PeerID remotePeerID, String library, int level, SynchronizeError error, int peerActiveSynchTasks, int peerAverageProgress);

    /**
     * Timeout in a synch task of a shared library
     *
     * @param remotePeerID         remote peer who synchronizes our shared library
     * @param library              reported library
     * @param level                level being synched on the library
     * @param peerActiveSynchTasks total number of active synch tasks for this peer
     * @param peerAverageProgress  average progress for the synch tasks of this peer
     */
    public void sharedSynchTimeout(UniqueIdentifier id, PeerID remotePeerID, String library, int level, int peerActiveSynchTasks, int peerAverageProgress);

    /**
     * Completion of a synch task of a shared library
     *
     * @param remotePeerID         remote peer who synchronizes our shared library
     * @param library              reported library
     * @param level                level being synched on the library
     * @param peerActiveSynchTasks total number of active synch tasks for this peer
     * @param peerAverageProgress  average progress for the synch tasks of this peer
     */
    public void sharedSynchCompleted(UniqueIdentifier id, PeerID remotePeerID, String library, int level, int peerActiveSynchTasks, int peerAverageProgress);
}
