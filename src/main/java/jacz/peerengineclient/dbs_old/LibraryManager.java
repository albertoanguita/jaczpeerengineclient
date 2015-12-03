package jacz.peerengineclient.dbs_old;

import jacz.peerengineservice.util.data_synchronization.SynchError;
import jacz.store.Database;
import jacz.store.IllegalDataException;
import jacz.store.common.LibraryItem;
import jacz.store.db_mediator.CorruptDataException;
import jacz.store.db_mediator.DBException;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.data_synchronization.ListAccessor;
import jacz.peerengineservice.util.data_synchronization.old.ListNotFoundException;
import jacz.peerengineservice.util.data_synchronization.old.ServerSynchRequestAnswer;
import jacz.peerengineservice.util.data_synchronization.old.SynchronizeError;
import jacz.util.concurrency.concurrency_controller.ConcurrencyController;
import jacz.util.concurrency.task_executor.ParallelTask;
import jacz.util.concurrency.task_executor.ParallelTaskExecutor;
import jacz.util.hash.hashdb.FileHashDatabase;
import jacz.util.identifier.UniqueIdentifier;
import jacz.util.lists.Triple;
import jacz.util.notification.ProgressNotificationWithError;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

/**
 * Manages all libraries (local, remotes and integrated)
 * <p/>
 * todo removed remote items???
 */
public class LibraryManager {

    // todo we synch the full store, remove these two
    private static class LibraryAndLevelList {

        final String library;

        final List<Integer> levelList;

        private LibraryAndLevelList(String library, List<Integer> levelList) {
            this.library = library;
            this.levelList = levelList;
        }
    }

    private static class LibraryAndLevel {

        final String library;

        final int level;

        private LibraryAndLevel(String library, int level) {
            this.library = library;
            this.level = level;
        }
    }

    /**
     * The integrated database, composed by the local database and the remote databases. This is what the user visualizes
     * <p/>
     * The items from the integrated database which have a related local item are the ones shared to other peers
     */
    private IntegratedDatabase integratedDatabase;

    /**
     * The local database, with items created by the user
     */
    private LocalDatabase localDatabase;

    /**
     * The remote databases, with libraries shared to us by friend peers
     */
    private Map<PeerID, RemoteDatabase> remoteDatabases;

    private ItemLockManager itemLockManager;

    /**
     * Relation of remote items that have been modified since the last library integration, and thus need to be re-integrated. We collect all these
     * items during the remote synching process, and once synching is complete, we use this information to integrate the libraries
     */
    private RemoteDatabasesIntegrator.RemoteModifiedItems remoteModifiedItems;

    /**
     * A concurrency controller for controlling the execution of the different integration processes
     * The controller allows several simultaneous integrations of remote items, but one local item integration which pauses all remote integrations
     * Local items have higher priorities than remote items
     */
    private final ConcurrencyController concurrencyController;

    private final RemoteDatabasesIntegrator remoteDatabasesIntegrator;

    /**
     * Stores the names of the libraries of the shared database that are modified and pending for notification to other peers. This allows merging
     * many notifications in a single one
     * todo remove
     */
    private final Map<String, Set<Integer>> modifiedSharedLibraries;

    private final GenericSynchProgressManager<LibraryAndLevelList> remoteSynchProgressManager;

    private final GenericSynchProgressManager<LibraryAndLevel> sharedSynchProgressManager;

    private final DelayedSynchRequestsManager delayedSynchRequestsManager;

    private final LibraryManagerNotifications libraryManagerNotifications;

    private boolean alive;


