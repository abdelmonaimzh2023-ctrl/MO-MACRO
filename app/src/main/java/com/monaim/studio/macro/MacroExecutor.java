package com.monaim.studio.macro;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.monaim.studio.service.MacroAccessibilityService;

import java.util.ArrayList;
import java.util.List;

public class MacroExecutor {

    private static final String TAG = "MacroExecutor";
    private static MacroExecutor instance;
    private Context context;
    private Handler mainHandler;
    private boolean isExecuting = false;
    private boolean isPaused = false;

    private List<MacroAccessibilityService.MacroAction> actionQueue;
    private int currentIndex = 0;

    private OnMacroStateListener stateListener;

    public interface OnMacroStateListener {
        void onMacroStarted();
        void onMacroPaused();
        void onMacroResumed();
        void onMacroStopped();
        void onMacroError(String error);
    }

    private MacroExecutor(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.actionQueue = new ArrayList<>();
    }

    public static synchronized MacroExecutor getInstance(Context context) {
        if (instance == null) {
            instance = new MacroExecutor(context);
        }
        return instance;
    }

    public void setStateListener(OnMacroStateListener listener) {
        this.stateListener = listener;
    }

    public boolean isExecuting() {
        return isExecuting;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void loadAndExecute(Context ctx, String scriptFileName) {
        JsonScriptLoader.ScriptData script = JsonScriptLoader.loadScript(ctx, scriptFileName);
        if (script == null) {
            logError("Failed to load script: " + scriptFileName);
            return;
        }
        executeScript(script);
    }

    public void executeScript(JsonScriptLoader.ScriptData script) {
        if (script == null || script.getActions().isEmpty()) {
            logError("Script is empty or null");
            return;
        }

        if (!MacroAccessibilityService.getInstance().isServiceRunning()) {
            logError("Accessibility Service is not running");
            return;
        }

        actionQueue.clear();
        currentIndex = 0;

        for (Action action : script.getActions()) {
            MacroAccessibilityService.MacroAction macroAction = convertAction(action);
            if (macroAction != null) {
                actionQueue.add(macroAction);
            }
        }

        isExecuting = true;
        isPaused = false;

        if (stateListener != null) {
            stateListener.onMacroStarted();
        }

        MacroAccessibilityService.getInstance().startMacro(actionQueue);
    }

    public void executeJsonString(Context ctx, String jsonString) {
        if (!JsonScriptLoader.validateScriptJson(jsonString)) {
            logError("Invalid JSON script");
            return;
        }
        try {
            JsonScriptLoader.ScriptData script = JsonScriptLoader.loadScriptFromAssets(ctx, jsonString);
            if (script == null) {
                JsonScriptLoader.importScriptFromString(ctx, jsonString, "temp_script.json");
                script = JsonScriptLoader.loadScript(ctx, "temp_script.json");
            }
            if (script != null) {
                executeScript(script);
            }
        } catch (Exception e) {
            logError("Error parsing JSON: " + e.getMessage());
        }
    }

    public void pauseExecution() {
        if (!isExecuting || isPaused) return;

        isPaused = true;
        MacroAccessibilityService.getInstance().pauseMacro();

        if (stateListener != null) {
            stateListener.onMacroPaused();
        }
    }

    public void resumeExecution() {
        if (!isExecuting || !isPaused) return;

        isPaused = false;
        MacroAccessibilityService.getInstance().resumeMacro();

        if (stateListener != null) {
            stateListener.onMacroResumed();
        }
    }

    public void stopExecution() {
        if (!isExecuting) return;

        isExecuting = false;
        isPaused = false;
        actionQueue.clear();
        currentIndex = 0;

        if (MacroAccessibilityService.getInstance() != null) {
            MacroAccessibilityService.getInstance().stopMacro();
        }

        if (stateListener != null) {
            stateListener.onMacroStopped();
        }
    }

    public void togglePauseResume() {
        if (isPaused) {
            resumeExecution();
        } else {
            pauseExecution();
        }
    }

    private MacroAccessibilityService.MacroAction convertAction(Action action) {
        if (action == null || action.getType() == null) return null;

        switch (action.getType().toLowerCase()) {
            case "tap":
                return MacroAccessibilityService.MacroAction.tap(
                        action.getX(), action.getY(), action.getDelay());

            case "swipe":
                return MacroAccessibilityService.MacroAction.swipe(
                        action.getX(), action.getY(),
                        action.getEndX(), action.getEndY(),
                        action.getDuration(), action.getDelay());

            case "delay":
                return MacroAccessibilityService.MacroAction.delay(action.getDelay());

            case "repeat":
                return MacroAccessibilityService.MacroAction.repeat(
                        action.getRepeat(), currentIndex);

            default:
                Log.w(TAG, "Unknown action type: " + action.getType());
                return null;
        }
    }

    private void logError(String message) {
        Log.e(TAG, message);
        mainHandler.post(() -> {
            if (stateListener != null) {
                stateListener.onMacroError(message);
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        });
    }

    public static List<Action> generateSampleActions() {
        List<Action> actions = new ArrayList<>();

        Action tap = new Action();
        tap.setType("tap");
        tap.setX(540);
        tap.setY(960);
        tap.setDelay(100);
        actions.add(tap);

        Action delay = new Action();
        delay.setType("delay");
        delay.setDelay(500);
        actions.add(delay);

        Action swipe = new Action();
        swipe.setType("swipe");
        swipe.setX(540);
        swipe.setY(1200);
        swipe.setEndX(540);
        swipe.setEndY(600);
        swipe.setDuration(200);
        swipe.setDelay(50);
        actions.add(swipe);

        return actions;
    }
}
