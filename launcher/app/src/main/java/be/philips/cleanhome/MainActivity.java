package be.philips.cleanhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
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
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
    private ScrollView viewport;
    private TextView headline;
    private TextView subtitle;
    private TextView allAppsButton;
    private boolean showingAllApps = false;

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
        if (showingAllApps) showAllApps();
        else showApps();
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
        if (showingAllApps) {
            showingAllApps = false;
            showApps();
        }
        // On HOME do not exit into an empty task.
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

        addHeaderButton(header, "App Store", this::openStore, true);
        header.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1f));
        allAppsButton = addHeaderButton(header, "Alle apps", () -> {
            showingAllApps = !showingAllApps;
            if (showingAllApps) showAllApps();
            else showApps();
        }, true);
        addHeaderButton(header, "Instellingen", this::openSettings, false);

        screen.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 1f));

        headline = label("Wat kijken we vandaag?", 29, WHITE, true);
        headline.setGravity(Gravity.CENTER);
        screen.addView(headline);
        subtitle = label("", 15, MUTED, false);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subParams.topMargin = dp(7);
        screen.addView(subtitle, subParams);
        subtitle.setVisibility(View.GONE);

        viewport = new ScrollView(this);
        viewport.setFillViewport(false);
        viewport.setVerticalScrollBarEnabled(false);
        viewport.setClipChildren(false);
        viewport.setClipToPadding(false);
        viewport.setFocusable(false);

        grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setGravity(Gravity.CENTER_HORIZONTAL);
        grid.setClipChildren(false);
        grid.setClipToPadding(false);
        viewport.addView(grid, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        DisplayMetrics dm = getResources().getDisplayMetrics();
        int heightDp = Math.round(dm.heightPixels / dm.density);
        int viewportDp = Math.max(205, Math.min(330, heightDp - 235));
        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(viewportDp));
        gridParams.topMargin = dp(24);
        screen.addView(viewport, gridParams);

        screen.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 1f));
        clock = label("--:--", 23, WHITE, false);
        clock.setGravity(Gravity.CENTER);
        screen.addView(clock);
        date = label("", 13, MUTED, false);
        date.setGravity(Gravity.CENTER);
        screen.addView(date);
    }

    private TextView addHeaderButton(LinearLayout header, String title,
                                     Runnable action, boolean rightMargin) {
        TextView button = label(title, 15, MUTED, false);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(18), dp(9), dp(18), dp(9));
        button.setFocusable(true);
        button.setClickable(true);
        button.setBackground(cardBackground(false, true));
        button.setOnFocusChangeListener((v, focused) -> {
            v.setBackground(cardBackground(focused, true));
            button.setTextColor(focused ? WHITE : MUTED);
            v.animate().scaleX(focused ? 1.04f : 1f)
                    .scaleY(focused ? 1.04f : 1f).setDuration(100).start();
        });
        button.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (rightMargin) params.rightMargin = dp(12);
        header.addView(button, params);
        return button;
    }

    private void showApps() {
        headline.setText("Wat kijken we vandaag?");
        subtitle.setVisibility(View.GONE);
        allAppsButton.setText("Alle apps");
        renderTiles(Arrays.asList(FAVORITES));
    }

    private void showAllApps() {
        headline.setText("Alle apps");
        subtitle.setText("OK: openen  ·  OK ingedrukt houden: verwijderen of app-info");
        subtitle.setVisibility(View.VISIBLE);
        allAppsButton.setText("Favorieten");

        PackageManager pm = getPackageManager();
        List<Favorite> installed = new ArrayList<>();
        for (ApplicationInfo info : pm.getInstalledApplications(0)) {
            if (getPackageName().equals(info.packageName)) continue;
            if (launchIntent(pm, info.packageName) == null) continue;
            CharSequence name = pm.getApplicationLabel(info);
            installed.add(new Favorite(name == null ? info.packageName : name.toString(),
                    info.packageName));
        }
        Collections.sort(installed, (a, b) -> a.name.compareToIgnoreCase(b.name));
        renderTiles(installed);
    }

    private void renderTiles(List<Favorite> apps) {
        grid.removeAllViews();
        viewport.scrollTo(0, 0);
        PackageManager pm = getPackageManager();
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int widthDp = Math.round(dm.widthPixels / dm.density);
        int tileWidth = Math.max(120, Math.min(210, (widthDp - 106 - 3 * 14) / 4));
        int tileHeight = Math.max(116, Math.min(138, Math.round(tileWidth * .69f)));

        View first = null;
        int rows = (apps.size() + 3) / 4;
        for (int rowNo = 0; rowNo < rows; rowNo++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setClipChildren(false);
            row.setClipToPadding(false);
            grid.addView(row);
            for (int column = 0; column < 4; column++) {
                int index = rowNo * 4 + column;
                if (index >= apps.size()) break;
                Favorite favorite = apps.get(index);
                View tile = makeTile(pm, favorite);
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                        dp(tileWidth), dp(tileHeight));
                if (column < 3) p.rightMargin = dp(14);
                if (rowNo < rows - 1) p.bottomMargin = dp(14);
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
        tile.setLongClickable(true);
        tile.setOnLongClickListener(v -> {
            showAppActions(favorite);
            return true;
        });
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

    private void openStore() {
        PackageManager pm = getPackageManager();
        Intent store = launchIntent(pm, "com.android.vending");
        if (store == null) {
            store = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://search?q=apps"));
            store.setPackage("com.android.vending");
        }
        try {
            store.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(store);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, "De Play Store kon niet geopend worden.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void showAppActions(Favorite favorite) {
        String pkg = resolvePackage(getPackageManager(), favorite);
        if (pkg == null) {
            Toast.makeText(this, "App niet gevonden.", Toast.LENGTH_SHORT).show();
            return;
        }
        final String selectedPkg = pkg;
        new AlertDialog.Builder(this)
                .setTitle(favorite.name)
                .setItems(new String[]{"Openen", "Verwijderen", "App-info", "Annuleren"},
                        (dialog, option) -> {
                            if (option == 0) openApp(favorite);
                            else if (option == 1) requestUninstall(selectedPkg);
                            else if (option == 2) openAppInfo(selectedPkg);
                        }).show();
    }

    private void requestUninstall(String pkg) {
        try {
            ApplicationInfo info = getPackageManager().getApplicationInfo(pkg, 0);
            if ((info.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                Toast.makeText(this, "Systeemapp: gebruik App-info om deze eventueel uit te schakelen.",
                        Toast.LENGTH_LONG).show();
                openAppInfo(pkg);
                return;
            }
            Intent intent = new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + pkg));
            startActivity(intent); // Android asks for confirmation. Never uninstall silently.
        } catch (PackageManager.NameNotFoundException
                 | ActivityNotFoundException | SecurityException e) {
            openAppInfo(pkg);
        }
    }

    private void openAppInfo(String pkg) {
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + pkg)));
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, "App-instellingen niet beschikbaar.",
                    Toast.LENGTH_LONG).show();
        }
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
