package com.mastermoh.near;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public final class ShareTextActivity extends AppCompatActivity {
    private TextInputEditText title, body;
    private MaterialButton save;
    private boolean saved;
    private boolean rejectedOversize;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_share_text);
        ScreenInsets.apply(this,findViewById(R.id.share_root));
        title = findViewById(R.id.shared_title);
        body = findViewById(R.id.shared_body);
        save = findViewById(R.id.save_shared);
        saved = state != null && state.getBoolean("saved");
        rejectedOversize = state != null && state.getBoolean("rejectedOversize");
        if (saved) { finish(); return; }
        if (state == null) receive(getIntent());
        findViewById(R.id.cancel_shared).setOnClickListener(v -> finish());
        save.setOnClickListener(v -> saveScript());
        body.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after) { }
            public void onTextChanged(CharSequence s,int start,int before,int count) { if (s.length() > 0) rejectedOversize = false; validate(); }
            public void afterTextChanged(Editable s) { }
        });
        body.setOnTouchListener((v, event) -> { v.getParent().requestDisallowInterceptTouchEvent(true); return false; });
        validate();
    }
    private void receive(Intent intent) {
        String text = "", subject = "";
        try {
            if (Intent.ACTION_SEND.equals(intent.getAction()) && "text/plain".equals(intent.getType())) {
                CharSequence incoming = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
                // Bound external text before the editable view performs expensive layout.
                if (incoming != null && incoming.length() > ScriptStore.MAX_CHARACTERS) rejectedOversize = true;
                else text = SharedText.normalize(incoming);
                subject = SharedText.title(intent.getCharSequenceExtra(Intent.EXTRA_SUBJECT));
            }
        } catch (RuntimeException invalidPayload) { /* The review stays empty for malformed external extras. */ }
        title.setText(subject.isEmpty() ? getString(R.string.untitled) : subject);
        body.setText(text);
        if (text.trim().matches("(?is)^https?://\\S+$"))
            ((TextView)findViewById(R.id.share_hint)).setText(R.string.shared_link_hint);
    }
    private void validate() {
        String value = body.getText() == null ? "" : body.getText().toString();
        save.setEnabled(!saved && SharedText.valid(value));
        ((TextInputLayout)findViewById(R.id.shared_body_layout)).setError(rejectedOversize ? getString(R.string.shared_rejected)
                : value.length() > ScriptStore.MAX_CHARACTERS ? getString(R.string.shared_too_long) : null);
    }
    private void saveScript() {
        String value = SharedText.normalize(body.getText());
        if (saved || !SharedText.valid(value)) return;
        saved = true; save.setEnabled(false);
        String name = SharedText.title(title.getText());
        new ScriptStore(this).create(name.isEmpty() ? getString(R.string.untitled) : name, value);
        startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        finish();
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out); out.putBoolean("saved", saved); out.putBoolean("rejectedOversize",rejectedOversize);
    }
}
