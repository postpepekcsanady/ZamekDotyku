package cz.postpepek.zamekdotyku;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
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
    public static final String ACTION_STOP = "cz.postpepek.zamekdotyku.STOP";

    private WindowManager windowManager;
    private Handler handler;
    private View floatingView;
    private View lockView;
    private WindowManager.LayoutParams floatingParams;
    private Runnable unlockRunnable;
    private Runnable tickRunnable;
    private long unlockStartedAt;
    private int unlockMs;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_SHOW_BUTTON : intent.getAction();
        if (ACTION_STOP.equals(action)) {
            hideLockOverlay(false);
            removeFloatingButton();
            stopSelf();
            return START_NOT_STICKY;
        }

        if (!hasOverlayPermission()) {
            Toast.makeText(this, "Chybí oprávnění pro zobrazení přes aplikace.", Toast.LENGTH_LONG).show();
            stopSelf();
            return START_NOT_STICKY;
        }

        unlockMs = readUnlockMs();
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

    private int readUnlockMs() {
        SharedPreferences preferences = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE);
        return preferences.getInt(MainActivity.PREF_UNLOCK_MS, MainActivity.DEFAULT_UNLOCK_MS);
    }

    private boolean hasOverlayPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
    }

    private void showFloatingButton() {
        if (floatingView != null) {
            return;
        }

        final TextView bubble = new TextView(this);
        bubble.setText("ZÁMEK");
        bubble.setTextColor(Color.WHITE);
        bubble.setTextSize(14);
        bubble.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        bubble.setGravity(Gravity.CENTER);
        bubble.setPadding(dp(16), dp(12), dp(16), dp(12));
        bubble.setBackground(rounded(Color.rgb(37, 99, 235), dp(30), 0, 0));

        floatingParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        floatingParams.gravity = Gravity.TOP | Gravity.START;
        floatingParams.x = dp(18);
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
                            unlockMs = readUnlockMs();
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
        root.setBackgroundColor(0x33000000);
        root.setClickable(true);
        root.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        root.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                return true;
            }
        });

        TextView topLabel = new TextView(this);
        topLabel.setText("Dotyk zamčený");
        topLabel.setTextColor(Color.WHITE);
        topLabel.setTextSize(14);
        topLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        topLabel.setGravity(Gravity.CENTER);
        topLabel.setPadding(dp(14), dp(8), dp(14), dp(8));
        topLabel.setBackground(rounded(0x99000000, dp(18), Color.WHITE, 1));
        FrameLayout.LayoutParams topParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        topParams.setMargins(0, dp(22), 0, 0);
        root.addView(topLabel, topParams);

        LinearLayout bottomPanel = new LinearLayout(this);
        bottomPanel.setOrientation(LinearLayout.VERTICAL);
        bottomPanel.setGravity(Gravity.CENTER);
        bottomPanel.setPadding(dp(16), dp(12), dp(16), dp(12));
        bottomPanel.setBackground(rounded(0xCC000000, dp(22), 0, 0));

        TextView hint = new TextView(this);
        hint.setText("Odemčení jen pro dospělého");
        hint.setTextColor(Color.rgb(226, 232, 240));
        hint.setTextSize(13);
        hint.setGravity(Gravity.CENTER);
        bottomPanel.addView(hint, panelParams());

        final TextView unlock = new TextView(this);
        unlock.setText(unlockText());
        unlock.setTextColor(Color.WHITE);
        unlock.setTextSize(16);
        unlock.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        unlock.setGravity(Gravity.CENTER);
        unlock.setPadding(dp(16), dp(13), dp(16), dp(13));
        unlock.setBackground(rounded(Color.rgb(37, 99, 235), dp(16), 0, 0));
        LinearLayout.LayoutParams unlockParams = panelParams();
        unlockParams.setMargins(0, dp(8), 0, 0);
        bottomPanel.addView(unlock, unlockParams);

        unlock.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startUnlockHold(unlock);
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        cancelUnlockHold(unlock);
                        return true;
                    default:
                        return true;
                }
            }
        });

        FrameLayout.LayoutParams bottomParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM);
        bottomParams.setMargins(dp(18), 0, dp(18), dp(26));
        root.addView(bottomPanel, bottomParams);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;

        lockView = root;
        windowManager.addView(lockView, params);
    }

    private void startUnlockHold(final TextView unlock) {
        cancelUnlockCallbacks();
        unlockStartedAt = System.currentTimeMillis();
        unlock.setText("Držte...");

        tickRunnable = new Runnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - unlockStartedAt;
                int remaining = Math.max(1, (int) Math.ceil((unlockMs - elapsed) / 1000.0));
                unlock.setText("Držte ještě " + remaining + " s");
                handler.postDelayed(this, 250);
            }
        };
        handler.postDelayed(tickRunnable, 250);

        unlockRunnable = new Runnable() {
            @Override
            public void run() {
                hideLockOverlay(true);
                Toast.makeText(OverlayService.this, "Dotyk je odemčený.", Toast.LENGTH_SHORT).show();
            }
        };
        handler.postDelayed(unlockRunnable, unlockMs);
    }

    private void cancelUnlockHold(TextView unlock) {
        cancelUnlockCallbacks();
        unlock.setText(unlockText());
    }

    private void cancelUnlockCallbacks() {
        if (unlockRunnable != null) {
            handler.removeCallbacks(unlockRunnable);
            unlockRunnable = null;
        }
        if (tickRunnable != null) {
            handler.removeCallbacks(tickRunnable);
            tickRunnable = null;
        }
    }

    private void hideLockOverlay(boolean showBubbleAgain) {
        cancelUnlockCallbacks();

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

    private String unlockText() {
        return "Podržet " + (unlockMs / 1000) + " s pro odemčení";
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

    private GradientDrawable rounded(int color, int radius, int strokeColor, int strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (strokeWidth > 0) {
            drawable.setStroke(dp(strokeWidth), strokeColor);
        }
        return drawable;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
