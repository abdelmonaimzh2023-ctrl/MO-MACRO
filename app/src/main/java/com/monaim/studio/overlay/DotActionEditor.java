package com.monaim.studio.overlay;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.monaim.studio.R;
import com.monaim.studio.macro.MacroExecutor;
import com.monaim.studio.macro.TouchRecorder;
import com.monaim.studio.macro.Action;
import com.monaim.studio.service.MacroAccessibilityService;

import java.util.ArrayList;
import java.util.List;

public class DotActionEditor {

    private Context context;
    private MacroDot dot;
    private SharedPreferences prefs;
    private AlertDialog dialog;
    private View rootView;

    private Button btnTap, btnSwipe, btnHold, btnAuto;
    private EditText etX1, etY1, etX2, etY2, etDuration, etRepeatCount, etRepeatDelay;
    private LinearLayout llEndCoords, llDuration, llRepeat;
    private Button btnRecord, btnTest, btnSave, btnDelete, btnClose;
    private TextView tvTitle;

    private String currentType = "hold_swipe";
    private OnEditorListener listener;

    public interface OnEditorListener {
        void onSaved();
        void onDeleted(MacroDot dot);
        void onClosed();
    }

    public DotActionEditor(Context context, MacroDot dot, OnEditorListener listener) {
        this.context = context;
        this.dot = dot;
        this.listener = listener;
        this.prefs = context.getSharedPreferences("mo_macro_dots", Context.MODE_PRIVATE);
    }

