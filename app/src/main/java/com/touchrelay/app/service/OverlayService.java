package com.touchrelay.app.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;

import androidx.core.app.NotificationCompat;

public class OverlayService extends Service {

    private WindowManager windowManager;
    private TouchCircleView overlayView;
    private static final String CHANNEL_ID = "TouchRelayOverlay";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, buildNotification());

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        overlayView = new TouchCircleView(this);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );

        windowManager.addView(overlayView, params);

        // Register as touch listener in accessibility service
        GestureAccessibilityService.touchListener = (x, y, action) -> {
            android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
            float px = x * dm.widthPixels;
            float py = y * dm.heightPixels;
            overlayView.updateTouch(px, py, action);
        };
    }

    @Override
    public void onDestroy() {
        GestureAccessibilityService.touchListener = null;
        if (windowManager != null && overlayView != null) {
            windowManager.removeView(overlayView);
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "TouchRelay Overlay", NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("TouchRelay")
                .setContentText("Overlay active")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .build();
    }

    // Custom view that draws touch circles
    static class TouchCircleView extends View {
        private float touchX = -1, touchY = -1;
        private boolean visible = false;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Handler handler = new Handler(Looper.getMainLooper());
        private Runnable hideRunnable;

        public TouchCircleView(android.content.Context context) {
            super(context);
            paint.setColor(Color.argb(180, 255, 100, 0));
            paint.setStyle(Paint.Style.FILL);
        }

        public void updateTouch(float x, float y, String action) {
            handler.post(() -> {
                touchX = x;
                touchY = y;

                if ("up".equals(action)) {
                    // Hide after short delay on finger up
                    if (hideRunnable != null) handler.removeCallbacks(hideRunnable);
                    hideRunnable = () -> {
                        visible = false;
                        invalidate();
                    };
                    handler.postDelayed(hideRunnable, 200);
                } else {
                    visible = true;
                    if (hideRunnable != null) handler.removeCallbacks(hideRunnable);
                }
                invalidate();
            });
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (visible && touchX >= 0) {
                // Outer ring
                paint.setColor(Color.argb(80, 255, 100, 0));
                canvas.drawCircle(touchX, touchY, 60, paint);
                // Inner circle
                paint.setColor(Color.argb(180, 255, 140, 0));
                canvas.drawCircle(touchX, touchY, 30, paint);
            }
        }
    }
}
