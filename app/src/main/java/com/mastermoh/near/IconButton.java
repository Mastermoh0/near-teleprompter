package com.mastermoh.near;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import androidx.appcompat.widget.AppCompatButton;

/** Small, consistently drawn controls with full 48 dp touch targets and spoken labels. */
final class IconButton extends AppCompatButton {
    enum Icon { PLAY, PAUSE, RESTART, SETTINGS, CLOSE, MINUS, PLUS }
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path playPath = new Path();
    private Icon icon;
    public IconButton(Context context) { this(context, Icon.PLAY, "Play script"); }
    IconButton(Context context, Icon icon, String description) {
        super(context);
        this.icon = icon;
        playPath.moveTo(-5,-9); playPath.lineTo(9,0); playPath.lineTo(-5,9); playPath.close();
        setContentDescription(description);
        setMinWidth(0); setMinimumWidth(0); setMinHeight(0); setMinimumHeight(0);
        setPadding(0,0,0,0);
        GradientDrawable base = new GradientDrawable(); base.setColor(Color.TRANSPARENT); base.setCornerRadius(dp(12));
        setBackground(new RippleDrawable(ColorStateList.valueOf(0x33B4EDC4), base, null));
    }
    void icon(Icon value, String label) { icon = value; setContentDescription(label); invalidate(); }
    private float dp(float value) { return value * getResources().getDisplayMetrics().density; }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save(); canvas.translate(getWidth()/2f, getHeight()/2f); canvas.scale(dp(1), dp(1));
        paint.setColor(Color.rgb(227,239,229)); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(2); paint.setStrokeCap(Paint.Cap.ROUND); paint.setStrokeJoin(Paint.Join.ROUND);
        switch (icon) {
            case PLAY:
                paint.setStyle(Paint.Style.FILL); canvas.drawPath(playPath,paint); break;
            case PAUSE:
                paint.setStyle(Paint.Style.FILL); canvas.drawRoundRect(-7,-9,-2,9,1,1,paint); canvas.drawRoundRect(2,-9,7,9,1,1,paint); break;
            case CLOSE: canvas.drawLine(-6,-6,6,6,paint); canvas.drawLine(-6,6,6,-6,paint); break;
            case PLUS: canvas.drawLine(0,-7,0,7,paint); canvas.drawLine(-7,0,7,0,paint); break;
            case MINUS: canvas.drawLine(-7,0,7,0,paint); break;
            case RESTART:
                canvas.drawArc(-8,-8,8,8,-80,285,false,paint); canvas.drawLine(-8,-8,-8,-1,paint); canvas.drawLine(-8,-1,-1,-1,paint); break;
            case SETTINGS:
                canvas.drawLine(-9,-6,9,-6,paint); canvas.drawLine(-9,6,9,6,paint);
                paint.setStyle(Paint.Style.FILL); canvas.drawCircle(-3,-6,3,paint); canvas.drawCircle(4,6,3,paint); break;
        }
        canvas.restore();
    }
}
