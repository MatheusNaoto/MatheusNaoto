package com.minimo.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextClock;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Launcher minimalista: só texto, fundo preto, sem ícones, sem animações,
 * sem bibliotecas externas e sem nada rodando em segundo plano.
 */
public class MainActivity extends Activity {

    private static final String PREFS = "minimo";
    private static final String KEY_FAVS = "favs";
    private static final int GREY = 0xFF8A8A8A;

    static final class App {
        final String label;
        final ComponentName component;

        App(String label, ComponentName component) {
            this.label = label;
            this.component = component;
        }

        String key() {
            return component.flattenToShortString();
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private final List<App> allApps = new ArrayList<>();
    private final List<App> shownApps = new ArrayList<>();
    private ArrayAdapter<App> adapter;

    private SharedPreferences prefs;
    private LinearLayout home;
    private LinearLayout favBox;
    private LinearLayout drawer;
    private EditText search;
    private float density;

    private final BroadcastReceiver packageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadApps();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        density = getResources().getDisplayMetrics().density;

        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(Color.BLACK);
            getWindow().setNavigationBarColor(Color.BLACK);
        }

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        root.addView(buildHome());
        root.addView(buildDrawer());
        setContentView(root);

        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_PACKAGE_ADDED);
        f.addAction(Intent.ACTION_PACKAGE_REMOVED);
        f.addAction(Intent.ACTION_PACKAGE_CHANGED);
        f.addDataScheme("package");
        registerReceiver(packageReceiver, f);

