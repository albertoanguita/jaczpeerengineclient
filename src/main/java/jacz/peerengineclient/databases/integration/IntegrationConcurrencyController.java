package jacz.peerengineclient.databases.integration;

import org.aanguita.jacuzzi.concurrency.concurrency_controller.ConcurrencyControllerAction;
import org.aanguita.jacuzzi.maps.ObjectCount;

/**
 * Created by Alberto on 06/12/2015.
 */
public class IntegrationConcurrencyController implements ConcurrencyControllerAction {

    public enum Activity {
        LOCAL_TO_INTEGRATED,
        REMOTE_TO_INTEGRATED,
        INTEGRATED_TO_SHARED
    }

    @Override
    public int maxNumberOfExecutionsAllowed() {
        return 1;
    }

    @Override
    public int getActivityPriority(String activity) {
        switch (activity) {
            case "LOCAL_TO_INTEGRATED":
                return 3;

            case "REMOTE_TO_INTEGRATED":
                return 2;

            case "INTEGRATED_TO_SHARED":
                return 1;

        }
        return 0;
    }

    @Override
    public boolean activityCanExecute(String activity, ObjectCount<String> numberOfExecutionsOfActivities) {
        // we only allow one activity at a time (max = 1), so concurrency issues are already solved
        return true;
    }

    @Override
    public void activityIsGoingToBegin(String activity, ObjectCount<String> numberOfExecutionsOfActivities) {
        // ignore
    }

    @Override
    public void activityHasEnded(String activity, ObjectCount<String> numberOfExecutionsOfActivities) {
        // ignore
    }
}
