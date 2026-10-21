package be.philips.cleanhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** A quiet TV home, not an Android settings or package-manager clone. */
public final class MainActivity extends Activity {
    private static final int WHITE = 0xFFF9F4ED;
    private static final int DIM = 0xFFC8BDB1;
    private static final int GOLD = 0xFFF6C78B;
    private static final int CARD = 0xD8232B35;
    private static final int FOCUSED = 0xEF344354;
    private static final Locale LOCALE = new Locale("nl", "BE");

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

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat clockFormat = new SimpleDateFormat("HH:mm", LOCALE);
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE d MMMM", LOCALE);

    private boolean drawerOpen;
    // Reuse the current tile views and icons when returning from another app.
    // A package change invalidates them, so install/uninstall stays in sync.
    private boolean tilesReady;
    private boolean resumed;
    private final BroadcastReceiver packageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            tilesReady = false;
            if (resumed) refreshTiles();
        }
    };

    private TextView homeTitle;
    private TextView drawerButton;
    private TextView time;
    private TextView date;
    private LinearLayout tileGrid;
    private ScrollView scroll;
    private View upperSpacer;
    private View lowerSpacer;
    private View firstTile;

    private final Runnable clockTick = new Runnable() {
        @Override public void run() {
            Date now = new Date();
            time.setText(clockFormat.format(now));
            date.setText(dateFormat.format(now));
            handler.postDelayed(this, 60_000L);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        buildScreen();
        IntentFilter packages = new IntentFilter();
        packages.addAction(Intent.ACTION_PACKAGE_ADDED);
        packages.addAction(Intent.ACTION_PACKAGE_REMOVED);
        packages.addAction(Intent.ACTION_PACKAGE_CHANGED);
        packages.addAction(Intent.ACTION_PACKAGE_REPLACED);
        packages.addDataScheme("package");
        registerReceiver(packageReceiver, packages);
    }

    @Override public void onResume() {
        super.onResume();
        resumed = true;
        if (!tilesReady) refreshTiles();
        handler.removeCallbacks(clockTick);
        clockTick.run();
    }

    @Override public void onPause() {
        resumed = false;
        handler.removeCallbacks(clockTick);
        super.onPause();
    }

    @Override public void onDestroy() {
        unregisterReceiver(packageReceiver);
        handler.removeCallbacks(clockTick);
        super.onDestroy();
    }

    private void refreshTiles() {
        if (drawerOpen) showAllApps(); else showFavorites();
    }

    @Override public void onBackPressed() {
        if (drawerOpen) {
            drawerOpen = false;
            showFavorites();
        }
    }

    private void buildScreen() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER_HORIZONTAL);
        page.setPadding(dp(42), dp(25), dp(42), dp(22));
        page.setClipChildren(false);
        page.setBackground(new CozyBackdrop());
        setContentView(page);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        page.addView(toolbar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        createToolbarButton(toolbar, "App Store", this::openPlayStore, true);
        toolbar.addView(new View(this), new LinearLayout.LayoutParams(0, dp(1), 1f));
        drawerButton = createToolbarButton(toolbar, "Alle apps", () -> {
            drawerOpen = !drawerOpen;
            if (drawerOpen) showAllApps(); else showFavorites();
        }, true);
        createToolbarButton(toolbar, "Instellingen", this::openSettings, false);

        upperSpacer = new View(this);
        page.addView(upperSpacer, new LinearLayout.LayoutParams(1, 0, 1f));

        homeTitle = text("Wat kijken we vandaag?", 28, WHITE, true);
        homeTitle.setGravity(Gravity.CENTER);
        page.addView(homeTitle);

        scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setFocusable(false);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);
        tileGrid = new LinearLayout(this);
        tileGrid.setOrientation(LinearLayout.VERTICAL);
        tileGrid.setGravity(Gravity.CENTER_HORIZONTAL);
        tileGrid.setClipChildren(false);
        tileGrid.setClipToPadding(false);
        scroll.addView(tileGrid, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout.LayoutParams contentLayout = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(300));
        contentLayout.topMargin = dp(26);
        page.addView(scroll, contentLayout);

        lowerSpacer = new View(this);
        page.addView(lowerSpacer, new LinearLayout.LayoutParams(1, 0, 1f));

        time = text("--:--", 25, WHITE, false);
        time.setGravity(Gravity.CENTER);
        page.addView(time);
        date = text("", 13, DIM, false);
        date.setGravity(Gravity.CENTER);
        page.addView(date);
    }

    private TextView createToolbarButton(LinearLayout bar, String title,
                                          Runnable action, boolean spaceAfter) {
        TextView button = text(title, 15, DIM, false);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(19), dp(10), dp(19), dp(10));
        button.setFocusable(true);
        button.setClickable(true);
        button.setBackground(tileBackground(false, true));
        button.setOnFocusChangeListener((v, focused) -> {
            v.setBackground(tileBackground(focused, true));
            button.setTextColor(focused ? WHITE : DIM);
        });
        button.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (spaceAfter) lp.rightMargin = dp(12);
        bar.addView(button, lp);
        return button;
    }

    private void setMode(boolean showDrawer) {
        homeTitle.setVisibility(showDrawer ? View.GONE : View.VISIBLE);
        drawerButton.setText(showDrawer ? "Favorieten" : "Alle apps");
        LinearLayout.LayoutParams upper = (LinearLayout.LayoutParams) upperSpacer.getLayoutParams();
        upper.weight = showDrawer ? 0f : 1f;
        upperSpacer.setLayoutParams(upper);
        LinearLayout.LayoutParams lower = (LinearLayout.LayoutParams) lowerSpacer.getLayoutParams();
        lower.weight = showDrawer ? 0f : 1f;
        lowerSpacer.setLayoutParams(lower);
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) scroll.getLayoutParams();
        if (showDrawer) {
            lp.height = 0;
            lp.weight = 1f;
            lp.topMargin = dp(20);
            lp.bottomMargin = dp(13);
        } else {
            lp.height = dp(300);
            lp.weight = 0f;
            lp.topMargin = dp(27);
            lp.bottomMargin = 0;
        }
        scroll.setLayoutParams(lp);
    }

    private void showFavorites() {
        setMode(false);
        render(Arrays.asList(FAVORITES), false);
        tilesReady = true;
    }

    private void showAllApps() {
        setMode(true);
        PackageManager pm = getPackageManager();
        List<Favorite> apps = new ArrayList<>();
        for (ApplicationInfo info : pm.getInstalledApplications(0)) {
            if (!info.enabled || getPackageName().equals(info.packageName)) continue;
            if (!visibleTVApp(info)) continue;
            if (findLaunchIntent(pm, info.packageName) == null) continue;
            CharSequence name = pm.getApplicationLabel(info);
            apps.add(new Favorite(name == null ? info.packageName : name.toString(),
                    info.packageName));
        }
        Collections.sort(apps, (a, b) -> a.name.compareToIgnoreCase(b.name));
        render(apps, true);
        tilesReady = true;
    }

    /** Only real TV apps; never vendor services, launchers, power/input/DRM services. */
    private boolean visibleTVApp(ApplicationInfo info) {
        String p = info.packageName;
        if (p.startsWith("com.android.") || p.startsWith("com.mediatek.")
                || p.startsWith("vendor.") || p.equals("android")) return false;

        if (p.startsWith("org.droidtv.")) {
            return p.equals("org.droidtv.playtv")
                    || p.equals("org.droidtv.contentexplorer")
                    || p.equals("org.droidtv.android.fitness")
                    || p.startsWith("org.droidtv.nettvapp");
        }
        if (p.startsWith("com.google.android.")) {
            return p.equals("com.google.android.youtube.tv")
                    || p.equals("com.google.android.youtube.tvmusic")
                    || p.equals("com.google.android.videos")
                    || p.equals("com.google.android.play.games")
                    || p.equals("com.google.android.tv");
        }
        // Other launchable apps (Play Store OR APK) are shown together.
        return true;
    }

    private void render(List<Favorite> apps, boolean managerMode) {
        tileGrid.removeAllViews();
        scroll.scrollTo(0, 0);
        firstTile = null;
        PackageManager pm = getPackageManager();
        float density = getResources().getDisplayMetrics().density;
        int screenWidthDp = Math.round(getResources().getDisplayMetrics().widthPixels / density);
        int cardWidth = Math.max(113, Math.min(208, (screenWidthDp - 112 - 3 * 15) / 4));
        int cardHeight = managerMode ? 152 : 134;

        for (int start = 0; start < apps.size(); start += 4) {
            LinearLayout line = new LinearLayout(this);
            line.setClipChildren(false);
            line.setClipToPadding(false);
            line.setGravity(Gravity.CENTER);
            tileGrid.addView(line);
            for (int i = start; i < Math.min(start + 4, apps.size()); i++) {
                Favorite favorite = apps.get(i);
                View card = createAppCard(pm, favorite, managerMode);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        dp(cardWidth), dp(cardHeight));
                if ((i + 1) % 4 != 0) lp.rightMargin = dp(15);
                lp.bottomMargin = dp(15);
                line.addView(card, lp);
                if (firstTile == null) firstTile = card;
            }
        }
        if (firstTile != null) {
            View initial = firstTile;
            tileGrid.post(initial::requestFocus);
        }
    }

    private View createAppCard(PackageManager pm, Favorite favorite, boolean managerMode) {
        LinearLayout card = new LinearLayout(this);
        card.setGravity(Gravity.CENTER);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(8), dp(9), dp(8), dp(9));
        card.setFocusable(true);
        card.setClickable(true);
        card.setLongClickable(true);
        card.setContentDescription(favorite.name);
        card.setBackground(tileBackground(false, false));

        String pkg = packageFor(pm, favorite);
        if (pkg != null) {
            try {
                ImageView icon = new ImageView(this);
                icon.setImageDrawable(pm.getApplicationIcon(pkg));
                icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(51), dp(51));
                ip.bottomMargin = dp(8);
                card.addView(icon, ip);
            } catch (PackageManager.NameNotFoundException ignored) {
                addInitial(card, favorite.name);
            }
        } else {
            addInitial(card, favorite.name);
        }

        TextView label = text(favorite.name, 14, WHITE, true);
        label.setGravity(Gravity.CENTER);
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        card.addView(label, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        card.setOnFocusChangeListener((v, focused) -> {
            v.setBackground(tileBackground(focused, false));
            v.setElevation(focused ? dp(12) : 0f);
            v.animate().cancel();
            v.animate().scaleX(focused ? 1.035f : 1f)
                    .scaleY(focused ? 1.035f : 1f).setDuration(120).start();
        });
        card.setOnClickListener(v -> openApp(favorite));
        card.setOnLongClickListener(v -> {
            showActions(favorite);
            return true;
        });
        card.setOnKeyListener((v, keyCode, event) -> {
            if (managerMode && event.getAction() == KeyEvent.ACTION_DOWN
                    && (keyCode == KeyEvent.KEYCODE_MENU
                        || ((keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                                || keyCode == KeyEvent.KEYCODE_ENTER)
                            && event.getRepeatCount() == 1))) {
                showActions(favorite);
                return true;
            }
            return false;
        });

        if (managerMode && pkg != null) {
            final String selected = pkg;
            boolean system = isSystemApp(pm, selected);
            TextView action = text(system ? "App-info" : "Verwijderen", 12, GOLD, false);
            action.setGravity(Gravity.CENTER);
            action.setFocusable(true);
            action.setClickable(true);
            action.setPadding(dp(9), dp(5), dp(9), dp(5));
            action.setBackground(actionBackground(false));
            action.setOnFocusChangeListener((v, focused) -> {
                v.setBackground(actionBackground(focused));
                action.setTextColor(focused ? WHITE : GOLD);
            });
            action.setOnClickListener(v -> {
                if (system) openAppInfo(selected);
                else uninstallApp(selected);
            });
            LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            ap.topMargin = dp(5);
            card.addView(action, ap);
        }
        return card;
    }

    private void addInitial(LinearLayout card, String name) {
        TextView first = text(name.isEmpty() ? "?" : name.substring(0, 1), 29, GOLD, true);
        first.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(51), dp(51));
        lp.bottomMargin = dp(8);
        card.addView(first, lp);
    }

    private String packageFor(PackageManager pm, Favorite favorite) {
        for (String p : favorite.packages) {
            if (findLaunchIntent(pm, p) != null) return p;
        }
        return null;
    }

    private Intent findLaunchIntent(PackageManager pm, String pkg) {
        Intent launch = pm.getLeanbackLaunchIntentForPackage(pkg);
        if (launch == null) launch = pm.getLaunchIntentForPackage(pkg);
        if (launch != null) return launch;

        Intent query = new Intent(Intent.ACTION_MAIN);
        query.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        query.setPackage(pkg);
        List<ResolveInfo> hits = pm.queryIntentActivities(query, 0);
        if (!hits.isEmpty() && hits.get(0).activityInfo != null) {
            ResolveInfo selected = hits.get(0);
            query.setClassName(selected.activityInfo.packageName, selected.activityInfo.name);
            return query;
        }
        return null;
    }

    private void openApp(Favorite favorite) {
        PackageManager pm = getPackageManager();
        for (String pkg : favorite.packages) {
            Intent start = findLaunchIntent(pm, pkg);
            if (start == null) continue;
            start.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            try {
                startActivity(start);
                return;
            } catch (SecurityException | ActivityNotFoundException ignored) { }
        }
        Toast.makeText(this, favorite.name + " kon niet geopend worden.",
                Toast.LENGTH_LONG).show();
    }

    private boolean isSystemApp(PackageManager pm, String pkg) {
        try {
            ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
            return (info.flags & (ApplicationInfo.FLAG_SYSTEM
                    | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
        } catch (PackageManager.NameNotFoundException ignored) {
            return true; // Fail closed: no deletion for an unknown package.
        }
    }

    private void showActions(Favorite favorite) {
        String pkg = packageFor(getPackageManager(), favorite);
        if (pkg == null) return;
        boolean system = isSystemApp(getPackageManager(), pkg);
        String[] options = system
                ? new String[]{"Openen", "App-info", "Annuleren"}
                : new String[]{"Openen", "Verwijderen", "App-info", "Annuleren"};
        new AlertDialog.Builder(this)
                .setTitle(favorite.name)
                .setItems(options, (dialog, position) -> {
                    String option = options[position];
                    if ("Openen".equals(option)) openApp(favorite);
                    else if ("Verwijderen".equals(option)) uninstallApp(pkg);
                    else if ("App-info".equals(option)) openAppInfo(pkg);
                })
                .show();
    }

    private void uninstallApp(String pkg) {
        if (pkg == null || pkg.equals(getPackageName())
                || isSystemApp(getPackageManager(), pkg)) {
            if (pkg != null) openAppInfo(pkg);
            return;
        }
        // Android owns the uninstall dialog. No silent removal and no root.
        try {
            Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE,
                    Uri.parse("package:" + pkg));
            intent.putExtra(Intent.EXTRA_RETURN_RESULT, false);
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            openAppInfo(pkg);
            Toast.makeText(this, "Verwijderen via Android App-info.", Toast.LENGTH_LONG).show();
        }
    }

    private void openAppInfo(String pkg) {
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + pkg)));
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, "App-instellingen zijn niet beschikbaar.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void openPlayStore() {
        Intent start = findLaunchIntent(getPackageManager(), "com.android.vending");
        if (start == null) {
            start = new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=apps"));
            start.setPackage("com.android.vending");
        }
        try {
            startActivity(start);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, "Play Store kon niet geopend worden.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void openSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, "Open Instellingen via de afstandsbediening.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private TextView text(String contents, float sizeSp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(contents);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        view.setTypeface(Typeface.create(
                bold ? "sans-serif-medium" : "sans-serif-light",
                bold ? Typeface.BOLD : Typeface.NORMAL));
        return view;
    }

    private GradientDrawable tileBackground(boolean focus, boolean pill) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(focus ? FOCUSED : pill ? 0xB728303A : CARD);
        drawable.setCornerRadius(dp(pill ? 15 : 18));
        drawable.setStroke(dp(focus ? 2 : 1), focus ? GOLD : 0x665F6D75);
        return drawable;
    }

    private GradientDrawable actionBackground(boolean focus) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(focus ? 0xAA8F6651 : 0x443B3734);
        drawable.setCornerRadius(dp(8));
        return drawable;
    }

    private int dp(float n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }

    /** Static atmospheric wallpaper, drawn without network requests or a large bitmap. */
    private static final class CozyBackdrop extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        @Override public void draw(Canvas canvas) {
            Rect bounds = getBounds();
            float w = bounds.width(), h = bounds.height();
            paint.setShader(new LinearGradient(0, 0, w, h,
                    new int[]{0xFF101B26, 0xFF1D2630, 0xFF171B22},
                    null, Shader.TileMode.CLAMP));
            canvas.drawRect(bounds, paint);

            // Warm indirect lamp light; subdued so text and app artwork stay readable.
            drawGlow(canvas, w * .80f, h * .16f, w * .68f, 0x487B573F);
            drawGlow(canvas, w * .07f, h * .85f, w * .58f, 0x26467786);
            drawGlow(canvas, w * .47f, h * 1.14f, w * .70f, 0x1A987D5E);

            paint.setShader(null);
            paint.setColor(0x136D7580);
            canvas.drawCircle(w * .90f, h * .05f, w * .27f, paint);
            paint.setColor(0x0EFFF1CF);
            canvas.drawCircle(w * .82f, h * .17f, w * .12f, paint);
        }

        private void drawGlow(Canvas canvas, float x, float y, float radius, int center) {
            paint.setShader(new RadialGradient(x, y, radius,
                    new int[]{center, center & 0x00FFFFFF, 0x00000000},
                    new float[]{0f, .58f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawCircle(x, y, radius, paint);
        }

        @Override public void setAlpha(int alpha) { }
        @Override public void setColorFilter(ColorFilter filter) { }
        @Override public int getOpacity() { return PixelFormat.OPAQUE; }
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
