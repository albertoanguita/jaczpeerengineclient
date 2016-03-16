package jacz.peerengineclient.util.synch;

import jacz.peerengineservice.PeerId;
import jacz.util.date_time.TimedEventRecord;

import java.util.HashMap;
import java.util.Map;

/**
 * This class registers the past synch processes (both for shared and remote), allowing us to know how long ago
 * a peer synched our shared library with us
 */
public class SynchRecord {

    /**
     * For each peer, stores the last time that we synched the shared library with him
     */
    private final Map<PeerId, TimedEventRecord> peerSharedSynchEvents;

    /**
     * events occurred more recently than this period of time are considered as recent (in ms)
     */
    private final long threshold;

    public SynchRecord(long threshold) {
        peerSharedSynchEvents = new HashMap<>();
        this.threshold = threshold;
    }

    public boolean lastSynchIsRecent(PeerId peerID) {
        return peerSharedSynchEvents.containsKey(peerID) && peerSharedSynchEvents.get(peerID).lastEventIsRecent();
    }

    public void newSynchWithPeer(PeerId peerID) {
        if (!peerSharedSynchEvents.containsKey(peerID)) {
            peerSharedSynchEvents.put(peerID, new TimedEventRecord(threshold, true));
        } else {
            peerSharedSynchEvents.get(peerID).newEvent();
        }
    }




//
//    /**
//     * For each peer, stores the last time that we synched the shared library with him
//     */
//    private final Map<PeerId, Long> peerSharedSynchEvents;
//
//    /**
//     * events occurred more recently than this period of time are considered as recent (in ms)
//     */
//    private final long threshold;
//
//    public SynchRecord(long threshold) {
//        peerSharedSynchEvents = new HashMap<>();
//        this.threshold = threshold;
//    }
//
//    public boolean lastSynchIsRecent(PeerId peerID) {
//        return peerSharedSynchEvents.containsKey(peerID) &&
//                peerSharedSynchEvents.get(peerID) > System.currentTimeMillis() - threshold;
//    }
//
//    public void newSynchWithPeer(PeerId peerID) {
//        peerSharedSynchEvents.put(peerID, System.currentTimeMillis());
//    }
}
