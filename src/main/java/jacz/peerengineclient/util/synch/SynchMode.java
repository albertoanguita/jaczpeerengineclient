package jacz.peerengineclient.util.synch;

/**
 * Created by Alberto on 22/12/2015.
 */
public enum SynchMode {
    SHARED,
    REMOTE;

    public boolean isShared() {
        return this == SHARED;
    }

    public boolean isRemote() {
        return this == REMOTE;
    }
}
