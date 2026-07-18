package com.monaim.studio.macro;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class TouchRecorder {

    private static final String TAG = "TouchRecorder";
    private static final String PREFS_RECORDING = "mo_macro_recording";

    private Context context;
    private Handler handler;
    private boolean isRecording = false;
    private long recordingStartTime = 0;
    private List<Action> recordedActions;
    private List<Long> actionTimestamps;
    private WindowManager windowManager;
    private View recordingIndicator;
    private TextView tvRecordingInfo;
    private OnRecordingListener listener;

    public interface OnRecordingListener {
        void onRecordingStarted();
        void onRecordingStopped(List<Action> actions);
        void onActionRecorded(Action action, int count);
    }

    public TouchRecorder(Context context) {
        this.context = context;
        this.handler = new Handler(Looper.getMainLooper());
        this.recordedActions = new ArrayList<>();
        this.actionTimestamps = new ArrayList<>();
    }

    public void setListener(OnRecordingListener listener) {
        this.listener = listener;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public int getActionCount() {
        return recordedActions.size();
    }

    public void startRecording() {
        recordedActions.clear();
        actionTimestamps.clear();
        isRecording = true;
        recordingStartTime = System.currentTimeMillis();

        Log.d(TAG, "Recording started");
        if (listener != null) {
            listener.onRecordingStarted();
        }
    }

    public void stopRecording() {
        isRecording = false;
        long totalDuration = System.currentTimeMillis() - recordingStartTime;

        Log.d(TAG, "Recording stopped. Total actions: " + recordedActions.size()
                + ", Duration: " + totalDuration + "ms");

        List<Action> result = new ArrayList<>(recordedActions);
        if (listener != null) {
            listener.onRecordingStopped(result);
        }
    }

    public void recordTouchEvent(MotionEvent event) {
        if (!isRecording) return;

        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - recordingStartTime;

        float x = event.getRawX();
        float y = event.getRawY();

        Action action = null;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                action = new Action();
                action.setType("tap_down");
                action.setX(x);
                action.setY(y);
                action.setDelay(0);
                Log.d(TAG, "Recorded DOWN at (" + x + ", " + y + ")");
                break;

            case MotionEvent.ACTION_UP:
                action = new Action();
                action.setType("tap_up");
                action.setX(x);
                action.setY(y);

                long downTime = actionTimestamps.isEmpty() ? elapsed :
                        actionTimestamps.get(actionTimestamps.size() - 1);
                action.setDelay(elapsed - downTime);
                Log.d(TAG, "Recorded UP at (" + x + ", " + y + ")");
                break;

            case MotionEvent.ACTION_MOVE:
                action = new Action();
                action.setType("move");
                action.setX(x);
                action.setY(y);
                action.setDelay(0);
                break;
        }

        if (action != null) {
            recordedActions.add(action);
            actionTimestamps.add(elapsed);

            if (listener != null) {
                listener.onActionRecorded(action, recordedActions.size());
            }
        }
    }

    public List<Action> compileRecordedActions() {
        List<Action> compiled = new ArrayList<>();

        float lastX = 0, lastY = 0;
        float swipeStartX = 0, swipeStartY = 0;
        long swipeStartTime = 0;
        boolean isSwiping = false;
        long lastEventTime = 0;

        for (int i = 0; i < recordedActions.size(); i++) {
            Action action = recordedActions.get(i);
            long timestamp = actionTimestamps.get(i);

            switch (action.getType()) {
                case "tap_down":
                    lastX = action.getX();
                    lastY = action.getY();
                    lastEventTime = timestamp;

                    if (i + 1 < recordedActions.size()) {
                        Action nextAction = recordedActions.get(i + 1);
                        if ("move".equals(nextAction.getType())) {
                            isSwiping = true;
                            swipeStartX = lastX;
                            swipeStartY = lastY;
                            swipeStartTime = timestamp;
                            i++;
                            continue;
                        }
                    }

                    Action tapAction = new Action();
                    tapAction.setType("tap");
                    tapAction.setX(lastX);
                    tapAction.setY(lastY);
                    tapAction.setDelay(lastEventTime > 0 ? timestamp - lastEventTime : 50);
                    compiled.add(tapAction);
                    break;

                case "move":
                    if (isSwiping) {
                        lastX = action.getX();
                        lastY = action.getY();

                        if (i + 1 >= recordedActions.size() ||
                                !"move".equals(recordedActions.get(i + 1).getType())) {

                            Action swipeAction = new Action();
                            swipeAction.setType("swipe");
                            swipeAction.setX(swipeStartX);
                            swipeAction.setY(swipeStartY);
                            swipeAction.setEndX(lastX);
                            swipeAction.setEndY(lastY);
                            swipeAction.setDuration(timestamp - swipeStartTime);
                            swipeAction.setDelay(0);
                            compiled.add(swipeAction);
                            isSwiping = false;
                        }
                    }
                    break;

                case "tap_up":
                    if (!isSwiping) {
                        if (!compiled.isEmpty()) {
                            compiled.get(compiled.size() - 1).setDelay(timestamp - lastEventTime);
                        }
                    }
                    lastEventTime = timestamp;
                    break;
            }
        }

        Log.d(TAG, "Compiled " + compiled.size() + " actions from " + recordedActions.size() + " events");
        return compiled;
    }

    public void saveRecordingSession(List<Action> actions) {
        if (actions == null || actions.isEmpty()) return;

        SharedPreferences prefs = context.getSharedPreferences(PREFS_RECORDING, Context.MODE_PRIVATE);
        int sessionId = prefs.getInt("session_count", 0) + 1;

        JsonScriptLoader.ScriptData script = new JsonScriptLoader.ScriptData();
        script.setName("Recording #" + sessionId);
        script.setDescription("Recorded on " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                java.util.Locale.US).format(new java.util.Date()));
        script.setVersion(1);
        script.setActions(actions);

        boolean saved = JsonScriptLoader.saveScript(context, script,
                "recording_" + sessionId + ".json");

        if (saved) {
            prefs.edit().putInt("session_count", sessionId).apply();
            Log.d(TAG, "Recording session " + sessionId + " saved");
        }
    }

    public static List<Action> processRawTouchEvents(List<float[]> events) {
        List<Action> actions = new ArrayList<>();

        for (int i = 0; i < events.size(); i++) {
            float[] event = events.get(i);
            Action action = new Action();
            action.setType("tap");
            action.setX(event[0]);
            action.setY(event[1]);
            action.setDelay(i < events.size() - 1 ? (long) event[2] : 100);
            actions.add(action);
        }

        return actions;
    }

    public void clearRecording() {
        recordedActions.clear();
        actionTimestamps.clear();
        recordingStartTime = 0;
    }
}
