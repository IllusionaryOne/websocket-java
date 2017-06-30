package fr.bmartel.protocol.websocket.client;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Random;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

import fr.bmartel.protocol.http.HttpFrame;
import fr.bmartel.protocol.http.states.HttpStates;
import fr.bmartel.protocol.websocket.WebSocketChannel;
import fr.bmartel.protocol.websocket.WebSocketHandshake;

public class WebsocketClient implements IWebsocketClientChannel {
    /**
     * socket server hostname
     */
    private String hostname = "";

    /**
     * socket server port
     */
    private int port = 0;

    /** set ssl encryption or not */
    private boolean ssl = false;

    private static String websocketResponseExpected = "";

    /**
     * define socket timeout (-1 if no timeout defined)
     */
    private int socketTimeout = -1;

    private WebSocketChannel websocketChannel = new WebSocketChannel();

    /**
     * socket object
     */
    private Socket socket = null;

    /** client event listener list */
    private ArrayList<IWebsocketClientEventListener> clientListenerList = new ArrayList<IWebsocketClientEventListener>();

    /**
     * thread used to read http inputstream data
     */
    private Thread readingThread = null;

    private volatile boolean websocket = false;

    /**
     * Build Client socket
     *
     * @param hostname
     * @param port
     */
    public WebsocketClient(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    /**
     * Create and connect socket
     *
     * @return
     * @throws IOException
     */
    @Override
    public void connect() {

        // close socket before recreating it
        if (socket != null) {
            closeSocket();
        }
        try {

            if (ssl) {

                TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[] { };
                        }

                        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException { }
                        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException { }
                    }
                };

                SSLContext ctx = SSLContext.getInstance("TLS");
                ctx.init(null, null, null);
                SSLSocketFactory sslserversocketfactory = ctx.getSocketFactory();

                /* create a SSL socket connection */
                socket = new Socket();
                socket = sslserversocketfactory.createSocket();
            } else {
                /* create a basic socket connection */
                socket = new Socket();
            }

            /* establish socket parameters */
            socket.setReuseAddress(true);

            socket.setKeepAlive(true);

            if (socketTimeout != -1) {
                socket.setSoTimeout(socketTimeout);
            }

            socket.connect(new InetSocketAddress(hostname, port));

            if (readingThread != null) {
                websocket = false;
                readingThread.join();
            }

            websocket = false;
            readingThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    do {
                        if (!websocket) {
                            try {
                                HttpFrame frame = new HttpFrame();

                                HttpStates httpStates = frame.parseHttp(socket.getInputStream());

                                // check handshake response from websocket
                                // server
                                if (WebSocketHandshake.isValidHandshakeResponse(frame, httpStates, websocketResponseExpected)) {
                                    websocket = true;

                                    for (int i = 0; i < clientListenerList.size(); i++) {
                                        clientListenerList.get(i).onSocketConnected();
                                    }
                                } else {
                                    websocket = false;
                                    closeSocket();
                                }
                            } catch (SocketException e) {
                                e.printStackTrace();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            // here we can read data coming from websocket
                            // server

                            try {
                                /* read something on websocket stream */
                                byte[] data = websocketChannel.decapsulateMessage(socket.getInputStream());

                                if (data == null) {
                                    closeSocket();
                                    websocket = false;
                                } else {
                                    // incoming data message received
                                    for (int i = 0; i < clientListenerList.size(); i++) {
                                        clientListenerList.get(i).onIncomingMessageReceived(data, WebsocketClient.this);
                                    }
                                }
                            } catch (Exception e) {
                                closeSocket();
                                websocket = false;
                            }
                        }
                    } while (websocket == true);

                    // socket is closed
                    for (int i = 0; i < clientListenerList.size(); i++) {
                        clientListenerList.get(i).onSocketClosed();
                    }

                }
            });
            readingThread.start();

            String websocketKey = new BigInteger(130, new Random(42424242)).toString(32);

            websocketResponseExpected = WebSocketHandshake.retrieveWebsocketAccept(websocketKey);

            write(WebSocketHandshake.buildWebsocketHandshakeRequest(websocketKey).getBytes("UTF-8"));

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (KeyManagementException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Set timeout for this socket
     *
     * @param socketTimeout
     */
    @Override
    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    private int write(final byte[] data) {
        try {
            synchronized (socket.getOutputStream()) {
                socket.getOutputStream().write(data);
                socket.getOutputStream().flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        return 0;
    }

    @Override
    public int writeMessage(String message) {
        try {
            this.websocketChannel.encapsulateMessage(message, socket.getOutputStream());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return -1;
        }
        return 0;
    }

    @Override
    public void closeSocket() {
        if (socket != null) {
            try {
                socket.getOutputStream().close();
                socket.getInputStream().close();
                socket.close();
            } catch (IOException e) {
            }
        }
        socket = null;
    }

    @Override
    public void closeSocketJoinRead() {
        closeSocket();
        if (readingThread != null) {
            websocket = false;
            try {
                readingThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean isConnected() {
        if (socket != null && socket.isConnected())
            return true;
        return false;
    }

    @Override
    public void addClientSocketEventListener(
        IWebsocketClientEventListener eventListener) {
        clientListenerList.add(eventListener);
    }

    @Override
    public void cleanEventListeners() {
        clientListenerList.clear();
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }
}
