package com.monaim.studio.overlay;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.monaim.studio.macro.TouchRecorder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FloatingWidgetManager extends Service {

    private static final String TAG = "DotManager";
    private static final int MAX_DOTS = 5;
    private static final String PREFS = "mo_macro_dots";

    private SharedPreferences prefs;
    private List<MacroDot> dots;
    private Handler handler;
    private BroadcastReceiver updateReceiver;
    private int dotCounter = 0;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        handler = new Handler(Looper.getMainLooper());
        dots = new ArrayList<>();
        dotCounter = prefs.getInt("dot_count", 0);

        // استعادة الأيقونات المحفوظة
        for (int i = 0; i < dotCounter; i++) {
            String id = "macro_" + i;
            try {
                MacroDot dot = new MacroDot(this, id, prefs);
                dot.setCallbacks(
                    () -> handleHoldStart(dot),
                    () -> handleHoldStop(dot),
                    () -> removeDot(dot)
                );
                dot.attach();
                dots.add(dot);
            } catch (Exception e) {
                Log.e(TAG, "Failed to restore dot: " + id, e);
            }
        }

        // أيقونة افتراضية إذا لا يوجد شيء
        if (dots.isEmpty()) {
            addNewDot();
        }

        registerReceivers();
    }

    private void registerReceivers() {
        updateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                String action = i.getAction();
                if ("com.monaim.studio.WIDGET_UPDATE".equals(action)) {
                    refreshAllDots();
                } else if ("com.monaim.studio.ADD_DOT".equals(action)) {
                    addNewDot();
                } else if ("com.monaim.studio.REMOVE_DOT".equals(action)) {
                    String id = i.getStringExtra("dot_id");
                    if (id != null) removeDotById(id);
                }
            }
        };
        IntentFilter f = new IntentFilter();
        f.addAction("com.monaim.studio.WIDGET_UPDATE");
        f.addAction("com.monaim.studio.ADD_DOT");
        f.addAction("com.monaim.studio.REMOVE_DOT");
        registerReceiver(updateReceiver, f);
    }

    // ========== ADD / REMOVE ==========
    public void addNewDot() {
        if (dots.size() >= MAX_DOTS) {
            Toast.makeText(this, "Max " + MAX_DOTS + " dots reached", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String id = "macro_" + (dotCounter++);
            MacroDot dot = new MacroDot(this, id, prefs);
            dot.setCallbacks(
                () -> handleHoldStart(dot),
                () -> handleHoldStop(dot),
                () -> removeDot(dot)
            );
            dot.attach();
            dots.add(dot);
            prefs.edit().putInt("dot_count", dotCounter).apply();
        } catch (Exception e) {
            Log.e(TAG, "Add dot failed", e);
        }
    }

    public void removeDot(MacroDot dot) {
        try {
            dot.detach();
            dots.remove(dot);
        } catch (Exception e) {
            Log.e(TAG, "Remove dot failed", e);
        }
    }

    public void removeDotById(String id) {
        Iterator<MacroDot> it = dots.iterator();
        while (it.hasNext()) {
            MacroDot d = it.next();
            if (d.getDotId().equals(id)) {
                try { d.detach(); } catch (Exception ignored) {}
                it.remove();
                break;
            }
        }
    }

    public void refreshAllDots() {
        for (MacroDot d : dots) {
            try { d.loadPrefs(prefs); d.refresh(); } catch (Exception ignored) {}
        }
    }

    // ========== HOLD ==========
    private void handleHoldStart(MacroDot dot) {
        try {
            Intent intent = new Intent("com.monaim.studio.HOLD_START");
            intent.setPackage(getPackageName());
            intent.putExtra("x1", dot.getActionX1());
            intent.putExtra("y1", dot.getActionY1());
            intent.putExtra("x2", dot.getActionX2());
            intent.putExtra("y2", dot.getActionY2());
            intent.putExtra("duration", dot.getActionDuration());
            sendBroadcast(intent);
        } catch (Exception e) { Log.e(TAG, "Hold start error", e); }
    }

    private void handleHoldStop(MacroDot dot) {
        try {
            Intent intent = new Intent("com.monaim.studio.HOLD_STOP");
            intent.setPackage(getPackageName());
            sendBroadcast(intent);
        } catch (Exception e) { Log.e(TAG, "Hold stop error", e); }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(updateReceiver); } catch (Exception ignored) {}
        for (MacroDot d : dots) {
            try {
                d.savePrefs(prefs.edit());
                d.detach();
            } catch (Exception ignored) {}
        }
        dots.clear();
    }
}
