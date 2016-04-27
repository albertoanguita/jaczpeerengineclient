package jacz.peerengineclient.databases.integration;

import jacz.database.DatabaseMediator;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Alberto on 28/12/2015.
 */
public class IntegrationEventsBridge implements IntegrationEvents {
    // todo logger

    private final IntegrationEvents integrationEvents;

    private final ExecutorService sequentialTaskExecutor;

    public IntegrationEventsBridge(IntegrationEvents integrationEvents) {
        this.integrationEvents = integrationEvents;
        this.sequentialTaskExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void newIntegratedItem(DatabaseMediator.ItemType type, Integer id) {
        sequentialTaskExecutor.submit(() -> integrationEvents.newIntegratedItem(type, id));
    }

    @Override
    public void integratedItemHasNewMediaContent(DatabaseMediator.ItemType type, Integer id) {
        sequentialTaskExecutor.submit(() -> integrationEvents.integratedItemHasNewMediaContent(type, id));
    }

    @Override
    public void integratedItemsRemoved() {
        sequentialTaskExecutor.submit(integrationEvents::integratedItemsRemoved);
    }

    public void stop() {
        sequentialTaskExecutor.shutdown();
    }
}
