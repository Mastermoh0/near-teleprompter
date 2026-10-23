package com.mastermoh.near;

import android.os.Build;
import android.view.View;
import android.view.WindowInsets;
import android.view.Window;

final class ScreenInsets {
    static void apply(android.app.Activity activity, View root) {
        protectNavigation(activity.getWindow(),root);
        root.post(() -> applyBarAppearance(activity,root));
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            applyBarAppearance(activity,view);
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets safe = insets.getInsets(WindowInsets.Type.systemBars()
                        | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
                view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            } else {
                view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                        insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            }
            return insets;
        });
        root.requestApplyInsets();
    }
    private static void applyBarAppearance(android.app.Activity activity, View root) {
        int surface = com.google.android.material.color.MaterialColors.getColor(root,com.google.android.material.R.attr.colorSurface);
        boolean dark = androidx.core.graphics.ColorUtils.calculateLuminance(surface) < 0.5;
        androidx.core.view.WindowInsetsControllerCompat controller = androidx.core.view.WindowCompat.getInsetsController(
                activity.getWindow(),activity.getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(!dark);
    }
    // Match the OS theme at the navigation edge, independently of the dark camera.
    // The app draws this protection because target 35+ makes navigation translucent.
    static void protectNavigation(Window window, View root) {
        int surface = root.getContext().getColor(R.color.near_surface);
        boolean dark = androidx.core.graphics.ColorUtils.calculateLuminance(surface) < 0.5;
        root.post(() -> androidx.core.view.WindowCompat.getInsetsController(window,window.getDecorView())
                .setAppearanceLightNavigationBars(!dark));
        if (Build.VERSION.SDK_INT >= 29) window.setNavigationBarContrastEnforced(false);
        root.setForeground(new android.graphics.drawable.Drawable() {
            private final android.graphics.Paint paint = new android.graphics.Paint();
            { paint.setColor(surface); }
            @Override public void draw(android.graphics.Canvas canvas) {
                WindowInsets insets = root.getRootWindowInsets();
                if (insets == null) return;
                androidx.core.graphics.Insets nav = androidx.core.view.WindowInsetsCompat.toWindowInsetsCompat(insets,root)
                        .getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars());
                int width = root.getWidth(), height = root.getHeight();
                if (nav.bottom > 0) canvas.drawRect(0,height-nav.bottom,width,height,paint);
                if (nav.left > 0) canvas.drawRect(0,0,nav.left,height,paint);
                if (nav.right > 0) canvas.drawRect(width-nav.right,0,width,height,paint);
            }
            @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
            @Override public void setColorFilter(android.graphics.ColorFilter filter) { paint.setColorFilter(filter); }
            @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
        });
    }
}
