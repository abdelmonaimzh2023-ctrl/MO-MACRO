package com.monaim.studio.macro;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.monaim.studio.service.MacroAccessibilityService;

import java.util.ArrayList;
import java.util.List;

public class MacroExecutor {

    private static final String TAG = "MacroExecutor";
    private static MacroExecutor instance;
    private Context context;
    private Handler handler;
    private boolean isExecuting = false;
    private boolean isPaused = false;
    private int currentIndex = 0;
    private List<Action> actionList;
    private OnMacroStateListener listener;

    public interface OnMacroStateListener {
        void onStarted();
        void onPaused();
        void onResumed();
        void onStopped();
        void onError(String msg);
    }

    private MacroExecutor(Context ctx) {
        this.context = ctx.getApplicationContext();
        this.handler = new Handler(Looper.getMainLooper());
        this.actionList = new ArrayList<>();
    }

    public static synchronized MacroExecutor getInstance(Context ctx) {
        if (instance == null) instance = new MacroExecutor(ctx);
        return instance;
    }

    public void setListener(OnMacroStateListener l) { this.listener = l; }

    public boolean isRunning() { return isExecuting; }
    public boolean isPaused() { return isPaused; }

    public void loadAndExecute(Context ctx, String fileName) {
        JsonScriptLoader.ScriptData script = JsonScriptLoader.loadScript(ctx, fileName);
        if (script == null || script.getActions().isEmpty()) {
            error("Failed to load script: " + fileName);
            return;
        }
        execute(script.getActions());
    }

    public void execute(List<Action> actions) {
        if (actions == null || actions.isEmpty()) { error("No actions to execute"); return; }
        if (MacroAccessibilityService.getInstance() == null) { error("Service not running"); return; }

        stop();
        actionList = new ArrayList<>(actions);
        currentIndex = 0;
        isExecuting = true;
        isPaused = false;

        if (listener != null) listener.onStarted();
        runNext();
    }

    public void pause() {
        if (!isExecuting || isPaused) return;
        isPaused = true;
        if (listener != null) listener.onPaused();
    }

    public void resume() {
        if (!isExecuting || !isPaused) return;
        isPaused = false;
        if (listener != null) listener.onResumed();
        runNext();
    }

    public void togglePauseResume() {
        if (isPaused) resume(); else pause();
    }

    public void stop() {
        isExecuting = false;
        isPaused = false;
        actionList.clear();
        currentIndex = 0;
        handler.removeCallbacksAndMessages(null);
        if (listener != null) listener.onStopped();
    }

    private void runNext() {
        if (!isExecuting || isPaused) return;
        if (currentIndex >= actionList.size()) { stop(); return; }

        Action action = actionList.get(currentIndex);
        currentIndex++;

        long delay = action.getDelay() > 0 ? action.getDelay() : 50;

        switch (action.getType() != null ? action.getType() : "delay") {
            case "tap":
                MacroAccessibilityService.getInstance().performTap(
                        action.getX(), action.getY(), delay, this::runNext);
                break;

            case "swipe":
                MacroAccessibilityService.getInstance().performSwipe(
                        action.getX(), action.getY(),
                        action.getEndX(), action.getEndY(),
                        action.getDuration() > 0 ? action.getDuration() : 100,
                        delay, this::runNext);
                break;

            case "repeat":
                if (action.getRepeat() > 1) {
                    action.setRepeat(action.getRepeat() - 1);
                    currentIndex = 0;
                    handler.postDelayed(this::runNext, delay);
                } else {
                    handler.postDelayed(this::runNext, delay);
                }
                break;

            default:
                handler.postDelayed(this::runNext, delay);
                break;
        }
    }

    private void error(String msg) {
        Log.e(TAG, msg);
        if (listener != null) listener.onError(msg);
    }
}
