package com.luckyframework.httpclient.core.websocket;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;

public class WebSocketTest {


    public static void main(String[] args) throws URISyntaxException {
        URI wsURI = new URI("ws://");
        WebSocketClient client = new WebSocketClient(wsURI) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {

            }

            @Override
            public void onMessage(String s) {

            }

            @Override
            public void onClose(int i, String s, boolean b) {

            }

            @Override
            public void onError(Exception e) {

            }
        };

        client.connect();
    }
}