    public LibraryManager(
            IntegratedDatabase integratedDatabase,
            LocalDatabase localDatabase,
            Map<PeerID, RemoteDatabase> remoteDatabases,
            LibraryManagerNotifications libraryManagerNotifications) {
        this.integratedDatabase = integratedDatabase;
        this.localDatabase = localDatabase;
        this.remoteDatabases = remoteDatabases;
        this.itemLockManager = new ItemLockManager();
        this.libraryManagerNotifications = libraryManagerNotifications;
        remoteModifiedItems = new RemoteDatabasesIntegrator.RemoteModifiedItems();
        concurrencyController = new LibraryManagerConcurrencyController();
        remoteDatabasesIntegrator = new RemoteDatabasesIntegrator(concurrencyController, this, remoteModifiedItems, integratedDatabase, localDatabase, remoteDatabases, itemLockManager);
        modifiedSharedLibraries = new HashMap<>();
        remoteSynchProgressManager = new GenericSynchProgressManager<>(
                new GenericSynchProgressManager.TaskInitializer<LibraryAndLevelList>() {
                    @Override
                    public void initiateTask(UniqueIdentifier id, PeerID peerID, LibraryAndLevelList synchData, ProgressNotificationWithError<Integer, SynchronizeError> progress) {
                        LibraryManager.this.libraryManagerNotifications.requestSynchList(peerID, synchData.library, synchData.levelList, progress);
                        LibraryManager.this.libraryManagerNotifications.remoteSynchStarted(id, peerID, synchData.library, synchData.levelList);
                    }
                },
                concurrencyController,
                new GenericSynchProgressManager.SynchProgressNotifications<LibraryAndLevelList>() {
                    @Override
                    public void progress(UniqueIdentifier id, PeerID remotePeerID, LibraryAndLevelList synchData, int progress, int peerActiveSynchTasks, int peerAverageProgress) {
                        LibraryManager.this.libraryManagerNotifications.remoteSynchProgress(id, remotePeerID, synchData.library, synchData.levelList, progress, peerActiveSynchTasks, peerAverageProgress);
                    }

                    @Override
                    public void error(UniqueIdentifier id, PeerID remotePeerID, LibraryAndLevelList synchData, SynchronizeError error, int peerActiveSynchTasks, int peerAverageProgress) {
                        LibraryManager.this.libraryManagerNotifications.remoteSynchError(id, remotePeerID, synchData.library, synchData.levelList, error, peerActiveSynchTasks, peerAverageProgress);
                        handleSynchProcessError(remotePeerID, true, synchData.library, synchData.levelList, error);
                    }

                    @Override
                    public void timeout(UniqueIdentifier id, PeerID remotePeerID, LibraryAndLevelList synchData, int peerActiveSynchTasks, int peerAverageProgress) {
                        LibraryManager.this.libraryManagerNotifications.remoteSynchTimeout(id, remotePeerID, synchData.library, synchData.levelList, peerActiveSynchTasks, peerAverageProgress);
                        handleSynchProcessTimeout(remotePeerID, true, synchData.library, synchData.levelList);
                    }

                    @Override
                    public void completed(UniqueIdentifier id, PeerID remotePeerID, LibraryAndLevelList synchData, int peerActiveSynchTasks, int peerAverageProgress) {
                        LibraryManager.this.libraryManagerNotifications.remoteSynchCompleted(id, remotePeerID, synchData.library, synchData.levelList, peerActiveSynchTasks, peerAverageProgress);
                    }
                }
        );
        sharedSynchProgressManager = new GenericSynchProgressManager<>(
                new GenericSynchProgressManager.TaskInitializer<LibraryAndLevel>() {
                    @Override
                    public void initiateTask(UniqueIdentifier id, PeerID peerID, LibraryAndLevel synchData, ProgressNotificationWithError<Integer, SynchronizeError> progress) {
                        LibraryManager.this.libraryManagerNotifications.sharedSynchStarted(id, peerID, synchData.library, synchData.level);
                    }
                },
                new GenericSynchProgressManager.SynchProgressNotifications<LibraryAndLevel>() {
                    @Override
                    public void progress(UniqueIdentifier id, PeerID remotePeerID, LibraryAndLevel synchData, int progress, int peerActiveSynchTasks, int peerAverageProgress) {
                        LibraryManager.this.libraryManagerNotifications.sharedSynchProgress(id, remotePeerID, synchData.library, synchData.level, progress, peerActiveSynchTasks, peerAverageProgress);
                    }

                    @Override
                    public void error(UniqueIdentifier id, PeerID remotePeerID, LibraryAndLevel synchData, SynchronizeError error, int peerActiveSynchTasks, int peerAverageProgress) {
                        LibraryManager.this.libraryManagerNotifications.sharedSynchError(id, remotePeerID, synchData.library, synchData.level, error, peerActiveSynchTasks, peerAverageProgress);
                        handleSynchProcessError(remotePeerID, false, synchData.library, synchData.level, error);
                    }

                    @Override
                    public void timeout(UniqueIdentifier id, PeerID remotePeerID, LibraryAndLevel synchData, int peerActiveSynchTasks, int peerAverageProgress) {
                        LibraryManager.this.libraryManagerNotifications.sharedSynchTimeout(id, remotePeerID, synchData.library, synchData.level, peerActiveSynchTasks, peerAverageProgress);
                        handleSynchProcessTimeout(remotePeerID, true, synchData.library, synchData.level);
                    }

                    @Override
                    public void completed(UniqueIdentifier id, PeerID remotePeerID, LibraryAndLevel synchData, int peerActiveSynchTasks, int peerAverageProgress) {
                        LibraryManager.this.libraryManagerNotifications.sharedSynchCompleted(id, remotePeerID, synchData.library, synchData.level, peerActiveSynchTasks, peerAverageProgress);
                    }
                }
        );
        delayedSynchRequestsManager = new DelayedSynchRequestsManager(this);
        alive = true;
    }

