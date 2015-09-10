package jacz.peerengineclient.file_system;

import jacz.peerengineservice.PeerID;

import java.util.HashMap;
import java.util.Map;

/**
 * Personal data information
 */
public class PersonalData {

    public final String ownNick;

    public final Map<PeerID, String> peerNicks;

    public PersonalData(String ownNick) {
        this(ownNick, new HashMap<PeerID, String>());
    }

    public PersonalData(String ownNick, Map<PeerID, String> peerNicks) {
        this.ownNick = ownNick;
        this.peerNicks = peerNicks;
    }
}
