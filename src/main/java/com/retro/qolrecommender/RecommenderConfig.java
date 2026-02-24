package com.retro.qolrecommender;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple JSON config stored in config/qolrecommender.json
 */
public final class RecommenderConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "qolrecommender.json";

    public boolean enabled = true;
    public boolean showOnFirstLaunchOnly = true;
    public boolean dismissed = false;

    public String windowTitle = "Optional Client QoL Mods";
    public List<Entry> entries = new ArrayList<>();

    public static final class Entry {
        public String name = "";
        public String description = "";

        /** Optional project page URL. If blank, Open page uses downloadUrl. */
        public String pageUrl = "";

        /** Direct .jar download URL (e.g., Modrinth CDN direct link). */
        public String downloadUrl = "";

        /** Used to detect if the jar is already installed: any mods filename containing this string (case-insensitive). */
        public String filenameHint = "";

        /** Used to Install listed dependancies from .jar URL (e.g., Modrinth CDN direct link). */
        public java.util.List<Dep> dependencies = new java.util.ArrayList<>();

        public static final class Dep {
            public String url = "";
            public String hint = ""; // optional
        }
    }

    public static Path configPath() {
        return ModIO.getConfigDir().resolve(FILE_NAME);
    }

    public static RecommenderConfig loadOrCreateDefault() {
        Path p = configPath();
        if (!Files.exists(p)) {
            RecommenderConfig cfg = defaultConfig();
            save(cfg);
            return cfg;
        }
        try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
            RecommenderConfig cfg = GSON.fromJson(r, RecommenderConfig.class);
            if (cfg == null) cfg = defaultConfig();
            if (cfg.entries == null) cfg.entries = new ArrayList<>();
            for (Entry e : cfg.entries) {
                if (e.dependencies == null) e.dependencies = new ArrayList<>();
                for (Entry.Dep d : e.dependencies) {
                    if (d == null) continue;
                    if (d.url == null) d.url = "";
                    if (d.hint == null) d.hint = "";
                }
            }
            return cfg;
        } catch (Exception e) {
            RecommenderConfig cfg = defaultConfig();
            cfg.windowTitle = "Optional Client QoL Mods (config parse error)";
            return cfg;
        }
    }

    public static void save(RecommenderConfig cfg) {
        Path p = configPath();
        try {
            Files.createDirectories(p.getParent());
            try (Writer w = Files.newBufferedWriter(p, StandardCharsets.UTF_8)) {
                GSON.toJson(cfg, w);
            }
        } catch (IOException ignored) {
        }
    }

    private static RecommenderConfig defaultConfig() {
        // Default entries are intentionally blank so you can paste direct Modrinth URLs.
        return new RecommenderConfig();
    }
}
