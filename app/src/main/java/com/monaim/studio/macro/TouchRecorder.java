package com.monaim.studio.macro;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class TouchRecorder {

    private static final String TAG = "TouchRecorder";
    private static TouchRecorder instance;
    private Context context;
    private Handler handler;
    private boolean isRecording = false;
    private boolean waitingForPosition = false;
    private List<Action> recordedActions;
    private OnPositionPickedListener positionListener;

    public interface OnPositionPickedListener {
        void onPositionPicked(float x, float y);
    }

    private TouchRecorder(Context ctx) {
        this.context = ctx.getApplicationContext();
        this.handler = new Handler(Looper.getMainLooper());
        this.recordedActions = new ArrayList<>();
    }

    public static synchronized TouchRecorder getInstance(Context ctx) {
        if (instance == null) instance = new TouchRecorder(ctx);
        return instance;
    }

    public boolean isRecording() { return isRecording; }
    public boolean isWaitingForPosition() { return waitingForPosition; }
    public int getActionCount() { return recordedActions != null ? recordedActions.size() : 0; }

    // ========== WAIT FOR SINGLE POSITION ==========
    public synchronized void waitForPosition(OnPositionPickedListener listener) {
        this.positionListener = listener;
        this.waitingForPosition = true;
    }

    public synchronized void cancelWaiting() {
        this.waitingForPosition = false;
        this.positionListener = null;
    }

    public synchronized void submitPosition(float x, float y) {
        if (!waitingForPosition) return;
        waitingForPosition = false;
        OnPositionPickedListener cb = positionListener;
        positionListener = null;
        if (cb != null) {
            handler.post(() -> cb.onPositionPicked(x, y));
        }
    }

    // ========== RECORDING ==========
    public synchronized void startRecording() {
        recordedActions.clear();
        isRecording = true;
    }

    public synchronized void stopRecording() {
        isRecording = false;
    }

    public synchronized void recordAction(Action action) {
        if (!isRecording || action == null) return;
        recordedActions.add(action);
    }

    public synchronized List<Action> getRecordedActions() {
        return new ArrayList<>(recordedActions);
    }

    public synchronized void clearRecording() {
        recordedActions.clear();
    }
}
