package jacz.peerengineclient.test;

import jacz.peerengineclient.databases.synch.DatabaseSynchEvents;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.data_synchronization.SynchError;

/**
 * Created by Alberto on 24/12/2015.
 */
public class DatabaseSynchEventsImpl implements DatabaseSynchEvents {

    @Override
    public void remoteSynchStarted(PeerID remotePeerID) {
        System.out.println("Remote synch started with " + TestUtil.formatPeer(remotePeerID));
    }

    @Override
    public void remoteSynchProgress(PeerID remotePeerID, int progress) {
        System.out.println("Remote synch progress with " + TestUtil.formatPeer(remotePeerID) + ", progress: " + progress);
    }

    @Override
    public void remoteSynchError(PeerID remotePeerID, SynchError error) {
        System.out.println("Remote synch error with " + TestUtil.formatPeer(remotePeerID) + ", error: " + error);
    }

    @Override
    public void remoteSynchTimeout(PeerID remotePeerID) {
        System.out.println("Remote synch timeout with " + TestUtil.formatPeer(remotePeerID));
    }

    @Override
    public void remoteSynchCompleted(PeerID remotePeerID) {
        System.out.println("Remote synch complete with " + TestUtil.formatPeer(remotePeerID));
    }

    @Override
    public void sharedSynchStarted(PeerID remotePeerID) {
        System.out.println("Shared synch started with " + TestUtil.formatPeer(remotePeerID));
    }

    @Override
    public void sharedSynchProgress(PeerID remotePeerID, int progress) {
        System.out.println("Shared synch progress with " + TestUtil.formatPeer(remotePeerID) + ", progress: " + progress);
    }

    @Override
    public void sharedSynchError(PeerID remotePeerID, SynchError error) {
        System.out.println("Shared synch error with " + TestUtil.formatPeer(remotePeerID) + ", progress: " + error);
    }

    @Override
    public void sharedSynchTimeout(PeerID remotePeerID) {
        System.out.println("Shared synch timeout with " + TestUtil.formatPeer(remotePeerID));
    }

    @Override
    public void sharedSynchCompleted(PeerID remotePeerID) {
        System.out.println("Shared synch complete with " + TestUtil.formatPeer(remotePeerID));
    }
}
