package jacz.peerengineclient;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.data_synchronization.ListAccessor;
import jacz.peerengineservice.util.data_synchronization.ListContainer;
import jacz.peerengineservice.util.data_synchronization.old.ListNotFoundException;
import jacz.peerengineservice.util.data_synchronization.old.NonIndexedListAccessorBridge;
import jacz.peerengineservice.util.data_synchronization.premade_lists.old.SimplePersonalData;
import jacz.peerengineclient.dbs_old.LibraryManager;

/**
 * List container implementation
 */
public class ListContainerImpl implements ListContainer {

    private final PeerEngineClient peerEngineClient;

    private final LibraryManager libraryManager;

    public ListContainerImpl(PeerEngineClient peerEngineClient, LibraryManager libraryManager) {
        this.peerEngineClient = peerEngineClient;
        this.libraryManager = libraryManager;
    }

    @Override
    public ListAccessor getListForTransmitting(PeerID peerID, String list) throws ListNotFoundException {
        if (list.equals(SimplePersonalData.getListName())) {
            return peerEngineClient.getOwnSimplePersonalDataListAccessor();
        } else {
            return libraryManager.getSharedListAccessor(list, peerEngineClient.getFileHashDatabase(), peerEngineClient.getBaseDataDir());
        }
    }

    @Override
    public ListAccessor getListForReceiving(PeerID peerID, String list) throws ListNotFoundException {
        if (list.equals(SimplePersonalData.getListName())) {
            SimplePersonalData simplePersonalData = peerEngineClient.getSimplePersonalData(peerID);
            if (simplePersonalData != null) {
                return new NonIndexedListAccessorBridge(simplePersonalData);
            } else {
                throw new ListNotFoundException();
            }
        } else {
            return libraryManager.getRemoteListAccessor(peerID, list, peerEngineClient.getFileHashDatabase(), peerEngineClient.getBaseDataDir());
        }
    }
}
