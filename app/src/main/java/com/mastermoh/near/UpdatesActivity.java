package com.mastermoh.near;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

public final class UpdatesActivity extends AppCompatActivity {
    private MaterialButton check;
    private boolean checking;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_updates);
        ScreenInsets.apply(this,findViewById(R.id.updates_root));
        MaterialToolbar toolbar=findViewById(R.id.updates_toolbar);
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationContentDescription(R.string.back_to_script); toolbar.setNavigationOnClickListener(v -> finish());
        UpdateChecker.prefs(this).edit().putBoolean("introSeen",true).apply();
        ((TextView)findViewById(R.id.installed_version)).setText(getString(R.string.installed_version,UpdateChecker.installed(this)));
        check=findViewById(R.id.check_updates); check.setOnClickListener(v -> checkNow());
        findViewById(R.id.download_update).setOnClickListener(v -> {
            String version=UpdateChecker.available(this);
            if(!version.isEmpty()) startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(UpdateChecker.downloadUrl(version))));
        });
        MaterialSwitch alerts=findViewById(R.id.update_alerts);
        alerts.setChecked(UpdateChecker.prefs(this).getBoolean("alerts",false));
        alerts.setOnCheckedChangeListener((button,enabled) -> {
            UpdateChecker.prefs(this).edit().putBoolean("alerts",enabled).apply(); UpdateChecker.schedule(this);
            if(enabled && Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},30);
            else if(enabled) UpdateChecker.notifyIfAvailable(this);
            permissionHint();
        });
        findViewById(R.id.update_notification_settings).setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName())));
        render();
    }
    private void checkNow() {
        if(checking) return;
        checking=true; check.setEnabled(false); check.setText(R.string.checking_updates);
        ((TextView)findViewById(R.id.update_status)).setText(R.string.checking_updates);
        UpdateChecker.NETWORK.execute(() -> {
            boolean success;
            try { UpdateChecker.check(getApplicationContext(),true); success=true; }
            catch(java.io.IOException unavailable) { success=false; }
            final boolean checked=success;
            runOnUiThread(() -> {
                if(isDestroyed()) return;
                checking=false; check.setEnabled(true); check.setText(R.string.check_updates); render();
                if(!checked) ((TextView)findViewById(R.id.update_status)).setText(R.string.update_check_failed);
            });
        });
    }
    private void render() {
        String version=UpdateChecker.available(this);
        int primary=com.google.android.material.color.MaterialColors.getColor(check,androidx.appcompat.R.attr.colorPrimary);
        int onPrimary=com.google.android.material.color.MaterialColors.getColor(check,com.google.android.material.R.attr.colorOnPrimary);
        check.setBackgroundTintList(android.content.res.ColorStateList.valueOf(version.isEmpty()?primary:android.graphics.Color.TRANSPARENT));
        check.setTextColor(version.isEmpty()?onPrimary:primary);
        check.setStrokeWidth(version.isEmpty()?0:Math.round(getResources().getDisplayMetrics().density));
        check.setStrokeColor(android.content.res.ColorStateList.valueOf(primary));
        ((TextView)findViewById(R.id.update_status)).setText(!version.isEmpty() ? getString(R.string.update_available,version)
                : getString(UpdateChecker.prefs(this).getLong("checked",0)>0 ? R.string.up_to_date : R.string.update_not_checked));
        findViewById(R.id.download_update).setVisibility(version.isEmpty() ? View.GONE : View.VISIBLE);
        TextView notes=findViewById(R.id.update_notes); notes.setVisibility(version.isEmpty() ? View.GONE : View.VISIBLE);
        notes.setText(UpdateChecker.prefs(this).getString("notes","")); permissionHint();
    }
    private void permissionHint() {
        boolean blocked=UpdateChecker.prefs(this).getBoolean("alerts",false) && !NotificationManagerCompat.from(this).areNotificationsEnabled();
        findViewById(R.id.update_permission_hint).setVisibility(blocked ? View.VISIBLE : View.GONE);
        ((TextView)findViewById(R.id.update_permission_hint)).setText(R.string.update_notifications_blocked);
        findViewById(R.id.update_notification_settings).setVisibility(blocked ? View.VISIBLE : View.GONE);
    }
    @Override protected void onResume() { super.onResume(); permissionHint(); }
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] results) {
        super.onRequestPermissionsResult(code,permissions,results); permissionHint();
        if(code==30) UpdateChecker.notifyIfAvailable(this);
    }
}
