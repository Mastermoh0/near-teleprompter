package com.mastermoh.near;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class UpdateChecker {
    static final long DAY=24*60*60*1000L;
    static final String REPO="https://github.com/Mastermoh0/near-teleprompter";
    static final ExecutorService NETWORK=Executors.newSingleThreadExecutor();
    private static final int JOB=12001;
    private static final Object LOCK=new Object();
    static SharedPreferences prefs(Context context) { return context.getSharedPreferences("updates",Context.MODE_PRIVATE); }
    static String installed(Context context) {
        try { return context.getPackageManager().getPackageInfo(context.getPackageName(),0).versionName; }
        catch(PackageManager.NameNotFoundException impossible) { throw new IllegalStateException(impossible); }
    }
    static String available(Context context) {
        String name=prefs(context).getString("version","");
        ReleaseVersion latest=ReleaseVersion.parse(name), current=ReleaseVersion.parse(installed(context));
        return latest!=null && current!=null && latest.compareTo(current)>0 ? latest.toString() : "";
    }
    static String downloadUrl(String version) {
        ReleaseVersion stable=ReleaseVersion.parse(version);
        if(stable==null) throw new IllegalArgumentException("Invalid release version");
        return REPO+"/releases/tag/v"+stable;
    }
    static boolean due(Context context) {
        long checked=prefs(context).getLong("checked",0), now=System.currentTimeMillis();
        long attempted=prefs(context).getLong("attempted",0);
        if(attempted<=now && now-attempted<15*60*1000L) return false;
        return checked==0 || checked>now || now-checked>=DAY;
    }
    static void check(Context context,boolean force) throws IOException {
        synchronized(LOCK) {
            if(!force && !due(context)) return;
            prefs(context).edit().putLong("attempted",System.currentTimeMillis()).apply();
            HttpURLConnection connection=(HttpURLConnection)new URL("https://api.github.com/repos/Mastermoh0/near-teleprompter/releases/latest").openConnection();
            connection.setConnectTimeout(5000); connection.setReadTimeout(7000);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Accept","application/vnd.github+json");
            connection.setRequestProperty("User-Agent","Near-Android/"+installed(context));
            try {
                if(connection.getResponseCode()!=200) throw new IOException("Release service unavailable");
                String response;
                long deadline=android.os.SystemClock.elapsedRealtime()+15000;
                try(InputStream input=connection.getInputStream(); ByteArrayOutputStream bytes=new ByteArrayOutputStream()) {
                    byte[] buffer=new byte[4096]; int count;
                    while((count=input.read(buffer))!=-1) {
                        if(Thread.currentThread().isInterrupted() || android.os.SystemClock.elapsedRealtime()>deadline || bytes.size()+count>128*1024) throw new IOException("Release response rejected");
                        bytes.write(buffer,0,count);
                    }
                    response=bytes.toString(StandardCharsets.UTF_8.name());
                }
                storeResponse(context,response);
            } finally { connection.disconnect(); }
        }
    }
    static void storeResponse(Context context,String response) throws IOException {
        try {
            JSONObject release=new JSONObject(response);
            ReleaseVersion version=ReleaseVersion.parse(release.optString("tag_name"));
            if(version==null || release.optBoolean("draft",true) || release.optBoolean("prerelease",true)
                    || !release.optString("html_url").equals(downloadUrl(version.toString()))) throw new IOException("Unrecognized stable release");
            org.json.JSONArray assets=release.optJSONArray("assets"); boolean installable=false;
            String apk="near-teleprompter-"+version+".apk";
            if(assets!=null) for(int i=0;i<assets.length();i++) {
                JSONObject asset=assets.optJSONObject(i);
                if(asset!=null && apk.equals(asset.optString("name")) && "uploaded".equals(asset.optString("state"))
                        && asset.optLong("size",0)>0 && (REPO+"/releases/download/v"+version+"/"+apk).equals(asset.optString("browser_download_url"))) installable=true;
            }
            if(!installable) throw new IOException("Release has no official APK");
            String notes=release.optString("body","");
            prefs(context).edit().putString("version",version.toString()).putString("notes",notes.substring(0,Math.min(notes.length(),3000)))
                    .putLong("checked",System.currentTimeMillis()).apply();
        } catch(org.json.JSONException invalid) { throw new IOException("Invalid release information",invalid); }
    }
    static void schedule(Context context) {
        JobScheduler scheduler=context.getSystemService(JobScheduler.class);
        if(!prefs(context).getBoolean("alerts",false)) { scheduler.cancel(JOB); return; }
        if(scheduler.getPendingJob(JOB)!=null) return;
        scheduler.schedule(new JobInfo.Builder(JOB,new ComponentName(context,UpdateJobService.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setRequiresBatteryNotLow(true)
                .setPeriodic(DAY).setPersisted(true).build());
    }
    static void notifyIfAvailable(Context context) {
        String version=available(context);
        SharedPreferences prefs=prefs(context);
        if(version.isEmpty() || !prefs.getBoolean("alerts",false) || version.equals(prefs.getString("notified",""))
                || CameraActivity.visible || PrompterService.running) return;
        if(Build.VERSION.SDK_INT>=33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) return;
        NotificationManager manager=context.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel("updates",context.getString(R.string.update_alerts),NotificationManager.IMPORTANCE_DEFAULT));
        if(!NotificationManagerCompat.from(context).areNotificationsEnabled()
                || manager.getNotificationChannel("updates").getImportance()==NotificationManager.IMPORTANCE_NONE) return;
        PendingIntent open=PendingIntent.getActivity(context,JOB,new Intent(context,UpdatesActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        manager.notify(JOB,new NotificationCompat.Builder(context,"updates").setSmallIcon(R.drawable.ic_app)
                .setContentTitle(context.getString(R.string.update_available,version)).setContentText(context.getString(R.string.update_notification_body))
                .setContentIntent(open).setAutoCancel(true).build());
        prefs.edit().putString("notified",version).apply();
    }
}
