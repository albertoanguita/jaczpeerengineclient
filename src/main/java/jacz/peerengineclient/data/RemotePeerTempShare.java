package jacz.peerengineclient.data;

import jacz.peerengineservice.PeerId;

import java.util.HashSet;
import java.util.Set;

/**
 * This class stores the resources offered by the temp downloads manager of a remote peer. These resources are
 * synchronized with an accessor, like the normal share, but without the timestamp (since just a few files are being
 * downloaded at a time, it is ok to share them all). Therefore, we just store the set of hashes from the last
 * synchronization.
 * <p>
 * In every synch process, we update this set, adding the new resources to the foreign stores and removing the ones
 * that are not present anymore.
 */
public class RemotePeerTempShare {

    private final PeerId remotePeerId;

    private final ForeignShares foreignShares;

    private final Set<String> tempResources;

    public RemotePeerTempShare(PeerId remotePeerId, ForeignShares foreignShares) {
        this.remotePeerId = remotePeerId;
        this.foreignShares = foreignShares;
        this.tempResources = new HashSet<>();
    }

    public void startNewSynch() {
        tempResources.clear();
    }

    public void addTempResource(String hash) {
        tempResources.add(hash);
    }

    public void completeSynch() {
        foreignShares.reportVolatileResources(remotePeerId, tempResources);
    }
}
