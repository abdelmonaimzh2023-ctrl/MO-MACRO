package com.monaim.studio.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;

public class MacroAccessibilityService extends AccessibilityService {

    private static MacroAccessibilityService instance;
    private Handler handler;
    private boolean isHolding = false;
    private float hx1, hy1, hx2, hy2;
    private long hdur;
    private BroadcastReceiver holdReceiver;
    private SharedPreferences prefs;

    public static MacroAccessibilityService getInstance() { return instance; }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() { stopHold(); }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        handler = new Handler(Looper.getMainLooper());
        prefs = getSharedPreferences("mo_macro_widget", MODE_PRIVATE);
        loadHoldCoords();
        registerHoldReceiver();
    }

    private void loadHoldCoords() {
        hx1 = prefs.getInt("hold_x1", 540);
        hy1 = prefs.getInt("hold_y1", 1200);
        hx2 = prefs.getInt("hold_x2", 540);
        hy2 = prefs.getInt("hold_y2", 600);
        hdur = prefs.getLong("hold_dur", 100L);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        stopHold();
        try { unregisterReceiver(holdReceiver); } catch (Exception ignored) {}
    }

    private void registerHoldReceiver() {
        holdReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                if ("com.monaim.studio.HOLD_START".equals(i.getAction())) {
                    loadHoldCoords();
                    startHoldSwipe();
                } else if ("com.monaim.studio.HOLD_STOP".equals(i.getAction())) {
                    stopHold();
                }
            }
        };
        IntentFilter f = new IntentFilter();
        f.addAction("com.monaim.studio.HOLD_START");
        f.addAction("com.monaim.studio.HOLD_STOP");
        registerReceiver(holdReceiver, f);
    }

    private void startHoldSwipe() {
        if (isHolding) return;
        isHolding = true;
        executeHoldLoop();
    }

    private void executeHoldLoop() {
        if (!isHolding) return;
        GestureDescription.Builder b = new GestureDescription.Builder();
        Path p = new Path();
        p.moveTo(hx1, hy1);
        p.lineTo(hx2, hy2);
        b.addStroke(new GestureDescription.StrokeDescription(p, 0, hdur));
        dispatchGesture(b.build(), new GestureResultCallback() {
            @Override public void onCompleted(GestureDescription gd) {
                if (isHolding) handler.postDelayed(() -> executeHoldLoop(), 5);
            }
            @Override public void onCancelled(GestureDescription gd) {
                if (isHolding) handler.postDelayed(() -> executeHoldLoop(), 5);
            }
        }, null);
    }

    private void stopHold() { isHolding = false; }

    public boolean isServiceRunning() { return instance != null; }
}
