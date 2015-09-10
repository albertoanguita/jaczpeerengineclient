package jacz.peerengineclient.file_system;

/**
 * Local network configuration
 */
public class NetworkConfig {

    /**
     * Port for listening to connections. 0 indicates a random port
     */
    public final int port;

    public NetworkConfig(int port) {
        this.port = port;
    }
}
