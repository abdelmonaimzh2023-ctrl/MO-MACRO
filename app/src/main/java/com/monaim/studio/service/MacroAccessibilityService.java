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
    private float holdStartX = 540f, holdStartY = 1200f, holdEndX = 540f, holdEndY = 600f;
    private long holdDuration = 100L;
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
            public void onReceive(Context context, Intent intent) {
                if ("com.monaim.studio.HOLD_START".equals(intent.getAction())) {
                    startHoldSwipe();
                } else if ("com.monaim.studio.HOLD_STOP".equals(intent.getAction())) {
                    stopHold();
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.monaim.studio.HOLD_START");
        filter.addAction("com.monaim.studio.HOLD_STOP");
        registerReceiver(holdReceiver, filter);
    }

    public void setHoldCoordinates(float x1, float y1, float x2, float y2, long duration) {
        this.holdStartX = x1;
        this.holdStartY = y1;
        this.holdEndX = x2;
        this.holdEndY = y2;
        this.holdDuration = Math.max(duration, 10L);
    }

    private void startHoldSwipe() {
        if (isHolding) return;
        isHolding = true;
        executeHoldLoop();
    }

    private void executeHoldLoop() {
        if (!isHolding) return;
        GestureDescription.Builder builder = new GestureDescription.Builder();
        Path path = new Path();
        path.moveTo(holdStartX, holdStartY);
        path.lineTo(holdEndX, holdEndY);
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, holdDuration));
        dispatchGesture(builder.build(), new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gd) {
                if (isHolding) handler.postDelayed(() -> executeHoldLoop(), 10);
            }
            @Override
            public void onCancelled(GestureDescription gd) {
                if (isHolding) handler.postDelayed(() -> executeHoldLoop(), 10);
            }
        }, null);
    }

    private void stopHold() {
        isHolding = false;
    }

    public void performTap(float x, float y, long delay, Runnable callback) {
        GestureDescription.Builder builder = new GestureDescription.Builder();
        Path path = new Path();
        path.moveTo(x, y);
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 1));
        dispatchGesture(builder.build(), new GestureResultCallback() {
            @Override public void onCompleted(GestureDescription gd) {
                if (callback != null) handler.postDelayed(callback, delay);
            }
            @Override public void onCancelled(GestureDescription gd) {
                if (callback != null) handler.postDelayed(callback, delay);
            }
        }, null);
    }

    public void performSwipe(float x1, float y1, float x2, float y2, long duration, long delay, Runnable callback) {
        GestureDescription.Builder builder = new GestureDescription.Builder();
        Path path = new Path();
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, duration));
        dispatchGesture(builder.build(), new GestureResultCallback() {
            @Override public void onCompleted(GestureDescription gd) {
                if (callback != null) handler.postDelayed(callback, delay);
            }
            @Override public void onCancelled(GestureDescription gd) {
                if (callback != null) handler.postDelayed(callback, delay);
            }
        }, null);
    }
}