        loadApps();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(packageReceiver);
        super.onDestroy();
    }

    /** Botão Home pressionado com o launcher já aberto: volta para a tela inicial. */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        showHome();
    }

    @Override
    public void onBackPressed() {
        if (drawer.getVisibility() == View.VISIBLE) {
            showHome();
        }
        // Na tela inicial o "voltar" não faz nada (comportamento de launcher).
    }

    // ---------------------------------------------------------------- UI

    private int dp(int v) {
        return Math.round(v * density);
    }

    private TextView text(String s, int sp, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setSingleLine(true);
        return t;
    }

    private View buildHome() {
        home = new LinearLayout(this);
        home.setOrientation(LinearLayout.VERTICAL);
        home.setPadding(dp(28), dp(48), dp(28), dp(32));

        TextClock clock = new TextClock(this);
        clock.setFormat24Hour("HH:mm");
        clock.setFormat12Hour("HH:mm");
        clock.setTextColor(Color.WHITE);
        clock.setTextSize(TypedValue.COMPLEX_UNIT_SP, 56);
        clock.setOnClickListener(v -> openIntent(new Intent(AlarmClock.ACTION_SHOW_ALARMS)));
        home.addView(clock);

        TextClock date = new TextClock(this);
        date.setFormat24Hour("EEEE, d 'de' MMMM");
        date.setFormat12Hour("EEEE, d 'de' MMMM");
        date.setTextColor(GREY);
        date.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        home.addView(date);

        View spacer = new View(this);
        home.addView(spacer, new LinearLayout.LayoutParams(0, 0, 1f));

        favBox = new LinearLayout(this);
        favBox.setOrientation(LinearLayout.VERTICAL);
        home.addView(favBox);

        TextView all = text("todos os apps  ›", 16, GREY);
        all.setPadding(0, dp(28), 0, dp(8));
        all.setOnClickListener(v -> showDrawer());
        home.addView(all);

        // Toque longo em qualquer área vazia: configurações do launcher.
        home.setOnLongClickListener(v -> {
            showLauncherMenu();
            return true;
        });
        return home;
    }

    private View buildDrawer() {
        drawer = new LinearLayout(this);
        drawer.setOrientation(LinearLayout.VERTICAL);
        drawer.setBackgroundColor(Color.BLACK);
        drawer.setPadding(dp(28), dp(32), dp(28), 0);
        drawer.setVisibility(View.GONE);

        search = new EditText(this);
        search.setHint("buscar…");
        search.setHintTextColor(0xFF555555);
        search.setTextColor(Color.WHITE);
        search.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        search.setSingleLine(true);
        search.setBackgroundColor(Color.TRANSPARENT);
        search.setPadding(0, dp(8), 0, dp(16));
        search.setImeOptions(EditorInfo.IME_ACTION_GO);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { filter(s.toString()); }
        });
        search.setOnEditorActionListener((v, actionId, event) -> {
            boolean go = actionId == EditorInfo.IME_ACTION_GO
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER);
            if (go && !shownApps.isEmpty()) {
                launch(shownApps.get(0));
                return true;
            }
            return false;
        });
        drawer.addView(search);

        ListView list = new ListView(this);
        list.setDivider(null);
        list.setSelector(android.R.color.transparent);
        list.setVerticalScrollBarEnabled(false);
        list.setOverScrollMode(View.OVER_SCROLL_NEVER);
        adapter = new ArrayAdapter<App>(this, 0, shownApps) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView t = convertView instanceof TextView
                        ? (TextView) convertView
                        : text("", 20, Color.WHITE);
                t.setPadding(0, dp(10), 0, dp(10));
                t.setText(getItem(position).label);
                return t;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener((AdapterView<?> p, View v, int pos, long id) -> launch(shownApps.get(pos)));
        list.setOnItemLongClickListener((p, v, pos, id) -> {
            showAppMenu(shownApps.get(pos));
            return true;
        });
        drawer.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        return drawer;
    }

    private void showDrawer() {
        search.setText("");
        drawer.setVisibility(View.VISIBLE);
        home.setVisibility(View.GONE);
    }

    private void showHome() {
        if (search != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(search.getWindowToken(), 0);
            search.setText("");
        }
        drawer.setVisibility(View.GONE);
        home.setVisibility(View.VISIBLE);
    }

    // ---------------------------------------------------------------- dados

    private void loadApps() {
        new Thread(() -> {
            PackageManager pm = getPackageManager();
            Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> infos = pm.queryIntentActivities(main, 0);
            final List<App> loaded = new ArrayList<>(infos.size());
            for (ResolveInfo ri : infos) {
                if (getPackageName().equals(ri.activityInfo.packageName)) continue;
                loaded.add(new App(ri.loadLabel(pm).toString(),
                        new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name)));
            }
            Collections.sort(loaded, (a, b) -> a.label.compareToIgnoreCase(b.label));
            runOnUiThread(() -> {
                allApps.clear();
                allApps.addAll(loaded);
                filter(search.getText().toString());
                renderFavorites();
            });
        }).start();
    }

    private void filter(String q) {
        String query = q.trim().toLowerCase(Locale.ROOT);
        shownApps.clear();
        for (App a : allApps) {
            if (query.isEmpty() || a.label.toLowerCase(Locale.ROOT).contains(query)) {
                shownApps.add(a);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private List<String> favKeys() {
        List<String> keys = new ArrayList<>();
        String raw = prefs.getString(KEY_FAVS, "");
        for (String k : raw.split(";")) {
            if (!k.isEmpty()) keys.add(k);
        }
        return keys;
    }

    private void saveFavKeys(List<String> keys) {
        StringBuilder sb = new StringBuilder();
        for (String k : keys) {
            if (sb.length() > 0) sb.append(';');
            sb.append(k);
        }
        prefs.edit().putString(KEY_FAVS, sb.toString()).apply();
        renderFavorites();
    }

    private App findApp(String key) {
        for (App a : allApps) {
            if (a.key().equals(key)) return a;
        }
        return null;
    }

    private void renderFavorites() {
        favBox.removeAllViews();
        List<String> keys = favKeys();
        if (keys.isEmpty()) {
            TextView hint = text("toque longo num app da lista\npara fixar aqui", 14, 0xFF555555);
            hint.setSingleLine(false);
            favBox.addView(hint);
            return;
        }
        for (String k : keys) {
            final App a = findApp(k);
            if (a == null) continue; // app desinstalado
            TextView t = text(a.label, 26, Color.WHITE);
            t.setPadding(0, dp(9), 0, dp(9));
            t.setOnClickListener(v -> launch(a));
            t.setOnLongClickListener(v -> {
                showAppMenu(a);
                return true;
            });
            favBox.addView(t);
        }
    }

    // ---------------------------------------------------------------- ações

    private void launch(App a) {
        Intent i = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(a.component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        openIntent(i);
    }

    private void openIntent(Intent i) {
        try {
            startActivity(i);
        } catch (Exception ignored) {
            // App indisponível: simplesmente não faz nada.
        }
    }

    private void showAppMenu(final App a) {
        final List<String> keys = favKeys();
        final boolean isFav = keys.contains(a.key());
        final int index = keys.indexOf(a.key());

        List<String> options = new ArrayList<>();
        options.add(isFav ? "Remover da tela inicial" : "Fixar na tela inicial");
        if (isFav && index > 0) options.add("Mover para cima");
        if (isFav && index < keys.size() - 1) options.add("Mover para baixo");
        options.add("Informações do app");
        options.add("Desinstalar");
        final String[] items = options.toArray(new String[0]);

        new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                .setTitle(a.label)
                .setItems(items, (d, which) -> {
                    String choice = items[which];
                    String pkg = a.component.getPackageName();
                    if (choice.startsWith("Fixar")) {
                        keys.add(a.key());
                        saveFavKeys(keys);
                    } else if (choice.startsWith("Remover")) {
                        keys.remove(a.key());
                        saveFavKeys(keys);
                    } else if (choice.equals("Mover para cima")) {
                        Collections.swap(keys, index, index - 1);
                        saveFavKeys(keys);
                    } else if (choice.equals("Mover para baixo")) {
                        Collections.swap(keys, index, index + 1);
                        saveFavKeys(keys);
                    } else if (choice.startsWith("Informações")) {
                        openIntent(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:" + pkg)));
                    } else if (choice.equals("Desinstalar")) {
                        openIntent(new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + pkg)));
                    }
                })
                .show();
    }

    private void showLauncherMenu() {
        final String[] items = {"Configurações do Android", "Escolher launcher padrão", "Limpar apps fixados"};
        new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                .setItems(items, (d, which) -> {
                    if (which == 0) {
                        openIntent(new Intent(Settings.ACTION_SETTINGS));
                    } else if (which == 1) {
                        openIntent(new Intent(Settings.ACTION_HOME_SETTINGS));
                    } else {
                        saveFavKeys(new ArrayList<>());
                    }
                })
                .show();
    }
}
