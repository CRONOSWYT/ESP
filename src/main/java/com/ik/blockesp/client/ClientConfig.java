package com.ik.blockesp.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Carga una lista de IDs de bloque desde config/blockesp.json (en el directorio del juego) o recursos por defecto.
 */
public class ClientConfig {
    private static final Gson GSON = new Gson();
    private static final String CONFIG_NAME = "blockesp.json";

    public static class BlockStyle {
        public boolean enabled = true;
        public int colorArgb = 0xFF00FF00; // ARGB por defecto: Verde
        public float opacity = 1.0f; // 0..1 (se usa para líneas)
    }

    public static class ConfigData {
        public int radius = 32;
        public boolean seeThrough = true;
        public java.util.Map<String, BlockStyle> blocks = new java.util.HashMap<>();
        public java.util.Map<String, BlockStyle> items = new java.util.HashMap<>();
    }

    public static ConfigData loadConfig() {
        JsonObject json = readJson();
        ConfigData data = new ConfigData();
        if (json == null) return data;

        if (json.has("radius")) {
            try { data.radius = json.get("radius").getAsInt(); } catch (Exception ignored) {}
        }
        if (json.has("seeThrough")) {
            try { data.seeThrough = json.get("seeThrough").getAsBoolean(); } catch (Exception ignored) {}
        }

        // Compat: si hay "targets" (antiguo formato), convertir a estilos por defecto
        if (json.has("targets")) {
            JsonArray arr = json.getAsJsonArray("targets");
            for (JsonElement el : arr) {
                String id = el.getAsString();
                BlockStyle st = new BlockStyle();
                st.enabled = true;
                data.blocks.put(id, st);
            }
        }

        if (json.has("blocks")) {
            JsonObject blocks = json.getAsJsonObject("blocks");
            for (String key : blocks.keySet()) {
                JsonObject o = blocks.getAsJsonObject(key);
                BlockStyle st = new BlockStyle();
                if (o.has("enabled")) st.enabled = o.get("enabled").getAsBoolean();
                if (o.has("color")) {
                    String hex = o.get("color").getAsString();
                    try {
                        // Admite formatos #RRGGBB o #AARRGGBB
                        if (hex.startsWith("#")) hex = hex.substring(1);
                        long v = Long.parseLong(hex, 16);
                        if (hex.length() == 6) {
                            st.colorArgb = (int)(0xFF000000L | v);
                        } else if (hex.length() == 8) {
                            st.colorArgb = (int)v;
                        }
                    } catch (Exception ignored) {}
                }
                if (o.has("opacity")) {
                    try { st.opacity = o.get("opacity").getAsFloat(); } catch (Exception ignored) {}
                }
                data.blocks.put(key, st);
            }
        }

        if (json.has("items")) {
            JsonObject items = json.getAsJsonObject("items");
            for (String key : items.keySet()) {
                JsonObject o = items.getAsJsonObject(key);
                BlockStyle st = new BlockStyle();
                if (o.has("enabled")) st.enabled = o.get("enabled").getAsBoolean();
                if (o.has("color")) {
                    String hex = o.get("color").getAsString();
                    try {
                        if (hex.startsWith("#")) hex = hex.substring(1);
                        long v = Long.parseLong(hex, 16);
                        if (hex.length() == 6) {
                            st.colorArgb = (int)(0xFF000000L | v);
                        } else if (hex.length() == 8) {
                            st.colorArgb = (int)v;
                        }
                    } catch (Exception ignored) {}
                }
                if (o.has("opacity")) {
                    try { st.opacity = o.get("opacity").getAsFloat(); } catch (Exception ignored) {}
                }
                data.items.put(key, st);
            }
        }

        return data;
    }

    public static void saveConfig(ConfigData data) {
        JsonObject root = new JsonObject();
        root.addProperty("radius", data.radius);
        root.addProperty("seeThrough", data.seeThrough);

        JsonObject blocks = new JsonObject();
        for (var e : data.blocks.entrySet()) {
            JsonObject o = new JsonObject();
            o.addProperty("enabled", e.getValue().enabled);
            String hex = String.format("#%08X", e.getValue().colorArgb);
            o.addProperty("color", hex);
            o.addProperty("opacity", e.getValue().opacity);
            blocks.add(e.getKey(), o);
        }
        root.add("blocks", blocks);

        JsonObject items = new JsonObject();
        for (var e : data.items.entrySet()) {
            JsonObject o = new JsonObject();
            o.addProperty("enabled", e.getValue().enabled);
            String hex = String.format("#%08X", e.getValue().colorArgb);
            o.addProperty("color", hex);
            o.addProperty("opacity", e.getValue().opacity);
            items.add(e.getKey(), o);
        }
        root.add("items", items);

        Path configPath = Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve(CONFIG_NAME);
        try {
            if (!Files.exists(configPath.getParent())) Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException ignored) {}
    }

    private static JsonObject readJson() {
        Path configPath = Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve(CONFIG_NAME);
        // Intentar leer archivo de configuración externo
        if (Files.exists(configPath)) {
            try (BufferedReader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
                return GSON.fromJson(reader, JsonObject.class);
            } catch (IOException ignored) {
            }
        }
        // Fallback: archivo por defecto en recursos
        try {
            InputStream is = ClientConfig.class.getClassLoader().getResourceAsStream(CONFIG_NAME);
            if (is != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    return GSON.fromJson(reader, JsonObject.class);
                }
            }
        } catch (Exception ignored) {
        }
        return defaultJson();
    }

    private static JsonObject defaultJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("radius", 32);
        JsonObject blocks = new JsonObject();
        JsonObject diamond = new JsonObject();
        diamond.addProperty("enabled", true);
        diamond.addProperty("color", "#FF00FF00");
        diamond.addProperty("opacity", 1.0);
        blocks.add("minecraft:diamond_ore", diamond);
        JsonObject debris = new JsonObject();
        debris.addProperty("enabled", true);
        debris.addProperty("color", "#FFFF8800");
        debris.addProperty("opacity", 0.9);
        blocks.add("minecraft:ancient_debris", debris);
        obj.add("blocks", blocks);
        return obj;
    }
}
