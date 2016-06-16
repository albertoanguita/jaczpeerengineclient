package jacz.peerengineclient.common;

import jacz.database.DatabaseMediator;
import jacz.peerengineclient.databases.integration.IntegrationEvents;

/**
 * Created by Alberto on 28/04/2016.
 */
public class IntegrationEventsImpl implements IntegrationEvents {

    @Override
    public void newIntegratedItem(DatabaseMediator.ItemType type, Integer id) {
        System.out.println("New integrated item. Type: " + type + ", id: " + id);
    }

    @Override
    public void integratedItemHasBeenModified(DatabaseMediator.ItemType type, Integer id, boolean hasNewMediaContent) {
        System.out.println("Integrated item has been modified. Type: " + type + ", id: " + id);
    }

    @Override
    public void integratedItemHasNewMedia(DatabaseMediator.ItemType type, Integer id) {
        System.out.println("Integrated item has new media. Type: " + type + ", id: " + id);
    }

    @Override
    public void integratedItemHasNewImage(DatabaseMediator.ItemType type, Integer id) {
        System.out.println("Integrated item has new image. Type: " + type + ", id: " + id);
    }

    @Override
    public void integratedItemRemoved(DatabaseMediator.ItemType type, Integer id) {
        System.out.println("Integrated item removed. Type: " + type + ", id: " + id);
    }
}
