package com.monaim.studio.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class UIHelper {

    private static final int STORAGE_PERMISSION_CODE = 2001;
    private static ProgressDialog currentProgressDialog;
    private static Toast currentToast;

    // ==================== LOADING DIALOGS ====================

    public static ProgressDialog showLoading(Context context, String message) {
        dismissLoading();
        ProgressDialog pd = new ProgressDialog(context);
        pd.setMessage(message != null ? message : "Please wait...");
        pd.setCancelable(false);
        pd.setIndeterminate(true);
        pd.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        try {
            pd.show();
            currentProgressDialog = pd;
        } catch (Exception ignored) {}
        return pd;
    }

    public static void dismissLoading() {
        try {
            if (currentProgressDialog != null && currentProgressDialog.isShowing()) {
                currentProgressDialog.dismiss();
            }
        } catch (Exception ignored) {}
        currentProgressDialog = null;
    }

    public static void dismissLoadingDelayed(long delayMs) {
        new Handler(Looper.getMainLooper()).postDelayed(UIHelper::dismissLoading, delayMs);
    }

    // ==================== CONFIRMATION DIALOGS ====================

    public interface ConfirmCallback {
        void onConfirm();
        default void onCancel() {}
    }

    public static void showConfirm(Context context, String title, String message,
                                   String positiveText, String negativeText,
                                   ConfirmCallback callback) {
        try {
            new AlertDialog.Builder(context)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(positiveText != null ? positiveText : "Yes",
                            (dialog, which) -> {
                                if (callback != null) callback.onConfirm();
                            })
                    .setNegativeButton(negativeText != null ? negativeText : "No",
                            (dialog, which) -> {
                                if (callback != null) callback.onCancel();
                            })
                    .setCancelable(false)
                    .show();
        } catch (Exception e) {
            if (callback != null) callback.onConfirm();
        }
    }

    public static void showDeleteConfirm(Context context, String itemName, ConfirmCallback callback) {
        showConfirm(context, "Delete Confirmation",
                "Are you sure you want to delete \"" + itemName + "\"?\nThis action cannot be undone.",
                "Delete", "Cancel", callback);
    }

    public static void showResetConfirm(Context context, ConfirmCallback callback) {
        showConfirm(context, "Reset Confirmation",
                "Reset all settings to defaults?\nThis will not delete your scripts.",
                "Reset", "Cancel", callback);
    }

    public static void showDeleteAllConfirm(Context context, ConfirmCallback callback) {
        showConfirm(context, "Delete All Scripts",
                "Are you sure you want to delete ALL scripts?\nThis cannot be undone!",
                "Delete All", "Cancel", callback);
    }

    public static void showOverwriteConfirm(Context context, String fileName, ConfirmCallback callback) {
        showConfirm(context, "File Exists",
                "\"" + fileName + "\" already exists.\nDo you want to overwrite it?",
                "Overwrite", "Cancel", callback);
    }

    // ==================== TOASTS ====================

    public static void toast(Context context, String message) {
        try {
            if (currentToast != null) currentToast.cancel();
            currentToast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
            currentToast.show();
        } catch (Exception ignored) {}
    }

    public static void toastLong(Context context, String message) {
        try {
            if (currentToast != null) currentToast.cancel();
            currentToast = Toast.makeText(context, message, Toast.LENGTH_LONG);
            currentToast.show();
        } catch (Exception ignored) {}
    }

    public static void toastSuccess(Context context, String message) {
        toast(context, "✓ " + message);
    }

    public static void toastError(Context context, String message) {
        toastLong(context, "✗ " + message);
    }

    public static void toastInfo(Context context, String message) {
        toast(context, "ⓘ " + message);
    }

    // ==================== INFO DIALOG ====================

    public static void showInfo(Context context, String title, String message) {
        try {
            new AlertDialog.Builder(context)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show();
        } catch (Exception ignored) {}
    }

    // ==================== STORAGE PERMISSION ====================

    public static boolean hasStoragePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return true;
        }
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestStoragePermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            toastInfo(activity, "Storage permission not needed on this device.");
            return;
        }
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            showConfirm(activity, "Storage Permission",
                    "MO MACRO needs storage permission to export and share scripts.",
                    "Grant", "Cancel",
                    new ConfirmCallback() {
                        @Override
                        public void onConfirm() {
                            ActivityCompat.requestPermissions(activity,
                                    new String[]{
                                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                            Manifest.permission.READ_EXTERNAL_STORAGE
                                    }, STORAGE_PERMISSION_CODE);
                        }
                    });
        } else {
            ActivityCompat.requestPermissions(activity,
                    new String[]{
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    }, STORAGE_PERMISSION_CODE);
        }
    }

    public static boolean isStoragePermissionGranted(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == STORAGE_PERMISSION_CODE) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) return false;
            }
            return true;
        }
        return false;
    }

    // ==================== CUSTOM THEMED DIALOG BUILDER ====================

    public static AlertDialog buildThemedDialog(Context context, String title, String message,
                                                String positive, String negative,
                                                android.content.DialogInterface.OnClickListener positiveListener,
                                                android.content.DialogInterface.OnClickListener negativeListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(positive, positiveListener);
        if (negative != null) builder.setNegativeButton(negative, negativeListener);
        builder.setCancelable(false);
        return builder.create();
    }

    // ==================== PROGRESS OVERLAY (in-app) ====================

    public static View createProgressOverlay(Context context, String message) {
        LinearLayout overlay = new LinearLayout(context);
        overlay.setOrientation(LinearLayout.VERTICAL);
        overlay.setGravity(Gravity.CENTER);
        overlay.setBackgroundColor(Color.argb(200, 26, 26, 46));
        overlay.setClickable(true);
        overlay.setFocusable(true);

        TextView tv = new TextView(context);
        tv.setText(message != null ? message : "Processing...");
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(16);
        tv.setPadding(32, 32, 32, 32);
        tv.setGravity(Gravity.CENTER);

        android.widget.ProgressBar pb = new android.widget.ProgressBar(context);
        pb.setIndeterminate(true);
        pb.setPadding(0, 0, 0, 16);

        overlay.addView(pb);
        overlay.addView(tv);

        return overlay;
    }

    public static void showOverlayLoading(LinearLayout rootLayout, String message) {
        if (rootLayout == null) return;
        View existing = rootLayout.findViewWithTag("loading_overlay");
        if (existing != null) rootLayout.removeView(existing);

        View overlay = createProgressOverlay(rootLayout.getContext(), message);
        overlay.setTag("loading_overlay");
        rootLayout.addView(overlay, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
    }

    public static void dismissOverlayLoading(LinearLayout rootLayout) {
        if (rootLayout == null) return;
        View overlay = rootLayout.findViewWithTag("loading_overlay");
        if (overlay != null) rootLayout.removeView(overlay);
    }
}
