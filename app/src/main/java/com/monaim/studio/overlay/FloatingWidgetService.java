package com.monaim.studio.overlay;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

public class FloatingWidgetService extends Service {

    private WindowManager windowManager;
    private View floatingWidget;
    private View expandedPanel;
    private WindowManager.LayoutParams widgetParams;
    private WindowManager.LayoutParams panelParams;
    private boolean panelVisible = false;
    private int initialX, initialY;
    private float initialTouchX, initialTouchY;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        setupFloatingWidget();
        setupExpandedPanel();
    }

    private void setupFloatingWidget() {
        LayoutInflater inflater = LayoutInflater.from(this);
        floatingWidget = inflater.inflate(R.layout.widget_floating, null);

        int layoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        widgetParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        widgetParams.gravity = Gravity.TOP | Gravity.START;
        widgetParams.x = 100;
        widgetParams.y = 300;

        ImageView ivIcon = floatingWidget.findViewById(R.id.iv_widget_icon);

        ivIcon.setOnClickListener(v -> {
            if (panelVisible) {
                hideExpandedPanel();
            } else {
                showExpandedPanel();
            }
        });

        ivIcon.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = widgetParams.x;
                        initialY = widgetParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        widgetParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                        widgetParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingWidget, widgetParams);
                        if (panelVisible) {
                            updatePanelPosition();
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (Math.abs(event.getRawX() - initialTouchX) < 10
                                && Math.abs(event.getRawY() - initialTouchY) < 10) {
                            return false;
                        }
                        return true;
                }
                return false;
            }
        });

        windowManager.addView(floatingWidget, widgetParams);
    }

    private void setupExpandedPanel() {
        LayoutInflater inflater = LayoutInflater.from(this);
        expandedPanel = inflater.inflate(R.layout.widget_panel, null);

        int layoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        panelParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        panelParams.gravity = Gravity.TOP | Gravity.START;

        expandedPanel.findViewById(R.id.btn_play).setOnClickListener(v -> {
            Toast.makeText(FloatingWidgetService.this, "Macro Started", Toast.LENGTH_SHORT).show();
        });

        expandedPanel.findViewById(R.id.btn_pause).setOnClickListener(v -> {
            Toast.makeText(FloatingWidgetService.this, "Macro Paused", Toast.LENGTH_SHORT).show();
        });

        expandedPanel.findViewById(R.id.btn_stop).setOnClickListener(v -> {
            hideExpandedPanel();
            Toast.makeText(FloatingWidgetService.this, "Macro Stopped", Toast.LENGTH_SHORT).show();
        });

        expandedPanel.findViewById(R.id.btn_record).setOnClickListener(v -> {
            Toast.makeText(FloatingWidgetService.this, "Recording Mode", Toast.LENGTH_SHORT).show();
        });
    }

    private void showExpandedPanel() {
        if (expandedPanel.getParent() != null) {
            windowManager.removeView(expandedPanel);
        }
        updatePanelPosition();
        windowManager.addView(expandedPanel, panelParams);
        panelVisible = true;
    }

    private void hideExpandedPanel() {
        if (expandedPanel.getParent() != null) {
            windowManager.removeView(expandedPanel);
        }
        panelVisible = false;
    }

    private void updatePanelPosition() {
        int[] location = new int[2];
        floatingWidget.getLocationOnScreen(location);
        panelParams.x = location[0] - 120;
        panelParams.y = location[1] - 180;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingWidget != null && floatingWidget.getParent() != null) {
            windowManager.removeView(floatingWidget);
        }
        if (expandedPanel != null && expandedPanel.getParent() != null) {
            windowManager.removeView(expandedPanel);
        }
    }
}
