package jacz.peerengineclient.util.synch;

import jacz.peerengineservice.PeerID;

import java.util.HashMap;
import java.util.Map;

/**
 * This class registers the past synch processes (both for shared and remote), allowing us to know how long ago
 * a peer synched our shared library with us
 */
public class SynchRecord {

    private static final long RECENTLY_THRESHOLD = 30000;

    /**
     * For each peer, stores the last time that we synched the shared library with him
     */
    private final Map<PeerID, Long> peerSharedSynchEvents;

    /**
     * events occurred more recently than this period of time are considered as recent (in ms)
     */
    private final long threshold;

    public SynchRecord(long threshold) {
        peerSharedSynchEvents = new HashMap<>();
        this.threshold = threshold;
    }

    public boolean lastSynchIsRecent(PeerID peerID) {
        return peerSharedSynchEvents.containsKey(peerID) &&
                peerSharedSynchEvents.get(peerID) > System.currentTimeMillis() - threshold;
    }

    public void newSynchWithPeer(PeerID peerID) {
        peerSharedSynchEvents.put(peerID, System.currentTimeMillis());
    }
}
