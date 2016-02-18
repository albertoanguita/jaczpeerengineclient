package jacz.peerengineclient;

import jacz.peerengineclient.data.PeerShareManager;
import jacz.peerengineclient.data.synch.FileHashDatabaseAccessor;
import jacz.peerengineclient.data.synch.TempFilesAccessor;
import jacz.peerengineclient.databases.DatabaseManager;
import jacz.peerengineclient.databases.synch.DatabaseAccessor;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.util.data_synchronization.AccessorNotFoundException;
import jacz.peerengineservice.util.data_synchronization.DataAccessor;
import jacz.peerengineservice.util.data_synchronization.DataAccessorContainer;
import jacz.peerengineservice.util.data_synchronization.ServerBusyException;

/**
 * todo complete
 */
public class DataAccessorContainerImpl implements DataAccessorContainer {

    private final DatabaseManager databaseManager;

    private final PeerShareManager peerShareManager;

    public DataAccessorContainerImpl(DatabaseManager databaseManager, PeerShareManager peerShareManager) {
        this.databaseManager = databaseManager;
        this.peerShareManager = peerShareManager;
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
            case DatabaseAccessor.NAME:
                return databaseManager.requestForSharedDatabaseSynchFromRemotePeer(peerID);

            case FileHashDatabaseAccessor.NAME:
                return peerShareManager.requestForLocalHashSynch(peerID);

            case TempFilesAccessor.NAME:
                return peerShareManager.requestForLocalTempFilesSynch(peerID);

            default:
                // todo error
                throw new AccessorNotFoundException();
        }
    }
}
