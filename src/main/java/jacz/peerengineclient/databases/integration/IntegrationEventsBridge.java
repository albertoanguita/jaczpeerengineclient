package jacz.peerengineclient.databases.integration;

import jacz.database.DatabaseMediator;
import jacz.util.concurrency.task_executor.ParallelTask;
import jacz.util.concurrency.task_executor.SequentialTaskExecutor;

/**
 * Created by Alberto on 28/12/2015.
 */
public class IntegrationEventsBridge implements IntegrationEvents {
    // todo logger

    private final IntegrationEvents integrationEvents;

    private final SequentialTaskExecutor sequentialTaskExecutor;

    public IntegrationEventsBridge(IntegrationEvents integrationEvents) {
        this.integrationEvents = integrationEvents;
        this.sequentialTaskExecutor = new SequentialTaskExecutor();
    }

    @Override
    public void newIntegratedItem(DatabaseMediator.ItemType type, Integer id) {
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                integrationEvents.newIntegratedItem(type, id);
            }
        });
    }

    @Override
    public void integratedItemHasNewMediaContent(DatabaseMediator.ItemType type, Integer id) {
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                integrationEvents.integratedItemHasNewMediaContent(type, id);
            }
        });
    }

    @Override
    public void integratedItemDeleted(DatabaseMediator.ItemType type, Integer id) {
        sequentialTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                integrationEvents.integratedItemDeleted(type, id);
            }
        });
    }

    public void stop() {
        sequentialTaskExecutor.stopAndWaitForFinalization();
    }
}
