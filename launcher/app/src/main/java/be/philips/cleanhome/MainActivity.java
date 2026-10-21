package be.philips.cleanhome;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Philips Clean Home: eight favorite streaming apps and nothing else.
 * No HDMI input handling, analytics, accessibility service, or system modifications.
 */
public final class MainActivity extends Activity {
    private static final int BACKGROUND = Color.rgb(8, 12, 22);
    private static final int CARD = Color.rgb(23, 30, 45);
    private static final int CARD_FOCUS = Color.rgb(35, 48, 69);
    private static final int WHITE = Color.rgb(244, 247, 253);
    private static final int MUTED = Color.rgb(158, 173, 194);
    private static final int ACCENT = Color.rgb(110, 187, 255);
    private static final Locale NL_BE = new Locale("nl", "BE");

    private static final Favorite[] FAVORITES = {
        new Favorite("TV Vlaanderen", "nl.streamgroup.vlaanderen"),
        new Favorite("Prime Video", "com.amazon.amazonvideo.livingroom"),
        new Favorite("GoPlay", "be.goplay.app.tv", "be.goplay.app"),
        new Favorite("VTM GO", "be.vmma.vtm.zenderapp"),
        new Favorite("VRT MAX", "be.vrt.vrtnu"),
        new Favorite("DAZN", "com.dazn"),
        new Favorite("IBO Pro Player", "com.ibopro.player"),
        new Favorite("Stremio", "com.stremio.one")
    };

    private final Handler clockHandler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat clockFormat = new SimpleDateFormat("HH:mm", NL_BE);
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE d MMMM", NL_BE);
    private TextView clock;
    private TextView date;
    private LinearLayout grid;

    private final Runnable updateClock = new Runnable() {
        @Override public void run() {
            if (clock != null && date != null) {
                Date now = new Date();
                clock.setText(clockFormat.format(now));
                date.setText(dateFormat.format(now));
            }
            clockHandler.postDelayed(this, 60_000L);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
    }

    @Override public void onResume() {
        super.onResume();
        showApps();
        clockHandler.removeCallbacks(updateClock);
        updateClock.run();
    }

    @Override public void onPause() {
        clockHandler.removeCallbacks(updateClock);
        super.onPause();
    }

    @Override public void onDestroy() {
        clockHandler.removeCallbacks(updateClock);
        super.onDestroy();
    }

    @Override public void onBackPressed() {
        // Do not exit into an empty task when used as HOME.
    }

    private void buildUi() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setGravity(Gravity.CENTER_HORIZONTAL);
        screen.setPadding(dp(38), dp(26), dp(38), dp(26));
        screen.setClipChildren(false);
        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{BACKGROUND, Color.rgb(13, 24, 39), BACKGROUND});
        screen.setBackground(bg);
        setContentView(screen);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        screen.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView brand = label("THUIS", 16, WHITE, true);
        brand.setLetterSpacing(.25f);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(brand, new LinearLayout.LayoutParams(0, dp(45), 1f));

        TextView settings = label("Instellingen", 15, MUTED, false);
        settings.setGravity(Gravity.CENTER);
        settings.setPadding(dp(18), dp(9), dp(18), dp(9));
        settings.setFocusable(true);
        settings.setClickable(true);
        settings.setBackground(cardBackground(false, true));
        settings.setOnFocusChangeListener((v, focused) -> {
            v.setBackground(cardBackground(focused, true));
            settings.setTextColor(focused ? WHITE : MUTED);
            v.animate().scaleX(focused ? 1.04f : 1f)
                    .scaleY(focused ? 1.04f : 1f).setDuration(100).start();
        });
        settings.setOnClickListener(v -> openSettings());
        header.addView(settings);

