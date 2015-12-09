package jacz.peerengineclient.libraries;

import jacz.store.database.DatabaseMediator;

/**
 * Events related to the integration of libraries are reported here. A separate, unique thread is in charge
 * of invoking these methods
 */
public interface LibraryIntegrationEvents {

    /**
     * An item in the integrated database has been modified. User should see an updated visualization
     *
     * @param itemType type of the item
     * @param id       id of the item in the integrated database
     */
    void integratedItemModified(DatabaseMediator.ItemType itemType, Integer id);


}
