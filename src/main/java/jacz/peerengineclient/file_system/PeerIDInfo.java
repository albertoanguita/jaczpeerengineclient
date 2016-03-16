package jacz.peerengineclient.file_system;

import jacz.peerengineservice.PeerId;

import java.util.Date;

/**
 * PeerId value and related information (creation date...)
 *
 * todo remove
 */
public class PeerIDInfo {

    public final PeerId peerID;

    public final int keySizeForPeerGeneration;

    public final Date creationDate;

    public PeerIDInfo(PeerId peerID, int keySizeForPeerGeneration) {
        this(peerID, keySizeForPeerGeneration, new Date());
    }

    public PeerIDInfo(PeerId peerID, int keySizeForPeerGeneration, Date creationDate) {
        this.peerID = peerID;
        this.keySizeForPeerGeneration = keySizeForPeerGeneration;
        this.creationDate = creationDate;
    }
}
