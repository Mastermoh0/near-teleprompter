package com.mastermoh.near;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.VideoView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

public final class ReviewActivity extends AppCompatActivity {
    private VideoView video;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        setContentView(root); ScreenInsets.apply(this,root);
        MaterialToolbar toolbar = new MaterialToolbar(this); toolbar.setTitle(R.string.review_title);
        toolbar.setTitleTextColor(getColor(R.color.near_reader_text));
        toolbar.setNavigationIconTint(getColor(R.color.near_reader_text));
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationContentDescription(R.string.back_to_script); toolbar.setNavigationOnClickListener(v -> finish());
        root.addView(toolbar,new LinearLayout.LayoutParams(-1,-2));
        video = new VideoView(this); root.addView(video,new LinearLayout.LayoutParams(-1,0,1));
        MediaController controller = new MediaController(this); controller.setAnchorView(video); video.setMediaController(controller);
        MaterialButton share = new MaterialButton(this); share.setText(R.string.share_video);
        LinearLayout.LayoutParams button = new LinearLayout.LayoutParams(-1,Math.round(56*getResources().getDisplayMetrics().density));
        int margin = Math.round(16*getResources().getDisplayMetrics().density); button.setMargins(margin,margin,margin,margin); root.addView(share,button);
        String last = getSharedPreferences("recordings",MODE_PRIVATE).getString("last","");
        if (last.isEmpty()) { Snackbar.make(root,R.string.review_not_ready,Snackbar.LENGTH_LONG).show(); share.setEnabled(false); return; }
        Uri uri = Uri.parse(last);
        video.setVideoURI(uri);
        video.setOnPreparedListener(player -> { if (state != null) video.seekTo(state.getInt("position")); video.start(); });
        video.setOnErrorListener((player,what,extra) -> { share.setEnabled(false); Snackbar.make(root,R.string.review_failed,Snackbar.LENGTH_INDEFINITE).show(); return true; });
        share.setOnClickListener(v -> {
            Intent send = new Intent(Intent.ACTION_SEND).setType("video/mp4").putExtra(Intent.EXTRA_STREAM,uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            send.setClipData(ClipData.newRawUri("Near video",uri));
            startActivity(Intent.createChooser(send,getString(R.string.share_video)));
        });
    }
    @Override protected void onSaveInstanceState(Bundle state) { super.onSaveInstanceState(state); if (video != null) state.putInt("position",video.getCurrentPosition()); }
    @Override protected void onStop() { if (video != null) video.pause(); super.onStop(); }
    @Override protected void onDestroy() { if (video != null) video.stopPlayback(); super.onDestroy(); }
}
