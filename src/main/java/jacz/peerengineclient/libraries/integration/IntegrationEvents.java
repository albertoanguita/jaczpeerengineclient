package jacz.peerengineclient.libraries.integration;

import jacz.store.database.DatabaseMediator;

/**
 * Events related to the integration of libraries are reported here. A separate, unique thread is in charge
 * of invoking these methods
 */
public interface IntegrationEvents {

    /**
     * A new integrated item has been created
     *
     * @param type type of the item
     * @param id   identifier of the new item
     */
    void newIntegratedItem(DatabaseMediator.ItemType type, Integer id);

    /**
     * An item in the integrated database has been modified and as a result has new media content
     *
     * @param type type of the item
     * @param id       id of the item in the integrated database that has new media content
     */
    void integratedItemHasNewMediaContent(DatabaseMediator.ItemType type, Integer id);

    void integratedItemDeleted(DatabaseMediator.ItemType type, Integer id);
}
