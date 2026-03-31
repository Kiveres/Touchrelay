package com.touchrelay.app.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;

import com.touchrelay.app.model.TouchEvent;

public class GestureAccessibilityService extends AccessibilityService {

    public static GestureAccessibilityService instance;

    // Active gesture state
    private GestureDescription.StrokeDescription activeStroke;
    private boolean gestureInProgress = false;
    private float lastX, lastY;

    private final Handler handler = new Handler(Looper.getMainLooper());

    // Listener for overlay circles
    public interface TouchListener {
        void onTouch(float x, float y, String action);
    }
    public static TouchListener touchListener;

    @Override
    public void onServiceConnected() {
        instance = this;
    }

    @Override
    public void onDestroy() {
        instance = null;
        super.onDestroy();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}

    public void handleTouchEvent(TouchEvent event) {
        handler.post(() -> {
            switch (event.action) {
                case TouchEvent.ACTION_DOWN:
                    handleDown(event.x, event.y);
                    break;
                case TouchEvent.ACTION_MOVE:
                    handleMove(event.x, event.y);
                    break;
                case TouchEvent.ACTION_UP:
                    handleUp(event.x, event.y);
                    break;
                case TouchEvent.ACTION_BUTTON:
                    handleButton(event.button);
                    break;
            }
            // Notify overlay
            if (touchListener != null && !TouchEvent.ACTION_BUTTON.equals(event.action)) {
                touchListener.onTouch(event.x, event.y, event.action);
            }
        });
    }

    private void handleDown(float nx, float ny) {
        float[] px = toPixels(nx, ny);
        lastX = px[0];
        lastY = px[1];

        Path path = new Path();
        path.moveTo(lastX, lastY);

        // Start stroke with willContinue=true so we can extend it
        activeStroke = new GestureDescription.StrokeDescription(
                path, 0, 1, true
        );

        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(activeStroke);

        dispatchGesture(builder.build(), null, null);
        gestureInProgress = true;
    }

    private void handleMove(float nx, float ny) {
        if (!gestureInProgress || activeStroke == null) return;

        float[] px = toPixels(nx, ny);
        float newX = px[0];
        float newY = px[1];

        // Skip tiny movements
        if (Math.abs(newX - lastX) < 1 && Math.abs(newY - lastY) < 1) return;

        Path path = new Path();
        path.moveTo(lastX, lastY);
        path.lineTo(newX, newY);

        lastX = newX;
        lastY = newY;

        // Continue the stroke — this is the key for smooth scrolling
        GestureDescription.StrokeDescription continued = activeStroke.continueStroke(
                path, 0, 16, true  // ~60fps timing
        );
        activeStroke = continued;

        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(activeStroke);
        dispatchGesture(builder.build(), null, null);
    }

    private void handleUp(float nx, float ny) {
        if (!gestureInProgress || activeStroke == null) return;

        float[] px = toPixels(nx, ny);
        float newX = px[0];
        float newY = px[1];

        Path path = new Path();
        path.moveTo(lastX, lastY);
        path.lineTo(newX, newY);

        // Final stroke — willContinue=false signals end of gesture
        GestureDescription.StrokeDescription finalStroke = activeStroke.continueStroke(
                path, 0, 16, false
        );

        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(finalStroke);
        dispatchGesture(builder.build(), null, null);

        activeStroke = null;
        gestureInProgress = false;
    }

    private void handleButton(String button) {
        switch (button) {
            case "back":
                performGlobalAction(GLOBAL_ACTION_BACK);
                break;
            case "home":
                performGlobalAction(GLOBAL_ACTION_HOME);
                break;
            case "recents":
                performGlobalAction(GLOBAL_ACTION_RECENTS);
                break;
        }
    }

    private float[] toPixels(float nx, float ny) {
        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        return new float[]{nx * dm.widthPixels, ny * dm.heightPixels};
    }
}
