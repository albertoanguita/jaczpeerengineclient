package jacz.peerengineclient.peer_share;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * Created by Alberto on 03/12/2015.
 */
public class SerializedHashItem implements Serializable, Comparable<SerializedHashItem> {

    public final int timestamp;

    public final String hash;

    public final boolean alive;

    public SerializedHashItem(int timestamp, String hash) {
        this(timestamp, hash, true);
    }

    public SerializedHashItem(int timestamp, String hash, boolean alive) {
        this.timestamp = timestamp;
        this.hash = hash;
        this.alive = alive;
    }

    @Override
    public int compareTo(@NotNull SerializedHashItem o) {
        return timestamp - o.timestamp;
    }
}
