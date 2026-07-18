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
import com.monaim.studio.ui.SettingsActivity;
import com.monaim.studio.ui.UIHelper;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Button btnToggleOverlay, btnSettings, btnAccessibility, btnLoadScript;
    private TextView tvStatusOverlay, tvStatusAccessibility, tvHintOverlay, tvHintAccessibility;
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
                UIHelper.showConfirm(this, "Stop Overlay",
                        "Are you sure you want to stop the floating widget?",
                        "Stop", "Cancel", () -> {
                            stopFloatingService();
                            UIHelper.toastInfo(this, "Widget stopped");
                        });
            } else {
                tryStartOverlay();
            }
        });

        btnSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));

        btnAccessibility.setOnClickListener(v -> openAccessibilitySettings());

        btnLoadScript.setOnClickListener(v -> {
            if (!overlayRunning) {
                UIHelper.toastInfo(this, "Please start the overlay first.");
            } else if (!isAccessibilityEnabled()) {
                UIHelper.toastInfo(this, "Please enable Accessibility Service first.");
            } else {
                UIHelper.toastSuccess(this, "Ready! Use the floating widget.");
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                UIHelper.toastSuccess(this, "Permission granted! Starting widget...");
                startFloatingService();
            } else {
                UIHelper.toastError(this, "Overlay permission denied");
            }
        }
    }

    private void tryStartOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            UIHelper.showConfirm(this, "Overlay Permission",
                    "MO MACRO needs overlay permission to show the floating widget above games.",
                    "Grant", "Cancel",
                    () -> {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + getPackageName()));
                        startActivityForResult(intent, 1001);
                    });
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
        UIHelper.toastSuccess(this, "Widget started");
    }

    private void stopFloatingService() {
        stopService(new Intent(this, FloatingWidgetService.class));
        overlayRunning = false;
        updateAllStatuses();
    }

    private void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
        UIHelper.toastLong(this, "Find 'MO MACRO' in the list and turn it ON");
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
            btnToggleOverlay.setText("Stop Widget");
            tvHintOverlay.setText("✓ Widget is active");
            tvHintOverlay.setTextColor(0xFF4CAF50);
        } else {
            tvStatusOverlay.setText("● Stopped");
            tvStatusOverlay.setTextColor(0xFF888888);
            btnToggleOverlay.setText("Start Widget");
            tvHintOverlay.setText("Tap to start floating widget");
            tvHintOverlay.setTextColor(0xFF888888);
        }
    }

    private void updateAccessibilityStatus() {
        if (isAccessibilityEnabled()) {
            tvStatusAccessibility.setText("● Enabled");
            tvStatusAccessibility.setTextColor(0xFF4CAF50);
            btnAccessibility.setText("✓ Enabled");
            tvHintAccessibility.setText("Ready to perform actions");
            tvHintAccessibility.setTextColor(0xFF4CAF50);
        } else {
            tvStatusAccessibility.setText("● Not Enabled");
            tvStatusAccessibility.setTextColor(0xFFFF9800);
            btnAccessibility.setText("Enable Service");
            tvHintAccessibility.setText("Required. Tap to open settings.");
            tvHintAccessibility.setTextColor(0xFFFF9800);
        }
    }
}
