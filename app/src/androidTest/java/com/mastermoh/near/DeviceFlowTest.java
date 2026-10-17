package com.mastermoh.near;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.ParcelFileDescriptor;
import android.provider.Settings;
import android.widget.EditText;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class DeviceFlowTest {
    private Context context;
    private Map<String,?> savedScripts, savedReader;
    @Before public void saveData() {
        context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        savedScripts=new HashMap<>(context.getSharedPreferences("scripts",Context.MODE_PRIVATE).getAll());
        savedReader=new HashMap<>(context.getSharedPreferences("reader",Context.MODE_PRIVATE).getAll());
        context.stopService(new Intent(context,PrompterService.class));
    }
    @After public void restoreData() {
        context.stopService(new Intent(context,PrompterService.class));
        restore("scripts",savedScripts); restore("reader",savedReader);
    }
    private void restore(String name,Map<String,?> data) {
        SharedPreferences.Editor editor=context.getSharedPreferences(name,Context.MODE_PRIVATE).edit().clear();
        for(Map.Entry<String,?> item:data.entrySet()) {
            Object value=item.getValue();
            if(value instanceof String) editor.putString(item.getKey(),(String)value);
            else if(value instanceof Integer) editor.putInt(item.getKey(),(Integer)value);
            else if(value instanceof Boolean) editor.putBoolean(item.getKey(),(Boolean)value);
        }
        assertTrue(editor.commit());
    }
    @Test public void editorSurvivesActivityRecreation() {
        try(ActivityScenario<MainActivity> activity=ActivityScenario.launch(MainActivity.class)) {
            activity.onActivity(screen -> {
                ((EditText)screen.findViewById(R.id.script_title)).setText("Offline take");
                ((EditText)screen.findViewById(R.id.script_body)).setText("One clear thought.\n\nAnother line to read.");
            });
            activity.recreate();
            activity.onActivity(screen -> {
                assertEquals("Offline take",((EditText)screen.findViewById(R.id.script_title)).getText().toString());
                assertEquals("One clear thought.\n\nAnother line to read.",((EditText)screen.findViewById(R.id.script_body)).getText().toString());
            });
        }
        assertEquals("Offline take",new ScriptStore(context).current().title);
    }
    @Test public void overlaySurvivesLeavingEditorAndCanClose() throws Exception {
        shell("appops set " + context.getPackageName() + " SYSTEM_ALERT_WINDOW allow");
        assertTrue(Settings.canDrawOverlays(context));
        new ScriptStore(context).create("Overlay test","A script that stays visible.\n\nRead this while the camera is open.\n\nPause and resume whenever you need.");
        try(ActivityScenario<MainActivity> activity=ActivityScenario.launch(MainActivity.class)) {
            activity.onActivity(screen -> screen.startForegroundService(new Intent(screen,PrompterService.class).setAction(PrompterService.START)));
            awaitRunning(true);
            activity.moveToState(Lifecycle.State.CREATED);
            assertTrue(PrompterService.running);
            context.startService(new Intent(context,PrompterService.class).setAction(PrompterService.TOGGLE));
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            context.startService(new Intent(context,PrompterService.class).setAction(PrompterService.STOP));
            awaitRunning(false);
        }
    }
    private void awaitRunning(boolean expected) throws InterruptedException {
        long deadline=android.os.SystemClock.elapsedRealtime()+3000;
        while(PrompterService.running!=expected && android.os.SystemClock.elapsedRealtime()<deadline) Thread.sleep(50);
        assertEquals(expected,PrompterService.running);
    }
    private void shell(String command) throws Exception {
        try(ParcelFileDescriptor descriptor=InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command);
            FileInputStream input=new FileInputStream(descriptor.getFileDescriptor())) {
            input.readAllBytes();
        }
    }
}
