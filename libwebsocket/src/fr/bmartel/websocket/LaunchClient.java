/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Bertrand Martel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package fr.bmartel.websocket;

import java.util.Scanner;

import fr.bmartel.protocol.websocket.client.IWebsocketClientChannel;
import fr.bmartel.protocol.websocket.client.IWebsocketClientEventListener;
import fr.bmartel.protocol.websocket.client.WebsocketClient;
import fr.bmartel.protocol.websocket.listeners.IClientEventListener;
import fr.bmartel.protocol.websocket.server.IWebsocketClient;
import fr.bmartel.protocol.websocket.server.WebsocketServer;

public class LaunchClient {

    /**
     * server hostname
     */
    private static String HOSTNAME = "morpheus.int.freaq.net";

    /**
     * server port
     */
    private static int PORT = 27005;

    public static void main(String[] args) {

        if (args[0] != null) {
            HOSTNAME = args[0];
        }
        if (args[1] != null) {
            PORT = Integer.parseInt(args[1]);
        }

        // new instance of client socket
        WebsocketClient clientSocket = new WebsocketClient(HOSTNAME, PORT);

        // set SSL encryption
        if (args[2] != null) {
            if (args[2].equals("ssl")) {
                clientSocket.setSsl(true);
            }
        }

        // add a client event listener to be notified for incoming http frames
        clientSocket.addClientSocketEventListener(new IWebsocketClientEventListener() {

            @Override
            public void onSocketConnected() {
                System.out.println("[CLIENT] Websocket client successfully connected");
            }

            @Override
            public void onSocketClosed() {
                System.out.println("[CLIENT] Websocket client disconnected");
            }

            @Override
            public void onIncomingMessageReceived(byte[] data, IWebsocketClientChannel channel) {
                System.out.println("[CLIENT] Received message from server : " + new String(data));
            }
        });

        clientSocket.connect();

        // you can choose here which command to send to the server
        Scanner scan = new Scanner(System.in);

        System.out.println("------------------------------------------------");
        System.out.println("Started Websocket chat with server " + HOSTNAME
                           + ":" + PORT);
        System.out.println("------------------------------------------------");
        System.out.println("List of chat command :");
        System.out.println("HELLO");
        System.out.println("GOODBYE");
        System.out.println("T_T");
        System.out.println("EXIT");
        System.out.println("------------------------------------------------");

        String command = "";

        while (!command.equals("EXIT")) {
            command = scan.nextLine();
            switch (command) {
            case "HELLO":
                clientSocket.writeMessage("HELLO");
                break;
            case "GOODBYE":
                clientSocket.writeMessage("GOODBYE");
                break;
            case "T_T":
                clientSocket.writeMessage("T_T");
                break;
            case "EXIT":
                break;
            default:
                System.out.println("Unknown command");
            }
        }
        System.out.println("Exiting Websocket chat ...");

        // socket will be closed and reading thread will die if it exists
        clientSocket.closeSocketJoinRead();

        // clean event listener list
        clientSocket.cleanEventListeners();
    }
}
