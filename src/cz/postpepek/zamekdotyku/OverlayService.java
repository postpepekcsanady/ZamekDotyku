package cz.postpepek.zamekdotyku;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class OverlayService extends Service {
    public static final String ACTION_SHOW_BUTTON = "cz.postpepek.zamekdotyku.SHOW_BUTTON";
    public static final String ACTION_LOCK_NOW = "cz.postpepek.zamekdotyku.LOCK_NOW";

    private WindowManager windowManager;
    private Handler handler;
    private View floatingView;
    private View lockView;
    private WindowManager.LayoutParams floatingParams;
    private Runnable unlockRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!hasOverlayPermission()) {
            Toast.makeText(this, "Chybí oprávnění pro zobrazení přes aplikace.", Toast.LENGTH_LONG).show();
            stopSelf();
            return START_NOT_STICKY;
        }

        String action = intent == null ? ACTION_SHOW_BUTTON : intent.getAction();
        if (ACTION_LOCK_NOW.equals(action)) {
            showLockOverlay();
        } else {
            showFloatingButton();
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        hideLockOverlay(false);
        removeFloatingButton();
        super.onDestroy();
    }

    private boolean hasOverlayPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
    }

    private void showFloatingButton() {
        if (floatingView != null) {
            return;
        }

        final TextView bubble = new TextView(this);
        bubble.setText("ZAMKNI");
        bubble.setTextColor(Color.WHITE);
        bubble.setTextSize(13);
        bubble.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        bubble.setGravity(Gravity.CENTER);
        bubble.setPadding(dp(14), dp(10), dp(14), dp(10));

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.rgb(37, 99, 235));
        background.setCornerRadius(dp(28));
        bubble.setBackground(background);

        floatingParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        floatingParams.gravity = Gravity.TOP | Gravity.START;
        floatingParams.x = dp(22);
        floatingParams.y = dp(120);

        bubble.setOnTouchListener(new View.OnTouchListener() {
            private int startX;
            private int startY;
            private float touchStartX;
            private float touchStartY;
            private boolean moved;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = floatingParams.x;
                        startY = floatingParams.y;
                        touchStartX = event.getRawX();
                        touchStartY = event.getRawY();
                        moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) (event.getRawX() - touchStartX);
                        int dy = (int) (event.getRawY() - touchStartY);
                        if (Math.abs(dx) > dp(6) || Math.abs(dy) > dp(6)) {
                            moved = true;
                        }
                        floatingParams.x = startX + dx;
                        floatingParams.y = startY + dy;
                        windowManager.updateViewLayout(floatingView, floatingParams);
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!moved) {
                            showLockOverlay();
                        }
                        return true;
                    default:
                        return true;
                }
            }
        });

        floatingView = bubble;
        windowManager.addView(floatingView, floatingParams);
    }

    private void removeFloatingButton() {
        if (floatingView == null) {
            return;
        }

        try {
            windowManager.removeView(floatingView);
        } catch (IllegalArgumentException ignored) {
        }
        floatingView = null;
    }

    private void showLockOverlay() {
        if (lockView != null) {
            return;
        }

        removeFloatingButton();

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xCC000000);
        root.setClickable(true);
        root.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                return true;
            }
        });

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER_HORIZONTAL);
        panel.setPadding(dp(24), dp(24), dp(24), dp(24));

        GradientDrawable panelBackground = new GradientDrawable();
        panelBackground.setColor(Color.WHITE);
        panelBackground.setCornerRadius(dp(18));
        panel.setBackground(panelBackground);

        TextView title = new TextView(this);
        title.setText("Dotyk je zamčený");
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(Color.rgb(17, 24, 39));
        title.setGravity(Gravity.CENTER);
        panel.addView(title, panelParams());

        TextView message = new TextView(this);
        message.setText("Video, hudba nebo aplikace poběží dál. Odemčení provedete podržením tlačítka dole.");
        message.setTextSize(15);
        message.setTextColor(Color.rgb(75, 85, 99));
        message.setGravity(Gravity.CENTER);
        message.setLineSpacing(dp(3), 1.0f);
        LinearLayout.LayoutParams messageParams = panelParams();
        messageParams.setMargins(0, dp(10), 0, dp(20));
        panel.addView(message, messageParams);

        final TextView unlock = new TextView(this);
        unlock.setText("Podržet 3 s pro odemčení");
        unlock.setTextColor(Color.WHITE);
        unlock.setTextSize(16);
        unlock.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        unlock.setGravity(Gravity.CENTER);
        unlock.setPadding(dp(16), dp(14), dp(16), dp(14));
        unlock.setBackground(rounded(Color.rgb(37, 99, 235), dp(14)));
        unlock.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        unlock.setText("Držte...");
                        unlockRunnable = new Runnable() {
                            @Override
                            public void run() {
                                hideLockOverlay(true);
                            }
                        };
                        handler.postDelayed(unlockRunnable, 3000);
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (unlockRunnable != null) {
                            handler.removeCallbacks(unlockRunnable);
                            unlockRunnable = null;
                        }
                        unlock.setText("Podržet 3 s pro odemčení");
                        return true;
                    default:
                        return true;
                }
            }
        });
        panel.addView(unlock, panelParams());

        FrameLayout.LayoutParams panelLayout = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        panelLayout.setMargins(dp(22), 0, dp(22), 0);
        root.addView(panel, panelLayout);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;

        lockView = root;
        windowManager.addView(lockView, params);
    }

    private void hideLockOverlay(boolean showBubbleAgain) {
        if (unlockRunnable != null) {
            handler.removeCallbacks(unlockRunnable);
            unlockRunnable = null;
        }

        if (lockView != null) {
            try {
                windowManager.removeView(lockView);
            } catch (IllegalArgumentException ignored) {
            }
            lockView = null;
        }

        if (showBubbleAgain) {
            showFloatingButton();
        }
    }

    private int overlayType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
        return WindowManager.LayoutParams.TYPE_PHONE;
    }

    private LinearLayout.LayoutParams panelParams() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}

