package jacz.peerengineclient.dbs_old;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.data_synchronization.old.SynchronizeError;
import jacz.util.concurrency.concurrency_controller.ConcurrencyController;
import jacz.util.concurrency.task_executor.ParallelTask;
import jacz.util.concurrency.task_executor.ParallelTaskExecutor;
import jacz.util.identifier.UniqueIdentifier;
import jacz.util.identifier.UniqueIdentifierFactory;
import jacz.util.lists.Duple;
import jacz.util.notification.ProgressNotificationWithError;

import java.util.HashMap;
import java.util.Map;

/**
 * Controls progress of synch tasks, and handles their initialization
 * <p/>
 * It also handles concurrency in remote synch tasks
 */
public class GenericSynchProgressManager<E> {

    public static interface TaskInitializer<E> {

        public void initiateTask(UniqueIdentifier id, PeerID peerID, E synchData, ProgressNotificationWithError<Integer, SynchronizeError> progress);
    }

    public static interface SynchProgressNotifications<E> {

        public void progress(UniqueIdentifier id, PeerID remotePeerID, E synchData, int progress, int peerActiveSynchTasks, int peerAverageProgress);

        public void error(UniqueIdentifier id, PeerID remotePeerID, E synchData, SynchronizeError error, int peerActiveSynchTasks, int peerAverageProgress);

        public void timeout(UniqueIdentifier id, PeerID remotePeerID, E synchData, int peerActiveSynchTasks, int peerAverageProgress);

        public void completed(UniqueIdentifier id, PeerID remotePeerID, E synchData, int peerActiveSynchTasks, int peerAverageProgress);
    }


    private static class SynchTask<E> implements ProgressNotificationWithError<Integer, SynchronizeError> {

        private final GenericSynchProgressManager<E> synchProgressManager;

        private final PeerID remotePeerID;

        private final UniqueIdentifier id;

        private final E synchData;

        private final String concurrentTask;

        private SynchTask(
                GenericSynchProgressManager<E> synchProgressManager,
                PeerID peerID,
                E synchData,
                String concurrentTask) {
            this.synchProgressManager = synchProgressManager;
            this.remotePeerID = peerID;
            this.id = UniqueIdentifierFactory.getOneStaticIdentifier();
            this.synchData = synchData;
            this.concurrentTask = concurrentTask;
        }

        public void initiateTask(TaskInitializer<E> taskInitializer) {
            taskInitializer.initiateTask(id, remotePeerID, synchData, this);
        }

        @Override
        public void error(SynchronizeError error) {
            synchProgressManager.synchTaskFailed(remotePeerID, id, synchData, error, concurrentTask);
        }

        @Override
        public void timeout() {
            synchProgressManager.synchTaskTimedOut(remotePeerID, id, synchData, concurrentTask);
        }

        @Override
        public void addNotification(Integer message) {
            synchProgressManager.addProgress(remotePeerID, id, synchData, message);
        }

        @Override
        public void completeTask() {
            synchProgressManager.synchTaskCompleted(remotePeerID, id, synchData, concurrentTask);
        }
    }

    /**
     * Stores the progress of all synch tasks of a specific peer
     */
    private static class PeerSynchTaskProgress {

        private final Map<UniqueIdentifier, Integer> tasksProgress;

        private PeerSynchTaskProgress() {
            tasksProgress = new HashMap<>();
        }

        Duple<Integer, Integer> addProgress(UniqueIdentifier id, int progress) {
            tasksProgress.put(id, progress);
            return getSizeAndAverage();
        }

        Duple<Integer, Integer> removeTask(UniqueIdentifier id) {
            tasksProgress.remove(id);
            return getSizeAndAverage();
        }

        boolean isEmpty() {
            return tasksProgress.isEmpty();
        }

        public Duple<Integer, Integer> getSizeAndAverage() {
            if (tasksProgress.size() > 0) {
                int average = 0;
                for (Integer progress : tasksProgress.values()) {
                    average += progress;
                }
                average /= tasksProgress.size();
                return new Duple<>(tasksProgress.size(), average);
            } else {
                return new Duple<>(0, 0);
            }
        }
    }

    private final TaskInitializer<E> taskInitializer;

    private final ConcurrencyController concurrencyController;

