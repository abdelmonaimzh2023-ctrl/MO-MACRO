package com.monaim.studio.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.PointF;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;

import java.util.ArrayList;
import java.util.List;

public class MacroAccessibilityService extends AccessibilityService {

    private static MacroAccessibilityService instance;
    private Handler handler;
    private boolean isRunning = false;
    private boolean isPaused = false;
    private List<MacroAction> actionList = new ArrayList<>();
    private int currentActionIndex = 0;

    public static MacroAccessibilityService getInstance() {
        return instance;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Not needed for gesture dispatch
    }

    @Override
    public void onInterrupt() {
        stopMacro();
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        stopMacro();
    }

    public boolean isServiceRunning() {
        return instance != null;
    }

    public void startMacro(List<MacroAction> actions) {
        if (actions == null || actions.isEmpty()) return;
        this.actionList = new ArrayList<>(actions);
        this.currentActionIndex = 0;
        this.isRunning = true;
        this.isPaused = false;
        executeNextAction();
    }

    public void pauseMacro() {
        isPaused = true;
    }

    public void resumeMacro() {
        if (isPaused && isRunning) {
            isPaused = false;
            executeNextAction();
        }
    }

    public void stopMacro() {
        isRunning = false;
        isPaused = false;
        actionList.clear();
        currentActionIndex = 0;
        handler.removeCallbacksAndMessages(null);
    }

    private void executeNextAction() {
        if (!isRunning || isPaused) return;
        if (currentActionIndex >= actionList.size()) {
            stopMacro();
            return;
        }

        MacroAction action = actionList.get(currentActionIndex);
        currentActionIndex++;

        long delay = action.getDelayAfter();

        switch (action.getType()) {
            case TAP:
                performTap(action.getX(), action.getY(), delay);
                break;
            case SWIPE:
                performSwipe(action.getX(), action.getY(), action.getEndX(), action.getEndY(),
                        action.getDuration(), delay);
                break;
            case DELAY:
                scheduleNext(delay);
                break;
            case REPEAT:
                if (action.getRepeatCount() > 0) {
                    action.decrementRepeat();
                    currentActionIndex = action.getRepeatStartIndex();
                    scheduleNext(50);
                } else {
                    scheduleNext(delay);
                }
                break;
        }
    }

    private void performTap(float x, float y, long delayAfter) {
        GestureDescription.Builder builder = new GestureDescription.Builder();
        Path path = new Path();
        path.moveTo(x, y);

        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 1));
        dispatchGesture(builder.build(), new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
                scheduleNext(delayAfter);
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
                scheduleNext(delayAfter);
            }
        }, null);
    }

    private void performSwipe(float startX, float startY, float endX, float endY,
                              long duration, long delayAfter) {
        GestureDescription.Builder builder = new GestureDescription.Builder();
        Path path = new Path();
        path.moveTo(startX, startY);
        path.lineTo(endX, endY);

        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, duration));
        dispatchGesture(builder.build(), new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
                scheduleNext(delayAfter);
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
                scheduleNext(delayAfter);
            }
        }, null);
    }

    private void scheduleNext(long delayMs) {
        handler.postDelayed(this::executeNextAction, Math.max(delayMs, 1));
    }

    public static class MacroAction {
        public enum Type { TAP, SWIPE, DELAY, REPEAT }

        private Type type;
        private float x, y, endX, endY;
        private long duration;
        private long delayAfter;
        private int repeatCount;
        private int repeatStartIndex;

        public MacroAction(Type type) {
            this.type = type;
        }

        public Type getType() { return type; }
        public float getX() { return x; }
        public float getY() { return y; }
        public float getEndX() { return endX; }
        public float getEndY() { return endY; }
        public long getDuration() { return duration; }
        public long getDelayAfter() { return delayAfter; }
        public int getRepeatCount() { return repeatCount; }
        public int getRepeatStartIndex() { return repeatStartIndex; }

        public void decrementRepeat() { if (repeatCount > 0) repeatCount--; }

        // Builder Pattern
        public MacroAction setPosition(float x, float y) { this.x = x; this.y = y; return this; }
        public MacroAction setEndPosition(float x, float y) { this.endX = x; this.endY = y; return this; }
        public MacroAction setDuration(long ms) { this.duration = ms; return this; }
        public MacroAction setDelayAfter(long ms) { this.delayAfter = ms; return this; }
        public MacroAction setRepeat(int count, int startIndex) {
            this.repeatCount = count; this.repeatStartIndex = startIndex; return this;
        }

        public static MacroAction tap(float x, float y, long delayAfter) {
            return new MacroAction(Type.TAP).setPosition(x, y).setDelayAfter(delayAfter);
        }

        public static MacroAction swipe(float x1, float y1, float x2, float y2,
                                        long duration, long delayAfter) {
            return new MacroAction(Type.SWIPE)
                    .setPosition(x1, y1).setEndPosition(x2, y2)
                    .setDuration(duration).setDelayAfter(delayAfter);
        }

        public static MacroAction delay(long ms) {
            return new MacroAction(Type.DELAY).setDelayAfter(ms);
        }

        public static MacroAction repeat(int count, int startIndex) {
            return new MacroAction(Type.REPEAT).setRepeat(count, startIndex);
        }
    }
}
