package jacz.peerengineclient.dbs_old.data_synch;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.data_synchronization.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * List accessor implementation for a generic ordered list of strings
 */
public abstract class ItemListAccessor implements ListAccessor {

    private Map<String, Integer> stringsAndPosition;

    public abstract Map<String, Integer> getStringsAndPosition();

    public abstract void notifyChange();

    @Override
    public int getLevelCount() {
        return 1;
    }

    @Override
    public void beginSynchProcess(Mode mode) {
        // ignore
    }

    @Override
    public Collection<IndexAndHash> getHashList(int level) throws DataAccessException {
        stringsAndPosition = getStringsAndPosition();
        List<IndexAndHash> indexAndHashList = new ArrayList<>();
        for (Map.Entry<String, Integer> stringAndPosition : stringsAndPosition.entrySet()) {
            indexAndHashList.add(new IndexAndHash(stringAndPosition.getKey() + "," + stringAndPosition.getValue(), null));
        }
        return indexAndHashList;
    }

    @Override
    public boolean hashEqualsElement(int level) {
        return true;
    }

    @Override
    public TransmissionType getTransmissionType(int level) {
        // ignore
        return null;
    }

    @Override
    public List<Integer> getInnerListLevels(int level) {
        // ignore
        return null;
    }

    @Override
    public ListAccessor getInnerList(String index, int level, boolean buildElementIfNeeded) throws ElementNotFoundException, DataAccessException {
        // ignore
        return null;
    }

    @Override
    public String getElementHash(String index, int requestLevel) throws ElementNotFoundException, DataAccessException {
        // ignore
        return null;
    }

    @Override
    public Serializable getElementObject(String index, int level) throws ElementNotFoundException, DataAccessException {
        // ignore
        return null;
    }

    @Override
    public byte[] getElementByteArray(String index, int level) throws ElementNotFoundException, DataAccessException {
        // ignore
        return new byte[0];
    }

    @Override
    public int getElementByteArrayLength(String index, int level) throws ElementNotFoundException, DataAccessException {
        // ignore
        return 0;
    }

    @Override
    public void addElementAsObject(String index, int level, Object element) throws DataAccessException {
        String[] split = index.split(",");
        String id = split[0];
        int position = Integer.parseInt(split[1]);
        stringsAndPosition.put(id, position);
    }

    @Override
    public void addElementAsByteArray(String index, int level, byte[] data) throws DataAccessException {
        // ignore
    }

    @Override
    public boolean mustEraseOldIndexes() {
        return true;
    }

    @Override
    public void eraseElements(Collection<String> indexes) throws DataAccessException {
        for (String index : indexes) {
            String id = index.split(",")[0];
            stringsAndPosition.remove(id);
        }
    }

    @Override
    public void endSynchProcess(Mode mode, boolean success) {
        if (mode.isClient()) {
            notifyChange();
        }
    }

    @Override
    public ServerSynchRequestAnswer initiateListSynchronizationAsServer(PeerID clientPeerID, int level, boolean singleElement) {
        return new ServerSynchRequestAnswer(ServerSynchRequestAnswer.Type.OK, null);
    }
}
