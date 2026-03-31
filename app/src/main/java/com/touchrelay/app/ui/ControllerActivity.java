package com.touchrelay.app.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.touchrelay.app.R;
import com.touchrelay.app.model.TouchEvent;
import com.touchrelay.app.network.TouchWebSocketClient;

import org.json.JSONException;
import org.json.JSONObject;

public class ControllerActivity extends AppCompatActivity {

    private TouchWebSocketClient wsClient;
    private TextView tvStatus;
    private View touchpad;
    private SharedPreferences prefs;
    private boolean connected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controller);

        prefs = getSharedPreferences("touchrelay", MODE_PRIVATE);

        tvStatus = findViewById(R.id.tv_status);
        touchpad = findViewById(R.id.touchpad);
        EditText etIp = findViewById(R.id.et_ip);
        Button btnConnect = findViewById(R.id.btn_connect);
        Button btnBack = findViewById(R.id.btn_back);
        Button btnHome = findViewById(R.id.btn_home);
        Button btnRecents = findViewById(R.id.btn_recents);
        Button btnDisconnect = findViewById(R.id.btn_disconnect);

        // Restore last IP
        String lastIp = prefs.getString("last_ip", "192.168.43.1");
        etIp.setText(lastIp);

        btnConnect.setOnClickListener(v -> {
            String ip = etIp.getText().toString().trim();
            if (ip.isEmpty()) {
                Toast.makeText(this, "Enter IP address", Toast.LENGTH_SHORT).show();
                return;
            }
            prefs.edit().putString("last_ip", ip).apply();
            connect(ip);
        });

        btnDisconnect.setOnClickListener(v -> disconnect());

        btnBack.setOnClickListener(v -> sendButton("back"));
        btnHome.setOnClickListener(v -> sendButton("home"));
        btnRecents.setOnClickListener(v -> sendButton("recents"));

        // Touchpad handles all touch events
        touchpad.setOnTouchListener((v, event) -> {
            if (!connected) return false;

            float nx = event.getX() / v.getWidth();
            float ny = event.getY() / v.getHeight();
            // Clamp to 0-1
            nx = Math.max(0, Math.min(1, nx));
            ny = Math.max(0, Math.min(1, ny));

            String action;
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    action = TouchEvent.ACTION_DOWN;
                    break;
                case MotionEvent.ACTION_MOVE:
                    action = TouchEvent.ACTION_MOVE;
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    action = TouchEvent.ACTION_UP;
                    break;
                default:
                    return true;
            }

            sendTouchEvent(action, nx, ny);
            return true;
        });
    }

    private void connect(String ip) {
        tvStatus.setText("Connecting...");
        try {
            wsClient = new TouchWebSocketClient("ws://" + ip + ":8080", new TouchWebSocketClient.ConnectionListener() {
                @Override
                public void onConnected() {
                    runOnUiThread(() -> {
                        connected = true;
                        tvStatus.setText("✓ Connected to " + ip);
                        tvStatus.setTextColor(0xFF4CAF50);
                    });
                }

                @Override
                public void onDisconnected() {
                    runOnUiThread(() -> {
                        connected = false;
                        tvStatus.setText("Disconnected");
                        tvStatus.setTextColor(0xFFFF5722);
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        tvStatus.setText("Error: " + error);
                        tvStatus.setTextColor(0xFFFF5722);
                    });
                }
            });
            wsClient.connect();
        } catch (Exception e) {
            tvStatus.setText("Failed: " + e.getMessage());
        }
    }

    private void disconnect() {
        if (wsClient != null) {
            wsClient.close();
        }
        connected = false;
        tvStatus.setText("Disconnected");
    }

    private void sendTouchEvent(String action, float x, float y) {
        try {
            JSONObject json = new JSONObject();
            json.put("action", action);
            json.put("x", x);
            json.put("y", y);
            json.put("pointerId", 0);
            wsClient.sendEvent(json.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void sendButton(String button) {
        if (!connected) return;
        try {
            JSONObject json = new JSONObject();
            json.put("action", TouchEvent.ACTION_BUTTON);
            json.put("button", button);
            wsClient.sendEvent(json.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        disconnect();
        super.onDestroy();
    }
}
