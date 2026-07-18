package com.monaim.studio.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.monaim.studio.macro.JsonScriptLoader;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "mo_macro_settings";
    private static final String KEY_OPACITY = "widget_opacity";
    private static final String KEY_SIZE = "widget_size";
    private static final String KEY_DEFAULT_DELAY = "default_delay";
    private static final String KEY_SWIPE_DURATION = "swipe_duration";
    private static final String KEY_VIBRATE_ON_ACTION = "vibrate_on_action";
    private static final String KEY_SHOW_COORDINATES = "show_coordinates";
    private static final String KEY_AUTO_SAVE = "auto_save";
    private static final String KEY_EXECUTION_SPEED = "execution_speed";
    private static final String KEY_LOOP_COUNT = "loop_count";
    private static final String KEY_TOUCH_INDICATOR = "touch_indicator";
    private static final String KEY_INDICATOR_COLOR = "indicator_color";
    private static final String KEY_INDICATOR_SIZE = "indicator_size";
    private static final String KEY_START_DELAY = "start_delay";
    private static final String KEY_RANDOMIZE_DELAY = "randomize_delay";
    private static final String KEY_RANDOMIZE_POSITION = "randomize_position";
    private static final String KEY_RANDOM_RANGE = "random_range";
    private static final String KEY_EDGE_PROTECTION = "edge_protection";
    private static final String KEY_SAFE_MODE = "safe_mode";
    private static final String KEY_LOG_ACTIONS = "log_actions";

    private SharedPreferences prefs;
    private SeekBar sbOpacity, sbSize, sbDefaultDelay, sbSwipeDuration, sbRandomRange;
    private SeekBar sbExecutionSpeed, sbLoopCount, sbIndicatorSize, sbStartDelay;
    private TextView tvOpacity, tvSize, tvDefaultDelay, tvSwipeDuration, tvRandomRange;
    private TextView tvExecutionSpeed, tvLoopCount, tvIndicatorSize, tvStartDelay;
    private CheckBox cbVibrate, cbShowCoordinates, cbAutoSave, cbTouchIndicator;
    private CheckBox cbRandomizeDelay, cbRandomizePosition, cbEdgeProtection;
    private CheckBox cbSafeMode, cbLogActions;
    private Spinner spIndicatorColor;
    private Button btnSaveSettings, btnResetSettings, btnExportSettings, btnImportSettings;
    private Button btnManageScripts, btnDeleteAllScripts, btnCreateSampleScript;
    private EditText etImportJson;
    private LinearLayout llImportPanel;

    private String[] colorNames = {"Red", "Green", "Blue", "Yellow", "Cyan", "Magenta", "White", "Orange"};
    private int[] colorValues = {
            0xFFE94560, 0xFF4CAF50, 0xFF2196F3, 0xFFFFEB3B,
            0xFF00BCD4, 0xFFFF00FF, 0xFFFFFFFF, 0xFFFF9800
    };
    private int selectedColor = 0xFFE94560;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupUI();
    }

    private void setupUI() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(32, 32, 32, 32);
        rootLayout.setBackgroundColor(0xFF1A1A2E);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Title
        TextView tvTitle = createTitle("MO MACRO Settings");
        rootLayout.addView(tvTitle);

        // === WIDGET SETTINGS ===
        rootLayout.addView(createSectionHeader("Widget Settings"));

        rootLayout.addView(createLabel("Widget Opacity"));
        sbOpacity = createSeekBar(10, 100, prefs.getInt(KEY_OPACITY, 90));
        tvOpacity = createValueLabel(sbOpacity.getProgress() + "%");
        rootLayout.addView(createSeekBarRow(sbOpacity, tvOpacity));
        sbOpacity.setOnSeekBarChangeListener(seekBarTextUpdater(tvOpacity, "%"));

        rootLayout.addView(createLabel("Widget Size"));
        sbSize = createSeekBar(20, 100, prefs.getInt(KEY_SIZE, 48));
        tvSize = createValueLabel(sbSize.getProgress() + "dp");
        rootLayout.addView(createSeekBarRow(sbSize, tvSize));
        sbSize.setOnSeekBarChangeListener(seekBarTextUpdater(tvSize, "dp"));

        // === ACTION DEFAULTS ===
        rootLayout.addView(createSectionHeader("Action Defaults"));

        rootLayout.addView(createLabel("Default Delay (ms)"));
        sbDefaultDelay = createSeekBar(1, 2000, prefs.getInt(KEY_DEFAULT_DELAY, 100));
        tvDefaultDelay = createValueLabel(sbDefaultDelay.getProgress() + "ms");
        rootLayout.addView(createSeekBarRow(sbDefaultDelay, tvDefaultDelay));
        sbDefaultDelay.setOnSeekBarChangeListener(seekBarTextUpdater(tvDefaultDelay, "ms"));

        rootLayout.addView(createLabel("Default Swipe Duration (ms)"));
        sbSwipeDuration = createSeekBar(10, 3000, prefs.getInt(KEY_SWIPE_DURATION, 200));
        tvSwipeDuration = createValueLabel(sbSwipeDuration.getProgress() + "ms");
        rootLayout.addView(createSeekBarRow(sbSwipeDuration, tvSwipeDuration));
        sbSwipeDuration.setOnSeekBarChangeListener(seekBarTextUpdater(tvSwipeDuration, "ms"));

        // === EXECUTION SETTINGS ===
        rootLayout.addView(createSectionHeader("Execution Settings"));

        rootLayout.addView(createLabel("Speed Multiplier"));
        sbExecutionSpeed = createSeekBar(10, 500, prefs.getInt(KEY_EXECUTION_SPEED, 100));
        tvExecutionSpeed = createValueLabel(sbExecutionSpeed.getProgress() + "%");
        rootLayout.addView(createSeekBarRow(sbExecutionSpeed, tvExecutionSpeed));
        sbExecutionSpeed.setOnSeekBarChangeListener(seekBarTextUpdater(tvExecutionSpeed, "%"));

        rootLayout.addView(createLabel("Loop Count (0 = infinite)"));
        sbLoopCount = createSeekBar(0, 100, prefs.getInt(KEY_LOOP_COUNT, 1));
        tvLoopCount = createValueLabel(sbLoopCount.getProgress() == 0 ? "∞" : String.valueOf(sbLoopCount.getProgress()));
        rootLayout.addView(createSeekBarRow(sbLoopCount, tvLoopCount));
        sbLoopCount.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) {
                tvLoopCount.setText(p == 0 ? "∞" : String.valueOf(p));
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });

        rootLayout.addView(createLabel("Start Delay (seconds)"));
        sbStartDelay = createSeekBar(0, 30, prefs.getInt(KEY_START_DELAY, 3));
        tvStartDelay = createValueLabel(sbStartDelay.getProgress() + "s");
        rootLayout.addView(createSeekBarRow(sbStartDelay, tvStartDelay));
        sbStartDelay.setOnSeekBarChangeListener(seekBarTextUpdater(tvStartDelay, "s"));

        // === TOUCH INDICATOR ===
        rootLayout.addView(createSectionHeader("Touch Indicator"));

        cbTouchIndicator = createCheckBox("Show Touch Indicator",
                prefs.getBoolean(KEY_TOUCH_INDICATOR, true));
        rootLayout.addView(cbTouchIndicator);

        rootLayout.addView(createLabel("Indicator Color"));
        spIndicatorColor = createColorSpinner();
        rootLayout.addView(spIndicatorColor);

        rootLayout.addView(createLabel("Indicator Size"));
        sbIndicatorSize = createSeekBar(5, 50, prefs.getInt(KEY_INDICATOR_SIZE, 20));
        tvIndicatorSize = createValueLabel(sbIndicatorSize.getProgress() + "dp");
        rootLayout.addView(createSeekBarRow(sbIndicatorSize, tvIndicatorSize));
        sbIndicatorSize.setOnSeekBarChangeListener(seekBarTextUpdater(tvIndicatorSize, "dp"));

        // === RANDOMIZATION ===
        rootLayout.addView(createSectionHeader("Randomization"));

        cbRandomizeDelay = createCheckBox("Randomize Delay (±%)",
                prefs.getBoolean(KEY_RANDOMIZE_DELAY, false));
        rootLayout.addView(cbRandomizeDelay);

        cbRandomizePosition = createCheckBox("Randomize Position (±px)",
                prefs.getBoolean(KEY_RANDOMIZE_POSITION, false));
        rootLayout.addView(cbRandomizePosition);

        rootLayout.addView(createLabel("Random Range"));
        sbRandomRange = createSeekBar(1, 50, prefs.getInt(KEY_RANDOM_RANGE, 5));
        tvRandomRange = createValueLabel("±" + sbRandomRange.getProgress() + "px");
        rootLayout.addView(createSeekBarRow(sbRandomRange, tvRandomRange));
        sbRandomRange.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) { tvRandomRange.setText("±" + p + "px"); }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });

        // === SAFETY ===
        rootLayout.addView(createSectionHeader("Safety"));

        cbEdgeProtection = createCheckBox("Edge Protection",
                prefs.getBoolean(KEY_EDGE_PROTECTION, true));
        rootLayout.addView(cbEdgeProtection);

        cbSafeMode = createCheckBox("Safe Mode",
                prefs.getBoolean(KEY_SAFE_MODE, false));
        rootLayout.addView(cbSafeMode);

        // === OTHER ===
        rootLayout.addView(createSectionHeader("Other"));

        cbVibrate = createCheckBox("Vibrate on Action",
                prefs.getBoolean(KEY_VIBRATE_ON_ACTION, false));
        rootLayout.addView(cbVibrate);

        cbShowCoordinates = createCheckBox("Show Coordinates",
                prefs.getBoolean(KEY_SHOW_COORDINATES, true));
        rootLayout.addView(cbShowCoordinates);

        cbAutoSave = createCheckBox("Auto-Save Scripts",
                prefs.getBoolean(KEY_AUTO_SAVE, true));
        rootLayout.addView(cbAutoSave);

        cbLogActions = createCheckBox("Log All Actions",
                prefs.getBoolean(KEY_LOG_ACTIONS, false));
        rootLayout.addView(cbLogActions);

        // === BUTTONS ===
        rootLayout.addView(createSectionHeader("Actions"));

        btnSaveSettings = createButton("Save Settings", 0xFF4CAF50);
        btnSaveSettings.setOnClickListener(v -> saveAllSettings());
        rootLayout.addView(btnSaveSettings);

        btnResetSettings = createButton("Reset to Defaults", 0xFFFFC107);
        btnResetSettings.setOnClickListener(v -> resetSettings());
        rootLayout.addView(btnResetSettings);

        btnExportSettings = createButton("Export Settings", 0xFF2196F3);
        btnExportSettings.setOnClickListener(v -> exportSettings());
        rootLayout.addView(btnExportSettings);

        btnImportSettings = createButton("Import Settings", 0xFF2196F3);
        btnImportSettings.setOnClickListener(v -> toggleImportPanel());
        rootLayout.addView(btnImportSettings);

        llImportPanel = new LinearLayout(this);
        llImportPanel.setOrientation(LinearLayout.VERTICAL);
        llImportPanel.setVisibility(View.GONE);
        llImportPanel.setPadding(0, 8, 0, 8);

        etImportJson = new EditText(this);
        etImportJson.setHint("Paste settings JSON here...");
        etImportJson.setTextColor(0xFFFFFFFF);
        etImportJson.setHintTextColor(0x88FFFFFF);
        etImportJson.setMinLines(4);
        etImportJson.setBackgroundColor(0x22FFFFFF);
        etImportJson.setPadding(16, 16, 16, 16);
        llImportPanel.addView(etImportJson);

        Button btnDoImport = createButton("Import Now", 0xFF4CAF50);
        btnDoImport.setOnClickListener(v -> importSettings());
        llImportPanel.addView(btnDoImport);

        rootLayout.addView(llImportPanel);

        // === SCRIPT MANAGEMENT ===
        rootLayout.addView(createSectionHeader("Script Management"));

        btnManageScripts = createButton("Manage Scripts", 0xFF9C27B0);
        btnManageScripts.setOnClickListener(v -> manageScripts());
        rootLayout.addView(btnManageScripts);

        btnCreateSampleScript = createButton("Create Sample Script", 0xFF00BCD4);
        btnCreateSampleScript.setOnClickListener(v -> createSampleScript());
        rootLayout.addView(btnCreateSampleScript);

        btnDeleteAllScripts = createButton("Delete All Scripts", 0xFFE94560);
        btnDeleteAllScripts.setOnClickListener(v -> deleteAllScripts());
        rootLayout.addView(btnDeleteAllScripts);

        // Version
        TextView tvVersion = new TextView(this);
        tvVersion.setText("MO MACRO v1.0.0 | com.monaim.studio");
        tvVersion.setTextColor(0x44FFFFFF);
        tvVersion.setTextSize(10);
        tvVersion.setPadding(0, 32, 0, 0);
        tvVersion.setGravity(android.view.Gravity.CENTER);
        rootLayout.addView(tvVersion);

        scrollView.addView(rootLayout);
        setContentView(scrollView);
    }

    // === UI Helper Methods ===

    private TextView createTitle(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(0xFFE94560);
        tv.setTextSize(24);
        tv.setPadding(0, 0, 0, 8);
        return tv;
    }

    private TextView createSectionHeader(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(0xFFE94560);
        tv.setTextSize(16);
        tv.setPadding(0, 24, 0, 12);
        return tv;
    }

    private TextView createLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(0xFFCCCCCC);
        tv.setTextSize(13);
        tv.setPadding(0, 8, 0, 4);
        return tv;
    }

    private TextView createValueLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(0xFFFFFFFF);
        tv.setTextSize(13);
        tv.setMinWidth(80);
        tv.setGravity(android.view.Gravity.END);
        return tv;
    }

    private SeekBar createSeekBar(int min, int max, int progress) {
        SeekBar sb = new SeekBar(this);
        sb.setMax(max - min);
        sb.setProgress(progress - min);
        sb.setTag(new int[]{min, max});
        return sb;
    }

    private LinearLayout createSeekBarRow(SeekBar sb, TextView tvValue) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        sb.setLayoutParams(params);
        row.addView(sb);
        row.addView(tvValue);
        return row;
    }

    private CheckBox createCheckBox(String text, boolean checked) {
        CheckBox cb = new CheckBox(this);
        cb.setText(text);
        cb.setTextColor(0xFFFFFFFF);
        cb.setChecked(checked);
        cb.setPadding(0, 8, 0, 8);
        return cb;
    }

    private Button createButton(String text, int color) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(0xFFFFFFFF);
        btn.setBackgroundColor(color);
        btn.setPadding(24, 16, 24, 16);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 8, 0, 8);
        btn.setLayoutParams(params);
        return btn;
    }

    private Spinner createColorSpinner() {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, colorNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        String savedColorName = prefs.getString(KEY_INDICATOR_COLOR, "Red");
        for (int i = 0; i < colorNames.length; i++) {
            if (colorNames[i].equals(savedColorName)) {
                spinner.setSelection(i);
                selectedColor = colorValues[i];
                break;
            }
        }

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedColor = colorValues[position];
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        return spinner;
    }

    private SeekBar.OnSeekBarChangeListener seekBarTextUpdater(TextView tv, String suffix) {
        return new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) {
                int[] range = (int[]) s.getTag();
                tv.setText((p + range[0]) + suffix);
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        };
    }

    private int getSeekBarValue(SeekBar sb) {
        int[] range = (int[]) sb.getTag();
        return sb.getProgress() + range[0];
    }

    // === Action Methods ===

    private void saveAllSettings() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_OPACITY, getSeekBarValue(sbOpacity));
        editor.putInt(KEY_SIZE, getSeekBarValue(sbSize));
        editor.putInt(KEY_DEFAULT_DELAY, getSeekBarValue(sbDefaultDelay));
        editor.putInt(KEY_SWIPE_DURATION, getSeekBarValue(sbSwipeDuration));
        editor.putInt(KEY_EXECUTION_SPEED, getSeekBarValue(sbExecutionSpeed));
        editor.putInt(KEY_LOOP_COUNT, getSeekBarValue(sbLoopCount));
        editor.putInt(KEY_START_DELAY, getSeekBarValue(sbStartDelay));
        editor.putInt(KEY_INDICATOR_SIZE, getSeekBarValue(sbIndicatorSize));
        editor.putInt(KEY_RANDOM_RANGE, getSeekBarValue(sbRandomRange));
        editor.putBoolean(KEY_VIBRATE_ON_ACTION, cbVibrate.isChecked());
        editor.putBoolean(KEY_SHOW_COORDINATES, cbShowCoordinates.isChecked());
        editor.putBoolean(KEY_AUTO_SAVE, cbAutoSave.isChecked());
        editor.putBoolean(KEY_TOUCH_INDICATOR, cbTouchIndicator.isChecked());
        editor.putBoolean(KEY_RANDOMIZE_DELAY, cbRandomizeDelay.isChecked());
        editor.putBoolean(KEY_RANDOMIZE_POSITION, cbRandomizePosition.isChecked());
        editor.putBoolean(KEY_EDGE_PROTECTION, cbEdgeProtection.isChecked());
        editor.putBoolean(KEY_SAFE_MODE, cbSafeMode.isChecked());
        editor.putBoolean(KEY_LOG_ACTIONS, cbLogActions.isChecked());
        editor.putString(KEY_INDICATOR_COLOR, colorNames[spIndicatorColor.getSelectedItemPosition()]);
        editor.apply();
        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
    }

    private void resetSettings() {
        sbOpacity.setProgress(80);
        sbSize.setProgress(28);
        sbDefaultDelay.setProgress(99);
        sbSwipeDuration.setProgress(190);
        sbExecutionSpeed.setProgress(90);
        sbLoopCount.setProgress(1);
        sbStartDelay.setProgress(3);
        sbIndicatorSize.setProgress(15);
        sbRandomRange.setProgress(4);
        cbVibrate.setChecked(false);
        cbShowCoordinates.setChecked(true);
        cbAutoSave.setChecked(true);
        cbTouchIndicator.setChecked(true);
        cbRandomizeDelay.setChecked(false);
        cbRandomizePosition.setChecked(false);
        cbEdgeProtection.setChecked(true);
        cbSafeMode.setChecked(false);
        cbLogActions.setChecked(false);
        spIndicatorColor.setSelection(0);
        Toast.makeText(this, "Settings reset to defaults", Toast.LENGTH_SHORT).show();
    }

    private String exportSettings() {
        try {
            JSONObject json = new JSONObject();
            json.put(KEY_OPACITY, getSeekBarValue(sbOpacity));
            json.put(KEY_SIZE, getSeekBarValue(sbSize));
            json.put(KEY_DEFAULT_DELAY, getSeekBarValue(sbDefaultDelay));
            json.put(KEY_SWIPE_DURATION, getSeekBarValue(sbSwipeDuration));
            json.put(KEY_EXECUTION_SPEED, getSeekBarValue(sbExecutionSpeed));
            json.put(KEY_LOOP_COUNT, getSeekBarValue(sbLoopCount));
            json.put(KEY_START_DELAY, getSeekBarValue(sbStartDelay));
            json.put(KEY_INDICATOR_SIZE, getSeekBarValue(sbIndicatorSize));
            json.put(KEY_RANDOM_RANGE, getSeekBarValue(sbRandomRange));
            json.put(KEY_VIBRATE_ON_ACTION, cbVibrate.isChecked());
            json.put(KEY_SHOW_COORDINATES, cbShowCoordinates.isChecked());
            json.put(KEY_AUTO_SAVE, cbAutoSave.isChecked());
            json.put(KEY_TOUCH_INDICATOR, cbTouchIndicator.isChecked());
            json.put(KEY_RANDOMIZE_DELAY, cbRandomizeDelay.isChecked());
            json.put(KEY_RANDOMIZE_POSITION, cbRandomizePosition.isChecked());
            json.put(KEY_EDGE_PROTECTION, cbEdgeProtection.isChecked());
            json.put(KEY_SAFE_MODE, cbSafeMode.isChecked());
            json.put(KEY_LOG_ACTIONS, cbLogActions.isChecked());
            json.put(KEY_INDICATOR_COLOR, colorNames[spIndicatorColor.getSelectedItemPosition()]);

            String exported = json.toString(4);
            etImportJson.setText(exported);
            llImportPanel.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Settings exported. Scroll down to see JSON.", Toast.LENGTH_LONG).show();
            return exported;
        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private void toggleImportPanel() {
        llImportPanel.setVisibility(
                llImportPanel.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }

    private void importSettings() {
        String jsonString = etImportJson.getText().toString().trim();
        if (jsonString.isEmpty()) {
            Toast.makeText(this, "Paste settings JSON first", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            JSONObject json = new JSONObject(jsonString);

            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = json.get(key);
                try {
                    switch (key) {
                        case KEY_OPACITY: sbOpacity.setProgress(((Number) value).intValue() - 10); break;
                        case KEY_SIZE: sbSize.setProgress(((Number) value).intValue() - 20); break;
                        case KEY_DEFAULT_DELAY: sbDefaultDelay.setProgress(((Number) value).intValue() - 1); break;
                        case KEY_SWIPE_DURATION: sbSwipeDuration.setProgress(((Number) value).intValue() - 10); break;
                        case KEY_EXECUTION_SPEED: sbExecutionSpeed.setProgress(((Number) value).intValue() - 10); break;
                        case KEY_LOOP_COUNT: sbLoopCount.setProgress(((Number) value).intValue()); break;
                        case KEY_START_DELAY: sbStartDelay.setProgress(((Number) value).intValue()); break;
                        case KEY_INDICATOR_SIZE: sbIndicatorSize.setProgress(((Number) value).intValue() - 5); break;
                        case KEY_RANDOM_RANGE: sbRandomRange.setProgress(((Number) value).intValue() - 1); break;
                        case KEY_VIBRATE_ON_ACTION: cbVibrate.setChecked((Boolean) value); break;
                        case KEY_SHOW_COORDINATES: cbShowCoordinates.setChecked((Boolean) value); break;
                        case KEY_AUTO_SAVE: cbAutoSave.setChecked((Boolean) value); break;
                        case KEY_TOUCH_INDICATOR: cbTouchIndicator.setChecked((Boolean) value); break;
                        case KEY_RANDOMIZE_DELAY: cbRandomizeDelay.setChecked((Boolean) value); break;
                        case KEY_RANDOMIZE_POSITION: cbRandomizePosition.setChecked((Boolean) value); break;
                        case KEY_EDGE_PROTECTION: cbEdgeProtection.setChecked((Boolean) value); break;
                        case KEY_SAFE_MODE: cbSafeMode.setChecked((Boolean) value); break;
                        case KEY_LOG_ACTIONS: cbLogActions.setChecked((Boolean) value); break;
                        case KEY_INDICATOR_COLOR:
                            for (int i = 0; i < colorNames.length; i++) {
                                if (colorNames[i].equals(value.toString())) {
                                    spIndicatorColor.setSelection(i);
                                    break;
                                }
                            }
                            break;
                    }
                } catch (Exception e) {
                    // skip invalid values
                }
            }
            Toast.makeText(this, "Settings imported", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Invalid JSON: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void manageScripts() {
        List<String> scripts = JsonScriptLoader.listSavedScripts(this);
        if (scripts.isEmpty()) {
            Toast.makeText(this, "No scripts found", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder sb = new StringBuilder("Saved Scripts:\n\n");
        for (String script : scripts) {
            sb.append("• ").append(script).append("\n");
        }
        sb.append("\n").append(scripts.size()).append(" script(s) total.");

        new android.app.AlertDialog.Builder(this)
                .setTitle("Script Manager")
                .setMessage(sb.toString())
                .setPositiveButton("OK", null)
                .setNeutralButton("Delete All", (d, w) -> deleteAllScripts())
                .show();
    }

    private void createSampleScript() {
        String sampleJson = JsonScriptLoader.generateSampleScript();
        boolean saved = JsonScriptLoader.importScriptFromString(this, sampleJson, "sample_script.json");
        Toast.makeText(this, saved ? "Sample script created" : "Failed", Toast.LENGTH_SHORT).show();
    }

    private void deleteAllScripts() {
        List<String> scripts = JsonScriptLoader.listSavedScripts(this);
        int count = 0;
        for (String script : scripts) {
            if (JsonScriptLoader.deleteScript(this, script)) count++;
        }
        Toast.makeText(this, count + " script(s) deleted", Toast.LENGTH_SHORT).show();
    }
}
