package com.monaim.studio.macro;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

public class ScriptExporter {

    private static final String TAG = "ScriptExporter";
    public static final String EXPORT_DIR = "MO_MACRO_Exports";

    public static boolean exportToFile(Context context, String jsonContent, String fileName) {
        try {
            File exportDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), EXPORT_DIR);
            if (!exportDir.exists()) {
                if (!exportDir.mkdirs()) {
                    Log.e(TAG, "Failed to create export directory");
                    return false;
                }
            }

            File file = new File(exportDir, fileName.endsWith(".json") ? fileName : fileName + ".json");
            FileWriter writer = new FileWriter(file);
            writer.write(jsonContent);
            writer.close();

            Log.d(TAG, "Script exported to: " + file.getAbsolutePath());

            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScanIntent.setData(Uri.fromFile(file));
            context.sendBroadcast(mediaScanIntent);

            Toast.makeText(context, "Exported to Downloads/" + EXPORT_DIR + "/" + file.getName(),
                    Toast.LENGTH_LONG).show();
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Export failed: " + e.getMessage());
            Toast.makeText(context, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    public static boolean shareScript(Context context, String jsonContent, String title) {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/json");
            shareIntent.putExtra(Intent.EXTRA_TEXT, jsonContent);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, title);
            context.startActivity(Intent.createChooser(shareIntent, "Share Script"));
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Share failed: " + e.getMessage());
            Toast.makeText(context, "Share failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    public static boolean copyToClipboard(Context context, String jsonContent) {
        try {
            ClipboardManager clipboard = (ClipboardManager)
                    context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("MO MACRO Script", jsonContent);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "Script copied to clipboard", Toast.LENGTH_SHORT).show();
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Copy failed: " + e.getMessage());
            return false;
        }
    }

    public static String compileScriptToJson(String name, String description,
                                             List<Action> actions) {
        try {
            org.json.JSONObject root = new org.json.JSONObject();
            root.put("name", name != null ? name : "MO MACRO Script");
            root.put("description", description != null ? description :
                    "Script exported on " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                            java.util.Locale.US).format(new java.util.Date()));
            root.put("version", 1);

            org.json.JSONArray actionsArray = new org.json.JSONArray();
            for (Action action : actions) {
                org.json.JSONObject actionObj = new org.json.JSONObject();
                actionObj.put("type", action.getType());
                actionObj.put("x", action.getX());
                actionObj.put("y", action.getY());
                actionObj.put("endX", action.getEndX());
                actionObj.put("endY", action.getEndY());
                actionObj.put("duration", action.getDuration());
                actionObj.put("delay", action.getDelay());
                actionObj.put("repeat", action.getRepeat());
                actionsArray.put(actionObj);
            }
            root.put("actions", actionsArray);

            return root.toString(4);

        } catch (Exception e) {
            Log.e(TAG, "Compile failed: " + e.getMessage());
            return null;
        }
    }

    public static List<String> listExportedScripts() {
        List<String> scripts = new java.util.ArrayList<>();
        File exportDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), EXPORT_DIR);
        if (exportDir.exists() && exportDir.isDirectory()) {
            File[] files = exportDir.listFiles((d, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    scripts.add(file.getName());
                }
            }
        }
        return scripts;
    }

    public static boolean deleteExportedScript(String fileName) {
        File exportDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), EXPORT_DIR);
        File file = new File(exportDir, fileName.endsWith(".json") ? fileName : fileName + ".json");
        return file.exists() && file.delete();
    }

    public static boolean deleteAllExports() {
        File exportDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), EXPORT_DIR);
        if (exportDir.exists() && exportDir.isDirectory()) {
            File[] files = exportDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            return true;
        }
        return false;
    }
}
