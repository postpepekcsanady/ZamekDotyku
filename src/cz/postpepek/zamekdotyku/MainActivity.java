package cz.postpepek.zamekdotyku;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    public static final String PREFS_NAME = "zamek_dotyku";
    public static final String PREF_UNLOCK_MS = "unlock_ms";
    public static final int DEFAULT_UNLOCK_MS = 5000;

    private TextView statusText;
    private TextView unlockText;
    private Button permissionButton;
    private Button floatingButton;
    private Button lockButton;
    private Button stopButton;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (!preferences.contains(PREF_UNLOCK_MS)) {
            preferences.edit().putInt(PREF_UNLOCK_MS, DEFAULT_UNLOCK_MS).apply();
        }
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePermissionState();
        updateUnlockText();
    }

    private void buildUi() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(Color.rgb(246, 248, 252));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(22), dp(28), dp(22), dp(28));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText("Zámek dotyku");
        title.setTextColor(Color.rgb(15, 23, 42));
        title.setTextSize(30);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title, matchWrap());

        TextView subtitle = new TextView(this);
        subtitle.setText("Dětský režim pro pohádky. Video běží dál, ale náhodné dotyky, pauza a přeskakování se zastaví.");
        subtitle.setTextColor(Color.rgb(71, 85, 105));
        subtitle.setTextSize(16);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setLineSpacing(dp(2), 1.0f);
        LinearLayout.LayoutParams subtitleParams = matchWrap();
        subtitleParams.setMargins(0, dp(12), 0, dp(22));
        root.addView(subtitle, subtitleParams);

        statusText = cardText("");
        root.addView(statusText, matchWrap());

        permissionButton = primaryButton("Povolit zobrazení přes aplikace");
        permissionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openOverlaySettings();
            }
        });
        root.addView(permissionButton, buttonParams());

        TextView childMode = sectionTitle("Dětský režim");
        LinearLayout.LayoutParams childModeParams = matchWrap();
        childModeParams.setMargins(0, dp(24), 0, dp(8));
        root.addView(childMode, childModeParams);

        unlockText = cardText("");
        root.addView(unlockText, matchWrap());

        LinearLayout unlockButtons = new LinearLayout(this);
        unlockButtons.setOrientation(LinearLayout.HORIZONTAL);
        unlockButtons.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams rowParams = matchWrap();
        rowParams.setMargins(0, dp(10), 0, 0);
        root.addView(unlockButtons, rowParams);

        unlockButtons.addView(smallChoiceButton("3 s", 3000), choiceParams());
        unlockButtons.addView(smallChoiceButton("5 s", 5000), choiceParams());
        unlockButtons.addView(smallChoiceButton("8 s", 8000), choiceParams());

        floatingButton = primaryButton("Spustit plovoucí tlačítko");
        floatingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startOverlayAction(OverlayService.ACTION_SHOW_BUTTON);
            }
        });
        root.addView(floatingButton, buttonParamsLarge());

        lockButton = secondaryButton("Zamknout dotyk hned");
        lockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startOverlayAction(OverlayService.ACTION_LOCK_NOW);
            }
        });
        root.addView(lockButton, buttonParams());

        stopButton = quietButton("Schovat plovoucí tlačítko");
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startOverlayAction(OverlayService.ACTION_STOP);
            }
        });
        root.addView(stopButton, buttonParams());

        TextView help = new TextView(this);
        help.setText("Jak to používat:\n1. Pusťte pohádku.\n2. Klepněte na plovoucí tlačítko ZÁMEK.\n3. Aplikace schová tlačítko a zablokuje dotyky.\n4. Odemknutí: podržet spodní tlačítko několik sekund.");
        help.setTextColor(Color.rgb(51, 65, 85));
        help.setTextSize(15);
        help.setLineSpacing(dp(4), 1.0f);
        LinearLayout.LayoutParams helpParams = matchWrap();
        helpParams.setMargins(0, dp(26), 0, 0);
        root.addView(help, helpParams);

        setContentView(scrollView);
    }

    private void updatePermissionState() {
        boolean hasPermission = hasOverlayPermission();
        if (hasPermission) {
            statusText.setText("Oprávnění je povolené. Zámek může fungovat nad pohádkou nebo YouTube.");
        } else {
            statusText.setText("Ještě je potřeba povolit oprávnění „Zobrazovat přes jiné aplikace“.");
        }
        floatingButton.setEnabled(hasPermission);
        lockButton.setEnabled(hasPermission);
        stopButton.setEnabled(hasPermission);
        permissionButton.setText(hasPermission ? "Oprávnění je povolené" : "Povolit zobrazení přes aplikace");
    }

    private void updateUnlockText() {
        int seconds = preferences.getInt(PREF_UNLOCK_MS, DEFAULT_UNLOCK_MS) / 1000;
        unlockText.setText("Odemčení je nastavené na podržení " + seconds + " sekund. Pro vnoučka doporučuji 5 nebo 8 sekund.");
    }

    private Button smallChoiceButton(String text, final int unlockMs) {
        Button button = quietButton(text);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                preferences.edit().putInt(PREF_UNLOCK_MS, unlockMs).apply();
                updateUnlockText();
                Toast.makeText(MainActivity.this, "Odemčení nastaveno.", Toast.LENGTH_SHORT).show();
            }
        });
        return button;
    }

    private boolean hasOverlayPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
    }

    private void openOverlaySettings() {
        if (hasOverlayPermission()) {
            Toast.makeText(this, "Oprávnění už je povolené.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private void startOverlayAction(String action) {
        if (!hasOverlayPermission()) {
            Toast.makeText(this, "Nejdřív povolte zobrazení přes ostatní aplikace.", Toast.LENGTH_LONG).show();
            openOverlaySettings();
            return;
        }

        Intent intent = new Intent(this, OverlayService.class);
        intent.setAction(action);
        startService(intent);
        if (OverlayService.ACTION_STOP.equals(action)) {
            Toast.makeText(this, "Plovoucí tlačítko je schované.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Hotovo. Teď můžete pustit pohádku.", Toast.LENGTH_SHORT).show();
        }
    }

    private TextView sectionTitle(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(Color.rgb(15, 23, 42));
        view.setTextSize(18);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private TextView cardText(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(15);
        view.setTextColor(Color.rgb(30, 41, 59));
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(16), dp(14), dp(16), dp(14));

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(dp(12));
        background.setStroke(dp(1), Color.rgb(219, 234, 254));
        view.setBackground(background);
        return view;
    }

    private Button primaryButton(String text) {
        return styledButton(text, Color.rgb(37, 99, 235), Color.WHITE, Color.rgb(37, 99, 235));
    }

    private Button secondaryButton(String text) {
        return styledButton(text, Color.WHITE, Color.rgb(37, 99, 235), Color.rgb(37, 99, 235));
    }

    private Button quietButton(String text) {
        return styledButton(text, Color.rgb(239, 246, 255), Color.rgb(29, 78, 216), Color.rgb(191, 219, 254));
    }

    private Button styledButton(String text, int backgroundColor, int textColor, int strokeColor) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(text);
        button.setTextSize(16);
        button.setTextColor(textColor);
        button.setPadding(dp(14), dp(12), dp(14), dp(12));

        GradientDrawable background = new GradientDrawable();
        background.setColor(backgroundColor);
        background.setCornerRadius(dp(12));
        background.setStroke(dp(1), strokeColor);
        button.setBackground(background);
        return button;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(0, dp(12), 0, 0);
        return params;
    }

    private LinearLayout.LayoutParams buttonParamsLarge() {
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(0, dp(22), 0, 0);
        return params;
    }

    private LinearLayout.LayoutParams choiceParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1);
        params.setMargins(dp(4), 0, dp(4), 0);
        return params;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
