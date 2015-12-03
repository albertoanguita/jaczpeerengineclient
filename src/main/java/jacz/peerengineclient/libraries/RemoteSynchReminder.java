package jacz.peerengineclient.libraries;

/**
 * Periodically reminds the library manager to request synching with other peers
 * <p/>
 * The code periodically checks if we are synching with any peer. If for several checks in a row, we are not synching
 * (e.g. 1 minute) then the synch request to all peers is issued
 */
public class RemoteSynchReminder {
}
