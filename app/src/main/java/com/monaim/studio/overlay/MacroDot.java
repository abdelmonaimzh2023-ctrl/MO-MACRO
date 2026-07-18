package com.monaim.studio.overlay;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

public class MacroDot extends View {

    private WindowManager windowManager;
    private WindowManager.LayoutParams params;
    private Paint dotPaint, ringPaint;
    private float cx, cy, dotR, ringR, ringThick;
    private int dotColor, ringColor, dotAlpha, ringAlpha;
    private String actionType = "hold_swipe";
    private int actionX1, actionY1, actionX2, actionY2;
    private long actionDuration = 100;
    private String dotId;

    private boolean isDragging = false, isHolding = false, holdTriggered = false;
    private float touchStartX, touchStartY;
    private int initialX, initialY;
    private long downTime;
    private static final long HOLD_MS = 120;

    private Runnable onHoldStartCallback, onHoldStopCallback;
    private Runnable onRequestDelete;

    public MacroDot(Context context, String id, SharedPreferences prefs) {
        super(context);
        this.dotId = id;
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ringPaint.setStyle(Paint.Style.STROKE);
        loadPrefs(prefs);
        setOnTouchListener(new DotTouchListener());
    }

    public void setCallbacks(Runnable holdStart, Runnable holdStop, Runnable delete) {
        this.onHoldStartCallback = holdStart;
        this.onHoldStopCallback = holdStop;
        this.onRequestDelete = delete;
    }

    public String getDotId() { return dotId; }
    public String getActionType() { return actionType; }
    public int getActionX1() { return actionX1; }
    public int getActionY1() { return actionY1; }
    public int getActionX2() { return actionX2; }
    public int getActionY2() { return actionY2; }
    public long getActionDuration() { return actionDuration; }

    // ========== LOAD / SAVE ==========
    public void loadPrefs(SharedPreferences p) {
        dotColor = p.getInt("dot_" + dotId + "_color", Color.RED);
        ringColor = p.getInt("dot_" + dotId + "_ring_color", Color.WHITE);
        dotAlpha = p.getInt("dot_" + dotId + "_alpha", 220);
        ringAlpha = p.getInt("dot_" + dotId + "_ring_alpha", 20);
        dotR = p.getFloat("dot_" + dotId + "_dot_radius", 18f);
        ringR = p.getFloat("dot_" + dotId + "_ring_radius", 40f);
        ringThick = p.getFloat("dot_" + dotId + "_ring_thick", 2f);
        actionType = p.getString("dot_" + dotId + "_action_type", "hold_swipe");
        actionX1 = p.getInt("dot_" + dotId + "_x1", 540);
        actionY1 = p.getInt("dot_" + dotId + "_y1", 1200);
        actionX2 = p.getInt("dot_" + dotId + "_x2", 540);
        actionY2 = p.getInt("dot_" + dotId + "_y2", 600);
        actionDuration = p.getLong("dot_" + dotId + "_duration", 100L);

        dotPaint.setColor(dotColor);
        dotPaint.setAlpha(dotAlpha);
        ringPaint.setColor(ringColor);
        ringPaint.setAlpha(ringAlpha);
        ringPaint.setStrokeWidth(ringThick);
        cx = ringR + 5;
        cy = ringR + 5;
        invalidate();
    }

    public void savePrefs(SharedPreferences.Editor e) {
        e.putInt("dot_" + dotId + "_color", dotColor);
        e.putInt("dot_" + dotId + "_ring_color", ringColor);
        e.putInt("dot_" + dotId + "_alpha", dotAlpha);
        e.putInt("dot_" + dotId + "_ring_alpha", ringAlpha);
        e.putFloat("dot_" + dotId + "_dot_radius", dotR);
        e.putFloat("dot_" + dotId + "_ring_radius", ringR);
        e.putFloat("dot_" + dotId + "_ring_thick", ringThick);
        e.putString("dot_" + dotId + "_action_type", actionType);
        e.putInt("dot_" + dotId + "_x1", actionX1);
        e.putInt("dot_" + dotId + "_y1", actionY1);
        e.putInt("dot_" + dotId + "_x2", actionX2);
        e.putInt("dot_" + dotId + "_y2", actionY2);
        e.putLong("dot_" + dotId + "_duration", actionDuration);
        e.putInt("dot_" + dotId + "_x", params != null ? params.x : 100);
        e.putInt("dot_" + dotId + "_y", params != null ? params.y : 300);
    }

    // ========== WINDOW MANAGEMENT ==========
    public void attach() {
        try {
            int size = (int)(ringR * 2 + 10);
            int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    : WindowManager.LayoutParams.TYPE_PHONE;

            int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                      | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                      | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH;

            params = new WindowManager.LayoutParams(size, size, type, flags, PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.TOP | Gravity.START;

            SharedPreferences p = getContext().getSharedPreferences("mo_macro_dots", Context.MODE_PRIVATE);
            params.x = p.getInt("dot_" + dotId + "_x", 100);
            params.y = p.getInt("dot_" + dotId + "_y", 300);

            windowManager.addView(this, params);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void detach() {
        try {
            if (getParent() != null) windowManager.removeView(this);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void refresh() {
        try {
            if (getParent() != null) {
                int size = (int)(ringR * 2 + 10);
                params.width = size;
                params.height = size;
                windowManager.updateViewLayout(this, params);
                cx = ringR + 5;
                cy = ringR + 5;
                invalidate();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ========== TOUCH ==========
    private class DotTouchListener implements OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            try {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        touchStartX = event.getRawX();
                        touchStartY = event.getRawY();
                        downTime = System.currentTimeMillis();
                        isDragging = false;
                        holdTriggered = false;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float dx = Math.abs(event.getRawX() - touchStartX);
                        float dy = Math.abs(event.getRawY() - touchStartY);
                        if (dx > 6 || dy > 6) {
                            isDragging = true;
                            params.x = initialX + (int)(event.getRawX() - touchStartX);
                            params.y = initialY + (int)(event.getRawY() - touchStartY);
                            try { windowManager.updateViewLayout(MacroDot.this, params); } catch (Exception ignored) {}
                            return true;
                        }
                        if (!isDragging && !holdTriggered && (System.currentTimeMillis() - downTime) >= HOLD_MS) {
                            holdTriggered = true;
                            onHoldStart();
                            return true;
                        }
                        return false;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (holdTriggered) onHoldStop();
                        isHolding = false;
                        holdTriggered = false;
                        if (!isDragging && (System.currentTimeMillis() - downTime) < 300) {
                            // نقر سريع = فتح/إغلاق اللوحة (يتحكم فيه المدير)
                        }
                        return false;
                }
            } catch (Exception e) { e.printStackTrace(); }
            return false;
        }
    }

    private void onHoldStart() {
        if (isHolding) return;
        isHolding = true;
        if (onHoldStartCallback != null) onHoldStartCallback.run();
    }

    private void onHoldStop() {
        if (onHoldStopCallback != null) onHoldStopCallback.run();
        isHolding = false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        try {
            if (cx > 0 && cy > 0) {
                canvas.drawCircle(cx, cy, ringR, ringPaint);
                canvas.drawCircle(cx, cy, dotR, dotPaint);
            }
        } catch (Exception ignored) {}
    }
}
