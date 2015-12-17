package jacz.peerengineclient;

import jacz.peerengineclient.libraries.LibraryManager;
import jacz.peerengineclient.libraries.synch.LibraryAccessor;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.data_synchronization.AccessorNotFoundException;
import jacz.peerengineservice.util.data_synchronization.DataAccessor;
import jacz.peerengineservice.util.data_synchronization.DataAccessorContainer;
import jacz.peerengineservice.util.data_synchronization.ServerBusyException;

/**
 * Created by Alberto on 12/12/2015.
 */
public class DataAccessorContainerImpl implements DataAccessorContainer {

    private final LibraryManager libraryManager;

    public DataAccessorContainerImpl(LibraryManager libraryManager) {
        this.libraryManager = libraryManager;
    }

    @Override
    public void peerConnected(PeerID peerID) {

    }

    @Override
    public void peerDisconnected(PeerID peerID) {

    }

    @Override
    public DataAccessor getAccessorForTransmitting(PeerID peerID, String dataAccessorName) throws AccessorNotFoundException, ServerBusyException {
        switch (dataAccessorName) {
            case LibraryAccessor.NAME:
                return libraryManager.requestForSharedLibrarySynchFromRemotePeer(peerID);

//            case "qwer":
//                return null;

            default:
                throw new AccessorNotFoundException();
        }
    }

    @Override
    public DataAccessor getAccessorForReceiving(PeerID peerID, String dataAccessorName) throws AccessorNotFoundException {
        return null;
    }
}
