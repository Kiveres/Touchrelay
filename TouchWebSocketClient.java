package com.touchrelay.app.network;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class TouchWebSocketClient extends WebSocketClient {

    public interface ConnectionListener {
        void onConnected();
        void onDisconnected();
        void onError(String error);
    }

    private ConnectionListener listener;

    public TouchWebSocketClient(String serverUri, ConnectionListener listener) throws Exception {
        super(new URI(serverUri));
        this.listener = listener;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        if (listener != null) listener.onConnected();
    }

    @Override
    public void onMessage(String message) {}

    @Override
    public void onClose(int code, String reason, boolean remote) {
        if (listener != null) listener.onDisconnected();
    }

    @Override
    public void onError(Exception ex) {
        if (listener != null) listener.onError(ex.getMessage());
    }

    public void sendEvent(String json) {
        if (isOpen()) {
            send(json);
        }
    }
}
