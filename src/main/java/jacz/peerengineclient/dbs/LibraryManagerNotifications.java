package jacz.peerengineclient.dbs;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.data_synchronization.old.SynchronizeError;
import jacz.util.identifier.UniqueIdentifier;
import jacz.util.notification.ProgressNotificationWithError;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Alberto
 * Date: 5/06/14
 * Time: 23:48
 * To change this template use File | Settings | File Templates.
 */
public interface LibraryManagerNotifications {

    /**
     * An item in the integrated database has been modified. User should see an updated visualization
     *
     * @param library
     * @param id
     */
    public void integratedItemModified(String library, String id);

    /**
     * Request to initiate the synchronization of a list of a remote peer
     *
     * @param peerID
     */
    public void requestSynchList(PeerID peerID, String library, List<Integer> levelList, ProgressNotificationWithError<Integer, SynchronizeError> progress);

    /**
     * The shared library (integrated items with a local item) has been modified. Other peers should be notified as soon as possible so they
     * request it to us
     *
     * @param modifiedSharedLibraries libraries from the shared database that have been modified
     */
    public void reportSharedLibraryModified(Map<String, List<Integer>> modifiedSharedLibraries);

    public void reportErrorAccessingDatabases();

    /**
     * The synch of a remote library begins
     *
     * @param id           id of this task
     * @param remotePeerID remote peer
     * @param library      library being synched
     * @param levelList    list of levels being lynched
     */
    public void remoteSynchStarted(UniqueIdentifier id, PeerID remotePeerID, String library, List<Integer> levelList);

    /**
     * Reports the progress in the synch of a remote library
     *
     * @param id                   id of this task
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
     * @param id                   id of this task
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
     * @param id                   id of this task
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
     * @param id                   id of this task
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
     * @param id           id of this task
     * @param remotePeerID remote peer
     * @param library      library being synched
     * @param level        level being synched on the library
     */
    public void sharedSynchStarted(UniqueIdentifier id, PeerID remotePeerID, String library, int level);

    /**
     * Reports the progress in the synch of a shared library
     *
     * @param id                   id of this task
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
     * @param id                   id of this task
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
     * @param id                   id of this task
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
     * @param id                   id of this task
     * @param remotePeerID         remote peer who synchronizes our shared library
     * @param library              reported library
     * @param level                level being synched on the library
     * @param peerActiveSynchTasks total number of active synch tasks for this peer
     * @param peerAverageProgress  average progress for the synch tasks of this peer
     */
    public void sharedSynchCompleted(UniqueIdentifier id, PeerID remotePeerID, String library, int level, int peerActiveSynchTasks, int peerAverageProgress);

    public void fatalErrorInSynch(SynchronizeError error);
}