    public void show() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            rootView = LayoutInflater.from(context).inflate(R.layout.popup_action_editor, null);
            builder.setView(rootView);
            builder.setCancelable(false);
            dialog = builder.create();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setType(
                    android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE
                );
            }

            bindViews();
            loadCurrentSettings();
            setupListeners();
            dialog.show();
        } catch (Exception e) {
            Toast.makeText(context, "Cannot open editor", Toast.LENGTH_SHORT).show();
        }
    }

    private void bindViews() {
        tvTitle = rootView.findViewById(R.id.tv_dot_title);
        btnTap = rootView.findViewById(R.id.btn_type_tap);
        btnSwipe = rootView.findViewById(R.id.btn_type_swipe);
        btnHold = rootView.findViewById(R.id.btn_type_hold);
        btnAuto = rootView.findViewById(R.id.btn_type_auto);
        etX1 = rootView.findViewById(R.id.et_x1);
        etY1 = rootView.findViewById(R.id.et_y1);
        etX2 = rootView.findViewById(R.id.et_x2);
        etY2 = rootView.findViewById(R.id.et_y2);
        etDuration = rootView.findViewById(R.id.et_duration);
        etRepeatCount = rootView.findViewById(R.id.et_repeat_count);
        etRepeatDelay = rootView.findViewById(R.id.et_repeat_delay);
        llEndCoords = rootView.findViewById(R.id.ll_end_coords);
        llDuration = rootView.findViewById(R.id.ll_duration);
        llRepeat = rootView.findViewById(R.id.ll_repeat);
        btnRecord = rootView.findViewById(R.id.btn_record_position);
        btnTest = rootView.findViewById(R.id.btn_test_action);
        btnSave = rootView.findViewById(R.id.btn_save_action);
        btnDelete = rootView.findViewById(R.id.btn_delete_dot);
        btnClose = rootView.findViewById(R.id.btn_close_editor);
    }

    private void loadCurrentSettings() {
        currentType = dot.getActionType();
        etX1.setText(String.valueOf(dot.getActionX1()));
        etY1.setText(String.valueOf(dot.getActionY1()));
        etX2.setText(String.valueOf(dot.getActionX2()));
        etY2.setText(String.valueOf(dot.getActionY2()));
        etDuration.setText(String.valueOf(dot.getActionDuration()));
        etRepeatCount.setText(String.valueOf(prefs.getInt("dot_" + dot.getDotId() + "_repeat_count", 0)));
        etRepeatDelay.setText(String.valueOf(prefs.getLong("dot_" + dot.getDotId() + "_repeat_delay", 100)));
        tvTitle.setText("Configure: " + dot.getDotId());
        updateTypeButtons();
        updateVisibility();
    }

    private void updateTypeButtons() {
        btnTap.setBackgroundColor(currentType.equals("tap") ? Color.parseColor("#E94560") : Color.parseColor("#444444"));
        btnSwipe.setBackgroundColor(currentType.equals("swipe") ? Color.parseColor("#E94560") : Color.parseColor("#444444"));
        btnHold.setBackgroundColor(currentType.equals("hold_swipe") ? Color.parseColor("#E94560") : Color.parseColor("#444444"));
        btnAuto.setBackgroundColor(currentType.equals("auto_click") ? Color.parseColor("#E94560") : Color.parseColor("#444444"));
    }

    private void updateVisibility() {
        boolean showEnd = currentType.equals("swipe") || currentType.equals("hold_swipe");
        boolean showDuration = currentType.equals("swipe") || currentType.equals("hold_swipe");
        boolean showRepeat = currentType.equals("auto_click");

        llEndCoords.setVisibility(showEnd ? View.VISIBLE : View.GONE);
        llDuration.setVisibility(showDuration ? View.VISIBLE : View.GONE);
        llRepeat.setVisibility(showRepeat ? View.VISIBLE : View.GONE);
    }

    private void setupListeners() {
        btnTap.setOnClickListener(v -> { currentType = "tap"; updateTypeButtons(); updateVisibility(); });
        btnSwipe.setOnClickListener(v -> { currentType = "swipe"; updateTypeButtons(); updateVisibility(); });
        btnHold.setOnClickListener(v -> { currentType = "hold_swipe"; updateTypeButtons(); updateVisibility(); });
        btnAuto.setOnClickListener(v -> { currentType = "auto_click"; updateTypeButtons(); updateVisibility(); });

        btnRecord.setOnClickListener(v -> {
            dialog.dismiss();
            Toast.makeText(context, "Tap anywhere on screen to set position...", Toast.LENGTH_LONG).show();
            TouchRecorder.getInstance(context).waitForPosition((x, y) -> {
                etX1.setText(String.valueOf((int)x));
                etY1.setText(String.valueOf((int)y));
                if (currentType.equals("swipe") || currentType.equals("hold_swipe")) {
                    Toast.makeText(context, "Now tap end position...", Toast.LENGTH_LONG).show();
                    TouchRecorder.getInstance(context).waitForPosition((x2, y2) -> {
                        etX2.setText(String.valueOf((int)x2));
                        etY2.setText(String.valueOf((int)y2));
                        show();
                    });
                } else {
                    show();
                }
            });
        });

        btnTest.setOnClickListener(v -> {
            testAction();
        });

        btnSave.setOnClickListener(v -> {
            saveAction();
            if (listener != null) listener.onSaved();
            dialog.dismiss();
        });

        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                .setTitle("Delete Dot")
                .setMessage("Remove \"" + dot.getDotId() + "\" permanently?")
                .setPositiveButton("Delete", (d, w) -> {
                    dialog.dismiss();
                    if (listener != null) listener.onDeleted(dot);
                })
                .setNegativeButton("Cancel", null)
                .show();
        });

        btnClose.setOnClickListener(v -> {
            dialog.dismiss();
            if (listener != null) listener.onClosed();
        });
    }

    private void testAction() {
        try {
            MacroAccessibilityService service = MacroAccessibilityService.getInstance();
            if (service == null) {
                Toast.makeText(context, "Accessibility Service not running", Toast.LENGTH_SHORT).show();
                return;
            }

            float x1 = parseFloat(etX1.getText().toString(), 540);
            float y1 = parseFloat(etY1.getText().toString(), 1200);
            float x2 = parseFloat(etX2.getText().toString(), 540);
            float y2 = parseFloat(etY2.getText().toString(), 600);
            long dur = parseLong(etDuration.getText().toString(), 100);
            int repeat = parseInt(etRepeatCount.getText().toString(), 0);
            long repeatDelay = parseLong(etRepeatDelay.getText().toString(), 100);

            switch (currentType) {
                case "tap":
                    service.performTap(x1, y1, 100, null);
                    Toast.makeText(context, "Tap executed", Toast.LENGTH_SHORT).show();
                    break;
                case "swipe":
                    service.performSwipe(x1, y1, x2, y2, dur, 100, null);
                    Toast.makeText(context, "Swipe executed", Toast.LENGTH_SHORT).show();
                    break;
                case "hold_swipe":
                    Toast.makeText(context, "Hold the dot to test Hold Swipe", Toast.LENGTH_SHORT).show();
                    break;
                case "auto_click":
                    executeAutoClickTest(service, x1, y1, repeat, repeatDelay);
                    break;
            }
        } catch (Exception e) {
            Toast.makeText(context, "Test failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void executeAutoClickTest(MacroAccessibilityService service, float x, float y, int count, long delay) {
        if (count <= 0) count = 3;
        final int[] remaining = {count};
        Runnable[] runner = new Runnable[1];
        runner[0] = () -> {
            if (remaining[0] <= 0) {
                Toast.makeText(context, "Auto click test done", Toast.LENGTH_SHORT).show();
                return;
            }
            service.performTap(x, y, delay, () -> {
                remaining[0]--;
                runner[0].run();
            });
        };
        runner[0].run();
        Toast.makeText(context, "Auto click started (" + count + " taps)", Toast.LENGTH_SHORT).show();
    }

    private void saveAction() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("dot_" + dot.getDotId() + "_action_type", currentType);
        editor.putInt("dot_" + dot.getDotId() + "_x1", parseInt(etX1.getText().toString(), 540));
        editor.putInt("dot_" + dot.getDotId() + "_y1", parseInt(etY1.getText().toString(), 1200));
        editor.putInt("dot_" + dot.getDotId() + "_x2", parseInt(etX2.getText().toString(), 540));
        editor.putInt("dot_" + dot.getDotId() + "_y2", parseInt(etY2.getText().toString(), 600));
        editor.putLong("dot_" + dot.getDotId() + "_duration", parseLong(etDuration.getText().toString(), 100));
        editor.putInt("dot_" + dot.getDotId() + "_repeat_count", parseInt(etRepeatCount.getText().toString(), 0));
        editor.putLong("dot_" + dot.getDotId() + "_repeat_delay", parseLong(etRepeatDelay.getText().toString(), 100));
        editor.apply();

        dot.loadPrefs(prefs);
        dot.refresh();
        Toast.makeText(context, "Saved: " + currentType, Toast.LENGTH_SHORT).show();
    }

    private float parseFloat(String s, float def) { try { return Float.parseFloat(s); } catch (Exception e) { return def; } }
    private long parseLong(String s, long def) { try { return Long.parseLong(s); } catch (Exception e) { return def; } }
    private int parseInt(String s, int def) { try { return Integer.parseInt(s); } catch (Exception e) { return def; } }
}