    private final SynchProgressNotifications<E> synchProgressNotifications;

    private final Map<PeerID, PeerSynchTaskProgress> peersProgress;

    GenericSynchProgressManager(TaskInitializer<E> taskInitializer, SynchProgressNotifications<E> synchProgressNotifications) {
        this.taskInitializer = taskInitializer;
        this.concurrencyController = null;
        this.synchProgressNotifications = synchProgressNotifications;
        peersProgress = new HashMap<>();
    }

    GenericSynchProgressManager(TaskInitializer<E> taskInitializer, ConcurrencyController concurrencyController, SynchProgressNotifications<E> synchProgressNotifications) {
        this.taskInitializer = taskInitializer;
        this.concurrencyController = concurrencyController;
        this.synchProgressNotifications = synchProgressNotifications;
        peersProgress = new HashMap<>();
    }


    synchronized ProgressNotificationWithError<Integer, SynchronizeError> initiateSynchTask(final PeerID remotePeerID, final E synchData) {
        final SynchTask<E> synchTask = new SynchTask<>(GenericSynchProgressManager.this, remotePeerID, synchData, null);
        ParallelTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                synchTask.initiateTask(taskInitializer);
            }
        });
        return synchTask;
    }

    synchronized void initiateSynchTask(final PeerID remotePeerID, final E synchData, final String concurrentTask) {
        final SynchTask<E> synchTask = new SynchTask<>(GenericSynchProgressManager.this, remotePeerID, synchData, concurrentTask);
        ParallelTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                concurrencyController.beginActivity(concurrentTask);
                synchTask.initiateTask(taskInitializer);
            }
        });
    }

    private synchronized void addProgress(PeerID peerID, UniqueIdentifier id, E synchData, int progress) {
        if (!peersProgress.containsKey(peerID)) {
            peersProgress.put(peerID, new PeerSynchTaskProgress());
        }
        Duple<Integer, Integer> sizeAndAverage = peersProgress.get(peerID).addProgress(id, progress);
        if (synchProgressNotifications != null) {
            synchProgressNotifications.progress(id, peerID, synchData, progress, sizeAndAverage.element1, sizeAndAverage.element2);
        }
    }

    private synchronized void synchTaskCompleted(PeerID peerID, UniqueIdentifier id, E synchData, String concurrentTask) {
        if (concurrentTask != null) {
            concurrencyController.endActivity(concurrentTask);
        }
        Duple<Integer, Integer> sizeAndAverage = removeTask(peerID, id);
        if (synchProgressNotifications != null) {
            synchProgressNotifications.completed(id, peerID, synchData, sizeAndAverage.element1, sizeAndAverage.element2);
        }
    }

    private synchronized void synchTaskFailed(PeerID peerID, UniqueIdentifier id, E synchData, SynchronizeError error, String concurrentTask) {
        if (concurrentTask != null) {
            concurrencyController.endActivity(concurrentTask);
        }
        Duple<Integer, Integer> sizeAndAverage = removeTask(peerID, id);
        if (synchProgressNotifications != null) {
            synchProgressNotifications.error(id, peerID, synchData, error, sizeAndAverage.element1, sizeAndAverage.element2);
        }
    }

    private synchronized void synchTaskTimedOut(PeerID peerID, UniqueIdentifier id, E synchData, String concurrentTask) {
        if (concurrentTask != null) {
            concurrencyController.endActivity(concurrentTask);
        }
        Duple<Integer, Integer> sizeAndAverage = removeTask(peerID, id);
        if (synchProgressNotifications != null) {
            synchProgressNotifications.timeout(id, peerID, synchData, sizeAndAverage.element1, sizeAndAverage.element2);
        }
    }

    private Duple<Integer, Integer> removeTask(PeerID peerID, UniqueIdentifier id) {
        if (peersProgress.containsKey(peerID)) {
            Duple<Integer, Integer> sizeAndAverage = peersProgress.get(peerID).removeTask(id);
            if (peersProgress.get(peerID).isEmpty()) {
                peersProgress.remove(peerID);
            }
            return sizeAndAverage;
        } else {
            // this peer was not even in the table of peers progress, return 0
            return new Duple<>(0, 0);
        }
    }
}
