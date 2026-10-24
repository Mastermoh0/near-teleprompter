package com.mastermoh.near;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Local Vosk model and foreground-only microphone. No speech service or network fallback. */
final class OfflineVoice implements AutoCloseable {
    interface Listener { void onReady(); void onWords(String words); void onError(); }
    private final Context context;
    private final Listener listener;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private volatile Model model;
    private volatile AudioRecord microphone;
    private volatile boolean closed;
    private volatile int generation;
    private boolean preparing;
    OfflineVoice(Context context,Listener listener) { this.context=context.getApplicationContext(); this.listener=listener; }
    boolean ready() { return model!=null && !closed; }
    void prepare() {
        if(preparing || ready() || closed) return;
        preparing=true;
        worker.execute(() -> {
            try {
                Model loaded=new Model(modelPath(context));
                if(closed) loaded.close(); else { model=loaded; main.post(() -> { if(!closed) listener.onReady(); }); }
            } catch(Exception | LinkageError failure) { main.post(() -> { preparing=false; if(!closed) listener.onError(); }); }
        });
    }
    static synchronized String modelPath(Context context) throws IOException {
        File root=new File(context.getNoBackupFilesDir(),"voice-en-us-0.15");
        File complete=new File(root,"complete");
        if(!complete.exists()) {
            copyAsset(context,"model-en-us",root);
            try(FileOutputStream marker=new FileOutputStream(complete)) { marker.write(1); }
        }
        return root.getAbsolutePath();
    }
    private static void copyAsset(Context context,String asset,File target) throws IOException {
        String[] children=context.getAssets().list(asset);
        if(children!=null && children.length>0) {
            if(!target.isDirectory() && !target.mkdirs()) throw new IOException("Cannot prepare speech model");
            for(String child:children) copyAsset(context,asset+"/"+child,new File(target,child));
        } else try(InputStream input=context.getAssets().open(asset); FileOutputStream output=new FileOutputStream(target)) {
            byte[] buffer=new byte[65536]; int count;
            while((count=input.read(buffer))!=-1) output.write(buffer,0,count);
        }
    }
    void start() {
        if(!ready()) return;
        final int token=++generation;
        worker.execute(() -> listen(token));
    }
    private void listen(int token) {
        if(closed || token!=generation) return;
        AudioRecord input=null;
        try(Recognizer recognizer=new Recognizer(model,16000)) {
            if(context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)!=android.content.pm.PackageManager.PERMISSION_GRANTED)
                throw new SecurityException("Microphone permission is off");
            int minimum=AudioRecord.getMinBufferSize(16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
            if(minimum<=0) throw new IOException("Microphone format unavailable");
            // Same UID and CAMCORDER source keep this capture in the camera's privacy policy.
            input=new AudioRecord(MediaRecorder.AudioSource.CAMCORDER,16000,AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,Math.max(minimum*2,6400));
            if(input.getState()!=AudioRecord.STATE_INITIALIZED) throw new IOException("Microphone unavailable");
            microphone=input;
            if(closed || token!=generation) return;
            input.startRecording();
            if(input.getRecordingState()!=AudioRecord.RECORDSTATE_RECORDING) throw new IOException("Cannot listen");
            short[] pcm=new short[1600]; String previous="";
            while(!closed && token==generation) {
                int count=input.read(pcm,0,pcm.length,AudioRecord.READ_BLOCKING);
                if(count<0) throw new IOException("Microphone read failed");
                if(count==0) continue;
                if(Build.VERSION.SDK_INT>=29 && input.getActiveRecordingConfiguration()!=null
                        && input.getActiveRecordingConfiguration().isClientSilenced()) throw new IOException("Microphone is silenced");
                boolean complete=recognizer.acceptWaveForm(pcm,count);
                JSONObject result=new JSONObject(complete ? recognizer.getResult() : recognizer.getPartialResult());
                String text=result.optString(complete ? "text" : "partial","");
                if(!text.isEmpty() && (!text.equals(previous) || complete)) {
                    previous=text;
                    main.post(() -> { if(!closed && token==generation) listener.onWords(text); });
                }
                if(complete) previous="";
            }
        } catch(Exception | LinkageError failure) {
            main.post(() -> { if(!closed && token==generation) listener.onError(); });
        } finally {
            if(input!=null) { try { input.stop(); } catch(IllegalStateException ignored) { } input.release(); }
            if(microphone==input) microphone=null;
        }
    }
    void stop() {
        generation++;
        AudioRecord active=microphone;
        if(active!=null) try { active.stop(); } catch(IllegalStateException ignored) { }
    }
    @Override public void close() {
        closed=true; stop();
        worker.execute(() -> { if(model!=null) { model.close(); model=null; } });
        worker.shutdown(); main.removeCallbacksAndMessages(null);
    }
}
