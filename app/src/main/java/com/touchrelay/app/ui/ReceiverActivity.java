package com.touchrelay.app.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.touchrelay.app.network.TouchWebSocketServer;
import com.touchrelay.app.service.GestureAccessibilityService;
import com.touchrelay.app.service.OverlayService;
import com.touchrelay.app.model.TouchEvent;
import com.touchrelay.app.R;

import org.json.JSONObject;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class ReceiverActivity extends AppCompatActivity {

    private TouchWebSocketServer server;
    private TextView tvStatus, tvIp, tvLog;
    private Button btnStart, btnStop;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receiver);

        prefs = getSharedPreferences("touchrelay", MODE_PRIVATE);

        tvStatus = findViewById(R.id.tv_status);
        tvIp = findViewById(R.id.tv_ip);
        tvLog = findViewById(R.id.tv_log);
        btnStart = findViewById(R.id.btn_start);
        btnStop = findViewById(R.id.btn_stop);
        Button btnAccessibility = findViewById(R.id.btn_accessibility);
        Button btnOverlay = findViewById(R.id.btn_overlay);

        tvIp.setText("IP: " + getLocalIpAddress());

        btnAccessibility.setOnClickListener(v -> {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        });

        btnOverlay.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())));
            } else {
                Toast.makeText(this, "Overlay permission already granted", Toast.LENGTH_SHORT).show();
            }
        });

        btnStart.setOnClickListener(v -> startServer());
        btnStop.setOnClickListener(v -> stopServer());
    }

    private void startServer() {
        // Check accessibility service
        if (GestureAccessibilityService.instance == null) {
            tvStatus.setText("⚠ Enable Accessibility Service first!");
            tvStatus.setTextColor(0xFFFF5722);
            return;
        }

        // Check overlay permission
        if (!Settings.canDrawOverlays(this)) {
            tvStatus.setText("⚠ Grant overlay permission first!");
            tvStatus.setTextColor(0xFFFF5722);
            return;
        }

        // Start overlay service
        startService(new Intent(this, OverlayService.class));

        // Start WebSocket server
        server = new TouchWebSocketServer(8080, new TouchWebSocketServer.MessageListener() {
            @Override
            public void onMessage(String message) {
                try {
                    JSONObject json = new JSONObject(message);
                    TouchEvent event = new TouchEvent();
                    event.action = json.getString("action");

                    if (TouchEvent.ACTION_BUTTON.equals(event.action)) {
                        event.button = json.getString("button");
                    } else {
                        event.x = (float) json.getDouble("x");
                        event.y = (float) json.getDouble("y");
                        event.pointerId = json.optInt("pointerId", 0);
                    }

                    if (GestureAccessibilityService.instance != null) {
                        GestureAccessibilityService.instance.handleTouchEvent(event);
                    }

                    runOnUiThread(() -> tvLog.setText("← " + event.action +
                            (event.x > 0 ? String.format(" (%.2f, %.2f)", event.x, event.y) : " " + event.button)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClientConnected() {
                runOnUiThread(() -> {
                    tvStatus.setText("✓ Controller connected!");
                    tvStatus.setTextColor(0xFF4CAF50);
                });
            }

            @Override
            public void onClientDisconnected() {
                runOnUiThread(() -> {
                    tvStatus.setText("Waiting for controller...");
                    tvStatus.setTextColor(0xFFFF9800);
                });
            }
        });

        try {
            server.start();
            tvStatus.setText("Waiting for controller...");
            tvStatus.setTextColor(0xFFFF9800);
            btnStart.setEnabled(false);
            btnStop.setEnabled(true);
        } catch (Exception e) {
            tvStatus.setText("Failed to start: " + e.getMessage());
        }
    }

    private void stopServer() {
        if (server != null) {
            try { server.stop(); } catch (Exception e) { e.printStackTrace(); }
            server = null;
        }
        stopService(new Intent(this, OverlayService.class));
        tvStatus.setText("Stopped");
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
    }

    private String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (!addr.isLoopbackAddress() && addr.getHostAddress().contains(".")) {
                        String ip = addr.getHostAddress();
                        if (ip.startsWith("192.168") || ip.startsWith("10.")) {
                            return ip;
                        }
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return "unknown";
    }

    @Override
    protected void onDestroy() {
        stopServer();
        super.onDestroy();
    }
}
