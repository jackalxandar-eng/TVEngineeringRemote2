package com.tvengineering.remote;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.tvengineering.remote.core.IrEngine;
import com.tvengineering.remote.data.DeviceProfile;
import com.tvengineering.remote.data.IrCommand;
import com.tvengineering.remote.data.ProfileStore;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(7, 11, 19);
    private static final int PANEL = Color.rgb(17, 26, 43);
    private static final int PANEL2 = Color.rgb(11, 19, 32);
    private static final int TEXT = Color.rgb(240, 245, 255);
    private static final int MUTED = Color.rgb(156, 176, 205);
    private static final int ACCENT = Color.rgb(79, 140, 255);
    private static final int DANGER = Color.rgb(230, 78, 103);
    private static final int SUCCESS = Color.rgb(57, 217, 138);

    private static final int REQ_IMPORT = 1001;
    private static final int REQ_EXPORT = 1002;

    private IrEngine ir;
    private ProfileStore store;
    private List<DeviceProfile> profiles;
    private DeviceProfile selected;
    private boolean engineerMode = false;

    private LinearLayout pageHost;
    private TextView statusText;
    private TextView logText;
    private Spinner profileSpinner;
    private final List<String> logLines = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ir = new IrEngine(this);
        store = new ProfileStore(this);
        profiles = store.load();
        selected = profiles.get(0);
        buildShell();
        showRemote();
        log("APP ready");
        log(ir.hasEmitter() ? "IR emitter detected" : "No Consumer IR emitter detected on this device");
    }

    private void buildShell() {
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(12), dp(10), dp(12), dp(10));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(14), dp(12), dp(14), dp(12));
        header.setBackgroundColor(PANEL);

        TextView title = text("TV Engineering Remote", 22, TEXT, true);
        header.addView(title);
        statusText = text(irStatus(), 12, ir.hasEmitter() ? SUCCESS : DANGER, false);
        statusText.setPadding(0, dp(4), 0, 0);
        header.addView(statusText);
        root.addView(header, matchWrap());

        HorizontalScrollView tabsScroll = new HorizontalScrollView(this);
        tabsScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setPadding(0, dp(8), 0, dp(8));
        tabs.addView(tabButton("Remote", v -> showRemote()));
        tabs.addView(tabButton("Profiles", v -> showProfiles()));
        tabs.addView(tabButton("Engineer", v -> showEngineer()));
        tabs.addView(tabButton("Service", v -> showService()));
        tabs.addView(tabButton("Logs", v -> showLogs()));
        tabsScroll.addView(tabs);
        root.addView(tabsScroll, matchWrap());

        pageHost = new LinearLayout(this);
        pageHost.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(pageHost, matchWrap());
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        setContentView(root);
    }

    private String irStatus() {
        return ir.hasEmitter() ? "IR READY • " + ir.carrierRangesText() : "IR unavailable • use a phone with Consumer IR / IR Blaster";
    }

    private void clearPage() {
        pageHost.removeAllViews();
        pageHost.setPadding(0, 0, 0, dp(24));
    }

    private void showRemote() {
        clearPage();
        addSectionTitle("Remote Control");
        profileSpinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, profileNames());
        profileSpinner.setAdapter(adapter);
        profileSpinner.setSelection(Math.max(0, profiles.indexOf(selected)));
        profileSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selected = profiles.get(position);
                log("Selected profile: " + selected.name);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        pageHost.addView(profileSpinner, matchWrap());

        addInfoCard("Hardware", irStatus());
        addInfoCard("Profile", selected.name + " • " + selected.manufacturer + " • " + selected.model);

        LinearLayout powerRow = horizontal();
        Button power = actionButton("POWER", DANGER, v -> sendKey("POWER"));
        powerRow.addView(power, weighted());
        Button source = actionButton("SOURCE", ACCENT, v -> sendKey("SOURCE"));
        powerRow.addView(source, weighted());
        pageHost.addView(powerRow, matchWrap());

        addRemoteRow(new String[]{"HOME", "MENU", "INFO"});
        addRemoteRow(new String[]{"UP"});
        addRemoteRow(new String[]{"LEFT", "OK", "RIGHT"});
        addRemoteRow(new String[]{"DOWN"});
        addRemoteRow(new String[]{"BACK", "EXIT", "MUTE"});
        addRemoteRow(new String[]{"VOL_DOWN", "VOL_UP", "CH_UP", "CH_DOWN"});
        addRemoteRow(new String[]{"PLAY_PAUSE", "STOP", "GUIDE"});

        addSectionTitle("Numeric keypad");
        for (int r = 0; r < 3; r++) {
            String[] keys = new String[3];
            for (int c = 0; c < 3; c++) keys[c] = String.valueOf(r * 3 + c + 1);
            addRemoteRow(keys);
        }
        addRemoteRow(new String[]{"0"});

        TextView hint = text("If a button is not defined in the selected profile, create/import its IR code in Engineer mode.", 12, MUTED, false);
        hint.setPadding(dp(4), dp(16), dp(4), 0);
        pageHost.addView(hint);
    }

    private void addRemoteRow(String[] keys) {
        LinearLayout row = horizontal();
        for (String key : keys) row.addView(actionButton(key, PANEL, v -> sendKey(key)), weighted());
        pageHost.addView(row, matchWrap());
    }

    private void sendKey(String key) {
        IrCommand c = selected.commands.get(key);
        if (c == null) {
            log("Missing command " + key + " in profile " + selected.name);
            toast("Command not defined: " + key);
            return;
        }
        if (c.service && !engineerMode) {
            toast("Engineer mode required");
            return;
        }
        if (c.dangerous) {
            new AlertDialog.Builder(this)
                    .setTitle("Sensitive IR command")
                    .setMessage("This command is marked dangerous and may change service/factory settings. Send only to a TV you are authorized to service.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Send", (d, w) -> transmit(c, key))
                    .show();
        } else transmit(c, key);
    }

    private void transmit(IrCommand c, String label) {
        try {
            ir.transmit(c);
            log("TX " + label + " • " + c.type + " • " + c.carrierHz + " Hz");
            toast("Sent: " + label);
        } catch (Exception e) {
            log("ERROR " + label + " • " + e.getMessage());
            toast(e.getMessage());
        }
    }

    private void showProfiles() {
        clearPage();
        addSectionTitle("TV Profiles");
        for (DeviceProfile p : profiles) {
            LinearLayout card = card();
            card.addView(text(p.name, 17, TEXT, true));
            card.addView(text(p.manufacturer + " • " + p.model + " • " + p.commands.size() + " commands", 12, MUTED, false));
            LinearLayout row = horizontal();
            row.addView(actionButton("Use", ACCENT, v -> { selected = p; showRemote(); }), weighted());
            row.addView(actionButton("Export", PANEL, v -> exportProfile(p)), weighted());
            if (!"starter".equals(p.id)) row.addView(actionButton("Delete", DANGER, v -> confirmDelete(p)), weighted());
            card.addView(row);
            pageHost.addView(card, matchWrap());
        }

        LinearLayout row = horizontal();
        row.addView(actionButton("New profile", ACCENT, v -> newProfileDialog()), weighted());
        row.addView(actionButton("Import JSON", PANEL, v -> importProfile()), weighted());
        pageHost.addView(row, matchWrap());

        addInfoCard("Profile format", "Profiles are local JSON files. Commands can be RAW microseconds, NEC address/command, 32-bit pulse-distance data, or learned Pronto 0000.");
    }

    private void newProfileDialog() {
        LinearLayout box = vertical();
        EditText name = input("Profile name");
        EditText maker = input("Manufacturer");
        EditText model = input("Model");
        box.addView(name); box.addView(maker); box.addView(model);
        new AlertDialog.Builder(this)
                .setTitle("Create TV profile")
                .setView(box)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Create", (d, w) -> {
                    DeviceProfile p = new DeviceProfile();
                    p.id = "p-" + System.currentTimeMillis();
                    p.name = nonEmpty(name.getText().toString(), "New TV");
                    p.manufacturer = nonEmpty(maker.getText().toString(), "Generic");
                    p.model = nonEmpty(model.getText().toString(), "Unknown");
                    profiles.add(p); selected = p; store.save(profiles); showProfiles();
                    log("Created profile " + p.name);
                }).show();
    }

    private void confirmDelete(DeviceProfile p) {
        new AlertDialog.Builder(this).setTitle("Delete profile?").setMessage(p.name)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (d, w) -> {
                    profiles.remove(p);
                    if (profiles.isEmpty()) profiles = store.load();
                    selected = profiles.get(0); store.save(profiles); showProfiles();
                }).show();
    }

    private void showEngineer() {
        clearPage();
        addSectionTitle("Engineering Lab");
        addInfoCard("IR hardware", irStatus());

        Button unlock = actionButton(engineerMode ? "Engineer mode: ENABLED" : "Unlock Engineer mode", engineerMode ? SUCCESS : DANGER, v -> unlockEngineer());
        pageHost.addView(unlock, matchWrap());

        if (!engineerMode) {
            addInfoCard("Locked", "Advanced command creation and service controls are disabled. Unlock only when you are working on equipment you own or are authorized to service.");
            return;
        }

        EditText key = input("Button key, e.g. POWER or PANEL_TEST");
        EditText name = input("Display name");
        Spinner type = new Spinner(this);
        type.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"RAW", "NEC", "PULSE32", "PRONTO"}));
        EditText carrier = input("Carrier Hz (default 38000)"); carrier.setText("38000");
        EditText address = input("NEC address: decimal or 0x.. ");
        EditText command = input("NEC command: decimal or 0x.. ");
        EditText data32 = input("32-bit data: decimal or 0x........");
        EditText raw = input("RAW microseconds: 9000,4500,560,...");
        raw.setMinLines(3);
        EditText pronto = input("Pronto 0000 hex string"); pronto.setMinLines(3);

        pageHost.addView(key); pageHost.addView(name); pageHost.addView(type); pageHost.addView(carrier);
        pageHost.addView(address); pageHost.addView(command); pageHost.addView(data32); pageHost.addView(raw); pageHost.addView(pronto);

        Button test = actionButton("Test current command", ACCENT, v -> {
            try { transmit(buildCommand(name, type, carrier, address, command, data32, raw, pronto, false, false), "TEST"); }
            catch (Exception e) { toast(e.getMessage()); log("Builder error: " + e.getMessage()); }
        });
        pageHost.addView(test, matchWrap());

        Button saveNormal = actionButton("Save as normal button", PANEL, v -> saveBuiltCommand(key, name, type, carrier, address, command, data32, raw, pronto, false, false));
        Button saveService = actionButton("Save as service command", PANEL, v -> saveBuiltCommand(key, name, type, carrier, address, command, data32, raw, pronto, true, false));
        Button saveDanger = actionButton("Save as sensitive service command", DANGER, v -> saveBuiltCommand(key, name, type, carrier, address, command, data32, raw, pronto, true, true));
        pageHost.addView(saveNormal, matchWrap()); pageHost.addView(saveService, matchWrap()); pageHost.addView(saveDanger, matchWrap());

        addInfoCard("Learn IR", "Android ConsumerIrManager transmits IR but does not provide an IR receiver API. To learn a remote, use an external receiver and import RAW or Pronto 0000 data here.");
    }

    private IrCommand buildCommand(EditText name, Spinner type, EditText carrier, EditText address, EditText command, EditText data32, EditText raw, EditText pronto, boolean service, boolean dangerous) {
        IrCommand c = new IrCommand();
        c.name = nonEmpty(name.getText().toString(), "COMMAND");
        c.type = IrCommand.Type.valueOf(type.getSelectedItem().toString());
        c.carrierHz = Integer.parseInt(nonEmpty(carrier.getText().toString(), "38000"));
        c.service = service; c.dangerous = dangerous;
        switch (c.type) {
            case NEC:
                c.address = IrEngine.parseIntFlexible(nonEmpty(address.getText().toString(), "0"));
                c.command = IrEngine.parseIntFlexible(nonEmpty(command.getText().toString(), "0"));
                break;
            case PULSE32:
                c.data32 = IrEngine.parseLongFlexible(nonEmpty(data32.getText().toString(), "0"));
                break;
            case PRONTO:
                c.pronto = pronto.getText().toString().trim();
                break;
            case RAW:
            default:
                c.rawPattern = IrEngine.parseRaw(raw.getText().toString());
                break;
        }
        return c;
    }

    private void saveBuiltCommand(EditText key, EditText name, Spinner type, EditText carrier, EditText address, EditText command, EditText data32, EditText raw, EditText pronto, boolean service, boolean dangerous) {
        try {
            String k = key.getText().toString().trim().toUpperCase(Locale.US);
            if (k.isEmpty()) throw new IllegalArgumentException("Enter a button key.");
            IrCommand c = buildCommand(name, type, carrier, address, command, data32, raw, pronto, service, dangerous);
            selected.commands.put(k, c);
            store.save(profiles);
            log("Saved " + k + " to " + selected.name + (service ? " [service]" : ""));
            toast("Saved: " + k);
        } catch (Exception e) { toast(e.getMessage()); }
    }

    private void unlockEngineer() {
        if (engineerMode) {
            engineerMode = false; showEngineer(); log("Engineer mode disabled"); return;
        }
        EditText phrase = input("Type ENGINEER");
        phrase.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        new AlertDialog.Builder(this)
                .setTitle("Engineer mode")
                .setMessage("Advanced IR commands can alter factory/service settings. Type ENGINEER to continue.")
                .setView(phrase)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Unlock", (d, w) -> {
                    if ("ENGINEER".equalsIgnoreCase(phrase.getText().toString().trim())) {
                        engineerMode = true; log("Engineer mode enabled"); showEngineer();
                    } else toast("Phrase did not match");
                }).show();
    }

    private void showService() {
        clearPage();
        addSectionTitle("Service / Hidden Commands");
        if (!engineerMode) {
            addInfoCard("Locked", "Unlock Engineer mode first. Service commands are loaded only from the active device profile; this app does not invent vendor factory codes.");
            pageHost.addView(actionButton("Unlock", DANGER, v -> unlockEngineer()), matchWrap());
            return;
        }
        boolean any = false;
        for (Map.Entry<String, IrCommand> e : selected.commands.entrySet()) {
            IrCommand c = e.getValue();
            if (!c.service) continue;
            any = true;
            LinearLayout row = card();
            row.addView(text(e.getKey() + " • " + c.name, 16, c.dangerous ? DANGER : TEXT, true));
            row.addView(text(c.type + " • " + c.carrierHz + " Hz" + (c.dangerous ? " • SENSITIVE" : ""), 12, MUTED, false));
            row.addView(actionButton("Send", c.dangerous ? DANGER : ACCENT, v -> sendKey(e.getKey())), matchWrap());
            pageHost.addView(row, matchWrap());
        }
        if (!any) addInfoCard("No service commands", "Add documented service commands for this exact TV model from Engineer mode, then they will appear here.");
    }

    private void showLogs() {
        clearPage();
        addSectionTitle("IR Event Log");
        logText = text(logTextValue(), 12, TEXT, false);
        logText.setTypeface(android.graphics.Typeface.MONOSPACE);
        logText.setPadding(dp(12), dp(12), dp(12), dp(12));
        logText.setBackgroundColor(PANEL2);
        pageHost.addView(logText, matchWrap());
        pageHost.addView(actionButton("Clear log", PANEL, v -> { logLines.clear(); showLogs(); }), matchWrap());
    }

    private void importProfile() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("application/json");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(i, REQ_IMPORT);
    }

    private void exportProfile(DeviceProfile p) {
        selected = p;
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.setType("application/json");
        i.putExtra(Intent.EXTRA_TITLE, safeFileName(p.name) + ".json");
        startActivityForResult(i, REQ_EXPORT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            if (requestCode == REQ_IMPORT) {
                String json = readAll(uri);
                DeviceProfile p = store.importProfile(json);
                profiles.add(p); selected = p; store.save(profiles); log("Imported profile " + p.name); showProfiles();
            } else if (requestCode == REQ_EXPORT) {
                String json = store.exportProfile(selected);
                try (OutputStream os = getContentResolver().openOutputStream(uri, "wt")) {
                    if (os == null) throw new IllegalStateException("Cannot open output document");
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }
                toast("Profile exported");
            }
        } catch (Exception e) { toast(e.getMessage()); log("File error: " + e.getMessage()); }
    }

    private String readAll(Uri uri) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = getContentResolver().openInputStream(uri);
             BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line; while ((line = br.readLine()) != null) sb.append(line).append('\n');
        }
        return sb.toString();
    }

    private List<String> profileNames() {
        List<String> names = new ArrayList<>();
        for (DeviceProfile p : profiles) names.add(p.name + " — " + p.manufacturer);
        return names;
    }

    private void log(String msg) {
        String t = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
        logLines.add(0, "[" + t + "] " + msg);
        if (logLines.size() > 200) logLines.remove(logLines.size() - 1);
        if (logText != null) logText.setText(logTextValue());
    }

    private String logTextValue() {
        if (logLines.isEmpty()) return "No events yet.";
        StringBuilder sb = new StringBuilder();
        for (String s : logLines) sb.append(s).append('\n');
        return sb.toString();
    }

    private void addSectionTitle(String title) {
        TextView t = text(title, 20, TEXT, true);
        t.setPadding(dp(4), dp(14), dp(4), dp(10));
        pageHost.addView(t);
    }

    private void addInfoCard(String title, String body) {
        LinearLayout c = card();
        c.addView(text(title, 15, TEXT, true));
        TextView b = text(body, 12, MUTED, false); b.setPadding(0, dp(5), 0, 0); c.addView(b);
        pageHost.addView(c, matchWrap());
    }

    private LinearLayout card() {
        LinearLayout c = vertical();
        c.setPadding(dp(14), dp(12), dp(14), dp(12));
        c.setBackgroundColor(PANEL);
        LinearLayout.LayoutParams lp = matchWrap();
        lp.setMargins(0, dp(6), 0, dp(6));
        c.setLayoutParams(lp);
        return c;
    }

    private Button tabButton(String label, View.OnClickListener l) {
        Button b = actionButton(label, PANEL, l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(120), dp(48));
        lp.setMargins(dp(4), 0, dp(4), 0);
        b.setLayoutParams(lp);
        return b;
    }

    private Button actionButton(String label, int color, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(TEXT);
        b.setTextSize(13);
        b.setAllCaps(false);
        b.setBackgroundColor(color);
        b.setOnClickListener(l);
        b.setPadding(dp(8), dp(4), dp(8), dp(4));
        LinearLayout.LayoutParams lp = matchWrap();
        lp.setMargins(dp(4), dp(5), dp(4), dp(5));
        b.setLayoutParams(lp);
        return b;
    }

    private EditText input(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(MUTED);
        e.setTextColor(TEXT);
        e.setSingleLine(false);
        e.setPadding(dp(12), dp(10), dp(12), dp(10));
        e.setBackgroundColor(PANEL2);
        LinearLayout.LayoutParams lp = matchWrap(); lp.setMargins(0, dp(5), 0, dp(5)); e.setLayoutParams(lp);
        return e;
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(sp); t.setTextColor(color);
        if (bold) t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        return t;
    }

    private LinearLayout vertical() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout horizontal() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private LinearLayout.LayoutParams matchWrap() { return new LinearLayout.LayoutParams(-1, -2); }
    private LinearLayout.LayoutParams weighted() { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(52), 1f); p.setMargins(dp(4), dp(4), dp(4), dp(4)); return p; }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
    private String nonEmpty(String s, String fallback) { return s == null || s.trim().isEmpty() ? fallback : s.trim(); }
    private String safeFileName(String s) { return s.replaceAll("[^a-zA-Z0-9._-]+", "_"); }
}
