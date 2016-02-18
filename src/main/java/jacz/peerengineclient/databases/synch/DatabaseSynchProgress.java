package jacz.peerengineclient.databases.synch;

import jacz.peerengineclient.util.synch.SynchMode;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.data_synchronization.SynchError;
import jacz.util.notification.ProgressNotificationWithError;

/**
 * Progress notifications for library synch processes
 */
public class DatabaseSynchProgress implements ProgressNotificationWithError<Integer, SynchError> {

    private final DatabaseSynchManager databaseSynchManager;

    private final SynchMode mode;

    private final PeerID otherPeerID;

    public DatabaseSynchProgress(DatabaseSynchManager databaseSynchManager, SynchMode mode, PeerID otherPeerID) {
        this.databaseSynchManager = databaseSynchManager;
        this.mode = mode;
        this.otherPeerID = otherPeerID;
    }

    @Override
    public void beginTask() {
        if (mode.isShared()) {
            databaseSynchManager.sharedDatabaseSynchBegins(otherPeerID);
        } else {
            databaseSynchManager.remoteDatabaseSynchBegins(otherPeerID);
        }
    }

    @Override
    public void addNotification(Integer message) {
        if (mode.isShared()) {
            databaseSynchManager.sharedDatabaseSynchProgress(otherPeerID, message);
        } else {
            databaseSynchManager.remoteDatabaseSynchProgress(otherPeerID, message);
        }
    }

    @Override
    public void completeTask() {
        if (mode.isShared()) {
            databaseSynchManager.sharedDatabaseSynchComplete(otherPeerID);
        } else {
            databaseSynchManager.remoteDatabaseSynchComplete(otherPeerID);
        }
    }

    @Override
    public void error(SynchError error) {
        if (mode.isShared()) {
            databaseSynchManager.sharedDatabaseSynchFailed(otherPeerID, error);
        } else {
            databaseSynchManager.remoteDatabaseSynchFailed(otherPeerID, error);
        }
    }

    @Override
    public void timeout() {
        if (mode.isShared()) {
            databaseSynchManager.sharedDatabaseSynchTimedOut(otherPeerID);
        } else {
            databaseSynchManager.remoteDatabaseSynchTimedOut(otherPeerID);
        }
    }
}
