package jacz.peerengineclient.stores;

import jacz.peerengineclient.dbs_old.LibraryManagerNotifications;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.data_synchronization.ServerSynchRequestAnswer;
import jacz.peerengineservice.util.data_synchronization.SynchError;
import jacz.util.identifier.UniqueIdentifier;
import jacz.util.identifier.UniqueIdentifierFactory;
import jacz.util.notification.ProgressNotificationWithError;

import java.util.HashMap;
import java.util.Map;

/**
 * This class manages all the ongoing synch processes, maintaining a table with the active synch processes
 * <p>
 * It also handles the synch requests, being able to deny them if they conflict with the ongoing synch processes
 * <p>
 * Finally, this class is in charge of reporting the progress of the active synch processes
 */
public class SynchManager {

    private enum Mode {
        SHARED,
        REMOTE;

        boolean isShared() {
            return this == SHARED;
        }

        boolean isRemote() {
            return this == REMOTE;
        }
    }

    private class SynchProcess implements ProgressNotificationWithError<Integer, SynchError> {

        private final SynchManager synchManager;

        private final UniqueIdentifier id;

        private final Mode mode;

        private final PeerID otherPeerID;

        public SynchProcess(SynchManager synchManager, Mode mode, PeerID otherPeerID) {
            this.synchManager = synchManager;
            id = UniqueIdentifierFactory.getOneStaticIdentifier();
            this.mode = mode;
            this.otherPeerID = otherPeerID;
        }

        public UniqueIdentifier getId() {
            return id;
        }

        @Override
        public void addNotification(Integer message) {
            if (mode.isShared()) {
                synchManager.sharedStoreSynchProgress(otherPeerID, message);
            } else {
                synchManager.remoteStoreSynchProgress(otherPeerID, message);
            }
        }

        @Override
        public void completeTask() {
            if (mode.isShared()) {
                synchManager.sharedStoreSynchComplete(getId(), otherPeerID);
            } else {
                synchManager.remoteStoreSynchComplete(getId(), otherPeerID);
            }
        }

        @Override
        public void error(SynchError error) {
            if (mode.isShared()) {
                synchManager.sharedStoreSynchFailed(getId(), otherPeerID, error);
            } else {
                synchManager.remoteStoreSynchFailed(getId(), otherPeerID, error);
            }
        }

        @Override
        public void timeout() {
            if (mode.isShared()) {
                synchManager.sharedStoreSynchTimedOut(getId(), otherPeerID);
            } else {
                synchManager.remoteStoreSynchTimedOut(getId(), otherPeerID);
            }
        }
    }

    private final LibraryManagerNotifications libraryManagerNotifications;

    private final Map<UniqueIdentifier, SynchProcess> activeSharedSynchs;

    private final Map<UniqueIdentifier, SynchProcess> activeRemoteSynchs;

    public SynchManager(LibraryManagerNotifications libraryManagerNotifications) {
        this.libraryManagerNotifications = libraryManagerNotifications;
        activeSharedSynchs = new HashMap<>();
        activeRemoteSynchs = new HashMap<>();
    }

    /**
     * A remote peer is requesting to get access to the shared library for synchronizing it with us
     * <p>
     * This process can happen along with any other process*. We just must take care that the retrieval of index and hash lists is properly
     * synchronized with other operations. A local or remote item integration might of course break the synchronization, but that is a risk that
     * we must assume, and the other peer will be notified of this.
     * <p>
     * The library manager will reject these requests if a remote integration is taking place, because it would most certainly break the synch
     * and we would be waisting bandwidth
     */
    public synchronized ServerSynchRequestAnswer requestForSharedLibrarySynch(PeerID peerID) {
        // todo is this the appropriate condition? we can share the integrated whenever we want, it is one single operation to perform, and it does not matter if it is being integrated
        // todo check for other reasons for server busy. Maybe if there are many synchs already happening
        if (!isRemoteDatabaseBeingIntegrated() && alive) {
            // synch process can proceed
            SynchProcess synchProcess = new SynchProcess(this, Mode.SHARED, peerID);
            activeSharedSynchs.put(synchProcess.getId(), synchProcess);
            return new ServerSynchRequestAnswer(ServerSynchRequestAnswer.Type.OK, synchProcess);
        } else {
            // deny
            return new ServerSynchRequestAnswer(ServerSynchRequestAnswer.Type.SERVER_BUSY, null);
        }
    }

    public synchronized void synchRemoteLibrary(PeerID peerID) {
        libraryManagerNotifications.requestSynchList(peerID, new SynchProcess(this, Mode.REMOTE, peerID));
    }

    private void sharedStoreSynchProgress(PeerID remotePeerID, Integer progress) {
        // todo remove, pass the notification object to the progress impl
    }

    private void sharedStoreSynchComplete(UniqueIdentifier id, PeerID remotePeerID) {
        activeSharedSynchs.remove(id);
        // todo report client
    }

    private void sharedStoreSynchFailed(UniqueIdentifier id, PeerID remotePeerID, SynchError error) {
        // todo check errors (fatal?)
        activeSharedSynchs.remove(id);
        // todo report client
    }

    private void sharedStoreSynchTimedOut(UniqueIdentifier id, PeerID remotePeerID) {
        activeSharedSynchs.remove(id);
        // todo report client
    }

    private void remoteStoreSynchProgress(PeerID remotePeerID, Integer progress) {
        // todo report client
    }

    private void remoteStoreSynchComplete(UniqueIdentifier id, PeerID remotePeerID) {
        activeRemoteSynchs.remove(id);
        // todo report client
    }

    private void remoteStoreSynchFailed(UniqueIdentifier id, PeerID remotePeerID, SynchError error) {
        // todo check errors (fatal?)
        activeRemoteSynchs.remove(id);
        // todo report client
    }

    private void remoteStoreSynchTimedOut(UniqueIdentifier id, PeerID remotePeerID) {
        activeRemoteSynchs.remove(id);
        // todo report client
    }


}
