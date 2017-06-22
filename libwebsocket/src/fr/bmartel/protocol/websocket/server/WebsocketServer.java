package fr.bmartel.protocol.websocket.server;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import fr.bmartel.protocol.websocket.listeners.IClientEventListener;

/**
 * Server socket
 *
 * @author Bertrand Martel
 */
public class WebsocketServer implements IWebsocketServer, IClientEventListener {

    /** boolean loop control for server instance running */
    private volatile boolean running = true;

    /** define which port we use for connection */
    private int port = 8443;

    /** set ssl encryption or not */
    private boolean ssl = false;

    /**
     * keystore certificate type
     */
    private String keystoreDefaultType = "";

    /**
     * trustore certificate type
     */
    private String trustoreDefaultType = "";

    /**
     * keystore file path
     */
    private String keystoreFile = "";

    /**
     * trustore file path
     */
    private String trustoreFile = "";

    /**
     * ssl protocol used
     */
    private String sslProtocol = "";

    /**
     * keystore file password
     */
    private String keystorePassword = "";

    /**
     * trustore file password
     */
    private String trustorePassword = "";

    /** define server socket object */
    private ServerSocket serverSocket;

    private boolean isServerClosed = false;

    /**
     * server event listener
     */
    private ArrayList<IClientEventListener> serverEventListenerList = new ArrayList<IClientEventListener>();

    /**
     * Initialize server
     */
    public WebsocketServer(int port) {
        this.port = port;
    }

    /**
     * Thread for handling incoming connections to prevent blocking.
     */
    private class AcceptRunnable implements Runnable {
        IClientEventListener thisServer;

        public AcceptRunnable(IClientEventListener thisServer) { 
            this.thisServer = thisServer;
        }

        public void run() {
            try {
                while (running) {
                    Socket newSocketConnection = serverSocket.accept();
                    newSocketConnection.setKeepAlive(true);
                    ServerSocketChannel server = new ServerSocketChannel(newSocketConnection, thisServer);
                    Thread newSocket = new Thread(server);
                    newSocket.start();
                }
                serverSocket.close();
            } catch (SocketException e) {
                stop();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * main loop for web server running
     */
    public void start() {
        try {
            /* server will be running while running == true */
            running = true;

            if (ssl) {
                /* Read in the keystore. */
                KeyStore ks = KeyStore.getInstance(keystoreDefaultType);
                ks.load(new FileInputStream(keystoreFile), keystorePassword.toCharArray());

                /* initialize key manager factory with chosen algorithm */
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(ks, keystorePassword.toCharArray());

                /* initialize trust manager factory with chosen algorithm */
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(ks);

                /* get SSL context chosen algorithm and bind to the keystore */
                SSLContext ctx = SSLContext.getInstance(sslProtocol);
                ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

                /* Open the socket. */
                SSLServerSocketFactory sslserversocketfactory = ctx.getServerSocketFactory();
                serverSocket = sslserversocketfactory.createServerSocket(port);
            } else {
                serverSocket = new ServerSocket(port);
            }

            /*
             * server thread main loop : accept a new connect each time
             * requested by correct client
             */
            AcceptRunnable acceptRunnable = new AcceptRunnable(this);
            new Thread(acceptRunnable, "WebSocketServer").start();
        } catch (SocketException e) {
            // e.printStackTrace();
            /* stop all thread and server socket */
            stop();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (CertificateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (KeyManagementException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Set ssl parameters
     *
     * @param keystoreDefaultType
     *            keystore certificates type
     * @param keystoreFile
     *            keystore file path
     * @param sslProtocol
     *            ssl protocol used
     * @param keystorePassword
     *            keystore password
     */
    public void setSSLParams(String keystoreDefaultType, String keystoreFile, String sslProtocol, String keystorePassword) {
        this.keystoreDefaultType = keystoreDefaultType;
        this.keystoreFile = keystoreFile;
        this.sslProtocol = sslProtocol;
        this.keystorePassword = keystorePassword;
    }

    /**
     * Stop server socket and stop running thread
     */
    private void stop() {
        running = false;

        if (!isServerClosed) {

            isServerClosed = true;

            /* close socket connection */
            closeServerSocket();
            /* disable loop */
        }
    }

    /** Stop server socket */
    private void closeServerSocket() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void closeServer() {
        stop();
    }

    /**
     * remove client from list
     */
    @Override
    public void onClientClose(IWebsocketClient client) {
        for (int i = 0; i < serverEventListenerList.size(); i++) {
            serverEventListenerList.get(i).onClientClose(client);
        }
    }

    @Override
    public void onClientConnection(IWebsocketClient client) {
        for (int i = 0; i < serverEventListenerList.size(); i++) {
            serverEventListenerList.get(i).onClientConnection(client);
        }
    }

    @Override
    public void onMessageReceivedFromClient(IWebsocketClient client,
                                            String message) {
        for (int i = 0; i < serverEventListenerList.size(); i++) {
            serverEventListenerList.get(i).onMessageReceivedFromClient(client,
                    message);
        }
    }

    @Override
    public void addServerEventListener(IClientEventListener listener) {
        serverEventListenerList.add(listener);
    }

    public boolean isSsl() {
        return ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }
}
