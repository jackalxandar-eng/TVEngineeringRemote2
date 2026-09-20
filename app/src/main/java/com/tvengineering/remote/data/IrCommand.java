package com.tvengineering.remote.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class IrCommand {
    public enum Type { RAW, NEC, PRONTO, PULSE32 }

    public String name;
    public Type type = Type.RAW;
    public int carrierHz = 38000;
    public int[] rawPattern = new int[0];
    public int address = 0;
    public int command = 0;
    public long data32 = 0;
    public String pronto = "";
    public boolean service = false;
    public boolean dangerous = false;

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("name", name);
        o.put("type", type.name());
        o.put("carrierHz", carrierHz);
        o.put("address", address);
        o.put("command", command);
        o.put("data32", data32);
        o.put("pronto", pronto);
        o.put("service", service);
        o.put("dangerous", dangerous);
        JSONArray arr = new JSONArray();
        for (int v : rawPattern) arr.put(v);
        o.put("raw", arr);
        return o;
    }

    public static IrCommand fromJson(JSONObject o) throws JSONException {
        IrCommand c = new IrCommand();
        c.name = o.optString("name", "COMMAND");
        try { c.type = Type.valueOf(o.optString("type", "RAW").toUpperCase()); }
        catch (Exception ignored) { c.type = Type.RAW; }
        c.carrierHz = o.optInt("carrierHz", 38000);
        c.address = o.optInt("address", 0);
        c.command = o.optInt("command", 0);
        c.data32 = o.optLong("data32", 0);
        c.pronto = o.optString("pronto", "");
        c.service = o.optBoolean("service", false);
        c.dangerous = o.optBoolean("dangerous", false);
        JSONArray a = o.optJSONArray("raw");
        if (a != null) {
            c.rawPattern = new int[a.length()];
            for (int i = 0; i < a.length(); i++) c.rawPattern[i] = a.optInt(i, 0);
        }
        return c;
    }
}