        screen.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 1f));

        TextView headline = label("Wat kijken we vandaag?", 29, WHITE, true);
        headline.setGravity(Gravity.CENTER);
        screen.addView(headline);
        TextView sub = label("Jouw apps. Geen afleiding.", 15, MUTED, false);
        sub.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subParams.topMargin = dp(7);
        screen.addView(sub, subParams);

        grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setClipChildren(false);
        grid.setClipToPadding(false);
        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gridParams.topMargin = dp(29);
        screen.addView(grid, gridParams);

        screen.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 1f));
        clock = label("--:--", 23, WHITE, false);
        clock.setGravity(Gravity.CENTER);
        screen.addView(clock);
        date = label("", 13, MUTED, false);
        date.setGravity(Gravity.CENTER);
        screen.addView(date);
    }

    private void showApps() {
        grid.removeAllViews();
        PackageManager pm = getPackageManager();
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int widthDp = Math.round(dm.widthPixels / dm.density);
        int tileWidth = Math.max(120, Math.min(210, (widthDp - 106 - 3 * 14) / 4));
        int tileHeight = Math.max(116, Math.min(138, Math.round(tileWidth * .69f)));

        View first = null;
        for (int rowNo = 0; rowNo < 2; rowNo++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setClipChildren(false);
            row.setClipToPadding(false);
            grid.addView(row);
            for (int column = 0; column < 4; column++) {
                Favorite favorite = FAVORITES[rowNo * 4 + column];
                View tile = makeTile(pm, favorite);
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                        dp(tileWidth), dp(tileHeight));
                if (column < 3) p.rightMargin = dp(14);
                if (rowNo == 0) p.bottomMargin = dp(14);
                row.addView(tile, p);
                if (first == null) first = tile;
            }
        }
        if (first != null) {
            View start = first;
            grid.post(start::requestFocus);
        }
    }

    private View makeTile(PackageManager pm, Favorite favorite) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setFocusable(true);
        tile.setClickable(true);
        tile.setContentDescription(favorite.name);
        tile.setPadding(dp(8), dp(12), dp(8), dp(12));
        tile.setBackground(cardBackground(false, false));

        String pkg = resolvePackage(pm, favorite);
        if (pkg != null) {
            try {
                Drawable icon = pm.getApplicationIcon(pkg);
                ImageView image = new ImageView(this);
                image.setImageDrawable(icon);
                image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(55), dp(55));
                iconParams.bottomMargin = dp(9);
                tile.addView(image, iconParams);
            } catch (PackageManager.NameNotFoundException e) {
                fallbackIcon(tile, favorite.name);
            }
        } else {
            fallbackIcon(tile, favorite.name);
        }

        TextView name = label(favorite.name, 15, WHITE, true);
        name.setGravity(Gravity.CENTER);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        tile.addView(name, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        tile.setOnFocusChangeListener((v, focused) -> {
            v.setBackground(cardBackground(focused, false));
            v.animate().cancel();
            v.animate().scaleX(focused ? 1.045f : 1f)
                    .scaleY(focused ? 1.045f : 1f)
                    .setDuration(110).start();
            v.setElevation(focused ? dp(12) : 0);
        });
        tile.setOnClickListener(v -> openApp(favorite));
        return tile;
    }

    private void fallbackIcon(LinearLayout tile, String name) {
        TextView initial = label(name.substring(0, 1), 31, ACCENT, true);
        initial.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(55), dp(55));
        p.bottomMargin = dp(9);
        tile.addView(initial, p);
    }

    private String resolvePackage(PackageManager pm, Favorite favorite) {
        for (String pkg : favorite.packages) {
            if (launchIntent(pm, pkg) != null) return pkg;
        }
        return null;
    }

    private Intent launchIntent(PackageManager pm, String pkg) {
        Intent launch = pm.getLeanbackLaunchIntentForPackage(pkg);
        if (launch != null) return launch;
        launch = pm.getLaunchIntentForPackage(pkg);
        if (launch != null) return launch;

        Intent leanback = new Intent(Intent.ACTION_MAIN);
        leanback.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        leanback.setPackage(pkg);
        List<ResolveInfo> matches = pm.queryIntentActivities(leanback, 0);
        if (!matches.isEmpty() && matches.get(0).activityInfo != null) {
            ResolveInfo hit = matches.get(0);
            leanback.setClassName(hit.activityInfo.packageName, hit.activityInfo.name);
            return leanback;
        }
        return null;
    }

    private void openApp(Favorite favorite) {
        PackageManager pm = getPackageManager();
        for (String pkg : favorite.packages) {
            Intent launch = launchIntent(pm, pkg);
            if (launch == null) continue;
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            try {
                startActivity(launch);
                return;
            } catch (ActivityNotFoundException | SecurityException ignored) {
                // GoPlay and other apps may use a different activity on this firmware.
            }
        }
        Toast.makeText(this, favorite.name + " kon niet geopend worden.",
                Toast.LENGTH_LONG).show();
    }

    private void openSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Open Instellingen met de afstandsbediening.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private GradientDrawable cardBackground(boolean focused, boolean settings) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(focused ? CARD_FOCUS : settings ? Color.rgb(18, 27, 40) : CARD);
        drawable.setCornerRadius(dp(settings ? 15 : 19));
        drawable.setStroke(dp(focused ? 2 : 1),
                focused ? ACCENT : Color.rgb(42, 52, 69));
        return drawable;
    }

    private TextView label(String text, float sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setTypeface(Typeface.create("sans-serif",
                bold ? Typeface.BOLD : Typeface.NORMAL));
        return view;
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class Favorite {
        final String name;
        final String[] packages;
        Favorite(String name, String... packages) {
            this.name = name;
            this.packages = packages;
        }
    }
}
