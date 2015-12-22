package jacz.peerengineclient.databases.synch;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.data_synchronization.SynchError;

/**
 * Events related to the synchronization of databases are reported here. A separate, unique thread is in charge
 * of invoking these methods
 */
public interface LibrarySynchEvents {

    /**
     * The synch of a remote library begins
     *
     * @param remotePeerID remote peer
     */
    void remoteSynchStarted(PeerID remotePeerID);

    /**
     * Reports the progress in the synch of a remote library
     *
     * @param remotePeerID remote peer whose library is being synched
     * @param progress     progress of the synch task
     */
    void remoteSynchProgress(PeerID remotePeerID, int progress);

    /**
     * Error in a synch task of a remote library
     *
     * @param remotePeerID remote peer whose library is being synched
     * @param error        error
     */
    void remoteSynchError(PeerID remotePeerID, SynchError error);

    /**
     * Timeout in a synch task of a remote library
     *
     * @param remotePeerID remote peer whose library is being synched
     */
    void remoteSynchTimeout(PeerID remotePeerID);

    /**
     * Completion of a synch task of a remote library
     *
     * @param remotePeerID remote peer whose library is being synched
     */
    void remoteSynchCompleted(PeerID remotePeerID);

    /**
     * The synch of the shared library begins
     *
     * @param remotePeerID remote peer
     */
    void sharedSynchStarted(PeerID remotePeerID);

    /**
     * Reports the progress in the synch of the shared library
     *
     * @param remotePeerID remote peer who synchronizes our shared library
     * @param progress     progress of the synch task (over 100)
     */
    void sharedSynchProgress(PeerID remotePeerID, int progress);

    /**
     * Error in a synch task of the shared library
     *
     * @param remotePeerID remote peer who synchronizes our shared library
     * @param error        error
     */
    void sharedSynchError(PeerID remotePeerID, SynchError error);

    /**
     * Timeout in a synch task of the shared library
     *
     * @param remotePeerID remote peer who synchronizes our shared library
     */
    void sharedSynchTimeout(PeerID remotePeerID);

    /**
     * Completion of a synch task of the shared library
     *
     * @param remotePeerID remote peer who synchronizes our shared library
     */
    void sharedSynchCompleted(PeerID remotePeerID);
}
