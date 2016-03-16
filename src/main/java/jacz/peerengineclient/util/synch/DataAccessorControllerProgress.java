package jacz.peerengineclient.util.synch;

import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.util.data_synchronization.SynchError;
import jacz.util.notification.ProgressNotificationWithError;

/**
 * Created by Alberto on 17/02/2016.
 */
public class DataAccessorControllerProgress implements ProgressNotificationWithError<Integer, SynchError> {

    private final DataAccessorController dataAccessorController;

    private final SynchMode mode;

    private final PeerId remotePeerId;

    private final ProgressNotificationWithError<Integer, SynchError> synchProgress;

    public DataAccessorControllerProgress(
            DataAccessorController dataAccessorController,
            SynchMode mode,
            PeerId remotePeerId,
            ProgressNotificationWithError<Integer, SynchError> synchProgress) {
        this.dataAccessorController = dataAccessorController;
        this.mode = mode;
        this.remotePeerId = remotePeerId;
        this.synchProgress = synchProgress;
    }

    @Override
    public void beginTask() {
        // ignore
        synchProgress.beginTask();
    }

    @Override
    public void addNotification(Integer message) {
        // ignore
        synchProgress.addNotification(message);
    }

    @Override
    public void completeTask() {
        notifyFinish();
        synchProgress.completeTask();
    }

    @Override
    public void error(SynchError error) {
        notifyFinish();
        synchProgress.error(error);
    }

    @Override
    public void timeout() {
        notifyFinish();
        synchProgress.timeout();
    }

    private void notifyFinish() {
        if (mode.isShared()) {
            dataAccessorController.localHashSynchFinished(remotePeerId);
        } else {
            dataAccessorController.remoteShareSynchFinished(remotePeerId);
        }
    }
}
