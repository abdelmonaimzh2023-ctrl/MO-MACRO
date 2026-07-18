package com.monaim.studio.macro;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import com.monaim.studio.ui.UIHelper;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class ScriptExporter {

    private static final String TAG = "ScriptExporter";
    public static final String EXPORT_DIR = "MO_MACRO_Exports";

    public static boolean exportToFile(Context context, String jsonContent, String fileName) {
        if (!UIHelper.hasStoragePermission(context)) {
            if (context instanceof Activity) {
                UIHelper.requestStoragePermission((Activity) context);
            } else {
                UIHelper.toastError(context, "Storage permission required");
            }
            return false;
        }

        try {
            File exportDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), EXPORT_DIR);
            if (!exportDir.exists() && !exportDir.mkdirs()) {
                UIHelper.toastError(context, "Cannot create export folder");
                return false;
            }

            File file = new File(exportDir, fileName.endsWith(".json") ? fileName : fileName + ".json");

            if (file.exists()) {
                UIHelper.toastInfo(context, "File already exists: " + file.getName());
            }

            FileWriter writer = new FileWriter(file);
            writer.write(jsonContent);
            writer.close();

            // Media scan
            Intent scan = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            scan.setData(Uri.fromFile(file));
            context.sendBroadcast(scan);

            UIHelper.toastSuccess(context, "Exported to Downloads/" + EXPORT_DIR);
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Export failed: " + e.getMessage());
            UIHelper.toastError(context, "Export failed: " + e.getMessage());
            return false;
        }
    }

    public static boolean shareScript(Context context, String jsonContent, String title) {
        try {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("application/json");
            share.putExtra(Intent.EXTRA_TEXT, jsonContent);
            share.putExtra(Intent.EXTRA_SUBJECT, title);
            context.startActivity(Intent.createChooser(share, "Share Script via"));
            return true;
        } catch (Exception e) {
            UIHelper.toastError(context, "Share failed");
            return false;
        }
    }

    public static boolean copyToClipboard(Context context, String jsonContent) {
        try {
            ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("MO MACRO Script", jsonContent));
            UIHelper.toastSuccess(context, "Copied to clipboard");
            return true;
        } catch (Exception e) {
            UIHelper.toastError(context, "Copy failed");
            return false;
        }
    }

    public static String compileToJson(String name, String desc, List<Action> actions) {
        try {
            org.json.JSONObject root = new org.json.JSONObject();
            root.put("name", name != null ? name : "Script");
            root.put("description", desc != null ? desc : "");
            root.put("version", 1);
            org.json.JSONArray arr = new org.json.JSONArray();
            for (Action a : actions) {
                org.json.JSONObject obj = new org.json.JSONObject();
                obj.put("type", a.getType());
                obj.put("x", a.getX()); obj.put("y", a.getY());
                obj.put("endX", a.getEndX()); obj.put("endY", a.getEndY());
                obj.put("duration", a.getDuration()); obj.put("delay", a.getDelay());
                obj.put("repeat", a.getRepeat());
                arr.put(obj);
            }
            root.put("actions", arr);
            return root.toString(4);
        } catch (Exception e) {
            return null;
        }
    }

    public static List<String> listExported() {
        List<String> s = new ArrayList<>();
        File dir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), EXPORT_DIR);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, n) -> n.endsWith(".json"));
            if (files != null) for (File f : files) s.add(f.getName());
        }
        return s;
    }

    public static boolean deleteExported(String name) {
        File f = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), EXPORT_DIR, name);
        return f.exists() && f.delete();
    }
}
