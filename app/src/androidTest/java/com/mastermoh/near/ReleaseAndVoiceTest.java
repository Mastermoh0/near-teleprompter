package com.mastermoh.near;

import android.Manifest;
import android.app.NotificationManager;
import android.app.job.JobScheduler;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.widget.Button;
import android.widget.TextView;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.vosk.Model;
import org.vosk.Recognizer;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ReleaseAndVoiceTest {
    private Context context;
    private final Map<String,Map<String,?>> saved=new HashMap<>();
    private Uri recording;
    @Before public void prepare() {
        context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        for(String name:new String[]{"scripts","reader","recordings","updates"}) saved.put(name,new HashMap<>(context.getSharedPreferences(name,0).getAll()));
        context.stopService(new Intent(context,PrompterService.class));
        UpdateChecker.prefs(context).edit().clear().putBoolean("introSeen",true).putLong("checked",System.currentTimeMillis()).commit();
        context.getSharedPreferences("reader",0).edit().putBoolean("voiceFollow",false).putBoolean("countdown",false).commit();
    }
    @After public void restore() {
        context.getSystemService(NotificationManager.class).cancel(12001);
        context.getSystemService(JobScheduler.class).cancel(12001);
        if(recording!=null) context.getContentResolver().delete(recording,null,null);
        for(Map.Entry<String,Map<String,?>> prefs:saved.entrySet()) {
            SharedPreferences.Editor edit=context.getSharedPreferences(prefs.getKey(),0).edit().clear();
            for(Map.Entry<String,?> entry:prefs.getValue().entrySet()) {
                Object value=entry.getValue();
                if(value instanceof String) edit.putString(entry.getKey(),(String)value);
                else if(value instanceof Long) edit.putLong(entry.getKey(),(Long)value);
                else if(value instanceof Integer) edit.putInt(entry.getKey(),(Integer)value);
                else if(value instanceof Boolean) edit.putBoolean(entry.getKey(),(Boolean)value);
            }
            assertTrue(edit.commit());
        }
    }
    private JSONObject release(String version) throws Exception {
        String url=UpdateChecker.downloadUrl(version);
        JSONObject apk=new JSONObject().put("name","near-teleprompter-"+version+".apk").put("state","uploaded").put("size",100)
                .put("browser_download_url",UpdateChecker.REPO+"/releases/download/v"+version+"/near-teleprompter-"+version+".apk");
        return new JSONObject().put("tag_name","v"+version).put("draft",false).put("prerelease",false).put("html_url",url)
                .put("body","New camera features").put("assets",new JSONArray().put(apk));
    }
    @Test public void stableOfficialReleaseIsCachedAndOlderReleaseIsNotOffered() throws Exception {
        UpdateChecker.storeResponse(context,release("99.0.0").toString());
        assertEquals("99.0.0",UpdateChecker.available(context)); assertFalse(UpdateChecker.due(context));
        UpdateChecker.storeResponse(context,release("1.0.0").toString()); assertEquals("",UpdateChecker.available(context));
    }
    @Test public void untrustedOrUninstallableReleasesDoNotReplaceCache() throws Exception {
        UpdateChecker.storeResponse(context,release("99.0.0").toString());
        for(JSONObject bad:new JSONObject[]{release("99.1.0").put("draft",true),release("99.1.0").put("prerelease",true),
                release("99.1.0").put("html_url","https://example.com"),release("99.1.0").put("assets",new JSONArray()),
                release("99.1.0").put("tag_name","../../evil")}) {
            try { UpdateChecker.storeResponse(context,bad.toString()); fail("Untrusted release accepted"); }
            catch(java.io.IOException expected) { }
            assertEquals("99.0.0",UpdateChecker.available(context));
        }
    }
    @Test public void alertRequiresOptInAndIsPostedOncePerVersion() throws Exception {
        allow(Manifest.permission.POST_NOTIFICATIONS);
        UpdateChecker.storeResponse(context,release("99.0.0").toString());
        UpdateChecker.notifyIfAvailable(context); assertFalse(UpdateChecker.prefs(context).contains("notified"));
        UpdateChecker.prefs(context).edit().putBoolean("alerts",true).commit();
        UpdateChecker.notifyIfAvailable(context); UpdateChecker.notifyIfAvailable(context);
        assertEquals("99.0.0",UpdateChecker.prefs(context).getString("notified",""));
        assertEquals(1,context.getSystemService(NotificationManager.class).getActiveNotifications().length);
    }
    @Test public void updateScreenCanEnableAndDisablePersistedDailyJob() {
        allow(Manifest.permission.POST_NOTIFICATIONS);
        try(ActivityScenario<UpdatesActivity> screen=ActivityScenario.launch(UpdatesActivity.class)) {
            screen.onActivity(activity -> activity.findViewById(R.id.update_alerts).performClick());
            android.app.job.JobInfo job=context.getSystemService(JobScheduler.class).getPendingJob(12001);
            assertNotNull(job); assertTrue(job.isPersisted()); assertEquals(UpdateChecker.DAY,job.getIntervalMillis());
            screen.onActivity(activity -> activity.findViewById(R.id.update_alerts).performClick());
            assertNull(context.getSystemService(JobScheduler.class).getPendingJob(12001));
        }
    }
    @Test public void offlineManualCheckDoesNotRemoveKnownUpdate() throws Exception {
        UpdateChecker.storeResponse(context,release("99.0.0").toString());
        try(ActivityScenario<UpdatesActivity> screen=ActivityScenario.launch(UpdatesActivity.class)) {
            screen.onActivity(activity -> activity.findViewById(R.id.check_updates).performClick());
            waitFor(() -> { boolean[] enabled={false}; screen.onActivity(a -> enabled[0]=a.findViewById(R.id.check_updates).isEnabled()); return enabled[0]; },20000);
            screen.onActivity(activity -> {
                assertEquals(activity.getString(R.string.update_check_failed),((TextView)activity.findViewById(R.id.update_status)).getText().toString());
                assertEquals(android.view.View.VISIBLE,activity.findViewById(R.id.download_update).getVisibility());
            });
        }
    }
    @Test public void bundledModelRecognizesRealEnglishAudioOffline() throws Exception {
        byte[] wav;
        try(InputStream source=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("vosk-test.wav"); ByteArrayOutputStream bytes=new ByteArrayOutputStream()) {
            byte[] buffer=new byte[4096]; int n; while((n=source.read(buffer))!=-1) bytes.write(buffer,0,n); wav=bytes.toByteArray();
        }
        ByteBuffer header=ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN);
        int start=12, size=0;
        while(start+8<=wav.length) {
            size=header.getInt(start+4);
            if(wav[start]=='d' && wav[start+1]=='a' && wav[start+2]=='t' && wav[start+3]=='a') { start+=8; break; }
            start+=8+size+(size&1);
        }
        assertTrue(size>0 && start+size<=wav.length);
        StringBuilder spoken=new StringBuilder();
        ScriptMatcher match=new ScriptMatcher("one zero zero zero one nine oh two i no zero one eight zero three");
        try(Model model=new Model(OfflineVoice.modelPath(context)); Recognizer recognizer=new Recognizer(model,16000)) {
            for(int pos=start;pos<start+size;pos+=3200) {
                int count=Math.min(3200,start+size-pos);
                byte[] pcm=java.util.Arrays.copyOfRange(wav,pos,pos+count);
                boolean complete=recognizer.acceptWaveForm(pcm,count);
                JSONObject result=new JSONObject(complete?recognizer.getResult():recognizer.getPartialResult());
                String text=result.optString(complete?"text":"partial","");
                match.accept(text); if(complete) spoken.append(text).append(' ');
            }
            String last=new JSONObject(recognizer.getFinalResult()).optString("text",""); spoken.append(last); match.accept(last);
        }
        assertTrue(spoken.toString(),spoken.toString().contains("one zero zero zero one"));
        assertTrue(spoken.toString(),spoken.toString().contains("eight zero three"));
        assertTrue("Recognized words did not advance the script",match.wordPosition()>=12);
    }
    @Test public void voiceAndCameraCaptureCoexistAndPauseDoesNotStopVideo() throws Exception {
        allow(Manifest.permission.CAMERA); allow(Manifest.permission.RECORD_AUDIO); allow(Manifest.permission.POST_NOTIFICATIONS);
        context.getSharedPreferences("reader",0).edit().putBoolean("voiceFollow",true).commit();
        context.getSharedPreferences("recordings",0).edit().remove("last").commit();
        new ScriptStore(context).create("Voice fixture","One clear thought. Look toward the lens. Read slowly and take your time while the camera continues recording.");
        UpdateChecker.storeResponse(context,release("99.0.0").toString());
        UpdateChecker.prefs(context).edit().putBoolean("alerts",true).commit();
        AudioManager audio=context.getSystemService(AudioManager.class);
        try(ActivityScenario<CameraActivity> screen=ActivityScenario.launch(CameraActivity.class)) {
            waitFor(() -> { boolean[] enabled={false}; screen.onActivity(a -> enabled[0]=a.findViewById(R.id.camera_record).isEnabled()); return enabled[0]; },45000);
            UpdateChecker.notifyIfAvailable(context); assertFalse(UpdateChecker.prefs(context).contains("notified"));
            screen.onActivity(a -> a.findViewById(R.id.camera_record).performClick());
            waitFor(() -> audio.getActiveRecordingConfigurations().size()>=2,20000);
            if(Build.VERSION.SDK_INT>=29) assertTrue("A microphone client was silenced",audio.getActiveRecordingConfigurations().stream().noneMatch(c -> c.isClientSilenced()));
            SystemClock.sleep(4000);
            screen.onActivity(a -> {
                assertEquals(a.getString(R.string.camera_stop),((Button)a.findViewById(R.id.camera_record)).getText().toString());
                assertEquals(a.getString(R.string.camera_pause),((Button)a.findViewById(R.id.camera_script_play)).getText().toString());
                a.findViewById(R.id.camera_script_play).performClick();
            });
            waitFor(() -> audio.getActiveRecordingConfigurations().size()==1,7000);
            screen.onActivity(a -> {
                assertEquals(a.getString(R.string.camera_stop),((Button)a.findViewById(R.id.camera_record)).getText().toString());
                a.findViewById(R.id.camera_record).performClick();
            });
            waitFor(() -> context.getSharedPreferences("recordings",0).contains("last"),25000);
            recording=Uri.parse(context.getSharedPreferences("recordings",0).getString("last",""));
            try(MediaMetadataRetriever metadata=new MediaMetadataRetriever()) {
                metadata.setDataSource(context,recording);
                assertEquals("yes",metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO));
                assertEquals("yes",metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO));
            }
        }
        waitFor(() -> audio.getActiveRecordingConfigurations().isEmpty(),7000);
    }
    private void allow(String permission) {
        if(!Manifest.permission.POST_NOTIFICATIONS.equals(permission) || Build.VERSION.SDK_INT>=33)
            InstrumentationRegistry.getInstrumentation().getUiAutomation().grantRuntimePermission(context.getPackageName(),permission);
    }
    private void waitFor(BooleanSupplier condition,long timeout) {
        long end=SystemClock.elapsedRealtime()+timeout;
        while(!condition.getAsBoolean() && SystemClock.elapsedRealtime()<end) SystemClock.sleep(100);
        assertTrue("Timed out waiting for feature state",condition.getAsBoolean());
    }
}
