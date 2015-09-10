package jacz.peerengineclient;

import jacz.peerengineservice.PeerID;
import jacz.util.network.IP4Port;

/**
 * This class stores different information about other peers (nick, amount of data sent/received).
 * <p/>
 * It is meant to be stored/retrieved from a persistence layer
 * <p/>
 * todo finish
 */
public class RemotePeerData {

    /**
     * Personal data
     */

    /**
     * The id of this friend peer
     */
    private final PeerID peerID;

    /**
     * Local nickname assigned to this peer (a user can assign local nicknames to peers with equal nicks in order
     * to differentiate them)
     */
    private String localNick;

    /**
     * Current ip and port of this peer (or last known value)
     */
    private IP4Port ipPort;

    /**
     * Whether this peer has its port open (or last known value)
     */
    private boolean openPort;

    /**
     * Total amount of bytes sent to this peer
     */
    private long bytesSent;

    /**
     * Total amount of bytes received from this peer
     */
    private long bytesReceived;

    // todo publickey, priviledges, validated or not, bytes en esta sesion, total bytes, date added, % of time connected, avg down speed, % requests accepted


    public RemotePeerData(PeerID peerID, String nick, String message, String state) {
        this.peerID = peerID;
    }

    public PeerID getPeerID() {
        return peerID;
    }

}
