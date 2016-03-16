package jacz.peerengineclient.databases.integration;

import jacz.database.DatabaseMediator;
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
        sequentialTaskExecutor.executeTask(() -> integrationEvents.newIntegratedItem(type, id));
    }

    @Override
    public void integratedItemHasNewMediaContent(DatabaseMediator.ItemType type, Integer id) {
        sequentialTaskExecutor.executeTask(() -> integrationEvents.integratedItemHasNewMediaContent(type, id));
    }

    @Override
    public void integratedItemsRemoved() {
        sequentialTaskExecutor.executeTask(integrationEvents::integratedItemsRemoved);
    }

    public void stop() {
        sequentialTaskExecutor.stopAndWaitForFinalization();
    }
}
