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
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.monaim.studio.overlay.FloatingWidgetManager;
import com.monaim.studio.ui.SettingsActivity;
import com.monaim.studio.ui.UIHelper;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Button btnToggleOverlay, btnSettings, btnAccessibility, btnAddDot;
    private TextView tvStatusOverlay, tvStatusAccessibility, tvHintOverlay, tvHintAccessibility;
    private boolean overlayRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnToggleOverlay = findViewById(R.id.btn_toggle_overlay);
        btnSettings = findViewById(R.id.btn_settings);
        btnAccessibility = findViewById(R.id.btn_accessibility);
        btnAddDot = findViewById(R.id.btn_add_dot);
        tvStatusOverlay = findViewById(R.id.tv_status_overlay);
        tvStatusAccessibility = findViewById(R.id.tv_status_accessibility);
        tvHintOverlay = findViewById(R.id.tv_hint_overlay);
        tvHintAccessibility = findViewById(R.id.tv_hint_accessibility);

        if (btnAddDot == null) {
            btnAddDot = new Button(this);
        }

        updateAllStatuses();

        btnToggleOverlay.setOnClickListener(v -> {
            if (overlayRunning) {
                UIHelper.showConfirm(this, "Stop Widgets",
                        "Stop all floating dots?", "Stop", "Cancel", () -> {
                            stopWidgetService();
                            UIHelper.toastInfo(this, "All widgets stopped");
                        });
            } else {
                tryStartOverlay();
            }
        });

        btnSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        btnAccessibility.setOnClickListener(v -> {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            UIHelper.toastLong(this, "Find 'MO MACRO' and turn it ON");
        });

        btnAddDot.setOnClickListener(v -> {
            if (overlayRunning) {
                sendBroadcast(new Intent("com.monaim.studio.ADD_DOT").setPackage(getPackageName()));
                UIHelper.toastSuccess(this, "New dot added");
            } else {
                UIHelper.toastInfo(this, "Start widgets first");
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
                startWidgetService();
            } else {
                UIHelper.toastError(this, "Overlay permission denied");
            }
        }
    }

    private void tryStartOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            UIHelper.showConfirm(this, "Overlay Permission",
                    "Required to show floating dots above games.", "Grant", "Cancel",
                    () -> startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName())), 1001));
        } else {
            startWidgetService();
        }
    }

    private void startWidgetService() {
        Intent s = new Intent(this, FloatingWidgetManager.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(s);
        else startService(s);
        overlayRunning = true;
        updateAllStatuses();
    }

    private void stopWidgetService() {
        stopService(new Intent(this, FloatingWidgetManager.class));
        overlayRunning = false;
        updateAllStatuses();
    }

    private boolean isAccessibilityEnabled() {
        AccessibilityManager am = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am == null) return false;
        List<AccessibilityServiceInfo> list = am.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        for (AccessibilityServiceInfo s : list) {
            if (s.getResolveInfo().serviceInfo.packageName.equals(getPackageName())) return true;
        }
        return false;
    }

    private void updateAllStatuses() {
        if (overlayRunning) {
            tvStatusOverlay.setText("● Running");
            tvStatusOverlay.setTextColor(0xFF4CAF50);
            btnToggleOverlay.setText("Stop Widgets");
            tvHintOverlay.setText("✓ Widgets active");
            tvHintOverlay.setTextColor(0xFF4CAF50);
        } else {
            tvStatusOverlay.setText("● Stopped");
            tvStatusOverlay.setTextColor(0xFF888888);
            btnToggleOverlay.setText("Start Widgets");
            tvHintOverlay.setText("Tap to start");
            tvHintOverlay.setTextColor(0xFF888888);
        }

        if (isAccessibilityEnabled()) {
            tvStatusAccessibility.setText("● Enabled");
            tvStatusAccessibility.setTextColor(0xFF4CAF50);
            btnAccessibility.setText("✓ Enabled");
            tvHintAccessibility.setText("Ready");
            tvHintAccessibility.setTextColor(0xFF4CAF50);
        } else {
            tvStatusAccessibility.setText("● Not Enabled");
            tvStatusAccessibility.setTextColor(0xFFFF9800);
            btnAccessibility.setText("Enable Service");
            tvHintAccessibility.setText("Required");
            tvHintAccessibility.setTextColor(0xFFFF9800);
        }
    }
}
