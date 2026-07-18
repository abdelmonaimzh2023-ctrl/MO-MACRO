package com.monaim.studio.macro;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class JsonScriptLoader {

    private static final String TAG = "JsonScriptLoader";
    private static final String SCRIPTS_DIR = "mo_macro_scripts";

    public static class ScriptData {
        private String name;
        private String description;
        private int version;
        private List<Action> actions;

        public ScriptData() {
            actions = new ArrayList<>();
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public int getVersion() { return version; }
        public void setVersion(int version) { this.version = version; }

        public List<Action> getActions() { return actions; }
        public void setActions(List<Action> actions) { this.actions = actions; }

        public void addAction(Action action) { this.actions.add(action); }
    }

    public static boolean saveScript(Context context, ScriptData script, String fileName) {
        try {
            File dir = new File(context.getFilesDir(), SCRIPTS_DIR);
            if (!dir.exists()) dir.mkdirs();

            JSONObject root = new JSONObject();
            root.put("name", script.getName());
            root.put("description", script.getDescription());
            root.put("version", script.getVersion());

            JSONArray actionsArray = new JSONArray();
            for (Action action : script.getActions()) {
                JSONObject actionObj = new JSONObject();
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

            File file = new File(dir, fileName.endsWith(".json") ? fileName : fileName + ".json");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(root.toString(4).getBytes());
            fos.close();

            Log.d(TAG, "Script saved: " + file.getAbsolutePath());
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error saving script: " + e.getMessage());
            return false;
        }
    }

    public static ScriptData loadScript(Context context, String fileName) {
        try {
            File dir = new File(context.getFilesDir(), SCRIPTS_DIR);
            File file = new File(dir, fileName.endsWith(".json") ? fileName : fileName + ".json");

            if (!file.exists()) {
                Log.e(TAG, "Script file not found: " + file.getAbsolutePath());
                return null;
            }

            FileInputStream fis = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            fis.close();

            return parseJson(sb.toString());

        } catch (Exception e) {
            Log.e(TAG, "Error loading script: " + e.getMessage());
            return null;
        }
    }

    public static ScriptData loadScriptFromAssets(Context context, String assetFileName) {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(context.getAssets().open("scripts/" + assetFileName)));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            return parseJson(sb.toString());

        } catch (Exception e) {
            Log.e(TAG, "Error loading script from assets: " + e.getMessage());
            return null;
        }
    }

    public static List<String> listSavedScripts(Context context) {
        List<String> scripts = new ArrayList<>();
        File dir = new File(context.getFilesDir(), SCRIPTS_DIR);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    scripts.add(file.getName());
                }
            }
        }
        return scripts;
    }

    public static boolean deleteScript(Context context, String fileName) {
        File dir = new File(context.getFilesDir(), SCRIPTS_DIR);
        File file = new File(dir, fileName.endsWith(".json") ? fileName : fileName + ".json");
        return file.exists() && file.delete();
    }

    public static String exportScriptToString(Context context, String fileName) {
        try {
            File dir = new File(context.getFilesDir(), SCRIPTS_DIR);
            File file = new File(dir, fileName.endsWith(".json") ? fileName : fileName + ".json");
            if (!file.exists()) return null;

            FileInputStream fis = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            fis.close();
            return sb.toString();

        } catch (Exception e) {
            Log.e(TAG, "Error exporting script: " + e.getMessage());
            return null;
        }
    }

    public static boolean importScriptFromString(Context context, String jsonString, String fileName) {
        try {
            File dir = new File(context.getFilesDir(), SCRIPTS_DIR);
            if (!dir.exists()) dir.mkdirs();

            JSONObject testParse = new JSONObject(jsonString);

            File file = new File(dir, fileName.endsWith(".json") ? fileName : fileName + ".json");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(jsonString.getBytes());
            fos.close();

            Log.d(TAG, "Script imported: " + file.getAbsolutePath());
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error importing script: " + e.getMessage());
            return false;
        }
    }

    private static ScriptData parseJson(String jsonString) throws JSONException {
        JSONObject root = new JSONObject(jsonString);
        ScriptData script = new ScriptData();

        script.setName(root.optString("name", "Unnamed Script"));
        script.setDescription(root.optString("description", ""));
        script.setVersion(root.optInt("version", 1));

        JSONArray actionsArray = root.optJSONArray("actions");
        if (actionsArray != null) {
            for (int i = 0; i < actionsArray.length(); i++) {
                JSONObject actionObj = actionsArray.getJSONObject(i);
                Action action = new Action();

                action.setType(actionObj.optString("type", "tap"));
                action.setX((float) actionObj.optDouble("x", 0));
                action.setY((float) actionObj.optDouble("y", 0));
                action.setEndX((float) actionObj.optDouble("endX", 0));
                action.setEndY((float) actionObj.optDouble("endY", 0));
                action.setDuration(actionObj.optLong("duration", 100));
                action.setDelay(actionObj.optLong("delay", 50));
                action.setRepeat(actionObj.optInt("repeat", 0));

                script.addAction(action);
            }
        }

        return script;
    }

    public static String generateSampleScript() {
        try {
            JSONObject root = new JSONObject();
            root.put("name", "Sample Script");
            root.put("description", "Example script for MO MACRO");
            root.put("version", 1);

            JSONArray actions = new JSONArray();

            JSONObject tap1 = new JSONObject();
            tap1.put("type", "tap");
            tap1.put("x", 540);
            tap1.put("y", 960);
            tap1.put("delay", 100);
            actions.put(tap1);

            JSONObject delay1 = new JSONObject();
            delay1.put("type", "delay");
            delay1.put("delay", 500);
            actions.put(delay1);

            JSONObject swipe1 = new JSONObject();
            swipe1.put("type", "swipe");
            swipe1.put("x", 540);
            swipe1.put("y", 1200);
            swipe1.put("endX", 540);
            swipe1.put("endY", 600);
            swipe1.put("duration", 200);
            swipe1.put("delay", 50);
            actions.put(swipe1);

            JSONObject tap2 = new JSONObject();
            tap2.put("type", "tap");
            tap2.put("x", 800);
            tap2.put("y", 400);
            tap2.put("delay", 1000);
            actions.put(tap2);

            root.put("actions", actions);
            return root.toString(4);

        } catch (Exception e) {
            return "{}";
        }
    }

    public static boolean validateScriptJson(String jsonString) {
        try {
            JSONObject root = new JSONObject(jsonString);
            if (!root.has("actions")) return false;
            JSONArray actions = root.getJSONArray("actions");
            for (int i = 0; i < actions.length(); i++) {
                JSONObject action = actions.getJSONObject(i);
                String type = action.optString("type", "");
                if (!type.equals("tap") && !type.equals("swipe")
                        && !type.equals("delay") && !type.equals("repeat")) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
