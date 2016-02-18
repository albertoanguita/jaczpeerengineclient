package jacz.peerengineclient.util.synch;

/**
 * Created by Alberto on 22/12/2015.
 */
public enum SynchMode {
    LOCAL,
    REMOTE;

    public boolean isShared() {
        return this == LOCAL;
    }

    public boolean isRemote() {
        return this == REMOTE;
    }
}
