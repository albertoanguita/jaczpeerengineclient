package jacz.peerengineclient.databases.integration;


import jacz.database.DatabaseMediator;

/**
 * Events related to the integration of databases are reported here. A separate, unique thread is in charge
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
     * An item in the integrated database has been modified
     *
     * @param type type of the item
     * @param id       id of the item in the integrated database
     */
    void integratedItemHasBeenModified(DatabaseMediator.ItemType type, Integer id, boolean hasNewMediaContent);

    // these two methods are currently deactivated. I need to change the algorithm. We must report in two cases:
    // 1) the item exists, and we add a new file that did not exist before -> go through all items and check the ones
    // that point to that hash
    // 2) I inflate an integrated item, and as a result it points to a new file that DOES exist
    void integratedItemHasNewMedia(DatabaseMediator.ItemType type, Integer id);

    void integratedItemHasNewImage(DatabaseMediator.ItemType type, Integer id);

    void integratedItemRemoved(DatabaseMediator.ItemType type, Integer id);
}
