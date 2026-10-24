package com.mastermoh.near;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.MediaStoreOutputOptions;
import androidx.camera.video.PendingRecording;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class CameraActivity extends AppCompatActivity {
    static volatile boolean visible;
    private static final int CAMERA_PERMISSION = 10, AUDIO_PERMISSION = 11;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ScrollSession session = new ScrollSession();
    private final ExecutorService fileChecks = Executors.newSingleThreadExecutor();
    private ProcessCameraProvider provider;
    private VideoCapture<Recorder> capture;
    private Recording recording;
    private PreviewView preview;
    private ReaderView reader;
    private PrompterSettings settings;
    private LinearLayout panel;
    private FrameLayout stage;
    private MaterialButton recordButton, playButton, qualityButton, permissionButton;
    private IconButton flipButton, reviewButton;
    private TextView status;
    private String scriptText;
    private boolean front = true, hd, ready, saving, audio, leaving, foreground, framePosted, destroyed;
    private boolean canFlip, canSelectQuality;
    private int countdown;
    private File legacyFile;
    private BottomSheetDialog settingsSheet;
    private float dragY, panelY;
    private OfflineVoice voice;
    private ScriptMatcher matcher;
    private boolean voiceMode, voicePlaying, audioInterrupted;
    private float voiceTarget;
    private long voiceFrame;

    private final Choreographer.FrameCallback frame = new Choreographer.FrameCallback() {
        @Override public void doFrame(long nanos) {
            framePosted = false;
            if(voiceMode && voicePlaying) {
                double elapsed=voiceFrame==0 ? 1.0/60 : Math.min(0.1,(nanos-voiceFrame)/1_000_000_000.0);
                voiceFrame=nanos;
                double distance=voiceTarget-session.position();
                session.seek(session.position()+distance*Math.min(1,elapsed*12)); reader.setOffset((float)session.position());
                if(Math.abs(distance)>0.5) scheduleFrame(); else voiceFrame=0;
                return;
            }
            if (session.playing()) {
                session.tick(nanos); reader.setOffset((float)session.position());
                if (session.playing()) scheduleFrame(); else updateControls();
            }
        }
    };
    private final Runnable countdownTick = new Runnable() {
        @Override public void run() {
            if (!foreground || countdown == 0) return;
            countdown--;
            if (countdown == 0) { reader.setCountdown(null); startRecording(); }
            else { reader.setCountdown(Integer.toString(countdown)); handler.postDelayed(this, 1000); }
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_camera);
        ScreenInsets.apply(this,findViewById(R.id.camera_root));
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        settings = new PrompterSettings(this);
        scriptText = new ScriptStore(this).current().body;
        matcher=new ScriptMatcher(scriptText);
        voiceMode=settings.voiceFollow();
        voice=new OfflineVoice(this,new OfflineVoice.Listener() {
            public void onReady() { if(!destroyed) updateControls(); }
            public void onWords(String words) {
                if(foreground && voiceMode && voicePlaying) {
                    voiceTarget=reader.offsetForCharacter(matcher.accept(words)); scheduleFrame();
                }
            }
            public void onError() {
                if(destroyed) return;
                pauseScript();
                if(!voice.ready()) { voiceMode=false; settings.voiceFollow(false); }
                status.setText(R.string.voice_unavailable); updateControls();
            }
        });
        hd = getPreferences(MODE_PRIVATE).getBoolean("hd", false);
        if (state != null) front = state.getBoolean("front", true);
        preview = findViewById(R.id.camera_preview);
        reader = findViewById(R.id.camera_reader);
        panel = findViewById(R.id.camera_panel);
        stage = findViewById(R.id.camera_stage);
        status = findViewById(R.id.camera_status);
        recordButton = findViewById(R.id.camera_record);
        playButton = findViewById(R.id.camera_script_play);
        qualityButton = findViewById(R.id.camera_quality);
        permissionButton = findViewById(R.id.camera_permission);
        addIcon(R.id.camera_back_slot, IconButton.Icon.BACK, getString(R.string.back_to_script)).setOnClickListener(v -> leave());
        flipButton = addIcon(R.id.camera_flip_slot, IconButton.Icon.FLIP, getString(R.string.flip_camera));
        flipButton.setOnClickListener(v -> { front = !front; bindCamera(); });
        addIcon(R.id.camera_restart_slot, IconButton.Icon.RESTART, "Restart script").setOnClickListener(v -> {
            pauseScript(); session.restart(); reader.setOffset(0); updateControls();
            matcher.seekCharacter(0); voiceTarget=0;
        });
        addIcon(R.id.camera_settings_slot, IconButton.Icon.SETTINGS, getString(R.string.camera_reader_settings)).setOnClickListener(v -> showReaderSettings());
        reviewButton = addIcon(R.id.camera_review_slot, IconButton.Icon.VIDEO, getString(R.string.camera_review));
        reviewButton.setOnClickListener(v -> startActivity(new Intent(this, ReviewActivity.class)));
        recordButton.setOnClickListener(v -> {
            if (countdown > 0) cancelCountdown();
            else if (recording != null) stopRecording();
            else if (ready && !saving) requestRecording();
        });
        playButton.setOnClickListener(v -> toggleScript());
        qualityButton.setOnClickListener(v -> { hd = !hd; getPreferences(MODE_PRIVATE).edit().putBoolean("hd", hd).apply(); bindCamera(); });
        permissionButton.setOnClickListener(v -> {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                if (getPreferences(MODE_PRIVATE).getBoolean("askedCamera", false) && !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA))
                    startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())));
                else askForCamera();
            } else bindCamera();
        });
        reader.setListener(new ReaderView.Listener() {
            public void onScrub(float delta) { pauseScript(); session.seek(session.position()+delta); reader.setOffset((float)session.position()); matcher.seekCharacter(reader.characterForOffset((float)session.position())); voiceTarget=(float)session.position(); }
            public void onTap() { toggleScript(); }
            public void onLayoutReady() { configureSession(); }
        });
        stage.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob) -> resizePanel());
        findViewById(R.id.camera_drag_handle).setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) { dragY = event.getRawY(); panelY = panel.getY(); return true; }
            if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                panel.setY(Math.max(0, Math.min(stage.getHeight()-panel.getHeight(), panelY+event.getRawY()-dragY))); return true;
            }
            if (event.getActionMasked() == MotionEvent.ACTION_UP) { v.performClick(); return true; }
            return true;
        });
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { leave(); }
        });
        adaptLayout(); applyReader(); updateControls();
        if(voiceMode) voice.prepare();
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) askForCamera();
    }
    private IconButton addIcon(int slot, IconButton.Icon icon, String label) {
        IconButton button = new IconButton(this, icon, label);
        ((FrameLayout)findViewById(slot)).addView(button, new FrameLayout.LayoutParams(-1,-1));
        return button;
    }
    private void askForCamera() {
        getPreferences(MODE_PRIVATE).edit().putBoolean("askedCamera", true).apply();
        requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION);
    }
    private void loadProvider() {
        if (provider != null) { bindCamera(); return; }
        var future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            if (destroyed) return;
            try { provider = future.get(); if (foreground) bindCamera(); }
            catch (Exception error) { android.util.Log.w("NearCamera","Camera provider unavailable",error); cameraError(); }
        }, ContextCompat.getMainExecutor(this));
    }
    private void bindCamera() {
        if (provider == null) { loadProvider(); return; }
        if (!foreground || saving || recording != null || countdown > 0) return;
        ready = false; updateControls();
        try {
            CameraSelector selector = front ? CameraSelector.DEFAULT_FRONT_CAMERA : CameraSelector.DEFAULT_BACK_CAMERA;
            if (!provider.hasCamera(selector)) { front = !front; selector = front ? CameraSelector.DEFAULT_FRONT_CAMERA : CameraSelector.DEFAULT_BACK_CAMERA; }
            provider.unbindAll();
            Preview cameraPreview = new Preview.Builder().setTargetAspectRatio(androidx.camera.core.AspectRatio.RATIO_16_9).build();
            cameraPreview.setSurfaceProvider(preview.getSurfaceProvider());
            Quality preferred = hd ? Quality.HD : Quality.FHD;
            java.util.List<Quality> supported = QualitySelector.getSupportedQualities(provider.getCameraInfo(selector));
            if (supported.isEmpty()) throw new IllegalStateException("No video quality available");
            Quality actual = supported.contains(preferred) ? preferred : supported.contains(Quality.HD) ? Quality.HD
                    : supported.contains(Quality.SD) ? Quality.SD : supported.get(0);
            Recorder recorder = new Recorder.Builder().setQualitySelector(QualitySelector.from(actual)).build();
            capture = VideoCapture.withOutput(recorder);
            capture.setTargetRotation(preview.getDisplay().getRotation());
            provider.bindToLifecycle(this, selector, cameraPreview, capture);
            qualityButton.setText(actual == Quality.UHD ? "4K" : actual == Quality.FHD ? "1080p" : actual == Quality.HD ? "720p" : "SD");
            canSelectQuality = supported.contains(Quality.FHD) && supported.contains(Quality.HD);
            canFlip = provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) && provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA);
            ready = true;
            status.setText(R.string.camera_ready); permissionButton.setVisibility(View.GONE); updateControls();
        } catch (Exception error) { android.util.Log.w("NearCamera","Could not bind camera",error); cameraError(); }
    }
    private void cameraError() {
        ready = false;
        status.setText(checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                ? R.string.camera_unavailable : R.string.camera_no_permission);
        permissionButton.setText(checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                ? R.string.retry_camera : R.string.allow_camera);
        permissionButton.setVisibility(View.VISIBLE); updateControls();
    }
    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == CAMERA_PERMISSION) {
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) loadProvider(); else cameraError();
        }
        if (requestCode == AUDIO_PERMISSION) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) beginCountdown(true);
            else new MaterialAlertDialogBuilder(this).setTitle(R.string.camera_mic_title).setMessage(R.string.camera_mic_body)
                    .setNegativeButton(R.string.cancel, null).setNeutralButton(R.string.camera_silent,(d,w) -> beginCountdown(false))
                    .setPositiveButton(R.string.open_settings,(d,w) -> startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:"+getPackageName())))).show();
        }
        if(requestCode==12 && checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED) startScript();
    }
    private void requestRecording() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) beginCountdown(true);
        else requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, AUDIO_PERMISSION);
    }
    private void beginCountdown(boolean withAudio) {
        if (!foreground || !ready || recording != null || saving || countdown > 0) return;
        audio = withAudio; pauseScript();
        if(!withAudio && voiceMode) { voiceMode=false; settings.voiceFollow(false); updateControls(); }
        if (settings.countdown()) {
            countdown = 3; reader.setCountdown("3"); handler.postDelayed(countdownTick,1000); updateControls();
        } else startRecording();
    }
    private void cancelCountdown() {
        handler.removeCallbacks(countdownTick); countdown = 0; reader.setCountdown(null); updateControls();
    }
    private void startRecording() {
        if (!foreground || !ready || capture == null) return;
        try {
            audioInterrupted=false;
            String filename = "Near_"+new SimpleDateFormat("yyyyMMdd_HHmmss_SSS",Locale.US).format(new Date())+".mp4";
            PendingRecording pending;
            legacyFile = null;
            if (Build.VERSION.SDK_INT >= 29) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Video.Media.DISPLAY_NAME,filename);
                values.put(MediaStore.Video.Media.MIME_TYPE,"video/mp4");
                values.put(MediaStore.Video.Media.RELATIVE_PATH,"Movies/Near");
                pending = capture.getOutput().prepareRecording(this, new MediaStoreOutputOptions.Builder(
                        getContentResolver(),MediaStore.Video.Media.EXTERNAL_CONTENT_URI).setContentValues(values).build());
            } else {
                File directory = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
                if (directory == null) directory = new File(getFilesDir(),"recordings");
                if (!directory.exists() && !directory.mkdirs()) throw new java.io.IOException();
                legacyFile = new File(directory,filename);
                pending = capture.getOutput().prepareRecording(this,new FileOutputOptions.Builder(legacyFile).build());
            }
            if (audio) {
                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
                    throw new SecurityException("Microphone permission changed before recording");
                pending = pending.withAudioEnabled();
            }
            recording = pending.start(ContextCompat.getMainExecutor(this), this::onVideoEvent);
            status.setText(R.string.camera_loading); updateControls();
        } catch (Exception error) { recording = null; saving = false; status.setText(R.string.camera_failed); updateControls(); }
    }
    private void onVideoEvent(VideoRecordEvent event) {
        if (event instanceof VideoRecordEvent.Start) {
            if (!foreground) { stopRecording(); return; }
            status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_NONE);
            status.setText(audio ? R.string.camera_recording : R.string.camera_recording_silent);
            if (session.finished()) { session.restart(); reader.setOffset(0); matcher.seekCharacter(0); }
            startScript(); updateControls();
        } else if (event instanceof VideoRecordEvent.Status) {
            if (!saving && !destroyed) {
                long seconds = event.getRecordingStats().getRecordedDurationNanos()/1_000_000_000;
                if(audio && event.getRecordingStats().getAudioStats().hasError()) {
                    audioInterrupted=true;
                    if(voicePlaying) pauseScript();
                }
                status.setText(getString(audioInterrupted ? R.string.recording_audio_interrupted : audio ? R.string.camera_recording : R.string.camera_recording_silent)
                        +String.format(Locale.getDefault()," · %02d:%02d",seconds/60,seconds%60));
            }
        } else if (event instanceof VideoRecordEvent.Finalize) {
            recording = null; saving = true; pauseScript(); updateControls();
            VideoRecordEvent.Finalize result = (VideoRecordEvent.Finalize)event;
            Uri savedUri = legacyFile == null ? result.getOutputResults().getOutputUri()
                    : FileProvider.getUriForFile(this,getPackageName()+".files",legacyFile);
            File file = legacyFile;
            boolean expectedAudio = audio;
            boolean interruptedAudio=audioInterrupted;
            fileChecks.execute(() -> {
                boolean playable = false, hasAudio = false;
                MediaMetadataRetriever metadata = new MediaMetadataRetriever();
                try {
                    metadata.setDataSource(this,savedUri);
                    playable = "yes".equals(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO))
                            && Long.parseLong(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)) > 0;
                    hasAudio = "yes".equals(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO));
                } catch (Exception ignored) { }
                finally { try { metadata.release(); } catch (java.io.IOException ignored) { } }
                final boolean valid = playable, sound = hasAudio;
                if (valid) getSharedPreferences("recordings",MODE_PRIVATE).edit().putString("last",savedUri.toString()).apply();
                else {
                    try { if (file != null) file.delete(); else if (!Uri.EMPTY.equals(savedUri)) getContentResolver().delete(savedUri,null,null); }
                    catch (RuntimeException ignored) { }
                }
                handler.post(() -> {
                    saving = false;
                    if (!destroyed) {
                        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
                        status.setText(!valid ? R.string.camera_failed : expectedAudio && !sound ? R.string.camera_audio_failed
                                : interruptedAudio ? R.string.saved_audio_interrupted : result.hasError() ? R.string.camera_partial : R.string.camera_saved);
                        updateControls();
                        if (leaving) finish(); else if (foreground) bindCameraAfterSave();
                    } else fileChecks.shutdown();
                });
            });
        }
    }
    private void bindCameraAfterSave() {
        CharSequence savedStatus = status.getText();
        bindCamera();
        if (ready) status.setText(savedStatus);
        updateControls();
    }
    private void stopRecording() {
        cancelCountdown(); pauseScript();
        if (recording != null && !saving) { saving = true; status.setText(R.string.camera_saving); recording.stop(); }
        updateControls();
    }
    private void leave() {
        if (saving) { leaving = true; return; }
        if (recording != null) new MaterialAlertDialogBuilder(this).setTitle(R.string.camera_stop_first).setMessage(R.string.camera_leave_body)
                .setNegativeButton(R.string.cancel,null).setPositiveButton(R.string.camera_stop_leave,(d,w) -> { leaving = true; stopRecording(); }).show();
        else { cancelCountdown(); finish(); }
    }
    private void configureSession() { session.configure(reader.maxOffset(),reader.pixelsPerSecond(settings.pace())); }
    private void scheduleFrame() {
        if (!framePosted && foreground) { framePosted = true; Choreographer.getInstance().postFrameCallback(frame); }
    }
    private void pauseScript() { session.pause(); voicePlaying=false; voiceFrame=0; if(voice!=null) voice.stop(); if (playButton != null) updateControls(); }
    private void startScript() {
        if(voiceMode) {
            if(!voice.ready()) return;
            if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},12); return;
            }
            voicePlaying=true; voiceTarget=(float)session.position(); voiceFrame=0; voice.start();
        } else { session.play(); scheduleFrame(); }
        updateControls();
    }
    private void toggleScript() {
        if (countdown > 0 || saving) return;
        if (session.playing() || voicePlaying) pauseScript();
        else {
            if (session.finished()) { session.restart(); reader.setOffset(0); matcher.seekCharacter(0); }
            startScript();
        }
        updateControls();
    }
    private void applyReader() {
        double progress = reader.maxOffset() == 0 ? 0 : session.position()/reader.maxOffset();
        reader.setScript(scriptText,settings.textSize()); reader.setCameraAppearance(settings.textOpacity());
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.argb(Math.round(255*settings.panelOpacity()/100f),17,23,19)); background.setCornerRadius(dp(18));
        panel.setBackground(background); panel.setClipToOutline(true);
        configureSession(); session.seek(progress*reader.maxOffset()); reader.setOffset((float)session.position());
        voiceTarget=reader.offsetForCharacter(matcher.character());
    }
    private void resizePanel() {
        if (stage.getHeight() <= 0) return;
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams)panel.getLayoutParams();
        int height = Math.min(dp(220),stage.getHeight());
        if (params.height != height) { params.height = height; panel.setLayoutParams(params); }
        panel.setY(Math.max(0,Math.min(stage.getHeight()-height,panel.getY())));
    }
    private void adaptLayout() {
        boolean landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        LinearLayout body = findViewById(R.id.camera_body);
        body.setOrientation(landscape ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        LinearLayout.LayoutParams stageParams = new LinearLayout.LayoutParams(landscape ? 0 : -1,landscape ? -1 : 0,1);
        stageParams.setMargins(dp(12),dp(12),dp(12),dp(12)); stage.setLayoutParams(stageParams);
        View controls = findViewById(R.id.camera_controls);
        controls.setLayoutParams(new LinearLayout.LayoutParams(landscape ? dp(280) : -1,landscape ? -1 : -2));
    }
    private void updateControls() {
        if (recordButton == null) return;
        boolean busy = recording != null || saving || countdown > 0;
        boolean modelReady=!voiceMode || voice.ready();
        recordButton.setEnabled(ready && !saving && modelReady);
        recordButton.setText(saving ? R.string.camera_saving : countdown > 0 ? R.string.camera_cancel
                : recording != null ? R.string.camera_stop : R.string.camera_record);
        recordButton.setBackgroundTintList(ColorStateList.valueOf(recording != null ? 0xFFB3261E : getColor(R.color.near_accent)));
        recordButton.setTextColor(recording != null ? Color.WHITE : 0xFF07381F);
        playButton.setEnabled(countdown == 0 && !saving && modelReady);
        playButton.setText(session.playing() || voicePlaying ? R.string.camera_pause : voiceMode ? R.string.follow_voice : R.string.camera_play);
        ((TextView)findViewById(R.id.camera_mode)).setText(voiceMode ? voice.ready() ? R.string.voice_english : R.string.voice_preparing : R.string.fixed_pace);
        qualityButton.setEnabled(!busy && ready && canSelectQuality);
        flipButton.setEnabled(!busy && ready && canFlip);
        reviewButton.setEnabled(!busy && getSharedPreferences("recordings",MODE_PRIVATE).contains("last"));
        flipButton.setAlpha(flipButton.isEnabled() ? 1f : 0.4f);
        reviewButton.setAlpha(reviewButton.isEnabled() ? 1f : 0.4f);
    }
    private void showReaderSettings() {
        settingsSheet = new BottomSheetDialog(this);
        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        LinearLayout content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(24),dp(12),dp(24),dp(24));
        TextView heading = new TextView(this); heading.setText(R.string.camera_reader_settings); heading.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleLarge);
        content.addView(heading);
        com.google.android.material.materialswitch.MaterialSwitch follow=new com.google.android.material.materialswitch.MaterialSwitch(this);
        follow.setId(R.id.voice_follow_switch); follow.setText(R.string.voice_english); follow.setChecked(voiceMode);
        follow.setTextColor(getColor(R.color.near_reader_text));
        follow.setEnabled(recording==null && !saving && countdown==0);
        LinearLayout.LayoutParams modeParams=new LinearLayout.LayoutParams(-1,-2); modeParams.topMargin=dp(16);
        follow.setMinHeight(dp(64)); content.addView(follow,modeParams);
        TextView hint=new TextView(this); hint.setText(R.string.voice_hint); hint.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall);
        content.addView(hint,new LinearLayout.LayoutParams(-1,-2));
        addSlider(content,R.string.text_size,18,42,1,settings.textSize(),v -> { settings.textSize(v); applyReader(); });
        Slider paceSlider=addSlider(content,R.string.speed,60,220,5,settings.pace(),v -> { settings.pace(v); configureSession(); });
        paceSlider.setEnabled(!voiceMode);
        follow.setOnCheckedChangeListener((button,checked) -> {
            pauseScript(); voiceMode=checked; settings.voiceFollow(checked); paceSlider.setEnabled(!checked);
            matcher.seekCharacter(reader.characterForOffset((float)session.position()));
            if(checked) voice.prepare();
            updateControls();
        });
        addSlider(content,R.string.panel_opacity,0,100,5,settings.panelOpacity(),v -> { settings.panelOpacity(v); applyReader(); });
        addSlider(content,R.string.letters_opacity,0,100,5,settings.textOpacity(),v -> { settings.textOpacity(v); applyReader(); });
        MaterialButton reset = new MaterialButton(this); reset.setText(R.string.reset_appearance);
        LinearLayout.LayoutParams resetParams = new LinearLayout.LayoutParams(-1,dp(56)); resetParams.topMargin=dp(8); content.addView(reset,resetParams);
        reset.setOnClickListener(v -> { settings.panelOpacity(55); settings.textOpacity(100); applyReader(); settingsSheet.dismiss(); });
        scroll.addView(content); settingsSheet.setContentView(scroll); settingsSheet.show();
        ScreenInsets.protectNavigation(settingsSheet.getWindow(),settingsSheet.getWindow().getDecorView());
    }
    private interface SettingChange { void change(int value); }
    private Slider addSlider(LinearLayout content,int label,int min,int max,int step,int value,SettingChange change) {
        TextView text = new TextView(this); text.setText(getString(label)+"  "+value); text.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelLarge);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2); lp.topMargin=dp(16); content.addView(text,lp);
        Slider slider = new Slider(this); slider.setContentDescription(getString(label)); slider.setValueFrom(min); slider.setValueTo(max); slider.setStepSize(step); slider.setValue(value);
        content.addView(slider,new LinearLayout.LayoutParams(-1,dp(48)));
        slider.addOnChangeListener((s,v,user) -> { int number = Math.round(v); text.setText(getString(label)+"  "+number); change.change(number); });
        return slider;
    }
    private int dp(int value) { return Math.round(value*getResources().getDisplayMetrics().density); }
    @Override protected void onStart() {
        super.onStart(); foreground = true; visible=true;
        // A cached provider can resolve before the preview has attached to a display.
        preview.post(() -> { if (foreground && !destroyed && checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) loadProvider(); });
    }
    @Override protected void onStop() {
        foreground = false; visible=false; cancelCountdown(); stopRecording(); pauseScript();
        super.onStop();
    }
    @Override protected void onSaveInstanceState(Bundle state) { super.onSaveInstanceState(state); state.putBoolean("front",front); }
    @Override public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config); stopRecording();
        adaptLayout();
        if (settingsSheet != null) settingsSheet.dismiss();
        if (capture != null && recording == null && !saving) capture.setTargetRotation(preview.getDisplay().getRotation());
        panel.setY(0); stage.post(this::resizePanel);
        if (recording == null && !saving) stage.post(this::bindCamera);
    }
    @Override protected void onDestroy() {
        destroyed = true; handler.removeCallbacks(countdownTick); Choreographer.getInstance().removeFrameCallback(frame);
        voice.close();
        if (settingsSheet != null) settingsSheet.dismiss();
        if (recording == null && !saving) fileChecks.shutdown();
        super.onDestroy();
    }
}
