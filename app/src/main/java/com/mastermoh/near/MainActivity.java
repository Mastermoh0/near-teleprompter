package com.mastermoh.near;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.View;
import android.view.WindowInsets;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends AppCompatActivity {
    private static final int IMPORT = 1, EXPORT = 2, NOTIFICATIONS = 3;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService files = Executors.newSingleThreadExecutor();
    private ScriptStore store;
    private ScriptStore.Script script;
    private PrompterSettings settings;
    private TextInputEditText title, body;
    private TextView stats;
    private ReaderView preview;
    private boolean loading;
    private String exportBody;
    private boolean receiverRegistered;
    private final Runnable refreshDraft = () -> {
        if (store != null) { store.save(); updatePreview(); }
    };
    private final BroadcastReceiver readerStatus = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            updateReaderState();
            String error = intent.getStringExtra("error");
            if (error != null) message(error);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        View root = findViewById(R.id.root);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets safe = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
                view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            } else {
                view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            }
            return insets;
        });
        boolean dark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        getWindow().getDecorView().setSystemUiVisibility(dark ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        settings = new PrompterSettings(this);
        store = new ScriptStore(this);
        script = store.current();
        title = findViewById(R.id.script_title);
        body = findViewById(R.id.script_body);
        stats = findViewById(R.id.script_stats);
        preview = findViewById(R.id.reader_preview);
        body.setOnTouchListener((view, event) -> {
            view.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        Menu menu = toolbar.getMenu();
        menu.add(0, 1, 0, R.string.import_script);
        menu.add(0, 2, 1, R.string.export_script);
        menu.add(0, 3, 2, R.string.delete_script);
        menu.add(0, 4, 3, R.string.help);
        menu.add(0, 5, 4, R.string.updates_title);
        toolbar.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1: importScript(); break;
                case 2: exportScript(); break;
                case 3: deleteScript(); break;
                case 4: new MaterialAlertDialogBuilder(this).setTitle(R.string.help).setMessage(R.string.help_body).setPositiveButton(android.R.string.ok, null).show(); break;
                case 5: startActivity(new Intent(this,UpdatesActivity.class)); break;
                default: return false;
            }
            return true;
        });
        findViewById(R.id.library).setOnClickListener(this::showLibrary);
        findViewById(R.id.updates_banner).setOnClickListener(v -> startActivity(new Intent(this,UpdatesActivity.class)));
        findViewById(R.id.launch).setOnClickListener(view -> launchReader());
        findViewById(R.id.record_in_near).setOnClickListener(view -> {
            handler.removeCallbacks(refreshDraft); store.save();
            if (script.body.trim().isEmpty()) { message(getString(R.string.empty_script)); body.requestFocus(); return; }
            stopService(new Intent(this, PrompterService.class));
            startActivity(new Intent(this, CameraActivity.class));
        });
        findViewById(R.id.close_reader).setOnClickListener(view -> {
            stopService(new Intent(this, PrompterService.class));
            handler.postDelayed(this::updateReaderState, 150);
        });

        Slider size = findViewById(R.id.text_size);
        size.setValue(settings.textSize());
        size.addOnChangeListener((slider, value, fromUser) -> {
            settings.textSize(Math.round(value)); updatePreview();
        });
        Slider pace = findViewById(R.id.pace);
        pace.setValue(settings.pace());
        pace.addOnChangeListener((slider, value, fromUser) -> {
            settings.pace(Math.round(value)); updatePreview();
        });
        MaterialButtonToggleGroup widths = findViewById(R.id.width_group);
        widths.check(widthId(settings.width()));
        widths.addOnButtonCheckedListener((group, id, checked) -> {
            if (checked) { settings.width(id == R.id.width_narrow ? 0 : id == R.id.width_medium ? 1 : 2); updatePreview(); }
        });
        MaterialSwitch countdown = findViewById(R.id.countdown);
        countdown.setChecked(settings.countdown());
        countdown.setOnCheckedChangeListener((button, checked) -> settings.countdown(checked));
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (loading) return;
                script.title = title.getText() == null ? "" : title.getText().toString();
                script.body = body.getText() == null ? "" : body.getText().toString();
                handler.removeCallbacks(refreshDraft);
                handler.postDelayed(refreshDraft, 350);
            }
            @Override public void afterTextChanged(Editable s) { }
        };
        title.addTextChangedListener(watcher); body.addTextChangedListener(watcher);
        showScript();
    }
    private int widthId(int width) { return width == 0 ? R.id.width_narrow : width == 1 ? R.id.width_medium : R.id.width_wide; }
    private void showScript() {
        loading = true;
        title.setText(script.title); body.setText(script.body);
        loading = false;
        updatePreview();
    }
    private void updatePreview() {
        int words = ScrollSession.wordCount(script.body);
        int seconds = (int) Math.ceil(words * 60.0 / settings.pace());
        stats.setText(String.format(Locale.getDefault(), "%d words · about %d:%02d at this pace", words, seconds / 60, seconds % 60));
        ((TextView) findViewById(R.id.size_value)).setText(String.format(Locale.getDefault(), "%d", settings.textSize()));
        ((TextView) findViewById(R.id.speed_value)).setText(String.format(Locale.getDefault(), "%d wpm", settings.pace()));
        preview.setScript(script.body, settings.textSize());
        preview.setMeasurePercent(settings.width() == 0 ? 65 : settings.width() == 1 ? 82 : 100);
        preview.setOffset(0);
    }
    private void showLibrary(View anchor) {
        store.save();
        List<ScriptStore.Script> entries = store.all();
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(0, 0, 0, R.string.new_script);
        for (int i = 0; i < entries.size(); i++) {
            String name = entries.get(i).title.trim();
            menu.getMenu().add(0, i + 1, i + 1, name.isEmpty() ? getString(R.string.untitled) : name)
                    .setCheckable(true).setChecked(entries.get(i).id.equals(script.id));
        }
        menu.setOnMenuItemClickListener(item -> {
            handler.removeCallbacks(refreshDraft);
            script = item.getItemId() == 0 ? store.create(getString(R.string.untitled), "") : entries.get(item.getItemId() - 1);
            store.select(script); showScript(); return true;
        });
        menu.show();
    }
    private void deleteScript() {
        new MaterialAlertDialogBuilder(this).setTitle(R.string.delete_script).setMessage(R.string.delete_message)
                .setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.delete, (dialog, which) -> {
                    handler.removeCallbacks(refreshDraft); store.delete(script); script = store.current(); showScript();
                }).show();
    }
    private void launchReader() {
        handler.removeCallbacks(refreshDraft); store.save();
        if (script.body.trim().isEmpty()) { message(getString(R.string.empty_script)); body.requestFocus(); return; }
        if (!Settings.canDrawOverlays(this)) {
            new MaterialAlertDialogBuilder(this).setTitle(R.string.overlay_permission_title).setMessage(R.string.overlay_permission_body)
                    .setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.open_settings, (dialog, which) -> {
                        try { startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))); }
                        catch (ActivityNotFoundException error) { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
                    }).show();
            return;
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                && !getPreferences(MODE_PRIVATE).getBoolean("askedNotifications", false)) {
            getPreferences(MODE_PRIVATE).edit().putBoolean("askedNotifications", true).apply();
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATIONS);
            return;
        }
        openReaderAndCamera();
    }
    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == NOTIFICATIONS) openReaderAndCamera();
    }
    private void openReaderAndCamera() {
        try {
            startForegroundService(new Intent(this, PrompterService.class).setAction(PrompterService.START));
            // Wait for the service's successful window attachment before leaving the visible activity.
            waitForReader(0);
        } catch (RuntimeException error) { message(getString(R.string.reader_failed)); }
    }
    private void waitForReader(int attempt) {
        handler.postDelayed(() -> {
            if (PrompterService.running) openCamera();
            else if (attempt < 9) waitForReader(attempt + 1);
            else message(getString(R.string.reader_failed));
        }, 100);
    }
    private void openCamera() {
        Intent camera = new Intent(MediaStore.INTENT_ACTION_VIDEO_CAMERA)
                .setPackage("com.google.android.GoogleCamera").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { startActivity(camera); }
        catch (ActivityNotFoundException error) {
            camera.setPackage(null);
            try { startActivity(camera); } catch (ActivityNotFoundException missing) { message(getString(R.string.camera_missing)); }
        }
    }
    private void updateReaderState() {
        boolean running = PrompterService.running;
        findViewById(R.id.close_reader).setVisibility(running ? View.VISIBLE : View.GONE);
        ((MaterialButton) findViewById(R.id.launch)).setText(running ? R.string.return_to_camera : R.string.launch);
    }
    private void importScript() {
        try { startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("text/*").addCategory(Intent.CATEGORY_OPENABLE), IMPORT); }
        catch (ActivityNotFoundException error) { message(getString(R.string.import_failed)); }
    }
    private void exportScript() {
        exportBody = script.body;
        String filename = script.title.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
        if (filename.isEmpty()) filename = "script";
        try { startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("text/plain").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE, filename + ".txt"), EXPORT); }
        catch (ActivityNotFoundException error) { message(getString(R.string.export_failed)); }
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putString("exportBody", exportBody);
    }
    @Override protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state); exportBody = state.getString("exportBody");
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (requestCode == IMPORT) files.execute(() -> {
            try (InputStream stream = getContentResolver().openInputStream(uri)) {
                if (stream == null) throw new java.io.IOException();
                InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT));
                StringBuilder text = new StringBuilder(); char[] buffer = new char[4096]; int count;
                while ((count = reader.read(buffer)) != -1) {
                    if (text.length() + count > ScriptStore.MAX_CHARACTERS) throw new java.io.IOException();
                    text.append(buffer, 0, count);
                }
                String imported = text.toString().replace("\r\n", "\n").replace('\r', '\n');
                if (imported.startsWith("\uFEFF")) imported = imported.substring(1);
                String name = getString(R.string.untitled);
                try (Cursor cursor = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) name = cursor.getString(0).replaceFirst("(?i)\\.txt$", "");
                }
                String finalName = name.length() > 80 ? name.substring(0, 80) : name;
                String finalText = imported;
                handler.post(() -> { if (!isDestroyed()) { handler.removeCallbacks(refreshDraft); store.save(); script = store.create(finalName, finalText); showScript(); } });
            } catch (Exception error) { handler.post(() -> message(getString(R.string.import_failed))); }
        });
        if (requestCode == EXPORT) {
            String snapshot = exportBody == null ? script.body : exportBody;
            files.execute(() -> {
                try (OutputStream output = getContentResolver().openOutputStream(uri, "wt")) {
                    if (output == null) throw new java.io.IOException();
                    output.write(snapshot.getBytes(StandardCharsets.UTF_8));
                    handler.post(() -> message(getString(R.string.exported)));
                } catch (Exception error) { handler.post(() -> message(getString(R.string.export_failed))); }
            });
        }
    }
    private void message(String text) { if (!isDestroyed()) Snackbar.make(findViewById(R.id.root), text, Snackbar.LENGTH_LONG).show(); }
    @Override protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(PrompterService.STATUS);
        ContextCompat.registerReceiver(this, readerStatus, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
        receiverRegistered = true;
    }
    @Override protected void onResume() {
        super.onResume();
        if (settings != null) {
            // A share review can create a script while this editor is stopped.
            handler.removeCallbacks(refreshDraft);
            store = new ScriptStore(this); script = store.current(); showScript();
            loading = true;
            ((Slider) findViewById(R.id.text_size)).setValue(settings.textSize());
            ((Slider) findViewById(R.id.pace)).setValue(settings.pace());
            loading = false;
            updateReaderState();
            refreshUpdates();
        }
    }
    private void refreshUpdates() {
        String version=UpdateChecker.available(this);
        com.google.android.material.button.MaterialButton banner=findViewById(R.id.updates_banner);
        banner.setVisibility(!version.isEmpty() || !UpdateChecker.prefs(this).getBoolean("introSeen",false) ? View.VISIBLE : View.GONE);
        banner.setText(version.isEmpty() ? getString(R.string.get_update_alerts) : getString(R.string.update_available,version));
        UpdateChecker.schedule(this); UpdateChecker.notifyIfAvailable(this);
        if(UpdateChecker.due(this)) UpdateChecker.NETWORK.execute(() -> {
            try { UpdateChecker.check(getApplicationContext(),false); }
            catch(java.io.IOException unavailable) { return; }
            runOnUiThread(() -> { if(!isDestroyed() && getLifecycle().getCurrentState().isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
                String found=UpdateChecker.available(this);
                if(!found.isEmpty()) { banner.setText(getString(R.string.update_available,found)); banner.setVisibility(View.VISIBLE); }
                UpdateChecker.notifyIfAvailable(this);
            }});
        });
    }
    @Override protected void onPause() { super.onPause(); handler.removeCallbacks(refreshDraft); if (store != null) store.save(); }
    @Override protected void onStop() { if (receiverRegistered) { unregisterReceiver(readerStatus); receiverRegistered = false; } super.onStop(); }
    @Override protected void onDestroy() { handler.removeCallbacksAndMessages(null); files.shutdown(); super.onDestroy(); }
}
