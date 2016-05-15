package jacz.peerengineclient.data;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * Created by Alberto on 03/12/2015.
 */
public class SerializedHashItem implements Serializable, Comparable<SerializedHashItem> {

    public final long timestamp;

    public final String hash;

    public final boolean alive;

    public SerializedHashItem(long timestamp, String hash) {
        this(timestamp, hash, true);
    }

    public SerializedHashItem(long timestamp, String hash, boolean alive) {
        this.timestamp = timestamp;
        this.hash = hash;
        this.alive = alive;
    }

    @Override
    public int compareTo(@NotNull SerializedHashItem o) {
        if (timestamp == o.timestamp) {
            return 0;
        } else {
            return timestamp < o.timestamp ? -1 : 1;
        }
    }

    @Override
    public String toString() {
        return "SerializedHashItem{" +
                "timestamp=" + timestamp +
                ", hash='" + hash + '\'' +
                ", alive=" + alive +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SerializedHashItem that = (SerializedHashItem) o;

        if (timestamp != that.timestamp) return false;
        if (alive != that.alive) return false;
        return hash.equals(that.hash);

    }

    @Override
    public int hashCode() {
        int result = (int) (timestamp ^ (timestamp >>> 32));
        result = 31 * result + hash.hashCode();
        result = 31 * result + (alive ? 1 : 0);
        return result;
    }
}
