package jacz.peerengineclient.test;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.data_synchronization.SynchronizeError;
import jacz.peerengineclient.dbs_old.LibraryManagerNotifications;
import jacz.util.identifier.UniqueIdentifier;
import jacz.util.notification.ProgressNotificationWithError;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Alberto
 * Date: 24/07/14
 * Time: 13:02
 * To change this template use File | Settings | File Templates.
 */
public class LibraryManagerNotificationsImpl implements LibraryManagerNotifications {

    @Override
    public void integratedItemModified(String library, String id) {
        System.out.println("Integrated item modified: " + library + "/" + id);
    }

    @Override
    public void requestSynchList(PeerID peerID, String library, List<Integer> levelList, ProgressNotificationWithError<Integer, SynchronizeError> progress) {
        System.out.println("Request to synch remote library: " + peerID + ", " + library + ", " + levelList);
    }

    @Override
    public void reportSharedLibraryModified(Map<String, List<Integer>> modifiedSharedLibraries) {
        System.out.println("The shared database was modified: " + modifiedSharedLibraries);
    }

    @Override
    public void reportErrorAccessingDatabases() {
        System.out.println("Error accessing the databases");
    }

    @Override
    public void remoteSynchStarted(UniqueIdentifier id, PeerID remotePeerID, String library, List<Integer> levelList) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void remoteSynchProgress(UniqueIdentifier id, PeerID remotePeerID, String library, List<Integer> levelList, int progress, int peerActiveSynchTasks, int peerAverageProgress) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void remoteSynchError(UniqueIdentifier id, PeerID remotePeerID, String library, List<Integer> levelList, SynchronizeError error, int peerActiveSynchTasks, int peerAverageProgress) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void remoteSynchTimeout(UniqueIdentifier id, PeerID remotePeerID, String library, List<Integer> levelList, int peerActiveSynchTasks, int peerAverageProgress) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void remoteSynchCompleted(UniqueIdentifier id, PeerID remotePeerID, String library, List<Integer> levelList, int peerActiveSynchTasks, int peerAverageProgress) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void sharedSynchStarted(UniqueIdentifier id, PeerID remotePeerID, String library, int level) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void sharedSynchProgress(UniqueIdentifier id, PeerID remotePeerID, String library, int level, int progress, int peerActiveSynchTasks, int peerAverageProgress) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void sharedSynchError(UniqueIdentifier id, PeerID remotePeerID, String library, int level, SynchronizeError error, int peerActiveSynchTasks, int peerAverageProgress) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void sharedSynchTimeout(UniqueIdentifier id, PeerID remotePeerID, String library, int level, int peerActiveSynchTasks, int peerAverageProgress) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void sharedSynchCompleted(UniqueIdentifier id, PeerID remotePeerID, String library, int level, int peerActiveSynchTasks, int peerAverageProgress) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void fatalErrorInSynch(SynchronizeError error) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
