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
    public void integratedItemHasBeenModified(DatabaseMediator.ItemType type, Integer id, boolean hasNewMediaContent) {
        logger.info("integrated item has been modified. type: " + type + ", id: " + id);
        sequentialTaskExecutor.submit(() -> integrationEvents.integratedItemHasBeenModified(type, id, hasNewMediaContent));
    }

    @Override
    public void integratedItemHasNewMedia(DatabaseMediator.ItemType type, Integer id) {
        // todo this is not true when adding local file. It is still not associated to the item
        logger.info("integrated item has new media. type: " + type + ", id: " + id);
        sequentialTaskExecutor.submit(() -> integrationEvents.integratedItemHasNewMedia(type, id));
    }

    @Override
    public void integratedItemHasNewImage(DatabaseMediator.ItemType type, Integer id) {
        logger.info("integrated item has new image. type: " + type + ", id: " + id);
        sequentialTaskExecutor.submit(() -> integrationEvents.integratedItemHasNewImage(type, id));
    }

    @Override
    public void integratedItemRemoved(DatabaseMediator.ItemType type, Integer id) {
        logger.info("integrated item removed");
        sequentialTaskExecutor.submit(() -> integrationEvents.integratedItemRemoved(type, id));
    }

    public void stop() {
        sequentialTaskExecutor.shutdown();
    }
}
