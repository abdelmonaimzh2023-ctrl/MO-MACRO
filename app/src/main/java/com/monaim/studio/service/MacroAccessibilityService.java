package com.monaim.studio.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;

public class MacroAccessibilityService extends AccessibilityService {

    private static MacroAccessibilityService instance;
    private Handler handler;
    private boolean isHolding = false;
    private float hx1 = 540, hy1 = 1200, hx2 = 540, hy2 = 600;
    private long hdur = 100;
    private BroadcastReceiver holdReceiver;

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
        registerHoldReceiver();
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
                try {
                    if ("com.monaim.studio.HOLD_START".equals(i.getAction())) {
                        hx1 = i.getFloatExtra("x1", 540);
                        hy1 = i.getFloatExtra("y1", 1200);
                        hx2 = i.getFloatExtra("x2", 540);
                        hy2 = i.getFloatExtra("y2", 600);
                        hdur = i.getLongExtra("duration", 100);
                        startHoldSwipe();
                    } else if ("com.monaim.studio.HOLD_STOP".equals(i.getAction())) {
                        stopHold();
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
        };
        IntentFilter f = new IntentFilter();
        f.addAction("com.monaim.studio.HOLD_START");
        f.addAction("com.monaim.studio.HOLD_STOP");
        registerReceiver(holdReceiver, f);
    }

    private synchronized void startHoldSwipe() {
        if (isHolding) return;
        isHolding = true;
        executeHoldLoop();
    }

    private void executeHoldLoop() {
        if (!isHolding) return;
        try {
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
        } catch (Exception e) {
            if (isHolding) handler.postDelayed(() -> executeHoldLoop(), 10);
        }
    }

    private synchronized void stopHold() {
        isHolding = false;
    }

    public boolean isServiceRunning() { return instance != null; }

    // ========== PERFORM TAP ==========
    public void performTap(float x, float y, long delayAfter, Runnable callback) {
        try {
            GestureDescription.Builder b = new GestureDescription.Builder();
            Path p = new Path();
            p.moveTo(x, y);
            b.addStroke(new GestureDescription.StrokeDescription(p, 0, 1));
            dispatchGesture(b.build(), new GestureResultCallback() {
                @Override public void onCompleted(GestureDescription gd) {
                    if (callback != null) handler.postDelayed(callback, delayAfter);
                }
                @Override public void onCancelled(GestureDescription gd) {
                    if (callback != null) handler.postDelayed(callback, delayAfter);
                }
            }, null);
        } catch (Exception e) {
            if (callback != null) handler.postDelayed(callback, delayAfter);
        }
    }

    // ========== PERFORM SWIPE ==========
    public void performSwipe(float x1, float y1, float x2, float y2,
                             long duration, long delayAfter, Runnable callback) {
        try {
            GestureDescription.Builder b = new GestureDescription.Builder();
            Path p = new Path();
            p.moveTo(x1, y1);
            p.lineTo(x2, y2);
            b.addStroke(new GestureDescription.StrokeDescription(p, 0, duration));
            dispatchGesture(b.build(), new GestureResultCallback() {
                @Override public void onCompleted(GestureDescription gd) {
                    if (callback != null) handler.postDelayed(callback, delayAfter);
                }
                @Override public void onCancelled(GestureDescription gd) {
                    if (callback != null) handler.postDelayed(callback, delayAfter);
                }
            }, null);
        } catch (Exception e) {
            if (callback != null) handler.postDelayed(callback, delayAfter);
        }
    }
}
