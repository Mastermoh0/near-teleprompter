package com.mastermoh.near;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityNodeInfo;

public final class ReaderView extends View {
    interface Listener { void onScrub(float delta); void onTap(); void onLayoutReady(); }
    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private StaticLayout layout;
    private LinearGradient fade;
    private String text = "";
    private float textSp = 26;
    private float offset;
    private float startY;
    private float lastY;
    private boolean moved;
    private Listener listener;
    private String countdown;
    private int measurePercent = 100;
    private boolean cameraAppearance;

    public ReaderView(Context context) { this(context, null); }
    public ReaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        textPaint.setColor(context.getColor(R.color.near_reader_text));
        textPaint.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        setBackgroundColor(context.getColor(R.color.near_reader));
        setFocusable(true);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }
    void setListener(Listener listener) { this.listener = listener; }
    void setScript(String value, float size) {
        if (text.equals(value) && textSp == size) return;
        text = value; textSp = size;
        setContentDescription(value);
        rebuild();
    }
    void setMeasurePercent(int value) { measurePercent = value; rebuild(); }
    void setOffset(float value) { offset = value; invalidate(); }
    void setCountdown(String value) { countdown = value; invalidate(); }
    void setCameraAppearance(int textOpacity) {
        cameraAppearance = true;
        setBackgroundColor(Color.TRANSPARENT);
        textPaint.setAlpha(Math.round(255 * textOpacity / 100f));
        invalidate();
    }
    float maxOffset() { return layout == null ? 0 : layout.getLineTop(layout.getLineCount() - 1); }
    float offsetForCharacter(int character) { return layout==null ? 0 : layout.getLineTop(layout.getLineForOffset(Math.max(0,Math.min(text.length(),character)))); }
    int characterForOffset(float position) { return layout==null ? 0 : layout.getLineStart(layout.getLineForVertical(Math.max(0,Math.round(position)))); }
    float pixelsPerSecond(int pace) {
        if (layout == null) return 0;
        int words = ScrollSession.wordCount(text);
        return words == 0 ? 0 : layout.getHeight() * pace / (words * 60f);
    }
    float lineHeight() { return layout == null ? dp(36) : layout.getLineBottom(0) - layout.getLineTop(0); }
    private float dp(float value) { return value * getResources().getDisplayMetrics().density; }
    @Override protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        fade = new LinearGradient(0, h - dp(44), 0, h, Color.TRANSPARENT, getContext().getColor(R.color.near_reader), Shader.TileMode.CLAMP);
        rebuild();
    }
    private void rebuild() {
        int width = Math.round((getWidth() - dp(36)) * measurePercent / 100f);
        if (width < 1) return;
        textPaint.setTextSize(textSp * getResources().getDisplayMetrics().scaledDensity);
        layout = StaticLayout.Builder.obtain(text, 0, text.length(), textPaint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(dp(5), 1.1f)
                .setIncludePad(false).build();
        offset = Math.min(offset, maxOffset());
        if (listener != null) listener.onLayoutReady();
        invalidate();
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (layout == null) return;
        float left = (getWidth() - layout.getWidth()) / 2f;
        paint.setShader(null);
        paint.setColor(Color.rgb(31, 48, 36));
        if (!cameraAppearance) canvas.drawRoundRect(left - dp(8), dp(12), left + layout.getWidth() + dp(8), dp(12) + lineHeight(), dp(8), dp(8), paint);
        canvas.save();
        canvas.clipRect(0, dp(12), getWidth(), getHeight());
        canvas.translate(left, dp(12) - offset);
        layout.draw(canvas);
        canvas.restore();
        paint.setShader(fade);
        if (!cameraAppearance) canvas.drawRect(0, getHeight() - dp(44), getWidth(), getHeight(), paint);
        paint.setShader(null);
        if (countdown != null) {
            paint.setColor(Color.argb(238,17,23,19)); canvas.drawRect(0,0,getWidth(),getHeight(),paint);
            paint.setColor(getContext().getColor(R.color.near_accent)); paint.setTextSize(dp(54)); paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(countdown, getWidth()/2f, getHeight()/2f - (paint.ascent()+paint.descent())/2, paint);
            paint.setTextAlign(Paint.Align.LEFT);
        }
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        if (listener == null) return super.onTouchEvent(event);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                startY = lastY = event.getY(); moved = false; getParent().requestDisallowInterceptTouchEvent(true); return true;
            case MotionEvent.ACTION_MOVE:
                if (Math.abs(event.getY() - startY) > ViewConfiguration.get(getContext()).getScaledTouchSlop()) moved = true;
                if (moved) listener.onScrub(lastY - event.getY());
                lastY = event.getY(); return true;
            case MotionEvent.ACTION_UP:
                if (!moved) performClick(); return true;
            case MotionEvent.ACTION_CANCEL: return true;
            default: return true;
        }
    }
    @Override public boolean performClick() { super.performClick(); if (listener != null) listener.onTap(); return true; }
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        if (listener != null) {
            info.setScrollable(true);
            info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);
            info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);
        }
    }
    @Override public boolean performAccessibilityAction(int action, android.os.Bundle args) {
        if (listener != null && (action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD || action == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)) {
            listener.onScrub(lineHeight() * (action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD ? 1 : -1)); return true;
        }
        return super.performAccessibilityAction(action, args);
    }
}
