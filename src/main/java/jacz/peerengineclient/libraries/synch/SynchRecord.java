package jacz.peerengineclient.libraries.synch;

import jacz.peerengineservice.PeerID;

import java.util.HashMap;
import java.util.Map;

/**
 * This class registers the past synch processes (both for shared and remote), allowing us to know how long ago
 * a peer synched our shared library with us
 */
class SynchRecord {

    // events occurred more recently than this period of time are considered as recent (in ms)
    private static final long RECENTLY_THRESHOLD = 30000;

    /**
     * For each peer, stores the last time that we synched the shared library with him
     */
    private final Map<PeerID, Long> peerSharedSynchEvents;

    SynchRecord() {
        peerSharedSynchEvents = new HashMap<>();
    }

    boolean lastSynchIsRecent(PeerID peerID) {
        return peerSharedSynchEvents.containsKey(peerID) &&
                peerSharedSynchEvents.get(peerID) > System.currentTimeMillis() - RECENTLY_THRESHOLD;
    }

    void newSynchWithPeer(PeerID peerID) {
        peerSharedSynchEvents.put(peerID, System.currentTimeMillis());
    }
}
