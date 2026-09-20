package com.tvengineering.remote.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ProfileStore {
    private static final String PREF = "tv_engineering_profiles";
    private static final String KEY = "profiles_json";
    private final SharedPreferences prefs;

    public ProfileStore(Context context) {
        prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public List<DeviceProfile> load() {
        List<DeviceProfile> out = new ArrayList<>();
        try {
            String saved = prefs.getString(KEY, null);
            if (saved != null) {
                JSONArray arr = new JSONArray(saved);
                for (int i = 0; i < arr.length(); i++) out.add(DeviceProfile.fromJson(arr.getJSONObject(i)));
            }
        } catch (Exception ignored) { }
        if (out.isEmpty()) out.add(createStarterProfile());
        return out;
    }

    public void save(List<DeviceProfile> profiles) {
        try {
            JSONArray arr = new JSONArray();
            for (DeviceProfile p : profiles) arr.put(p.toJson());
            prefs.edit().putString(KEY, arr.toString()).apply();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String exportProfile(DeviceProfile p) {
        try { return p.toJson().toString(2); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    public DeviceProfile importProfile(String json) {
        try { return DeviceProfile.fromJson(new JSONObject(json)); }
        catch (Exception e) { throw new IllegalArgumentException("Invalid profile JSON: " + e.getMessage(), e); }
    }

    private DeviceProfile createStarterProfile() {
        DeviceProfile p = new DeviceProfile();
        p.id = "starter";
        p.name = "Engineering Lab Profile";
        p.manufacturer = "Generic";
        p.model = "Import or create commands";
        return p;
    }
}
