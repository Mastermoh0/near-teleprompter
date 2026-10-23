package com.mastermoh.near;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.SystemClock;
import android.widget.Button;
import android.widget.EditText;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class NewFeaturesTest {
    private Context context;
    private final Map<String,Map<String,?>> originals = new HashMap<>();
    private Uri recorded;
    @Before public void prepare() {
        context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        for(String name:new String[]{"scripts","reader","recordings"}) originals.put(name,new HashMap<>(context.getSharedPreferences(name,0).getAll()));
        context.stopService(new Intent(context,PrompterService.class));
        new ScriptStore(context).create("Keep this draft","My original draft must survive sharing.\n\nRead slowly and keep this recording running while the words pause.\n\nThis is the end of the test script.");
    }
    @After public void restore() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            for (android.app.Activity activity : new java.util.ArrayList<>(androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
                    .getInstance().getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)))
                if (activity instanceof MainActivity) activity.finish();
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        if(recorded!=null) context.getContentResolver().delete(recorded,null,null);
        for(Map.Entry<String,Map<String,?>> entry:originals.entrySet()) {
            SharedPreferences.Editor editor=context.getSharedPreferences(entry.getKey(),0).edit().clear();
            for(Map.Entry<String,?> item:entry.getValue().entrySet()) {
                Object value=item.getValue();
                if(value instanceof String) editor.putString(item.getKey(),(String)value);
                else if(value instanceof Integer) editor.putInt(item.getKey(),(Integer)value);
                else if(value instanceof Boolean) editor.putBoolean(item.getKey(),(Boolean)value);
            }
            assertTrue(editor.commit());
        }
    }
    private Intent share(String text) {
        return new Intent(context,ShareTextActivity.class).setAction(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT,text).putExtra(Intent.EXTRA_SUBJECT,"Shared title").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }
    @Test public void shareReviewSurvivesRotationAndSavesOnceWithoutReplacingDraft() {
        int before=new ScriptStore(context).all().size();
        try(ActivityScenario<ShareTextActivity> screen=ActivityScenario.launch(share("First line\r\n\r\nSecond line"))) {
            screen.onActivity(activity -> {
                assertEquals("First line\n\nSecond line",((EditText)activity.findViewById(R.id.shared_body)).getText().toString());
                ((EditText)activity.findViewById(R.id.shared_title)).setText("Edited title");
            });
            screen.recreate();
            screen.onActivity(activity -> {
                assertEquals("Edited title",((EditText)activity.findViewById(R.id.shared_title)).getText().toString());
                activity.findViewById(R.id.save_shared).performClick();
            });
        }
        ScriptStore store=new ScriptStore(context);
        assertEquals(before+1,store.all().size());
        assertEquals("Edited title",store.current().title);
        assertTrue(store.all().stream().anyMatch(s -> s.title.equals("Keep this draft")));
    }
    @Test public void cancelDoesNotChangeTheLibrary() {
        int before=new ScriptStore(context).all().size();
        try(ActivityScenario<ShareTextActivity> screen=ActivityScenario.launch(share("New text"))) {
            screen.onActivity(activity -> activity.findViewById(R.id.cancel_shared).performClick());
        }
        assertEquals(before,new ScriptStore(context).all().size());
        assertEquals("Keep this draft",new ScriptStore(context).current().title);
    }
    @Test public void oversizedAndEmptySharesCannotSave() {
        try(ActivityScenario<ShareTextActivity> screen=ActivityScenario.launch(share("A".repeat(100001)))) {
            screen.onActivity(activity -> {
                assertFalse(activity.findViewById(R.id.save_shared).isEnabled());
                assertEquals(0,((EditText)activity.findViewById(R.id.shared_body)).length());
                assertNotNull(((com.google.android.material.textfield.TextInputLayout)activity.findViewById(R.id.shared_body_layout)).getError());
                ((EditText)activity.findViewById(R.id.shared_body)).setText("\n  ");
                assertFalse(activity.findViewById(R.id.save_shared).isEnabled());
            });
        }
    }
    @Test public void unsupportedShareCannotImportAttachmentsAsText() {
        Intent intent=share("not a plain-text share").setType("image/png");
        try(ActivityScenario<ShareTextActivity> screen=ActivityScenario.launch(intent)) {
            screen.onActivity(activity -> assertFalse(activity.findViewById(R.id.save_shared).isEnabled()));
        }
    }
    @Test public void cameraSavesVideoAndAudioWhileScriptCanPause() throws Exception {
        allowCamera();
        context.getSharedPreferences("reader",0).edit().putBoolean("countdown",false).commit();
        context.getSharedPreferences("recordings",0).edit().remove("last").commit();
        try(ActivityScenario<CameraActivity> screen=ActivityScenario.launch(CameraActivity.class)) {
            waitFor(() -> enabled(screen,R.id.camera_record),20000);
            screen.onActivity(activity -> activity.findViewById(R.id.camera_record).performClick());
            waitFor(() -> text(screen,R.id.camera_record).equals(context.getString(R.string.camera_stop)),5000);
            SystemClock.sleep(4000);
            screen.onActivity(activity -> {
                Button play=activity.findViewById(R.id.camera_script_play);
                if(play.getText().toString().equals(activity.getString(R.string.camera_pause))) play.performClick();
                assertEquals(activity.getString(R.string.camera_play),play.getText().toString());
                assertEquals(activity.getString(R.string.camera_stop),((Button)activity.findViewById(R.id.camera_record)).getText().toString());
                activity.findViewById(R.id.camera_record).performClick();
            });
            waitFor(() -> context.getSharedPreferences("recordings",0).contains("last"),20000);
            recorded=Uri.parse(context.getSharedPreferences("recordings",0).getString("last",""));
            MediaMetadataRetriever metadata=new MediaMetadataRetriever();
            try {
                metadata.setDataSource(context,recorded);
                assertEquals("yes",metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO));
                assertEquals("yes",metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO));
                assertTrue(Long.parseLong(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))>1000);
            } finally { metadata.release(); }
        }
    }
    @Test public void backgroundingFinalizesTheTake() throws Exception {
        allowCamera();
        context.getSharedPreferences("reader",0).edit().putBoolean("countdown",false).commit();
        context.getSharedPreferences("recordings",0).edit().remove("last").commit();
        try(ActivityScenario<CameraActivity> screen=ActivityScenario.launch(CameraActivity.class)) {
            waitFor(() -> enabled(screen,R.id.camera_record),20000);
            screen.onActivity(activity -> activity.findViewById(R.id.camera_record).performClick());
            SystemClock.sleep(4000);
            screen.moveToState(Lifecycle.State.CREATED);
            waitFor(() -> context.getSharedPreferences("recordings",0).contains("last"),20000);
            recorded=Uri.parse(context.getSharedPreferences("recordings",0).getString("last",""));
        }
        assertNotNull(recorded);
    }
    private void allowCamera() {
        var ui=InstrumentationRegistry.getInstrumentation().getUiAutomation();
        ui.grantRuntimePermission(context.getPackageName(),Manifest.permission.CAMERA);
        ui.grantRuntimePermission(context.getPackageName(),Manifest.permission.RECORD_AUDIO);
    }
    private boolean enabled(ActivityScenario<CameraActivity> screen,int id) {
        boolean[] value={false}; screen.onActivity(activity -> value[0]=activity.findViewById(id).isEnabled()); return value[0];
    }
    private String text(ActivityScenario<CameraActivity> screen,int id) {
        String[] value={""}; screen.onActivity(activity -> value[0]=((Button)activity.findViewById(id)).getText().toString()); return value[0];
    }
    private void waitFor(BooleanSupplier condition,long timeout) {
        long deadline=SystemClock.elapsedRealtime()+timeout;
        while(!condition.getAsBoolean() && SystemClock.elapsedRealtime()<deadline) SystemClock.sleep(100);
        assertTrue("Timed out waiting for app state",condition.getAsBoolean());
    }
}
