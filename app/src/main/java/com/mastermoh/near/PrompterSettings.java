package com.mastermoh.near;

import android.content.Context;
import android.content.SharedPreferences;

final class PrompterSettings {
    private final SharedPreferences prefs;
    PrompterSettings(Context context) { prefs = context.getSharedPreferences("reader", Context.MODE_PRIVATE); }
    int textSize() { return Math.max(18, Math.min(42, prefs.getInt("textSize", 26))); }
    int pace() { return Math.max(60, Math.min(220, prefs.getInt("pace", 120))); }
    int width() { return Math.max(0, Math.min(2, prefs.getInt("width", 1))); }
    boolean countdown() { return prefs.getBoolean("countdown", true); }
    boolean voiceFollow() { return prefs.getBoolean("voiceFollow",false); }
    void voiceFollow(boolean value) { prefs.edit().putBoolean("voiceFollow",value).apply(); }
    int panelOpacity() { return Math.max(0, Math.min(100, prefs.getInt("cameraPanelOpacity", 55))); }
    int textOpacity() { return Math.max(0, Math.min(100, prefs.getInt("cameraTextOpacity", 100))); }
    void panelOpacity(int value) { prefs.edit().putInt("cameraPanelOpacity", value).apply(); }
    void textOpacity(int value) { prefs.edit().putInt("cameraTextOpacity", value).apply(); }
    void textSize(int value) { prefs.edit().putInt("textSize", Math.max(18, Math.min(42, value))).apply(); }
    void pace(int value) { prefs.edit().putInt("pace", Math.max(60, Math.min(220, value))).apply(); }
    void width(int value) { prefs.edit().putInt("width", value).apply(); }
    void countdown(boolean value) { prefs.edit().putBoolean("countdown", value).apply(); }
    int x() { return prefs.getInt("x", -1); }
    int y() { return prefs.getInt("y", -1); }
    void position(int x, int y) { prefs.edit().putInt("x", x).putInt("y", y).apply(); }
}
