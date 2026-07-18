package com.monaim.studio.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.monaim.studio.macro.JsonScriptLoader;
import com.monaim.studio.overlay.FloatingWidgetService;
import org.json.JSONObject;
import java.util.Iterator;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS = "mo_macro_widget";
    private SharedPreferences prefs;
    private SeekBar sbDotSize, sbRingSize, sbRingThick, sbDotAlpha, sbRingAlpha;
    private TextView tvDotSize, tvRingSize, tvRingThick, tvDotAlpha, tvRingAlpha;
    private Button btnDotColor, btnRingColor, btnApply, btnReset, btnScripts, btnSample;
    private int dotColor = Color.RED, ringColor = Color.WHITE;
    private EditText etHoldStartX, etHoldStartY, etHoldEndX, etHoldEndY, etHoldDur;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        buildUI();
    }

    private void buildUI() {
        ScrollView sv = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32,32,32,32);
        root.setBackgroundColor(0xFF1A1A2E);

        dotColor = prefs.getInt("dot_color", Color.RED);
        ringColor = prefs.getInt("ring_color", Color.WHITE);

        TextView title = new TextView(this); title.setText("Widget Customization"); title.setTextColor(0xFFE94560); title.setTextSize(22f);
        root.addView(title);

        root.addView(label("Dot Size (dp)"));
        sbDotSize = seek((int)prefs.getFloat("dot_radius",22f), 4, 60);
        tvDotSize = val(); root.addView(row(sbDotSize, tvDotSize, "dp"));

        root.addView(label("Ring Radius (dp)"));
        sbRingSize = seek((int)prefs.getFloat("ring_radius",50f), 10, 150);
        tvRingSize = val(); root.addView(row(sbRingSize, tvRingSize, "dp"));

        root.addView(label("Ring Thickness (dp)"));
        sbRingThick = seek((int)prefs.getFloat("ring_thickness",2f), 1, 20);
        tvRingThick = val(); root.addView(row(sbRingThick, tvRingThick, "dp"));

        root.addView(label("Dot Alpha (0-255)"));
        sbDotAlpha = seek(prefs.getInt("dot_alpha",220), 10, 255);
        tvDotAlpha = val(); root.addView(row(sbDotAlpha, tvDotAlpha, ""));

        root.addView(label("Ring Alpha (0-255)"));
        sbRingAlpha = seek(prefs.getInt("ring_alpha",20), 1, 255);
        tvRingAlpha = val(); root.addView(row(sbRingAlpha, tvRingAlpha, ""));

        btnDotColor = btn("Dot Color: #" + Integer.toHexString(dotColor).toUpperCase(), dotColor);
        btnDotColor.setOnClickListener(v -> pickColor(true));
        root.addView(btnDotColor);

        btnRingColor = btn("Ring Color: #" + Integer.toHexString(ringColor).toUpperCase(), ringColor);
        btnRingColor.setOnClickListener(v -> pickColor(false));
        root.addView(btnRingColor);

        root.addView(section("Hold Swipe Coordinates"));
        root.addView(label("Start X")); etHoldStartX = edit(String.valueOf(prefs.getInt("hold_x1",540)));
        root.addView(etHoldStartX);
        root.addView(label("Start Y")); etHoldStartY = edit(String.valueOf(prefs.getInt("hold_y1",1200)));
        root.addView(etHoldStartY);
        root.addView(label("End X")); etHoldEndX = edit(String.valueOf(prefs.getInt("hold_x2",540)));
        root.addView(etHoldEndX);
        root.addView(label("End Y")); etHoldEndY = edit(String.valueOf(prefs.getInt("hold_y2",600)));
        root.addView(etHoldEndY);
        root.addView(label("Duration (ms)")); etHoldDur = edit(String.valueOf(prefs.getLong("hold_dur",100L)));
        root.addView(etHoldDur);

        btnApply = btn("Apply & Restart Widget", 0xFF4CAF50);
        btnApply.setOnClickListener(v -> applySettings());
        root.addView(btnApply);

        btnReset = btn("Reset Defaults", 0xFFFFC107);
        btnReset.setOnClickListener(v -> resetDefaults());
        root.addView(btnReset);

        btnScripts = btn("Manage Scripts", 0xFF9C27B0);
        btnScripts.setOnClickListener(v -> showScripts());
        root.addView(btnScripts);

        btnSample = btn("Create Sample Script", 0xFF00BCD4);
        btnSample.setOnClickListener(v -> {
            JsonScriptLoader.importScriptFromString(this, JsonScriptLoader.generateSampleScript(), "sample.json");
            Toast.makeText(this, "Sample created", Toast.LENGTH_SHORT).show();
        });
        root.addView(btnSample);

        sv.addView(root);
        setContentView(sv);
    }

    private void pickColor(boolean isDot) {
        final int[] colors = {Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.CYAN, Color.MAGENTA, Color.WHITE, Color.GRAY};
        String[] names = {"Red","Green","Blue","Yellow","Cyan","Magenta","White","Gray"};
        new android.app.AlertDialog.Builder(this)
                .setTitle("Pick Color")
                .setItems(names, (d, i) -> {
                    if (isDot) { dotColor = colors[i]; btnDotColor.setText("Dot Color: #"+Integer.toHexString(dotColor).toUpperCase()); btnDotColor.setBackgroundColor(dotColor); }
                    else { ringColor = colors[i]; btnRingColor.setText("Ring Color: #"+Integer.toHexString(ringColor).toUpperCase()); btnRingColor.setBackgroundColor(ringColor); }
                }).show();
    }

    private void applySettings() {
        prefs.edit()
                .putFloat("dot_radius", sbDotSize.getProgress())
                .putFloat("ring_radius", sbRingSize.getProgress())
                .putFloat("ring_thickness", sbRingThick.getProgress())
                .putInt("dot_alpha", sbDotAlpha.getProgress())
                .putInt("ring_alpha", sbRingAlpha.getProgress())
                .putInt("dot_color", dotColor)
                .putInt("ring_color", ringColor)
                .putInt("hold_x1", parseInt(etHoldStartX.getText().toString(), 540))
                .putInt("hold_y1", parseInt(etHoldStartY.getText().toString(), 1200))
                .putInt("hold_x2", parseInt(etHoldEndX.getText().toString(), 540))
                .putInt("hold_y2", parseInt(etHoldEndY.getText().toString(), 600))
                .putLong("hold_dur", parseLong(etHoldDur.getText().toString(), 100L))
                .apply();

        sendBroadcast(new Intent("com.monaim.studio.WIDGET_UPDATE"));
        Toast.makeText(this, "Settings saved. Restart widget from main screen.", Toast.LENGTH_LONG).show();
    }

    private void resetDefaults() {
        sbDotSize.setProgress(22); sbRingSize.setProgress(50); sbRingThick.setProgress(2);
        sbDotAlpha.setProgress(220); sbRingAlpha.setProgress(20);
        dotColor = Color.RED; ringColor = Color.WHITE;
        btnDotColor.setText("Dot Color: #FF0000"); btnRingColor.setText("Ring Color: #FFFFFF");
        etHoldStartX.setText("540"); etHoldStartY.setText("1200"); etHoldEndX.setText("540"); etHoldEndY.setText("600"); etHoldDur.setText("100");
        prefs.edit().clear().apply();
        Toast.makeText(this, "Defaults restored", Toast.LENGTH_SHORT).show();
    }

    private void showScripts() {
        List<String> s = JsonScriptLoader.listSavedScripts(this);
        if (s.isEmpty()) { Toast.makeText(this, "No scripts", Toast.LENGTH_SHORT).show(); return; }
        StringBuilder sb = new StringBuilder();
        for (String x : s) sb.append("• ").append(x).append("\n");
        new android.app.AlertDialog.Builder(this).setTitle("Scripts").setMessage(sb.toString()).setPositiveButton("OK",null).show();
    }

    private int parseInt(String s, int def) { try { return Integer.parseInt(s); } catch (Exception e) { return def; } }
    private long parseLong(String s, long def) { try { return Long.parseLong(s); } catch (Exception e) { return def; } }

    private TextView label(String t) { TextView tv = new TextView(this); tv.setText(t); tv.setTextColor(0xFFCCCCCC); tv.setPadding(0,12,0,4); return tv; }
    private TextView section(String t) { TextView tv = new TextView(this); tv.setText(t); tv.setTextColor(0xFFE94560); tv.setTextSize(16f); tv.setPadding(0,24,0,8); return tv; }
    private TextView val() { TextView tv = new TextView(this); tv.setTextColor(0xFFFFFFFF); tv.setMinWidth(80); tv.setGravity(android.view.Gravity.END); return tv; }
    private SeekBar seek(int prog, int min, int max) { SeekBar sb = new SeekBar(this); sb.setMax(max-min); sb.setProgress(prog-min); sb.setTag(new int[]{min,max}); return sb; }
    private LinearLayout row(SeekBar sb, TextView tv, String suf) {
        LinearLayout r = new LinearLayout(this); r.setOrientation(LinearLayout.HORIZONTAL); r.setGravity(android.view.Gravity.CENTER_VERTICAL);
        sb.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1)); r.addView(sb); r.addView(tv);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) { int[] t = (int[])s.getTag(); tv.setText((p+t[0])+suf); }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        return r;
    }
    private Button btn(String text, int color) { Button b = new Button(this); b.setText(text); b.setTextColor(0xFFFFFFFF); b.setBackgroundColor(color); b.setPadding(24,16,24,16); LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,8,0,8); b.setLayoutParams(p); return b; }
    private EditText edit(String val) { EditText e = new EditText(this); e.setText(val); e.setTextColor(0xFFFFFFFF); e.setBackgroundColor(0x22FFFFFF); e.setPadding(16,16,16,16); return e; }
}
