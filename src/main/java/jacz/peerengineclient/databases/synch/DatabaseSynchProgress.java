package jacz.peerengineclient.databases.synch;

import jacz.peerengineclient.util.synch.SynchMode;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.util.data_synchronization.SynchError;
import org.aanguita.jacuzzi.notification.ProgressNotificationWithError;

/**
 * Progress notifications for library synch processes
 */
public class DatabaseSynchProgress implements ProgressNotificationWithError<Integer, SynchError> {

    private final DatabaseSynchManager databaseSynchManager;

    private final SynchMode mode;

    private final PeerId otherPeerId;

    public DatabaseSynchProgress(DatabaseSynchManager databaseSynchManager, SynchMode mode, PeerId otherPeerId) {
        this.databaseSynchManager = databaseSynchManager;
        this.mode = mode;
        this.otherPeerId = otherPeerId;
    }

    @Override
    public void beginTask() {
        if (mode.isShared()) {
            databaseSynchManager.sharedDatabaseSynchBegins(otherPeerId);
        } else {
            databaseSynchManager.remoteDatabaseSynchBegins(otherPeerId);
        }
    }

    @Override
    public void addNotification(Integer message) {
        if (mode.isShared()) {
            databaseSynchManager.sharedDatabaseSynchProgress(otherPeerId, message);
        } else {
            databaseSynchManager.remoteDatabaseSynchProgress(otherPeerId, message);
        }
    }

    @Override
    public void completeTask() {
        if (mode.isShared()) {
            databaseSynchManager.sharedDatabaseSynchComplete(otherPeerId);
        } else {
            databaseSynchManager.remoteDatabaseSynchComplete(otherPeerId);
        }
    }

    @Override
    public void error(SynchError error) {
        if (mode.isShared()) {
            databaseSynchManager.sharedDatabaseSynchFailed(otherPeerId, error);
        } else {
            databaseSynchManager.remoteDatabaseSynchFailed(otherPeerId, error);
        }
    }

    @Override
    public void timeout() {
        if (mode.isShared()) {
            databaseSynchManager.sharedDatabaseSynchTimedOut(otherPeerId);
        } else {
            databaseSynchManager.remoteDatabaseSynchTimedOut(otherPeerId);
        }
    }
}
