package jacz.peerengineclient;

import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.data_synchronization.ListAccessor;
import jacz.peerengineservice.util.data_synchronization.ListContainer;
import jacz.peerengineservice.util.data_synchronization.old.ListNotFoundException;
import jacz.peerengineservice.util.data_synchronization.old.NonIndexedListAccessorBridge;
import jacz.peerengineservice.util.data_synchronization.premade_lists.old.SimplePersonalData;
import jacz.peerengineclient.dbs.LibraryManager;

/**
 * List container implementation
 */
public class ListContainerImpl implements ListContainer {

    private final JPeerEngineClient jPeerEngineClient;

    private final LibraryManager libraryManager;

    public ListContainerImpl(JPeerEngineClient jPeerEngineClient, LibraryManager libraryManager) {
        this.jPeerEngineClient = jPeerEngineClient;
        this.libraryManager = libraryManager;
    }

    @Override
    public ListAccessor getListForTransmitting(PeerID peerID, String list) throws ListNotFoundException {
        if (list.equals(SimplePersonalData.getListName())) {
            return jPeerEngineClient.getOwnSimplePersonalDataListAccessor();
        } else {
            return libraryManager.getSharedListAccessor(list, jPeerEngineClient.getFileHashDatabase(), jPeerEngineClient.getBaseDataDir());
        }
    }

    @Override
    public ListAccessor getListForReceiving(PeerID peerID, String list) throws ListNotFoundException {
        if (list.equals(SimplePersonalData.getListName())) {
            SimplePersonalData simplePersonalData = jPeerEngineClient.getSimplePersonalData(peerID);
            if (simplePersonalData != null) {
                return new NonIndexedListAccessorBridge(simplePersonalData);
            } else {
                throw new ListNotFoundException();
            }
        } else {
            return libraryManager.getRemoteListAccessor(peerID, list, jPeerEngineClient.getFileHashDatabase(), jPeerEngineClient.getBaseDataDir());
        }
    }
}
