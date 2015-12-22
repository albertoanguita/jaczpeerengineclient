package jacz.peerengineclient;


import jacz.database.DatabaseMediator;

/**
 * Created by Alberto on 13/12/2015.
 */
public class DownloadInfo {

    public final DatabaseMediator.ItemType containerType;

    public final Integer containedId;

    public final DatabaseMediator.ItemType itemType;

    public final String itemHash;

    public final String itemName;

    public DownloadInfo(
            DatabaseMediator.ItemType containerType,
            Integer containedId,
            DatabaseMediator.ItemType itemType,
            String itemHash,
            String itemName) {
        this.containerType = containerType;
        this.containedId = containedId;
        this.itemType = itemType;
        this.itemHash = itemHash;
        this.itemName = itemName;
    }
}
