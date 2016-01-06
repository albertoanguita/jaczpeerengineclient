package jacz.peerengineclient;


import jacz.database.DatabaseMediator;

/**
 * Created by Alberto on 13/12/2015.
 */
public class DownloadInfo {

    public enum Type {
        VIDEO_FILE,
        SUBTITLE_FILE,
        IMAGE;

        public boolean isMedia() {
            return this == VIDEO_FILE || this == SUBTITLE_FILE;
        }
    }

    public final Type type;

    public final DatabaseMediator.ItemType containerType;

    public final Integer containerId;

    /**
     * null for images
     */
    public final Integer itemId;

    public final String fileHash;

    public final String fileName;

    public DownloadInfo(
            Type type,
            DatabaseMediator.ItemType containerType,
            Integer containerId,
            Integer itemId,
            String fileHash,
            String fileName) {
        this.type = type;
        this.containerType = containerType;
        this.containerId = containerId;
        this.itemId = itemId;
        this.fileHash = fileHash;
        this.fileName = fileName;
    }
}
