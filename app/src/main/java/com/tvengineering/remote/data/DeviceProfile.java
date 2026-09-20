package com.tvengineering.remote.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

public class DeviceProfile {
    public String id = "profile";
    public String name = "Unnamed TV";
    public String manufacturer = "Generic";
    public String model = "Unknown";
    public final LinkedHashMap<String, IrCommand> commands = new LinkedHashMap<>();

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("name", name);
        o.put("manufacturer", manufacturer);
        o.put("model", model);
        JSONArray arr = new JSONArray();
        for (Map.Entry<String, IrCommand> e : commands.entrySet()) {
            JSONObject c = e.getValue().toJson();
            c.put("key", e.getKey());
            arr.put(c);
        }
        o.put("commands", arr);
        return o;
    }

    public static DeviceProfile fromJson(JSONObject o) throws JSONException {
        DeviceProfile p = new DeviceProfile();
        p.id = o.optString("id", "profile-" + System.currentTimeMillis());
        p.name = o.optString("name", "Unnamed TV");
        p.manufacturer = o.optString("manufacturer", "Generic");
        p.model = o.optString("model", "Unknown");
        JSONArray arr = o.optJSONArray("commands");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject cj = arr.getJSONObject(i);
                String key = cj.optString("key", cj.optString("name", "CMD_" + i));
                p.commands.put(key, IrCommand.fromJson(cj));
            }
        }
        return p;
    }
}
