package com.monaim.studio;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.monaim.studio.overlay.FloatingWidgetService;
import com.monaim.studio.ui.SettingsActivity;

public class MainActivity extends AppCompatActivity {

    private Button btnToggleOverlay;
    private Button btnSettings;
    private Button btnAccessibility;
    private TextView tvStatusOverlay;
    private TextView tvStatusAccessibility;
    private boolean overlayRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnToggleOverlay = findViewById(R.id.btn_toggle_overlay);
        btnSettings = findViewById(R.id.btn_settings);
        btnAccessibility = findViewById(R.id.btn_accessibility);
        tvStatusOverlay = findViewById(R.id.tv_status_overlay);
        tvStatusAccessibility = findViewById(R.id.tv_status_accessibility);

        updateOverlayStatus();
        updateAccessibilityStatus();

        btnToggleOverlay.setOnClickListener(v -> {
            if (overlayRunning) {
                stopFloatingService();
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                    requestOverlayPermission();
                } else {
                    startFloatingService();
                }
            }
        });

        btnSettings.setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
        });

        btnAccessibility.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
            Toast.makeText(this, "Find MO MACRO and enable it", Toast.LENGTH_LONG).show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateOverlayStatus();
        updateAccessibilityStatus();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                startFloatingService();
            } else {
                Toast.makeText(this, "Overlay permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void requestOverlayPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, 1001);
    }

    private void startFloatingService() {
        Intent service = new Intent(this, FloatingWidgetService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(service);
        } else {
            startService(service);
        }
        overlayRunning = true;
        updateOverlayStatus();
    }

    private void stopFloatingService() {
        Intent service = new Intent(this, FloatingWidgetService.class);
        stopService(service);
        overlayRunning = false;
        updateOverlayStatus();
    }

    private void updateOverlayStatus() {
        if (overlayRunning) {
            tvStatusOverlay.setText("● Running");
            tvStatusOverlay.setTextColor(0xFF4CAF50);
            btnToggleOverlay.setText("Stop Overlay");
        } else {
            tvStatusOverlay.setText("● Stopped");
            tvStatusOverlay.setTextColor(0xFFE94560);
            btnToggleOverlay.setText("Start Overlay");
        }
    }

    private void updateAccessibilityStatus() {
        // Basic check - will be enhanced later
        tvStatusAccessibility.setText("Check Settings → Accessibility → MO MACRO");
        tvStatusAccessibility.setTextColor(0xFFFFC107);
    }
}
