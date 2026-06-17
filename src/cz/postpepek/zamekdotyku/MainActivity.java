package cz.postpepek.zamekdotyku;

import android.app.Activity;
import android.content.Intent;
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
    private TextView statusText;
    private Button permissionButton;
    private Button floatingButton;
    private Button lockButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePermissionState();
    }

    private void buildUi() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(Color.rgb(247, 250, 252));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(22), dp(28), dp(22), dp(28));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText("Zámek dotyku");
        title.setTextColor(Color.rgb(17, 24, 39));
        title.setTextSize(30);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title, matchWrap());

        TextView subtitle = new TextView(this);
        subtitle.setText("Jednoduché zamčení dotyku třeba u videa, pohádky nebo hudby. Obrazovka běží dál, ale nechtěné dotyky se zastaví.");
        subtitle.setTextColor(Color.rgb(75, 85, 99));
        subtitle.setTextSize(16);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setLineSpacing(dp(2), 1.0f);
        LinearLayout.LayoutParams subtitleParams = matchWrap();
        subtitleParams.setMargins(0, dp(12), 0, dp(24));
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

        floatingButton = secondaryButton("Spustit plovoucí tlačítko");
        floatingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startOverlayAction(OverlayService.ACTION_SHOW_BUTTON);
            }
        });
        root.addView(floatingButton, buttonParams());

        lockButton = primaryButton("Zamknout dotyk hned");
        lockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startOverlayAction(OverlayService.ACTION_LOCK_NOW);
            }
        });
        root.addView(lockButton, buttonParams());

        TextView help = new TextView(this);
        help.setText("Použití:\n1. Nejdřív povolte oprávnění.\n2. Spusťte plovoucí tlačítko.\n3. Klepnutím na tlačítko zamknete dotyk.\n4. Odemčení: podržte tlačítko 3 sekundy.");
        help.setTextColor(Color.rgb(55, 65, 81));
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
            statusText.setText("Oprávnění je povolené. Aplikace může zobrazit zámek přes ostatní aplikace.");
        } else {
            statusText.setText("Ještě je potřeba povolit oprávnění „Zobrazovat přes jiné aplikace“.");
        }
        floatingButton.setEnabled(hasPermission);
        lockButton.setEnabled(hasPermission);
        permissionButton.setText(hasPermission ? "Oprávnění je povolené" : "Povolit zobrazení přes aplikace");
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
        Toast.makeText(this, "Hotovo. Můžete přejít do jiné aplikace.", Toast.LENGTH_SHORT).show();
    }

    private TextView cardText(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(15);
        view.setTextColor(Color.rgb(31, 41, 55));
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
        return styledButton(text, Color.rgb(37, 99, 235), Color.WHITE);
    }

    private Button secondaryButton(String text) {
        return styledButton(text, Color.WHITE, Color.rgb(37, 99, 235));
    }

    private Button styledButton(String text, int backgroundColor, int textColor) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(text);
        button.setTextSize(16);
        button.setTextColor(textColor);
        button.setPadding(dp(14), dp(12), dp(14), dp(12));

        GradientDrawable background = new GradientDrawable();
        background.setColor(backgroundColor);
        background.setCornerRadius(dp(12));
        background.setStroke(dp(1), Color.rgb(37, 99, 235));
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

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}

