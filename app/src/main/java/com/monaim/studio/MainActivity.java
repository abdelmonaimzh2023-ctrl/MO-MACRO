package com.monaim.studio;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.monaim.studio.overlay.FloatingWidgetService;
import com.monaim.studio.service.MacroAccessibilityService;
import com.monaim.studio.ui.SettingsActivity;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Button btnToggleOverlay;
    private Button btnSettings;
    private Button btnAccessibility;
    private Button btnLoadScript;
    private TextView tvStatusOverlay;
    private TextView tvStatusAccessibility;
    private TextView tvHintOverlay;
    private TextView tvHintAccessibility;
    private boolean overlayRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnToggleOverlay = findViewById(R.id.btn_toggle_overlay);
        btnSettings = findViewById(R.id.btn_settings);
        btnAccessibility = findViewById(R.id.btn_accessibility);
        btnLoadScript = findViewById(R.id.btn_load_script);
        tvStatusOverlay = findViewById(R.id.tv_status_overlay);
        tvStatusAccessibility = findViewById(R.id.tv_status_accessibility);
        tvHintOverlay = findViewById(R.id.tv_hint_overlay);
        tvHintAccessibility = findViewById(R.id.tv_hint_accessibility);

        updateAllStatuses();

        btnToggleOverlay.setOnClickListener(v -> {
            if (overlayRunning) {
                stopFloatingService();
            } else {
                tryStartOverlay();
            }
        });

        btnSettings.setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
        });

        btnAccessibility.setOnClickListener(v -> {
            openAccessibilitySettings();
        });

        btnLoadScript.setOnClickListener(v -> {
            if (overlayRunning && isAccessibilityEnabled()) {
                Toast.makeText(this, "Ready! Use floating widget to control macros.",
                        Toast.LENGTH_LONG).show();
            } else if (!overlayRunning) {
                Toast.makeText(this, "Please start the overlay first.",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Please enable Accessibility Service first.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAllStatuses();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001) {
            // عاد من شاشة صلاحية التراكب - حاول التشغيل مباشرة
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                startFloatingService();
            } else {
                Toast.makeText(this, "Overlay permission required to show floating widget",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void tryStartOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            // لا توجد صلاحية - اطلبها بدلاً من إغلاق التطبيق
            tvHintOverlay.setText("ⓘ Overlay permission needed. Tap to grant.");
            tvHintOverlay.setTextColor(0xFFFF9800);
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 1001);
        } else {
            startFloatingService();
        }
    }

    private void startFloatingService() {
        Intent service = new Intent(this, FloatingWidgetService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(service);
        } else {
            startService(service);
        }
        overlayRunning = true;
        updateAllStatuses();
        Toast.makeText(this, "MO MACRO overlay started", Toast.LENGTH_SHORT).show();
    }

    private void stopFloatingService() {
        Intent service = new Intent(this, FloatingWidgetService.class);
        stopService(service);
        overlayRunning = false;
        updateAllStatuses();
    }

    private void openAccessibilitySettings() {
        // يفتح إعدادات Accessibility مباشرة - لا يغلق التطبيق أبداً
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
        Toast.makeText(this, "Find 'MO MACRO' in the list and turn it ON",
                Toast.LENGTH_LONG).show();
    }

    private boolean isAccessibilityEnabled() {
        AccessibilityManager am = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am == null) return false;
        List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        for (AccessibilityServiceInfo service : enabledServices) {
            if (service.getResolveInfo().serviceInfo.packageName.equals(getPackageName())) {
                return true;
            }
        }
        return false;
    }

    private void updateAllStatuses() {
        updateOverlayStatus();
        updateAccessibilityStatus();
    }

    private void updateOverlayStatus() {
        if (overlayRunning) {
            tvStatusOverlay.setText("● Running");
            tvStatusOverlay.setTextColor(0xFF4CAF50);
            btnToggleOverlay.setText("Stop Overlay");
            btnToggleOverlay.setBackgroundColor(0xFFE94560);
            tvHintOverlay.setText("✓ Overlay is active");
            tvHintOverlay.setTextColor(0xFF4CAF50);
        } else {
            tvStatusOverlay.setText("● Stopped");
            tvStatusOverlay.setTextColor(0xFF888888);
            btnToggleOverlay.setText("Start Overlay");
            btnToggleOverlay.setBackgroundColor(0xFF4CAF50);
            tvHintOverlay.setText("Start overlay to show floating widget");
            tvHintOverlay.setTextColor(0xFF888888);
        }
    }

    private void updateAccessibilityStatus() {
        if (isAccessibilityEnabled()) {
            tvStatusAccessibility.setText("● Enabled");
            tvStatusAccessibility.setTextColor(0xFF4CAF50);
            btnAccessibility.setText("Accessibility ✓");
            btnAccessibility.setBackgroundColor(0xFF4CAF50);
            tvHintAccessibility.setText("✓ Ready to perform taps and swipes");
            tvHintAccessibility.setTextColor(0xFF4CAF50);
        } else {
            tvStatusAccessibility.setText("● Not Enabled");
            tvStatusAccessibility.setTextColor(0xFFFF9800);
            btnAccessibility.setText("Enable Accessibility");
            btnAccessibility.setBackgroundColor(0xFFFF9800);
            tvHintAccessibility.setText("Required for macro to work. Tap to enable.");
            tvHintAccessibility.setTextColor(0xFFFF9800);
        }
    }
}
