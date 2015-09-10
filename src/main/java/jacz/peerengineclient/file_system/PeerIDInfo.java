package jacz.peerengineclient.file_system;

import jacz.peerengineservice.PeerID;

import java.util.Date;

/**
 * PeerID value and related information (creation date...)
 */
public class PeerIDInfo {

    public final PeerID peerID;

    public final int keySizeForPeerGeneration;

    public final Date creationDate;

    public PeerIDInfo(PeerID peerID, int keySizeForPeerGeneration) {
        this(peerID, keySizeForPeerGeneration, new Date());
    }

    public PeerIDInfo(PeerID peerID, int keySizeForPeerGeneration, Date creationDate) {
        this.peerID = peerID;
        this.keySizeForPeerGeneration = keySizeForPeerGeneration;
        this.creationDate = creationDate;
    }
}
