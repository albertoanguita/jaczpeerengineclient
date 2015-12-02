package jacz.peerengineclient.dbs_old;

import jacz.util.concurrency.concurrency_controller.ActivityListAndPriorities;
import jacz.util.concurrency.concurrency_controller.ConcurrencyController;
import jacz.util.maps.ObjectCount;

/**
* This class handles synchronization of the different activities that happen in the library manager
*/
public class LibraryManagerConcurrencyController extends ConcurrencyController {

    /**
     * Task for integrating a single local item in the integrated database
     */
    public static final String INTEGRATE_LOCAL_ITEM = "INTEGRATE_LOCAL_ITEM";

    /**
     * Task for integrating all modified remote libraries in the integrated database. During this task, many INTEGRATE_REMOTE_ITEM tasks are executed
     */
    public static final String INTEGRATE_REMOTE_LIBRARIES = "INTEGRATE_REMOTE_LIBRARIES";

    /**
     * Task for integrating a single remote item in the integrated database. These tasks are spanned during the INTEGRATE_REMOTE_LIBRARIES task
     */
    public static final String INTEGRATE_REMOTE_ITEM = "INTEGRATE_REMOTE_ITEM";

    /**
     * Task of synching a remote library, as requested by the user. This task takes all accumulated synch tasks and executes them.
     * If there were no remote libraries to synch waiting, or there was already a synch task executing, this task ends as soon as starts, so we
     * don't span many of these. The first task that created the synch progress manager will be the only one that survives during the process, and
     * the one that completes ones the synch progress manager indicates that all remote libraries have been synched
     */
    public static final String SYNCH_REMOTE_LIBRARY = "SYNCH_REMOTE_LIBRARY";

    public static final String STOP = "STOP";



    @Override
    protected void initializeActivityList(ActivityListAndPriorities activityListAndPriorities) {
        // local item integration has the fastest priority, as we want local changes to be instantly visualized
        // synch tasks can overrun remote library integrations, so all the synch tasks get aggregated and we get all remote items before
        // integrating
        activityListAndPriorities.addActivity(INTEGRATE_LOCAL_ITEM, 3);
        activityListAndPriorities.addActivity(INTEGRATE_REMOTE_LIBRARIES, 0);
        activityListAndPriorities.addActivity(INTEGRATE_REMOTE_ITEM, 0);
        activityListAndPriorities.addActivity(SYNCH_REMOTE_LIBRARY, 1);
        activityListAndPriorities.addActivity(STOP, -1);
    }


    @Override
    protected boolean activityCanExecute(String activity, ObjectCount<String> numberOfExecutionsOfActivities) {
        switch (activity) {
            case INTEGRATE_LOCAL_ITEM:
                // local item integration can happen only if there is no task for integrating a remote item, or another local item
                // it can happen during a remote library integration and during a remote library synch
                return numberOfExecutionsOfActivities.getObjectCount(INTEGRATE_LOCAL_ITEM) == 0 &&
                        numberOfExecutionsOfActivities.getObjectCount(INTEGRATE_REMOTE_ITEM) == 0;

            case INTEGRATE_REMOTE_LIBRARIES:
                // a remote library integration can only happen if we are not synchronizing libraries
                // we also exclude the possibility of several remote libraries integrations, although this should not happen
                return numberOfExecutionsOfActivities.getObjectCount(SYNCH_REMOTE_LIBRARY) == 0 &&
                        numberOfExecutionsOfActivities.getObjectCount(INTEGRATE_REMOTE_LIBRARIES) == 0;

            case INTEGRATE_REMOTE_ITEM:
                // a remote item integration can only happen during a remote library integration, but this is assumed since this tasks is in
                // charge of creating remote item integration tasks
                // we only have to check that no other local or remote items are being integrated
                // we don't care about remote library synch
                return numberOfExecutionsOfActivities.getObjectCount(INTEGRATE_LOCAL_ITEM) == 0 &&
                        numberOfExecutionsOfActivities.getObjectCount(INTEGRATE_REMOTE_ITEM) == 0;

            case SYNCH_REMOTE_LIBRARY:
                // remote library synch can only happen when we are not integrating a remote library (since this includes remote item integration)
                // we don't care about local item integration
                // it can happen with other remote library synch tasks
                // it overruns pending remote integrations, to aggregate al synch tasks together
                return numberOfExecutionsOfActivities.getObjectCount(INTEGRATE_REMOTE_LIBRARIES) == 0;

            case STOP:
                // the stop waits until all other activities are concluded
                // todo, in the future, be able to interrupt the integration activity. This implies saving the not yet integrated items
                return numberOfExecutionsOfActivities.getTotalCount() == 0;

            default:
                // cannot happen
                return false;
        }
    }
}
