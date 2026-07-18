package com.monaim.studio.overlay;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.monaim.studio.R;
import com.monaim.studio.macro.TouchRecorder;

public class FloatingWidgetService extends Service {

    private static final String TAG = "FloatingWidget";

    private WindowManager windowManager;
    private FloatingDotView floatingDotView;
    private LinearLayout expandedPanel;
    private WindowManager.LayoutParams dotParams, panelParams;
    private boolean isAttached = false, panelVisible = false;
    private Handler mainHandler;
    private SharedPreferences prefs;

    private boolean isHolding = false, holdTriggered = false, isDragging = false;
    private long downTime;
    private float touchStartX, touchStartY;
    private int initialX, initialY;
    private static final long HOLD_THRESHOLD_MS = 120;

    private BroadcastReceiver updateReceiver;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mainHandler = new Handler(Looper.getMainLooper());
        prefs = getSharedPreferences("mo_macro_widget", MODE_PRIVATE);
        createFloatingDot();
        createExpandedPanel();
        registerUpdateReceiver();
    }

    // ========== FLOATING DOT ==========
    private void createFloatingDot() {
        try {
            floatingDotView = new FloatingDotView(this);
            floatingDotView.updateDrawing(prefs);

            int layoutType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    : WindowManager.LayoutParams.TYPE_PHONE;

            int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                      | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                      | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
                      | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;

            int size = (int) (prefs.getFloat("ring_radius", 50f) * 2 + 10);
            dotParams = new WindowManager.LayoutParams(size, size, layoutType, flags, PixelFormat.TRANSLUCENT);
            dotParams.gravity = Gravity.TOP | Gravity.START;
            dotParams.x = prefs.getInt("widget_x", 50);
            dotParams.y = prefs.getInt("widget_y", 500);

            floatingDotView.setOnTouchListener((v, event) -> {
                try {
                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            initialX = dotParams.x;
                            initialY = dotParams.y;
                            touchStartX = event.getRawX();
                            touchStartY = event.getRawY();
                            downTime = System.currentTimeMillis();
                            holdTriggered = false;
                            isDragging = false;
                            return true;

                        case MotionEvent.ACTION_MOVE:
                            float dx = Math.abs(event.getRawX() - touchStartX);
                            float dy = Math.abs(event.getRawY() - touchStartY);
                            if (dx > 8 || dy > 8) {
                                isDragging = true;
                                dotParams.x = initialX + (int)(event.getRawX() - touchStartX);
                                dotParams.y = initialY + (int)(event.getRawY() - touchStartY);
                                updateViewSafe(floatingDotView, dotParams);
                                if (panelVisible) updatePanelPosition();
                                return true;
                            }
                            if (!isDragging && !holdTriggered && (System.currentTimeMillis() - downTime) >= HOLD_THRESHOLD_MS) {
                                holdTriggered = true;
                                onHoldStart();
                                return true;
                            }
                            return false;

                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            if (holdTriggered) onHoldStop();
                            if (!isDragging && !holdTriggered && panelVisible) {
                                hidePanel();
                            } else if (!isDragging && !holdTriggered && !panelVisible) {
                                showPanel();
                            }
                            isHolding = false;
                            isDragging = false;
                            holdTriggered = false;
                            return true;
                    }
                } catch (Exception e) { Log.e(TAG, "Touch error", e); }
                return false;
            });

            windowManager.addView(floatingDotView, dotParams);
            isAttached = true;
        } catch (Exception e) { Log.e(TAG, "Dot creation failed", e); }
    }

    // ========== EXPANDED PANEL ==========
    private void createExpandedPanel() {
        try {
            LayoutInflater inflater = LayoutInflater.from(this);
            expandedPanel = (LinearLayout) inflater.inflate(R.layout.widget_panel, null);

            int layoutType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    : WindowManager.LayoutParams.TYPE_PHONE;

            panelParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    layoutType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
            );
            panelParams.gravity = Gravity.TOP | Gravity.START;

            // Hold button
            ImageButton btnHold = expandedPanel.findViewById(R.id.btn_hold);
            if (btnHold != null) btnHold.setOnClickListener(v -> {
                Toast.makeText(this, "Hold the dot to activate Hold Swipe", Toast.LENGTH_SHORT).show();
            });

            // Set Position button
            ImageButton btnSetPos = expandedPanel.findViewById(R.id.btn_set_position);
            if (btnSetPos == null) {
                btnSetPos = new ImageButton(this);
                btnSetPos.setImageResource(R.drawable.ic_target);
                btnSetPos.setBackground(null);
                btnSetPos.setLayoutParams(new LinearLayout.LayoutParams(40, 40));
                expandedPanel.addView(btnSetPos, 1);
            }
            btnSetPos.setOnClickListener(v -> startSetPositionMode());

            // Play
            ImageButton btnPlay = expandedPanel.findViewById(R.id.btn_play);
            if (btnPlay != null) btnPlay.setOnClickListener(v ->
                    Toast.makeText(this, "Play script", Toast.LENGTH_SHORT).show());

            // Stop
            ImageButton btnStop = expandedPanel.findViewById(R.id.btn_stop);
            if (btnStop != null) btnStop.setOnClickListener(v -> {
                hidePanel();
                Toast.makeText(this, "Stopped", Toast.LENGTH_SHORT).show();
            });

            // Record
            ImageButton btnRecord = expandedPanel.findViewById(R.id.btn_record);
            if (btnRecord != null) btnRecord.setOnClickListener(v ->
                    Toast.makeText(this, "Recording...", Toast.LENGTH_SHORT).show());

        } catch (Exception e) { Log.e(TAG, "Panel creation failed", e); }
    }

    private void startSetPositionMode() {
        hidePanel();
        Toast.makeText(this, "Tap anywhere on screen to set position", Toast.LENGTH_LONG).show();

        TouchRecorder.getInstance(this).waitForPosition((x, y) -> {
            prefs.edit().putInt("hold_x1", (int)x).putInt("hold_y1", (int)y).apply();
            Toast.makeText(this, "Position set: (" + (int)x + ", " + (int)y + ")", Toast.LENGTH_SHORT).show();
            showPanel();
        });
    }

    // ========== PANEL VISIBILITY ==========
    private void showPanel() {
        try {
            if (expandedPanel.getParent() != null) windowManager.removeView(expandedPanel);
            updatePanelPosition();
            windowManager.addView(expandedPanel, panelParams);
            panelVisible = true;
        } catch (Exception e) { Log.e(TAG, "Show panel failed", e); }
    }

    private void hidePanel() {
        try {
            if (expandedPanel.getParent() != null) windowManager.removeView(expandedPanel);
            panelVisible = false;
        } catch (Exception e) { Log.e(TAG, "Hide panel failed", e); }
    }

    private void updatePanelPosition() {
        try {
            int[] loc = new int[2];
            floatingDotView.getLocationOnScreen(loc);
            panelParams.x = loc[0] - 120;
            panelParams.y = loc[1] - 200;
        } catch (Exception e) {}
    }

    // ========== HOLD ==========
    private void onHoldStart() {
        if (isHolding) return;
        isHolding = true;
        try {
            Intent intent = new Intent("com.monaim.studio.HOLD_START");
            intent.setPackage(getPackageName());
            sendBroadcast(intent);
        } catch (Exception e) {}
    }

    private void onHoldStop() {
        try {
            Intent intent = new Intent("com.monaim.studio.HOLD_STOP");
            intent.setPackage(getPackageName());
            sendBroadcast(intent);
        } catch (Exception e) {}
        isHolding = false;
    }

    // ========== RECEIVER ==========
    private void registerUpdateReceiver() {
        updateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                if ("com.monaim.studio.WIDGET_UPDATE".equals(i.getAction())) {
                    updateAppearance();
                }
            }
        };
        registerReceiver(updateReceiver, new IntentFilter("com.monaim.studio.WIDGET_UPDATE"));
    }

    private void updateAppearance() {
        if (floatingDotView != null && isAttached) {
            floatingDotView.updateDrawing(prefs);
            int size = (int)(prefs.getFloat("ring_radius", 50f) * 2 + 10);
            dotParams.width = size;
            dotParams.height = size;
            updateViewSafe(floatingDotView, dotParams);
        }
    }

    private void updateViewSafe(View v, WindowManager.LayoutParams p) {
        try { windowManager.updateViewLayout(v, p); } catch (Exception ignored) {}
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(updateReceiver); } catch (Exception ignored) {}
        try { if (floatingDotView != null) windowManager.removeView(floatingDotView); } catch (Exception ignored) {}
        try { if (expandedPanel != null) windowManager.removeView(expandedPanel); } catch (Exception ignored) {}
        isAttached = false;
        if (dotParams != null) {
            prefs.edit().putInt("widget_x", dotParams.x).putInt("widget_y", dotParams.y).apply();
        }
    }

    // ========== CUSTOM VIEW ==========
    public static class FloatingDotView extends View {
        private Paint dotPaint, ringPaint;
        private float cx, cy, dotR, ringR;

        public FloatingDotView(Context context) {
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
                try {
                    canvas.drawCircle(cx, cy, ringR, ringPaint);
                    canvas.drawCircle(cx, cy, dotR, dotPaint);
                } catch (Exception ignored) {}
            }
        }
    }
}
