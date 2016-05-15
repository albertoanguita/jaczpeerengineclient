package jacz.peerengineclient.affinity;

import jacz.database.VideoFile;
import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineservice.PeerId;
import jacz.util.date_time.TimedEventRecordSet;
import jacz.util.numeric.NumericUtil;
import org.apache.commons.collections4.CollectionUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Alberto on 10/05/2016.
 */
public class AffinityCalculator {

    /**
     * 30 minutes for affinity recalculation
     */
    private static final long TOO_RECENT_THRESHOLD = 1000L * 60L * 30L;

    private static final int MIN_AFFINITY = 0;

    private static final int MAX_AFFINITY = 25;

    private final PeerEngineClient peerEngineClient;

    private final TimedEventRecordSet<PeerId> affinityEvents;

    public AffinityCalculator(PeerEngineClient peerEngineClient) {
        this.peerEngineClient = peerEngineClient;
        affinityEvents = new TimedEventRecordSet<>(TOO_RECENT_THRESHOLD);
    }

    /**
     * Updates the affinity with a peer
     *
     * @param peerId the peer to update teh affinity with
     */
    public void updateAffinity(PeerId peerId) {
        if (!affinityEvents.lastEventIsRecent(peerId)) {
            newAffinityCalculationWithPeer(peerId);
        }
    }

    private void newAffinityCalculationWithPeer(PeerId peerID) {
        affinityEvents.newEvent(peerID);
        calculatePeerAffinity(peerID);
    }

    private void calculatePeerAffinity(PeerId peerID) {
        try {
            Set<String> ourFiles = peerEngineClient.getFileAPI().getAvailableHashes();
            String peerDB = peerEngineClient.getDatabases().getRemoteDB(peerID);
            List<VideoFile> peerVideoFiles = VideoFile.getVideoFiles(peerDB);
            Set<String> peerFiles = new HashSet<>();
            for (VideoFile videoFile : peerVideoFiles) {
                peerFiles.add(videoFile.getHash());
            }
            int commonFilesCount = CollectionUtils.intersection(ourFiles, peerFiles).size();
            float affinityFloat = 100f * (float) commonFilesCount / (float) ourFiles.size();
            int affinity = (int) affinityFloat;
            affinity = NumericUtil.limitInRange(affinity, MIN_AFFINITY, MAX_AFFINITY);
            peerEngineClient.updatePeerAffinity(peerID, affinity);
        } catch (IOException e) {
            // todo log
        }
    }
}
