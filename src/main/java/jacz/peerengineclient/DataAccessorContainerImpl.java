package jacz.peerengineclient;

import jacz.peerengineclient.data.PeerShareManager;
import jacz.peerengineclient.data.synch.FileHashDatabaseAccessor;
import jacz.peerengineclient.data.synch.TempFilesAccessor;
import jacz.peerengineclient.databases.DatabaseManager;
import jacz.peerengineclient.databases.synch.DatabaseAccessor;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.util.data_synchronization.AccessorNotFoundException;
import jacz.peerengineservice.util.data_synchronization.DataAccessor;
import jacz.peerengineservice.util.data_synchronization.DataAccessorContainer;
import jacz.peerengineservice.util.data_synchronization.ServerBusyException;

/**
 * Data Accessor container implementation for the peer client
 */
public class DataAccessorContainerImpl implements DataAccessorContainer {

    private final DatabaseManager databaseManager;

    private final PeerShareManager peerShareManager;

    public DataAccessorContainerImpl(DatabaseManager databaseManager, PeerShareManager peerShareManager) {
        this.databaseManager = databaseManager;
        this.peerShareManager = peerShareManager;
    }

    @Override
    public void peerConnected(PeerId peerId) {

    }

    @Override
    public void peerDisconnected(PeerId peerID) {

    }

    @Override
    public DataAccessor getAccessorForTransmitting(PeerId peerID, String dataAccessorName) throws AccessorNotFoundException, ServerBusyException {
        switch (dataAccessorName) {
            case DatabaseAccessor.NAME:
                return databaseManager.requestForSharedDatabaseSynchFromRemotePeer(peerID);

            case FileHashDatabaseAccessor.NAME:
                return peerShareManager.requestForLocalHashSynch(peerID);

            case TempFilesAccessor.NAME:
                return peerShareManager.requestForLocalTempFilesSynch(peerID);

            default:
                // todo fatal error
                throw new AccessorNotFoundException();
        }
    }
}
