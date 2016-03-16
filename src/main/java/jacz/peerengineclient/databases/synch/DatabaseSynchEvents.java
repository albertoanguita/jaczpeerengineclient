package jacz.peerengineclient.databases.synch;

import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.util.data_synchronization.SynchError;

/**
 * Events related to the synchronization of databases are reported here. A separate, unique thread is in charge
 * of invoking these methods
 */
public interface DatabaseSynchEvents {

    /**
     * The synch of a remote library begins
     *
     * @param remotePeerId remote peer
     */
    void remoteSynchStarted(PeerId remotePeerId);

    /**
     * Reports the progress in the synch of a remote library
     *
     * @param remotePeerId remote peer whose library is being synched
     * @param progress     progress of the synch task
     */
    void remoteSynchProgress(PeerId remotePeerId, int progress);

    /**
     * Error in a synch task of a remote library
     *
     * @param remotePeerId remote peer whose library is being synched
     * @param error        error
     */
    void remoteSynchError(PeerId remotePeerId, SynchError error);

    /**
     * Timeout in a synch task of a remote library
     *
     * @param remotePeerId remote peer whose library is being synched
     */
    void remoteSynchTimeout(PeerId remotePeerId);

    /**
     * Completion of a synch task of a remote library
     *
     * @param remotePeerId remote peer whose library is being synched
     */
    void remoteSynchCompleted(PeerId remotePeerId);

    /**
     * The synch of the shared library begins
     *
     * @param remotePeerId remote peer
     */
    void sharedSynchStarted(PeerId remotePeerId);

    /**
     * Reports the progress in the synch of the shared library
     *
     * @param remotePeerId remote peer who synchronizes our shared library
     * @param progress     progress of the synch task (over 100)
     */
    void sharedSynchProgress(PeerId remotePeerId, int progress);

    /**
     * Error in a synch task of the shared library
     *
     * @param remotePeerId remote peer who synchronizes our shared library
     * @param error        error
     */
    void sharedSynchError(PeerId remotePeerId, SynchError error);

    /**
     * Timeout in a synch task of the shared library
     *
     * @param remotePeerId remote peer who synchronizes our shared library
     */
    void sharedSynchTimeout(PeerId remotePeerId);

    /**
     * Completion of a synch task of the shared library
     *
     * @param remotePeerId remote peer who synchronizes our shared library
     */
    void sharedSynchCompleted(PeerId remotePeerId);
}
