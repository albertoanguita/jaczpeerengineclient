package jacz.peerengineclient;


import jacz.database.DatabaseMediator;

import java.io.Serializable;
import java.util.HashMap;

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

    public final Integer superContainerId;

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
            Integer superContainerId,
            Integer itemId,
            String fileHash,
            String fileName) {
        this.type = type;
        this.containerType = containerType;
        this.containerId = containerId;
        this.superContainerId = superContainerId;
        this.itemId = itemId;
        this.fileHash = fileHash;
        this.fileName = fileName;
    }

    public HashMap<String, Serializable> buildUserDictionary() {
        HashMap<String, Serializable> userDictionary = new HashMap<>();
        userDictionary.put("type", type);
        userDictionary.put("containerType", containerType);
        userDictionary.put("containerId", containerId);
        userDictionary.put("superContainerId", superContainerId);
        userDictionary.put("itemId", itemId);
        userDictionary.put("fileHash", fileHash);
        userDictionary.put("fileName", fileName);
        return userDictionary;
    }

    public static DownloadInfo buildDownloadInfo(HashMap<String, Serializable> userDictionary) {
        return new DownloadInfo(
                (Type) userDictionary.get("type"),
                (DatabaseMediator.ItemType) userDictionary.get("containerType"),
                (Integer) userDictionary.get("containerId"),
                (Integer) userDictionary.get("superContainerId"),
                (Integer) userDictionary.get("itemId"),
                (String) userDictionary.get("fileHash"),
                (String) userDictionary.get("fileName")
        );
    }
}
