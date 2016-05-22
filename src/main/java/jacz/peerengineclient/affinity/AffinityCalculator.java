package jacz.peerengineclient.affinity;

import jacz.database.VideoFile;
import jacz.peerengineclient.PeerEngineClient;
import jacz.peerengineservice.PeerId;
import jacz.util.date_time.TimedEventRecordSet;
import jacz.util.numeric.NumericUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class takes care of updating affinity values for other peers. Affinity values help establish better connections
 * (the peer engine uses this values to prioritize connections with peers holding higher affinities)
 * <p>
 * Affinities are calculated based on the amount of common files shared betwee us and other peers. The more common
 * files, the higher the affinity
 */
public class AffinityCalculator {

    private final static Logger logger = LoggerFactory.getLogger(AffinityCalculator.class);

    /**
     * 60 minutes for affinity recalculation
     */
    private static final long TOO_RECENT_THRESHOLD = 1000L * 60L * 60L;

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
            logger.info("Updated affinity with peer " + peerID.toString() + ". New affinity is " + affinity);
        } catch (IOException e) {
            logger.info("Could not calculate affinity for peer " + peerID.toString());
        }
    }
}
