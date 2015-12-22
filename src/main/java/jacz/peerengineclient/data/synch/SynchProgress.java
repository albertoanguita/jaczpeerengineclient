package jacz.peerengineclient.data.synch;

import jacz.peerengineclient.data.PeerShareManager;
import jacz.peerengineclient.util.synch.SynchMode;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.data_synchronization.SynchError;
import jacz.util.notification.ProgressNotificationWithError;

/**
 * Created by Alberto on 22/12/2015.
 */
public class SynchProgress implements ProgressNotificationWithError<Integer, SynchError> {

    private final PeerShareManager peerShareManager;

    private final SynchMode mode;

    private final PeerID remotePeerID;

    public SynchProgress(PeerShareManager peerShareManager, SynchMode mode, PeerID remotePeerID) {
        this.peerShareManager = peerShareManager;
        this.mode = mode;
        this.remotePeerID = remotePeerID;
    }

    @Override
    public void beginTask() {
        // ignore
    }

    @Override
    public void addNotification(Integer message) {
        // ignore
    }

    @Override
    public void completeTask() {
        notifyFinish();
    }

    @Override
    public void error(SynchError error) {
        notifyFinish();
    }

    @Override
    public void timeout() {
        notifyFinish();
    }

    private void notifyFinish() {
        if (mode.isShared()) {
            peerShareManager.localHashSynchFinished(remotePeerID);
        } else {
            peerShareManager.remoteShareSynchFinished(remotePeerID);
        }
    }
}
