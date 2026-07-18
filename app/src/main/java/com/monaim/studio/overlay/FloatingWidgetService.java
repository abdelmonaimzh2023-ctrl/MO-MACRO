package com.monaim.studio.overlay;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

public class FloatingWidgetService extends Service {

    private WindowManager windowManager;
    private FloatingDotView floatingDotView;
    private WindowManager.LayoutParams dotParams;
    private boolean isAttached = false;
    private Handler mainHandler;
    private SharedPreferences prefs;

    private boolean isHolding = false;
    private long downTime;
    private static final long HOLD_THRESHOLD_MS = 120;
    private boolean holdTriggered = false;
    private boolean isDragging = false;
    private float initialTouchX, initialTouchY;
    private int initialX, initialY;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mainHandler = new Handler(Looper.getMainLooper());
        prefs = getSharedPreferences("mo_macro_widget", MODE_PRIVATE);
        loadPrefs();
        createFloatingDot();
    }

    private void loadPrefs() {
        // ستُستخدم في FloatingDotView
    }

    private void createFloatingDot() {
        floatingDotView = new FloatingDotView(this);
        floatingDotView.updateDrawing(prefs);

        int layoutType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        // === التعديل السحري هنا ===
        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                  | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                  | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
                  | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;

        dotParams = new WindowManager.LayoutParams(
                (int) (prefs.getFloat("ring_radius", 50f) * 2 + 10),
                (int) (prefs.getFloat("ring_radius", 50f) * 2 + 10),
                layoutType,
                flags,
                PixelFormat.TRANSLUCENT
        );
        dotParams.gravity = Gravity.TOP | Gravity.START;
        dotParams.x = prefs.getInt("widget_x", 50);
        dotParams.y = prefs.getInt("widget_y", 500);

        floatingDotView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = dotParams.x;
                        initialY = dotParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        downTime = System.currentTimeMillis();
                        holdTriggered = false;
                        isDragging = false;
                        // لا نرجع true فوراً لنسمح بمرور اللمس
                        return false;

                    case MotionEvent.ACTION_MOVE:
                        float dx = Math.abs(event.getRawX() - initialTouchX);
                        float dy = Math.abs(event.getRawY() - initialTouchY);

                        if (dx > 8 || dy > 8) {
                            isDragging = true;
                            dotParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                            dotParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                            try {
                                windowManager.updateViewLayout(floatingDotView, dotParams);
                            } catch (Exception ignored) {}
                            return true;
                        }

                        if (!isDragging && !holdTriggered &&
                                (System.currentTimeMillis() - downTime) >= HOLD_THRESHOLD_MS) {
                            holdTriggered = true;
                            onHoldStart();
                            return true;
                        }
                        return false;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (holdTriggered) {
                            onHoldStop();
                        }
                        isHolding = false;
                        isDragging = false;
                        holdTriggered = false;
                        return false;
                }
                return false;
            }
        });

        try {
            windowManager.addView(floatingDotView, dotParams);
            isAttached = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void onHoldStart() {
        if (isHolding) return;
        isHolding = true;
        sendBroadcast(new Intent("com.monaim.studio.HOLD_START"));
    }

    private void onHoldStop() {
        sendBroadcast(new Intent("com.monaim.studio.HOLD_STOP"));
        isHolding = false;
    }

    public void updateAppearance() {
        if (floatingDotView != null && isAttached) {
            floatingDotView.updateDrawing(prefs);
            dotParams.width = (int) (prefs.getFloat("ring_radius", 50f) * 2 + 10);
            dotParams.height = (int) (prefs.getFloat("ring_radius", 50f) * 2 + 10);
            try {
                windowManager.updateViewLayout(floatingDotView, dotParams);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isAttached && floatingDotView != null) {
            try { windowManager.removeView(floatingDotView); } catch (Exception ignored) {}
        }
        isAttached = false;
        if (dotParams != null) {
            prefs.edit().putInt("widget_x", dotParams.x).putInt("widget_y", dotParams.y).apply();
        }
    }

    public static class FloatingDotView extends View {
        private Paint dotPaint, ringPaint;
        private float cx, cy, dotR, ringR;

        public FloatingDotView(android.content.Context context) {
            super(context);
            dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            ringPaint.setStyle(Paint.Style.STROKE);
        }

        void updateDrawing(SharedPreferences p) {
            dotPaint.setColor(p.getInt("dot_color", Color.RED));
            dotPaint.setAlpha(p.getInt("dot_alpha", 220));
            ringPaint.setColor(p.getInt("ring_color", Color.WHITE));
            ringPaint.setAlpha(p.getInt("ring_alpha", 20));
            ringPaint.setStrokeWidth(p.getFloat("ring_thickness", 2f));
            ringR = p.getFloat("ring_radius", 50f);
            dotR = p.getFloat("dot_radius", 22f);
            cx = ringR + 5;
            cy = ringR + 5;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (cx > 0 && cy > 0) {
                canvas.drawCircle(cx, cy, ringR, ringPaint);
                canvas.drawCircle(cx, cy, dotR, dotPaint);
            }
        }
    }
}
