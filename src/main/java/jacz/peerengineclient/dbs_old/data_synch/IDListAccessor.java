package jacz.peerengineclient.dbs_old.data_synch;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.data_synchronization.old.*;
import jacz.util.lists.Duple;
import jacz.util.string.StringOps;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * todo move to Peer engine
 * Generic list accessor for ordered non-repeating lists of Strings
 */
public abstract class IDListAccessor implements NonIndexedListAccessor {

    private List<String> hashList;

    protected IDListAccessor(List<String> idList) {
        hashList = new ArrayList<>();
        for (int i = 0; i < idList.size() - 1; i++) {
            hashList.add(idList.get(i) + "," + idList.get(i + 1));
        }
        hashList.add(idList.get(idList.size() - 1) + ",");
    }

    public abstract void reportNewList(List<String> idList);

    @Override
    public void beginSynchProcess(ListAccessor.Mode mode) {
        // ignore
    }

    @Override
    public synchronized Collection<String> getHashList() throws DataAccessException {
        return hashList;
    }

    @Override
    public boolean hashEqualsElement() {
        // ids are smaller than hashes
        return true;
    }

    @Override
    public TransmissionType getTransmissionType() {
        return TransmissionType.OBJECT;
    }

    @Override
    public boolean mustRequestElement(String index, int level, String hash) throws DataAccessException {
        return true;
    }

    @Override
    public Serializable getElementObject(String index) throws ElementNotFoundException, DataAccessException {
        // ignore
        return null;
    }

    @Override
    public byte[] getElementByteArray(String hash) throws ElementNotFoundException, DataAccessException {
        // ignore
        return null;
    }


    @Override
    public int getElementByteArrayLength(String index) throws ElementNotFoundException, DataAccessException {
        // ignore
        return 0;
    }

    @Override
    public void addElementAsObject(Object element) throws DataAccessException {
        hashList.add((String) element);
    }

    @Override
    public synchronized void addElementAsByteArray(byte[] data) throws DataAccessException {
        // ignore
    }

    @Override
    public boolean mustEraseOldIndexes() {
        return true;
    }

    @Override
    public void eraseElements(Collection<String> indexes) throws DataAccessException {
        hashList.removeAll(indexes);
    }

    @Override
    public void endSynchProcess(ListAccessor.Mode mode, boolean success) {
        try {
            if (mode.isClient()) {
                List<String> idList = new ArrayList<>();
                if (!hashList.isEmpty()) {
                    Duple<String, String> firstHash = separateHash(hashList.remove(0));
                    idList.add(firstHash.element1);
                    idList.add(firstHash.element2);
                }
                while (!hashList.isEmpty()) {
                    boolean found = false;
                    for (int i = 0; i < hashList.size(); i++) {
                        Duple<String, String> hash = separateHash(hashList.get(i));
                        if (hash.element1.equals(idList.get(idList.size() - 1))) {
                            idList.add(hash.element2);
                            found = true;
                        } else if (hash.element2 != null && hash.element2.equals(idList.get(0))) {
                            hashList.remove(i);
                            idList.add(0, hash.element1);
                            found = true;
                        }
                        if (found) {
                            hashList.remove(i);
                            break;
                        }
                    }
                    if (!found) {
                        // a whole iteration without finding a match, error
                        throw new Exception();
                    }
                }
                reportNewList(idList);
            }
        } catch (Exception e) {
            // error parsing, report the error
            reportNewList(null);
        }
    }

    private static Duple<String, String> separateHash(String hash) throws ParseException {
        List<String> tokens = StringOps.separateTokens(hash, ",", false, "", 2);
        return new Duple<>(tokens.get(0), tokens.get(1));
    }

    @Override
    public ServerSynchRequestAnswer initiateListSynchronizationAsServer(PeerID clientPeerID, boolean singleElement) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

}