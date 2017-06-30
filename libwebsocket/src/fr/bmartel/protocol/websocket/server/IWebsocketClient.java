package fr.bmartel.protocol.websocket.server;

import java.net.SocketAddress;

public interface IWebsocketClient {

    /**
     *  close websoclet client object
     *
     * @return
     * 		0 if success -1 if error
     */
    public int close();

    /**
     * Send a message to websocket client
     *
     * @param string
     * 		Message to be sent to client
     * @return
     *		0 if success -1 if error
     */
    public int sendMessage(String message);
    public int send(String message);

    /**
     * Returns the hostname (or IP address) of the client.
     *
     * @return
     *      IP/Hostname on success; "Socket is NULL" on error
     */
    public String getHostName();
    public String getHostAddress();

    /**
     * Returns the remote port of the client.
     *
     * @return
     *     Socket port number; 0 on error
     */
    public int getPort();

    /**
     * Returns the remote socket address object of the client.
     *
     * @return
     *      SocketAddress object.
     */
    public SocketAddress getRemoteSocketAddress();
}
