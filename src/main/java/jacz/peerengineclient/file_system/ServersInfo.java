package jacz.peerengineclient.file_system;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Alberto
 * Date: 20/05/14
 * Time: 16:39
 * To change this template use File | Settings | File Templates.
 */
public class ServersInfo {

    public static final class ServerInfo {

        public final String ip;

        public final int port;

        public ServerInfo(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }
    }

    public final List<ServerInfo> servers;

    public ServersInfo(ServerInfo server) {
        servers = new ArrayList<>();
        servers.add(server);
    }

    public ServersInfo(List<ServerInfo> servers) {
        this.servers = servers;
    }
}
