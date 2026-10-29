package com.custom.mytv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends Activity {

    private static final String YOUTUBE_TV_PKG = "com.google.android.youtube.tv";
    private static final String YOUTUBE_PKG = "com.google.android.youtube";
    private static final String ANILIBRIA_PKG_1 = "tv.anilibria.app";
    private static final String ANILIBRIA_PKG_2 = "org.anilibria.tv";
    private static final String CHROME_PKG = "com.android.chrome";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View cardYoutube = findViewById(R.id.card_youtube);
        View cardAnilibria = findViewById(R.id.card_anilibria);
        View cardGoogle = findViewById(R.id.card_google);
        View btnHdmi = findViewById(R.id.btn_hdmi);
        View btnSettings = findViewById(R.id.btn_settings);
        View btnEmergency = findViewById(R.id.btn_emergency);

        setupCardFocus(cardYoutube);
        setupCardFocus(cardAnilibria);
        setupCardFocus(cardGoogle);
        setupCardFocus(btnHdmi);
        setupCardFocus(btnSettings);
        setupCardFocus(btnEmergency);

        cardYoutube.setOnClickListener(v -> launchYoutube());
        cardAnilibria.setOnClickListener(v -> launchAnilibria());
        cardGoogle.setOnClickListener(v -> launchGoogle());
        btnHdmi.setOnClickListener(v -> launchHdmiInputs());
        btnSettings.setOnClickListener(v -> launchSettings());
        btnEmergency.setOnClickListener(v -> showEmergencyMenu());

        // Default focus on YouTube card for instant TV remote control
        cardYoutube.post(() -> cardYoutube.requestFocus());
    }

    private void setupCardFocus(View view) {
        if (view == null) return;
        view.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                v.animate().scaleX(1.06f).scaleY(1.06f).setDuration(120).start();
                v.setElevation(16f);
            } else {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
                v.setElevation(0f);
            }
        });
    }

    private void launchYoutube() {
        if (launchAppByPackage(YOUTUBE_TV_PKG)) return;
        if (launchAppByPackage(YOUTUBE_PKG)) return;

        // Fallback to browser YouTube TV
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/tv"));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "YouTube не найден", Toast.LENGTH_SHORT).show();
        }
    }

    private void launchAnilibria() {
        if (launchAppByPackage(ANILIBRIA_PKG_1)) return;
        if (launchAppByPackage(ANILIBRIA_PKG_2)) return;

        // Try searching for any installed package containing 'anilibria'
        PackageManager pm = getPackageManager();
        for (android.content.pm.ApplicationInfo info : pm.getInstalledApplications(PackageManager.GET_META_DATA)) {
            if (info.packageName != null && info.packageName.toLowerCase().contains("anilibria")) {
                Intent launch = pm.getLaunchIntentForPackage(info.packageName);
                if (launch != null) {
                    startActivity(launch);
                    return;
                }
            }
        }

        Toast.makeText(this, "Anilibria TV не найдена. Установите APK приложения", Toast.LENGTH_LONG).show();
    }

    private void launchGoogle() {
        if (launchAppByPackage(CHROME_PKG)) return;

        // Fallback to general browser or search
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Браузер не найден", Toast.LENGTH_SHORT).show();
        }
    }

    private void launchHdmiInputs() {
        // 1. Try standard Android TV Input Settings
        try {
            Intent intent = new Intent("android.settings.TV_INPUT_SETTINGS");
            startActivity(intent);
            return;
        } catch (Exception ignored) {}

        // 2. Try Live TV Passthrough / TV Inputs
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("content://android.media.tv/passthrough"));
            startActivity(intent);
            return;
        } catch (Exception ignored) {}

        // 3. Try standard Live TV system packages
        if (launchAppByPackage("com.google.android.tv")) return;
        if (launchAppByPackage("com.android.tv")) return;

        // 4. Try manufacturer-specific input apps (Sony, Xiaomi, TCL, etc.)
        PackageManager pm = getPackageManager();
        for (android.content.pm.ApplicationInfo info : pm.getInstalledApplications(PackageManager.GET_META_DATA)) {
            String pkg = info.packageName.toLowerCase();
            if (pkg.contains("tvinput") || pkg.contains("inputs") || pkg.contains("sourcemanager")) {
                Intent launch = pm.getLaunchIntentForPackage(info.packageName);
                if (launch != null) {
                    startActivity(launch);
                    return;
                }
            }
        }

        // Fallback: General Settings
        Toast.makeText(this, "Открываем настройки ТВ для выбора входа", Toast.LENGTH_SHORT).show();
        launchSettings();
    }

    private void launchSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        } catch (Exception e) {
            Toast.makeText(this, "Не удалось открыть настройки", Toast.LENGTH_SHORT).show();
        }
    }

    private void showEmergencyMenu() {
        String[] options = {
            "🏠 Выйти в стандартный экран ТВ",
            "⚙️ Настройки этого приложения (Удалить / Отключить)",
            "🔄 Перезапустить этот экран",
            "❌ Отмена"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setTitle("🆘 Меню спасения (Сброс)");
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    // Exit to default TV launcher
                    launchStockLauncher();
                    break;
                case 1:
                    // Open App Details in Settings where user can Clear Defaults or Uninstall
                    try {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    } catch (Exception e) {
                        launchSettings();
                    }
                    break;
                case 2:
                    // Recreate / Restart activity
                    recreate();
                    break;
                case 3:
                    dialog.dismiss();
                    break;
            }
        });
        builder.show();
    }

    private void launchStockLauncher() {
        PackageManager pm = getPackageManager();
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);

        List<ResolveInfo> launchers = pm.queryIntentActivities(homeIntent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo info : launchers) {
            if (!info.activityInfo.packageName.equals(getPackageName())) {
                Intent launch = pm.getLaunchIntentForPackage(info.activityInfo.packageName);
                if (launch != null) {
                    startActivity(launch);
                    return;
                }
            }
        }

        // Try well-known TV launcher package names
        if (launchAppByPackage("com.google.android.apps.tv.launcherx")) return;
        if (launchAppByPackage("com.google.android.tvlauncher")) return;
        if (launchAppByPackage("com.android.launcher")) return;

        Toast.makeText(this, "Стандартный лаунчер не найден", Toast.LENGTH_SHORT).show();
    }

    private boolean launchAppByPackage(String packageName) {
        PackageManager pm = getPackageManager();
        Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launchIntent);
            return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        // Prevent launcher from exiting on Back button press; return focus to primary card
        View cardYoutube = findViewById(R.id.card_youtube);
        if (cardYoutube != null) {
            cardYoutube.requestFocus();
        }
    }
}
