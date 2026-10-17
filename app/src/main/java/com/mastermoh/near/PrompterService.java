package com.mastermoh.near;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.Choreographer;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;

public final class PrompterService extends Service {
    static final String START = "com.mastermoh.near.START";
    static final String STOP = "com.mastermoh.near.STOP";
    static final String TOGGLE = "com.mastermoh.near.TOGGLE";
    static final String STATUS = "com.mastermoh.near.STATUS";
    private static final String CHANNEL = "reader";
    private static final int NOTIFICATION_ID = 41;
    static volatile boolean running;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ScrollSession session = new ScrollSession();
    private PrompterSettings settings;
    private WindowManager windows;
    private WindowManager.LayoutParams params;
    private LinearLayout window;
    private LinearLayout adjustments;
    private ReaderView reader;
    private TextView status;
    private TextView sizeLabel, paceLabel;
    private IconButton play;
    private String text = "";
    private int remaining;
    private boolean frameScheduled;
    private boolean screenReceiverRegistered;
    private int screenWidth, screenHeight;
    private float startX, startY;
    private int startWindowX, startWindowY;

    private final BroadcastReceiver screenOff = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { pause(); }
    };
    private final Choreographer.FrameCallback frame = new Choreographer.FrameCallback() {
        @Override public void doFrame(long time) {
            frameScheduled = false;
            if (reader == null) return;
            boolean stillPlaying = session.tick(time);
            reader.setOffset((float) session.position());
            if (stillPlaying) scheduleFrame(); else updateControls();
        }
    };
    private final Runnable countdownTick = new Runnable() {
        @Override public void run() {
            if (reader == null || remaining <= 0) return;
            remaining--;
            if (remaining == 0) {
                reader.setCountdown(null); session.play(); updateControls(); scheduleFrame();
            } else {
                reader.setCountdown(Integer.toString(remaining)); handler.postDelayed(this, 1000);
            }
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        settings = new PrompterSettings(this);
        windows = (WindowManager) getSystemService(WINDOW_SERVICE);
        NotificationChannel channel = new NotificationChannel(CHANNEL, getString(R.string.notification_channel), NotificationManager.IMPORTANCE_LOW);
        channel.setShowBadge(false);
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(screenOff, new IntentFilter(Intent.ACTION_SCREEN_OFF), RECEIVER_NOT_EXPORTED);
        else registerReceiver(screenOff, new IntentFilter(Intent.ACTION_SCREEN_OFF));
        screenReceiverRegistered = true;
    }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || STOP.equals(intent.getAction())) { stopSelf(); return START_NOT_STICKY; }
        try {
            if (Build.VERSION.SDK_INT >= 34) startForeground(NOTIFICATION_ID, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            else startForeground(NOTIFICATION_ID, notification());
            if (TOGGLE.equals(intent.getAction())) {
                if (reader != null) toggle(); else stopSelf();
                return START_NOT_STICKY;
            }
            if (!Settings.canDrawOverlays(this)) { fail(getString(R.string.permission_missing)); return START_NOT_STICKY; }
            pause();
            text = new ScriptStore(this).current().body;
            if (text.trim().isEmpty()) { fail(getString(R.string.empty_script)); return START_NOT_STICKY; }
            if (window == null) createWindow();
            session.restart();
            reader.setScript(text, settings.textSize());
            reader.setOffset(0);
            updateGeometry(false);
            running = true;
            updateControls();
            sendBroadcast(new Intent(STATUS).setPackage(getPackageName()));
        } catch (RuntimeException error) { fail(getString(R.string.reader_failed)); }
        return START_NOT_STICKY;
    }
    private Notification notification() {
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent toggle = PendingIntent.getService(this, 1, new Intent(this, PrompterService.class).setAction(TOGGLE), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop = PendingIntent.getService(this, 2, new Intent(this, PrompterService.class).setAction(STOP), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, CHANNEL).setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.notification_title)).setContentText(getString(R.string.notification_body))
                .setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true).setCategory(Notification.CATEGORY_SERVICE)
                .addAction(new Notification.Action.Builder(null, getString(R.string.pause_resume), toggle).build())
                .addAction(new Notification.Action.Builder(null, getString(R.string.close_reader), stop).build()).build();
    }
    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private TextView label(String value, float size) {
        TextView view = new TextView(this);
        view.setText(value); view.setTextSize(size); view.setTextColor(0xFFE3EFE5);
        view.setGravity(Gravity.CENTER_VERTICAL); view.setIncludeFontPadding(false);
        return view;
    }
    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL); return row;
    }
    private void createWindow() {
        window = new LinearLayout(this); window.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable background = new GradientDrawable(); background.setColor(Color.rgb(17,23,19)); background.setCornerRadius(dp(18));
        background.setStroke(dp(1), Color.rgb(63,79,67)); window.setBackground(background); window.setClipToOutline(true); window.setElevation(dp(6));

        LinearLayout header = row();
        status = label("Ready", 12); status.setPadding(dp(14),0,dp(8),0);
        status.setMinHeight(dp(48)); status.setContentDescription("Drag to move the reader");
        status.setFocusable(true);
        header.addView(status, new LinearLayout.LayoutParams(0, dp(48), 1));
        status.setOnTouchListener((view, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    startX=event.getRawX(); startY=event.getRawY(); startWindowX=params.x; startWindowY=params.y; return true;
                case MotionEvent.ACTION_MOVE:
                    params.x=startWindowX+Math.round(event.getRawX()-startX); params.y=startWindowY+Math.round(event.getRawY()-startY);
                    clampPosition(); updateWindow(); return true;
                case MotionEvent.ACTION_UP:
                    settings.position(params.x,params.y); view.performClick(); return true;
                default: return true;
            }
        });
        IconButton close = new IconButton(this, IconButton.Icon.CLOSE, getString(R.string.close_reader));
        close.setOnClickListener(view -> stopSelf()); header.addView(close, new LinearLayout.LayoutParams(dp(48),dp(48)));
        window.addView(header);

        reader = new ReaderView(this);
        reader.setListener(new ReaderView.Listener() {
            @Override public void onScrub(float delta) {
                pause(); session.seek(session.position()+delta); reader.setOffset((float) session.position()); updateControls();
            }
            @Override public void onTap() { toggle(); }
            @Override public void onLayoutReady() { configureSession(); }
        });
        window.addView(reader, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(152)));

        LinearLayout controls = row(); controls.setPadding(dp(6),0,dp(6),dp(4));
        IconButton restart = new IconButton(this, IconButton.Icon.RESTART, "Restart script");
        restart.setOnClickListener(view -> { pause(); session.restart(); reader.setOffset(0); updateControls(); });
        controls.addView(restart,new LinearLayout.LayoutParams(dp(48),dp(48)));
        play = new IconButton(this, IconButton.Icon.PLAY, "Play script");
        GradientDrawable primary = new GradientDrawable(); primary.setColor(0xFF31503A); primary.setCornerRadius(dp(14));
        play.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x44B4EDC4), primary, null));
        play.setOnClickListener(view -> toggle());
        LinearLayout.LayoutParams playParams = new LinearLayout.LayoutParams(0,dp(48),1); playParams.setMargins(dp(8),0,dp(8),0);
        controls.addView(play,playParams);
        IconButton adjust = new IconButton(this, IconButton.Icon.SETTINGS, "Show or hide reader settings");
        adjust.setOnClickListener(view -> {
            adjustments.setVisibility(adjustments.getVisibility()==View.VISIBLE ? View.GONE : View.VISIBLE);
            window.post(() -> { clampPosition(); updateWindow(); });
        });
        controls.addView(adjust,new LinearLayout.LayoutParams(dp(48),dp(48))); window.addView(controls);

        adjustments = new LinearLayout(this); adjustments.setOrientation(LinearLayout.VERTICAL); adjustments.setPadding(dp(6),dp(4),dp(6),dp(8));
        LinearLayout size = row(); sizeLabel = label("",12); sizeLabel.setPadding(dp(8),0,0,0);
        size.addView(sizeLabel,new LinearLayout.LayoutParams(0,dp(48),1));
        IconButton smaller = new IconButton(this,IconButton.Icon.MINUS,"Smaller text");
        smaller.setOnClickListener(view -> changeSize(-2)); size.addView(smaller,new LinearLayout.LayoutParams(dp(48),dp(48)));
        IconButton larger = new IconButton(this,IconButton.Icon.PLUS,"Larger text");
        larger.setOnClickListener(view -> changeSize(2));
        LinearLayout.LayoutParams largerParams = new LinearLayout.LayoutParams(dp(48),dp(48));
        largerParams.setMarginStart(dp(8)); size.addView(larger,largerParams);
        adjustments.addView(size);
        LinearLayout pace = row(); paceLabel = label("",12); paceLabel.setPadding(dp(8),0,0,0);
        pace.addView(paceLabel,new LinearLayout.LayoutParams(0,dp(48),1));
        IconButton slower = new IconButton(this,IconButton.Icon.MINUS,"Scroll slower");
        slower.setOnClickListener(view -> changePace(-5)); pace.addView(slower,new LinearLayout.LayoutParams(dp(48),dp(48)));
        IconButton faster = new IconButton(this,IconButton.Icon.PLUS,"Scroll faster");
        faster.setOnClickListener(view -> changePace(5));
        LinearLayout.LayoutParams fasterParams = new LinearLayout.LayoutParams(dp(48),dp(48));
        fasterParams.setMarginStart(dp(8)); pace.addView(faster,fasterParams);
        LinearLayout.LayoutParams paceRowParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);
        paceRowParams.topMargin=dp(8);
        adjustments.addView(pace,paceRowParams); adjustments.setVisibility(View.GONE); window.addView(adjustments);

        params = new WindowManager.LayoutParams(dp(320),WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.LEFT;
        if (Build.VERSION.SDK_INT >= 28) params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER;
        params.setTitle("Near teleprompter");
        readScreenSize();
        params.x = settings.x() < 0 ? (screenWidth-params.width)/2 : settings.x();
        params.y = settings.y() < 0 ? dp(12) : settings.y();
        windows.addView(window,params);
    }
    private void configureSession() {
        if (reader == null) return;
        session.configure(reader.maxOffset(),reader.pixelsPerSecond(settings.pace()));
    }
    private void changeSize(int delta) {
        pause();
        double progress = reader.maxOffset() == 0 ? 0 : session.position()/reader.maxOffset();
        settings.textSize(settings.textSize()+delta); reader.setScript(text,settings.textSize());
        configureSession(); session.seek(progress*reader.maxOffset()); reader.setOffset((float)session.position()); updateControls();
    }
    private void changePace(int delta) { settings.pace(settings.pace()+delta); configureSession(); updateControls(); }
    private void toggle() {
        if (session.playing() || remaining > 0) { pause(); return; }
        if (session.finished()) { session.restart(); reader.setOffset(0); }
        if (reader.maxOffset() <= 0) { updateControls(); return; }
        if (settings.countdown()) {
            remaining=3; reader.setCountdown("3"); handler.postDelayed(countdownTick,1000); updateControls();
        } else { session.play(); updateControls(); scheduleFrame(); }
    }
    private void pause() {
        handler.removeCallbacks(countdownTick); remaining=0; session.pause();
        if (frameScheduled) Choreographer.getInstance().removeFrameCallback(frame);
        frameScheduled=false;
        if (reader != null) reader.setCountdown(null);
        updateControls();
    }
    private void scheduleFrame() {
        if (session.playing() && !frameScheduled) { frameScheduled=true; Choreographer.getInstance().postFrameCallback(frame); }
    }
    private void updateControls() {
        if (play == null) return;
        boolean playing = session.playing() || remaining > 0;
        play.icon(playing ? IconButton.Icon.PAUSE : IconButton.Icon.PLAY, playing ? "Pause script" : "Play script");
        String state = remaining > 0 ? "Starting" : session.playing() ? "Scrolling" : session.finished() ? "End of script" : session.position() > 0 ? "Paused" : "Ready";
        status.setText(String.format(Locale.getDefault(),"%s · %d wpm",state,settings.pace()));
        sizeLabel.setText(String.format(Locale.getDefault(),"Text size  %d",settings.textSize()));
        paceLabel.setText(String.format(Locale.getDefault(),"Pace  %d wpm",settings.pace()));
    }
    private void readScreenSize() {
        if (Build.VERSION.SDK_INT >= 30) {
            android.view.WindowMetrics metrics = windows.getCurrentWindowMetrics();
            android.graphics.Insets bars = metrics.getWindowInsets().getInsetsIgnoringVisibility(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            screenWidth=metrics.getBounds().width()-bars.left-bars.right;
            screenHeight=metrics.getBounds().height()-bars.top-bars.bottom;
        } else {
            android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
            windows.getDefaultDisplay().getMetrics(metrics); screenWidth=metrics.widthPixels; screenHeight=metrics.heightPixels;
        }
    }
    private void updateGeometry(boolean rotated) {
        if (window == null) return;
        readScreenSize();
        float ratio = settings.width()==0 ? 0.62f : settings.width()==1 ? 0.80f : 0.96f;
        params.width = Math.min(screenWidth-dp(8),Math.max(dp(220),Math.round(screenWidth*ratio)));
        if (rotated) { params.x=(screenWidth-params.width)/2; params.y=dp(12); }
        LinearLayout.LayoutParams readerParams=(LinearLayout.LayoutParams)reader.getLayoutParams();
        readerParams.height=Math.min(dp(152),Math.max(dp(84),screenHeight-dp(216)));
        reader.setLayoutParams(readerParams);
        clampPosition(); updateWindow();
        window.post(() -> { if (window != null) { clampPosition(); updateWindow(); configureSession(); updateControls(); } });
    }
    private void clampPosition() {
        int height=window.getHeight()>0 ? window.getHeight() : dp(252);
        params.x=Math.max(0,Math.min(Math.max(0,screenWidth-params.width),params.x));
        params.y=Math.max(0,Math.min(Math.max(0,screenHeight-height),params.y));
    }
    private void updateWindow() {
        if (window == null || !window.isAttachedToWindow()) return;
        try { windows.updateViewLayout(window,params); }
        catch (RuntimeException error) { fail(getString(R.string.reader_failed)); }
    }
    @Override public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config); pause(); updateGeometry(true);
    }
    private void fail(String error) { sendBroadcast(new Intent(STATUS).setPackage(getPackageName()).putExtra("error",error)); stopSelf(); }
    @Override public void onDestroy() {
        pause(); handler.removeCallbacksAndMessages(null);
        if (window != null) { try { windows.removeView(window); } catch (IllegalArgumentException ignored) { } window=null; reader=null; }
        if (screenReceiverRegistered) unregisterReceiver(screenOff);
        running=false; stopForeground(STOP_FOREGROUND_REMOVE);
        sendBroadcast(new Intent(STATUS).setPackage(getPackageName()));
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