    IntegratedDatabase getIntegratedDatabase() {
        return integratedDatabase;
    }

    LocalDatabase getLocalDatabase() {
        return localDatabase;
    }

    Map<PeerID, RemoteDatabase> getRemoteDatabases() {
        return remoteDatabases;
    }

    ItemLockManager getItemLockManager() {
        return itemLockManager;
    }

    /**
     * An item in the local database was modified, and it must be reflected in the integrated database immediately
     * <p/>
     * This method is blocking, and execution completes once the integrated database has been updated with the local changes. The request must
     * therefore be immediately attended
     *
     * @param library      local library of the modified item
     * @param elementIndex index of the modified item
     */
    public synchronized void localItemModified(String library, String elementIndex) {
        // local item modifications are served as soon as possible, interrupting if necessary the integration of remote items
        // the method is blocking, waiting until the item has been integrated
        if (alive) {
            concurrencyController.beginActivity(LibraryManagerConcurrencyController.INTEGRATE_LOCAL_ITEM);
            try {
                Triple<Set<String>, String, Boolean> hasChangedItemIdAndHasLocal = integrateLocalItem(library, elementIndex);
                if (hasChangedItemIdAndHasLocal.element1 != null) {
                    reportIntegratedItemModified(library, hasChangedItemIdAndHasLocal.element1, hasChangedItemIdAndHasLocal.element2, hasChangedItemIdAndHasLocal.element3, true);
                }
            } catch (Exception e) {
                databasesCannotBeAccessed();
            }
            concurrencyController.endActivity(LibraryManagerConcurrencyController.INTEGRATE_LOCAL_ITEM);
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * Performs the integration of a local item in the integrated database. Permission must have been previously gained, as this method does not
     * check the conditions for it
     *
     * @param library      local library of the modified item
     * @param elementIndex index of the modified item
     */
    private Triple<Set<String>, String, Boolean> integrateLocalItem(String library, String elementIndex) throws IllegalDataException, DBException, ParseException, IOException, CorruptDataException {
        LibraryItem localItem = localDatabase.getDatabase().getItem(library, elementIndex);
        return ItemIntegrator.integrateExternalItem(integratedDatabase, localDatabase, remoteDatabases, library, itemLockManager, null, elementIndex, localItem, localDatabase.getItemsToIntegratedItems());
    }

    synchronized void reportIntegratedItemModified(final String library, Set<String> fields, final String itemId, boolean isAlsoShared, boolean flushNotifications) {
        if (!fields.isEmpty()) {
            Set<Integer> levels = new HashSet<>();
            for (String field : fields) {
                levels.add(ListAccessorManager.getFieldLevel(library, field));
            }
            ParallelTaskExecutor.executeTask(new ParallelTask() {
                @Override
                public void performTask() {
                    libraryManagerNotifications.integratedItemModified(library, itemId);
                }
            });
            if (isAlsoShared) {
                if (!modifiedSharedLibraries.containsKey(library)) {
                    modifiedSharedLibraries.put(library, new HashSet<Integer>());
                }
                modifiedSharedLibraries.get(library).addAll(levels);
                if (flushNotifications) {
                    flushSharedLibraryModificationsNotification();
                }
            }
        }
    }

    synchronized void flushSharedLibraryModificationsNotification() {
        if (!modifiedSharedLibraries.isEmpty()) {
            final Map<String, List<Integer>> librariesToNotify = new HashMap<>();
            for (Map.Entry<String, Set<Integer>> entry : modifiedSharedLibraries.entrySet()) {
                librariesToNotify.put(entry.getKey(), new ArrayList<>(entry.getValue()));
                Collections.sort(librariesToNotify.get(entry.getKey()));
            }
            modifiedSharedLibraries.clear();
            ParallelTaskExecutor.executeTask(new ParallelTask() {
                @Override
                public void performTask() {
                    libraryManagerNotifications.reportSharedLibraryModified(librariesToNotify);
                }
            });
        }
    }

    /**
     * An item from a remote database has been modified, and therefore integration is required with the integrated database
     * This method is invoked by a synch task being currently run. It is the only way this can happen.
     * <p/>
     * This method is invoked by the list accessors of the remote databases
     *
     * @param peerID       peer whose library must be synched
     * @param library      library to synch
     * @param elementIndex index of the element being synched
     */
    public synchronized void remoteItemModified(PeerID peerID, String library, String elementIndex) {
        remoteModifiedItems.addItem(peerID, library, elementIndex);
        remoteDatabasesIntegrator.remoteDatabaseIntegrationRequested();
    }

    /**
     * Allows other sub-modules notifying that any of the databases could not be accessed
     */
    synchronized void databasesCannotBeAccessed() {
        // todo differentiate errors
        libraryManagerNotifications.reportErrorAccessingDatabases();
    }

    /**
     * A remote peer informed that his shared library was modified, and therefore we must synchronize it
     * todo remove
     *
     * @param peerID    peer whose library must be synched
     * @param library   library to synch
     * @param levelList list of levels to synch
     */
    public synchronized void remoteLibrariesMustBeSynched(PeerID peerID, String library, List<Integer> levelList) {
        if (alive) {
            remoteSynchProgressManager.initiateSynchTask(peerID, new LibraryAndLevelList(library, levelList), LibraryManagerConcurrencyController.SYNCH_REMOTE_LIBRARY);
        } else {
            throw new IllegalStateException();
        }
    }

    public synchronized ListAccessor getSharedListAccessor(String library, FileHashDatabase fileHashDatabase, String baseDir) {
        return getListAccessor(integratedDatabase.getDatabase(), library, fileHashDatabase, null, baseDir);
    }

    public synchronized ListAccessor getRemoteListAccessor(PeerID peerID, String library, FileHashDatabase fileHashDatabase, String baseDir) throws ListNotFoundException {
        if (remoteDatabases.containsKey(peerID)) {
            return getListAccessor(remoteDatabases.get(peerID).getDatabase(), library, fileHashDatabase, peerID, baseDir);
        } else {
            throw new ListNotFoundException();
        }
    }

    private ListAccessor getListAccessor(Database database, String library, FileHashDatabase fileHashDatabase, PeerID remotePeerID, String baseDir) {
        return ListAccessorManager.getListAccessor(database, library, fileHashDatabase, this, remotePeerID, baseDir);
    }

    /**
     * A remote peer is requesting to get access to the shared library for synchronizing it with us
     * <p/>
     * This process can happen along with any other process*. We just must take care that the retrieval of index and hash lists is properly
     * synchronized with other operations. A local or remote item integration might of course break the synchronization, but that is a risk that
     * we must assume, and the other peer will be notified of this.
     * <p/>
     * The library manager will reject these requests if a remote integration is taking place, because it would most certainly break the synch
     * and we would be waisting bandwidth
     */
    public synchronized ServerSynchRequestAnswer requestForSharedLibrarySynchFromRemotePeer(PeerID peerID, String list, int level) {
        if (!isRemoteDatabaseBeingIntegrated() && alive) {
            // synch process can proceed
            return acceptRequestForSharedLibrarySynch(peerID, list, level);
        } else {
            // deny
            return new ServerSynchRequestAnswer(ServerSynchRequestAnswer.Type.SERVER_BUSY, null);
        }
    }

    /**
     * A remote peer is requesting to get access to the shared library for synchronizing a specific element
     * <p/>
     * This process can happen along with any other process*. A local or remote item integration might of course break the synchronization,
     * but that is a risk that we must assume, and the other peer will be notified of this.
     * <p/>
     * The library manager will never reject these requests even if a remote integration is taking place, because it is rare that the synch
     * process breaks, and the other peer needs this urgently
     */
    public synchronized ServerSynchRequestAnswer requestForSharedLibraryItemSynchFromRemotePeer(PeerID peerID, String list, int level) {
        return acceptRequestForSharedLibrarySynch(peerID, list, level);
    }

    private ServerSynchRequestAnswer acceptRequestForSharedLibrarySynch(PeerID peerID, String list, int level) {
        return new ServerSynchRequestAnswer(ServerSynchRequestAnswer.Type.OK, sharedSynchProgressManager.initiateSynchTask(peerID, new LibraryAndLevel(list, level)));
    }

    private void handleSynchProcessError(PeerID remotePeerID, boolean isRemoteLibrary, String library, int level, SynchronizeError error) {
        List<Integer> levelList = new ArrayList<>();
        levelList.add(level);
        handleSynchProcessError(remotePeerID, isRemoteLibrary, library, levelList, error);
    }

    private void handleSynchProcessError(PeerID remotePeerID, boolean isRemoteLibrary, String library, List<Integer> levelList, SynchError error) {
        // some errors produce a fatal synch error. Others require requesting the synch again
        switch (error.type) {

            case DISCONNECTED:
                // do nothing
                break;

            case PEER_CLIENT_BUSY:
            case SERVER_BUSY:
            case ELEMENT_NOT_FOUND:
            case ELEMENT_CHANGED_IN_SERVER:
            case DATA_TRANSFER_FAILED:
            case UNDEFINED:
                if (isRemoteLibrary) {
                    programRemoteLibrarySynchTaskForLater(remotePeerID, library, levelList);
                }
                break;

            case ERROR_IN_PROTOCOL:
            case UNKNOWN_LIST:
            case INVALID_LEVEL:
            case DIFFERENT_LISTS_CONFIG:
            case REQUEST_DENIED:
            case DATA_ACCESS_ERROR:
                // fatal error
                libraryManagerNotifications.fatalErrorInSynch(error);
                break;
        }
    }

    private void handleSynchProcessTimeout(PeerID remotePeerID, boolean isRemoteLibrary, String library, int level) {
        List<Integer> levelList = new ArrayList<>();
        levelList.add(level);
        handleSynchProcessTimeout(remotePeerID, isRemoteLibrary, library, levelList);
    }

    private void handleSynchProcessTimeout(PeerID remotePeerID, boolean isRemoteLibrary, String library, List<Integer> levelList) {
        if (isRemoteLibrary) {
            programRemoteLibrarySynchTaskForLater(remotePeerID, library, levelList);
        }
    }

    private void programRemoteLibrarySynchTaskForLater(PeerID remotePeerID, String library, List<Integer> levelList) {
        delayedSynchRequestsManager.addDelayedTask(remotePeerID, library, levelList);
    }

    /**
     * Adds a new peer in the list of peers with databases. This should be invoked when we have a new friend peer. This information is stored
     * in the config information
     *
     * @param peerID new friend peer
     */
    public synchronized void addPeer(String path, PeerID peerID) throws IOException, DBException, CorruptDataException {
        // todo check is alive
        if (!remoteDatabases.containsKey(peerID)) {
            RemoteDatabase remoteDatabase = LibraryManagerIO.createNewRemoteDatabase(path, peerID);
            remoteDatabases.put(peerID, remoteDatabase);
        }
    }

    /**
     * Removes a peer from the database handling. Its stored databases will be removed and erased from the integrated information
     *
     * @param peerID peer which is no longer friend.
     */
    public synchronized void removePeer(String path, PeerID peerID) {
        if (remoteDatabases.containsKey(peerID)) {
            RemoteDatabase remoteDatabase = remoteDatabases.remove(peerID);
            LibraryManagerIO.removeRemoteDatabase(path, peerID);
            // todo remove from integrated, and copy necessary info to local database
        }
    }

    private boolean isRemoteDatabaseBeingIntegrated() {
        return remoteDatabasesIntegrator.isCurrentlyIntegrating();
    }

    /**
     * Issues an order for stopping all actions. Databases will be no longer modified. The method blocks until operation is complete
     * <p/>
     * This must be invoked before saving the state of the library manager
     */
    public void stop() {
        synchronized (this) {
            alive = false;
        }
        delayedSynchRequestsManager.stop();
        concurrencyController.beginActivity(LibraryManagerConcurrencyController.STOP);
        concurrencyController.endActivity(LibraryManagerConcurrencyController.STOP);
    }
}
