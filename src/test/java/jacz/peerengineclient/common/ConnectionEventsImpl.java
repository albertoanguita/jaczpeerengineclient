package jacz.peerengineclient.common;

import jacz.peerengineservice.client.connection.ConnectionEvents;
import jacz.peerengineservice.client.connection.ConnectionState;
import jacz.util.network.IP4Port;

/**
 * Connection events
 */
public class ConnectionEventsImpl implements ConnectionEvents {

    @Override
    public void localPortModified(ConnectionState state) {
        System.out.println("Local port modified: " + state.getLocalPort());
    }

    @Override
    public void externalPortModified(ConnectionState state) {
        System.out.println("External port modified: " + state.getExternalPort());
    }

    @Override
    public void initializingConnection(ConnectionState state) {
        System.out.println("Initializing connection");
    }

    @Override
    public void localAddressFetched(ConnectionState state) {
        System.out.println("Local address fetched. Local address: " + state.getLocalAddress() + ". State: " + state);
    }

    @Override
    public void couldNotFetchLocalAddress(ConnectionState state) {
        System.out.println("Could not fetch local address. State: " + state);
    }

    @Override
    public void tryingToFetchExternalAddress(ConnectionState state) {
        System.out.println("Trying to fetch external address. State: " + state);
    }

    @Override
    public void externalAddressFetched(ConnectionState state) {
        System.out.println("External address fetched. External address: " + state.getExternalAddress() + ". Has gateway: " + state.isHasGateway() + ". State: " + state);
    }

    @Override
    public void couldNotFetchExternalAddress(ConnectionState state) {
        System.out.println("Could not fetch external address. State: " + state);
    }

    @Override
    public void unrecognizedMessageFromServer(ConnectionState state) {
        System.out.println("Unrecognized message from server. State: " + state);
    }

    @Override
    public void tryingToConnectToServer(ConnectionState state) {
        System.out.println("Trying to connect to server. State: " + state);
    }

    @Override
    public void connectionToServerEstablished(ConnectionState state) {
        System.out.println("Connected to server. State: " + state);
    }

    @Override
    public void registrationRequired(ConnectionState state) {
        System.out.println("Registration with server required. State: " + state);
    }

    @Override
    public void localServerUnreachable(ConnectionState state) {
        System.out.println("Local server unreachable. State: " + state);
    }

    @Override
    public void unableToConnectToServer(ConnectionState state) {
        System.out.println("Unable to connect to server. State: " + state);
    }

    @Override
    public void tryingToOpenLocalServer(ConnectionState state) {
        System.out.println("Trying to open Local server. State: " + state);
    }

    @Override
    public void localServerOpen(ConnectionState state) {
        System.out.println("Local server open. State: " + state);
    }

    @Override
    public void couldNotOpenLocalServer(ConnectionState state) {
        System.out.println("Could not open local server. State: " + state);
    }

    @Override
    public void tryingToCloseLocalServer(ConnectionState state) {
        System.out.println("Trying to close local server. State: " + state);
    }

    @Override
    public void localServerClosed(ConnectionState state) {
        System.out.println("Local server closed. State: " + state);
    }

    @Override
    public void tryingToCreateNATRule(ConnectionState state) {
        System.out.println("Trying to create NAT rule. State: " + state);
    }

    @Override
    public void NATRuleCreated(ConnectionState state) {
        System.out.println("NAT rule created. State: " + state);
    }

    @Override
    public void couldNotFetchUPNPGateway(ConnectionState state) {
        System.out.println("Could not fetch UPNP gateway. State: " + state);
    }

    @Override
    public void errorCreatingNATRule(ConnectionState state) {
        System.out.println("Error creating NAT rule. State: " + state);
    }

    @Override
    public void tryingToDestroyNATRule(ConnectionState state) {
        System.out.println("Trying to destroy NAT rule. State: " + state);
    }

    @Override
    public void NATRuleDestroyed(ConnectionState state) {
        System.out.println("NAT rule destroyed. State: " + state);
    }

    @Override
    public void couldNotDestroyNATRule(ConnectionState state) {
        System.out.println("Could not destroy NAT rule. State: " + state);
    }

    @Override
    public void listeningConnectionsWithoutNATRule(ConnectionState state) {
        System.out.println("Listening connections without NAT rule. State: " + state);
    }

    @Override
    public void disconnectedFromServer(ConnectionState state) {
        System.out.println("Disconnected from server. State: " + state);
    }

    @Override
    public void failedToRefreshServerConnection(ConnectionState state) {
        System.out.println("Failed to refresh server connection. State: " + state);
    }

    @Override
    public void tryingToRegisterWithServer(ConnectionState state) {
        System.out.println("Trying to register with server. State: " + state);
    }

    @Override
    public void registrationSuccessful(ConnectionState state) {
        System.out.println("Registration with server successful. State: " + state);
    }

    @Override
    public void alreadyRegistered(ConnectionState state) {
        System.out.println("Already registered. State: " + state);
    }

    @Override
    public void peerCouldNotConnectToUs(Exception e, IP4Port ip4Port) {
        System.out.println("Peer failed to connect to us from " + ip4Port.toString() + ". " + e.getMessage());
    }

    @Override
    public void localServerError(ConnectionState state, Exception e) {
        System.out.println("Error in the peer connections listener. All connections closed. Error: " + e.getMessage());
    }
}
