package jacz.peerengineclient.databases.integration;

import jacz.database.DatabaseMediator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Alberto on 28/12/2015.
 */
public class IntegrationEventsBridge implements IntegrationEvents {

    private final static Logger logger = LoggerFactory.getLogger(IntegrationEventsBridge.class);

    private final IntegrationEvents integrationEvents;

    private final ExecutorService sequentialTaskExecutor;

    public IntegrationEventsBridge(IntegrationEvents integrationEvents) {
        this.integrationEvents = integrationEvents;
        this.sequentialTaskExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void newIntegratedItem(DatabaseMediator.ItemType type, Integer id) {
        logger.info("new integrated item. type: " + type + ", id: " + id);
        sequentialTaskExecutor.submit(() -> integrationEvents.newIntegratedItem(type, id));
    }

    @Override
    public void integratedItemHasNewMediaContent(DatabaseMediator.ItemType type, Integer id) {
        logger.info("integrated item has new media content. type: " + type + ", id: " + id);
        sequentialTaskExecutor.submit(() -> integrationEvents.integratedItemHasNewMediaContent(type, id));
    }

    @Override
    public void integratedItemsRemoved() {
        logger.info("integrated item removed");
        sequentialTaskExecutor.submit(integrationEvents::integratedItemsRemoved);
    }

    public void stop() {
        sequentialTaskExecutor.shutdown();
    }
}
