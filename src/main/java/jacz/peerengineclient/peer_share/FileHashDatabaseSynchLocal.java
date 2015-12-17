package jacz.peerengineclient.peer_share;

import jacz.peerengineservice.util.data_synchronization.DataAccessException;
import jacz.util.maps.DoubleMap;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by Alberto on 03/12/2015.
 */
public class FileHashDatabaseSynchLocal extends FileHashDatabaseSynch {

    private final Map<Integer, String> deletedHashes;

    private Integer nextTimestamp;

    public FileHashDatabaseSynchLocal(DoubleMap<Integer, String> activeHashes, Map<Integer, String> deletedHashes, String id) {
        super(activeHashes, id);
        this.deletedHashes = deletedHashes;
    }

    public void addHash(String hash) {
        activeHashes.put(nextTimestamp++, hash);
    }

    public void removeHash(String hash) {
        int timestamp = activeHashes.removeReverse(hash);
        deletedHashes.put(timestamp, hash);
    }

    @Override
    public Integer getLastTimestamp() throws DataAccessException {
        // cannot happen in local
        return null;
    }

    @Override
    public List<? extends Serializable> getElementsFrom(final int fromTimestamp) throws DataAccessException {
        List<SerializedHashItem> items =
                activeHashes
                        .entrySet()
                        .stream()
                        .filter(entry -> entry.getKey() >= fromTimestamp)
                        .map(entry -> new SerializedHashItem(entry.getKey(), entry.getValue()))
                        .collect(Collectors.toList());
        items.addAll(
                deletedHashes
                        .entrySet()
                        .stream()
                        .filter(entry -> entry.getKey() >= fromTimestamp)
                        .map(entry -> new SerializedHashItem(entry.getKey(), entry.getValue(), false))
                        .collect(Collectors.toList()));
        Collections.sort(items);
        return null;
    }


}
