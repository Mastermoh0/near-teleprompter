package com.mastermoh.near;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class ScriptStore {
    static final int MAX_CHARACTERS = 100_000;
    static final class Script {
        final String id;
        String title;
        String body;
        Script(String id, String title, String body) { this.id = id; this.title = title; this.body = body; }
    }
    private final SharedPreferences prefs;
    private final Context context;
    private final List<Script> scripts = new ArrayList<>();
    ScriptStore(Context context) {
        this.context = context;
        prefs = context.getSharedPreferences("scripts", Context.MODE_PRIVATE);
        try {
            JSONArray data = new JSONArray(prefs.getString("library", "[]"));
            for (int i = 0; i < data.length(); i++) {
                JSONObject item = data.getJSONObject(i);
                scripts.add(new Script(item.getString("id"), item.optString("title"), item.optString("body")));
            }
        } catch (JSONException ignored) { /* A malformed library is retained in prefs until the next explicit save. */ }
        if (scripts.isEmpty()) {
            scripts.add(new Script(UUID.randomUUID().toString(), context.getString(R.string.starter_title), context.getString(R.string.starter_body)));
        }
    }
    List<Script> all() { return new ArrayList<>(scripts); }
    Script current() {
        String selected = prefs.getString("selected", "");
        for (Script script : scripts) if (script.id.equals(selected)) return script;
        return scripts.get(0);
    }
    void select(Script script) { prefs.edit().putString("selected", script.id).apply(); }
    Script create(String title, String body) {
        Script script = new Script(UUID.randomUUID().toString(), title, body);
        scripts.add(0, script); select(script); save(); return script;
    }
    void delete(Script script) {
        scripts.remove(script);
        if (scripts.isEmpty()) scripts.add(new Script(UUID.randomUUID().toString(), context.getString(R.string.untitled), ""));
        select(scripts.get(0)); save();
    }
    void save() {
        JSONArray data = new JSONArray();
        for (Script script : scripts) {
            JSONObject item = new JSONObject();
            try { item.put("id", script.id); item.put("title", script.title); item.put("body", script.body); }
            catch (JSONException impossible) { throw new IllegalStateException(impossible); }
            data.put(item);
        }
        prefs.edit().putString("library", data.toString()).apply();
    }
}
