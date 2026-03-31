package com.touchrelay.app.network;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;

public class TouchWebSocketServer extends WebSocketServer {

    public interface MessageListener {
        void onMessage(String message);
        void onClientConnected();
        void onClientDisconnected();
    }

    private MessageListener listener;

    public TouchWebSocketServer(int port, MessageListener listener) {
        super(new InetSocketAddress(port));
        this.listener = listener;
        setReuseAddr(true);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        if (listener != null) listener.onClientConnected();
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        if (listener != null) listener.onClientDisconnected();
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        if (listener != null) listener.onMessage(message);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {}
}
