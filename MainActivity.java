package com.touchrelay.app.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Button;
import android.widget.TextView;

import com.touchrelay.app.R;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("touchrelay", MODE_PRIVATE);

        Button btnController = findViewById(R.id.btn_controller);
        Button btnReceiver = findViewById(R.id.btn_receiver);
        TextView tvVersion = findViewById(R.id.tv_version);

        tvVersion.setText("TouchRelay v1.0");

        btnController.setOnClickListener(v -> {
            prefs.edit().putString("mode", "controller").apply();
            startActivity(new Intent(this, ControllerActivity.class));
        });

        btnReceiver.setOnClickListener(v -> {
            prefs.edit().putString("mode", "receiver").apply();
            startActivity(new Intent(this, ReceiverActivity.class));
        });

        // Auto-launch last mode
        String lastMode = prefs.getString("mode", null);
        if ("controller".equals(lastMode)) {
            startActivity(new Intent(this, ControllerActivity.class));
        } else if ("receiver".equals(lastMode)) {
            startActivity(new Intent(this, ReceiverActivity.class));
        }
    }
}
