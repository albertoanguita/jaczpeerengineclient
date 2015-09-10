package jacz.peerengineclient.dbs;

import jacz.peerengineservice.PeerID;
import jacz.util.concurrency.timer.SimpleTimerAction;
import jacz.util.concurrency.timer.Timer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class handles delayed synch requests. Synch requests that have failed or have been denied, and that must be repeated after a period of time
 */
public class DelayedSynchRequestsManager implements SimpleTimerAction {

    private static class DelayedSynchTask {

        private final PeerID remotePeerID;

        private final String library;

        private final List<Integer> levelList;

        private DelayedSynchTask(PeerID remotePeerID, String library, List<Integer> levelList) {
            this.remotePeerID = remotePeerID;
            this.library = library;
            this.levelList = levelList;
        }
    }

    private static final long DELAY = 30000;


    private final LibraryManager libraryManager;

    /**
     * Maps the ids of each active timer to its corresponding delayed task
     */
    private final Map<Timer, DelayedSynchTask> delayedTasks;

    public DelayedSynchRequestsManager(LibraryManager libraryManager) {
        this.libraryManager = libraryManager;
        delayedTasks = new HashMap<>();
    }

    synchronized void addDelayedTask(PeerID remotePeerID, String library, List<Integer> levelList) {
        delayedTasks.put(new Timer(DELAY, this), new DelayedSynchTask(remotePeerID, library, levelList));
    }

    @Override
    public synchronized Long wakeUp(Timer timer) {
        DelayedSynchTask delayedSynchTask = delayedTasks.remove(timer);
        if (delayedSynchTask != null) {
            try {
                libraryManager.remoteLibrariesMustBeSynched(delayedSynchTask.remotePeerID, delayedSynchTask.library, delayedSynchTask.levelList);
            } catch (IllegalStateException e) {
                // the library manager is no longer alive, do nothing
            }
        }
        // kill this timer
        return 0l;
    }

    synchronized void stop() {
        for (Timer timer : delayedTasks.keySet()) {
            timer.kill();
        }
    }
}
