package jacz.peerengineclient.libraries.synch;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.data_synchronization.SynchError;
import jacz.util.notification.ProgressNotificationWithError;

/**
 * Progress notifications for library synch processes
 */
public class LibrarySynchProgress implements ProgressNotificationWithError<Integer, SynchError> {

    enum Mode {
        SHARED,
        REMOTE;

        boolean isShared() {
            return this == SHARED;
        }

        boolean isRemote() {
            return this == REMOTE;
        }
    }

    private final LibrarySynchManager librarySynchManager;

    private final Mode mode;

    private final PeerID otherPeerID;

    public LibrarySynchProgress(LibrarySynchManager librarySynchManager, Mode mode, PeerID otherPeerID) {
        this.librarySynchManager = librarySynchManager;
        this.mode = mode;
        this.otherPeerID = otherPeerID;
    }

    @Override
    public void beginTask() {
        if (mode.isShared()) {
            librarySynchManager.sharedLibrarySynchBegins(otherPeerID);
        } else {
            librarySynchManager.remoteLibrarySynchBegins(otherPeerID);
        }
    }

    @Override
    public void addNotification(Integer message) {
        if (mode.isShared()) {
            librarySynchManager.sharedLibrarySynchProgress(otherPeerID, message);
        } else {
            librarySynchManager.remoteLibrarySynchProgress(otherPeerID, message);
        }
    }

    @Override
    public void completeTask() {
        if (mode.isShared()) {
            librarySynchManager.sharedLibrarySynchComplete(otherPeerID);
        } else {
            librarySynchManager.remoteLibrarySynchComplete(otherPeerID);
        }
    }

    @Override
    public void error(SynchError error) {
        if (mode.isShared()) {
            librarySynchManager.sharedLibrarySynchFailed(otherPeerID, error);
        } else {
            librarySynchManager.remoteLibrarySynchFailed(otherPeerID, error);
        }
    }

    @Override
    public void timeout() {
        if (mode.isShared()) {
            librarySynchManager.sharedLibrarySynchTimedOut(otherPeerID);
        } else {
            librarySynchManager.remoteLibrarySynchTimedOut(otherPeerID);
        }
    }
}
