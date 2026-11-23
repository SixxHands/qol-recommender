package com.retro.qolrecommender.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import com.retro.qolrecommender.RecommenderException;
import com.retro.qolrecommender.config.model.DependencyData;
import com.retro.qolrecommender.config.model.RecommendedModData;
import com.retro.qolrecommender.config.model.RecommenderConfigData;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class JsonConfigStore implements IConfigStore
{
    private static final Gson Serializer = new GsonBuilder().setPrettyPrinting().create();

    private final Path _configPath;

    public JsonConfigStore(Path configPath)
    {
        _configPath = Objects.requireNonNull(configPath, "configPath must not be null");
    }

    @Override
    public Path configPath()
    {
        return _configPath;
    }

    @Override
    public RecommenderConfigData loadOrCreateDefault() throws RecommenderException
    {
        if (!Files.exists(_configPath))
        {
            RecommenderConfigData defaults = RecommenderConfigData.defaults();
            save(defaults);
            return defaults;
        }

        try (Reader reader = Files.newBufferedReader(_configPath, StandardCharsets.UTF_8))
        {
            StoredConfigData stored = Serializer.fromJson(reader, StoredConfigData.class);
            if (stored == null)
            {
                return RecommenderConfigData.defaults();
            }
            return readConfigData(stored);
        }
        catch (IOException | JsonParseException ex)
        {
            throw new RecommenderException("Failed to read config file: " + _configPath, ex);
        }
    }

    @Override
    public void save(RecommenderConfigData config) throws RecommenderException
    {
        Objects.requireNonNull(config, "config must not be null");
        try
        {
            Path parent = _configPath.getParent();
            if (parent != null)
            {
                Files.createDirectories(parent);
            }
            try (Writer writer = Files.newBufferedWriter(_configPath, StandardCharsets.UTF_8))
            {
                Serializer.toJson(buildStoredConfig(config), writer);
            }
        }
        catch (IOException ex)
        {
            throw new RecommenderException("Failed to write config file: " + _configPath, ex);
        }
    }

    private static RecommenderConfigData readConfigData(StoredConfigData stored)
    {
        List<RecommendedModData> entries = new ArrayList<>();
        if (stored._entries != null)
        {
            for (StoredEntryData item : stored._entries)
            {
                if (item != null)
                {
                    entries.add(readEntryData(item));
                }
            }
        }
        return new RecommenderConfigData(
                stored._enabled,
                stored._showOnFirstLaunchOnly,
                stored._dismissed,
                stored._windowTitle,
                entries
        );
    }

    private static RecommendedModData readEntryData(StoredEntryData item)
    {
        List<DependencyData> dependencies = new ArrayList<>();
        if (item._dependencies != null)
        {
            for (StoredDependencyData dependency : item._dependencies)
            {
                if (dependency != null)
                {
                    dependencies.add(new DependencyData(dependency._url, dependency._hint));
                }
            }
        }
        return new RecommendedModData(
                item._name,
                item._description,
                item._pageUrl,
                item._downloadUrl,
                item._filenameHint,
                dependencies
        );
    }

    private static StoredConfigData buildStoredConfig(RecommenderConfigData config)
    {
        StoredConfigData stored = new StoredConfigData();
        stored._enabled = config.enabled();
        stored._showOnFirstLaunchOnly = config.showOnFirstLaunchOnly();
        stored._dismissed = config.dismissed();
        stored._windowTitle = config.windowTitle();
        stored._entries = new ArrayList<>();

        for (RecommendedModData entry : config.entries())
        {
            stored._entries.add(buildStoredEntry(entry));
        }
        return stored;
    }

    private static StoredEntryData buildStoredEntry(RecommendedModData entry)
    {
        StoredEntryData stored = new StoredEntryData();
        stored._name = entry.name();
        stored._description = entry.description();
        stored._pageUrl = entry.pageUrl();
        stored._downloadUrl = entry.downloadUrl();
        stored._filenameHint = entry.filenameHint();
        stored._dependencies = new ArrayList<>();

        for (DependencyData dependency : entry.dependencies())
        {
            StoredDependencyData storedDependency = new StoredDependencyData();
            storedDependency._url = dependency.url();
            storedDependency._hint = dependency.hint();
            stored._dependencies.add(storedDependency);
        }
        return stored;
    }

    // Keep the original JSON field names so existing config/qolrecommender.json files keep working.
    private static final class StoredConfigData
    {
        @SerializedName("enabled")
        private boolean _enabled = true;

        @SerializedName("showOnFirstLaunchOnly")
        private boolean _showOnFirstLaunchOnly = true;

        @SerializedName("dismissed")
        private boolean _dismissed = false;

        @SerializedName("windowTitle")
        private String _windowTitle = RecommenderConfigData.DefaultWindowTitle;

        @SerializedName("entries")
        private List<StoredEntryData> _entries = new ArrayList<>();
    }

    private static final class StoredEntryData
    {
        @SerializedName("name")
        private String _name = "";

        @SerializedName("description")
        private String _description = "";

        @SerializedName("pageUrl")
        private String _pageUrl = "";

        @SerializedName("downloadUrl")
        private String _downloadUrl = "";

        @SerializedName("filenameHint")
        private String _filenameHint = "";

        @SerializedName("dependencies")
        private List<StoredDependencyData> _dependencies = new ArrayList<>();
    }

    private static final class StoredDependencyData
    {
        @SerializedName("url")
        private String _url = "";

        @SerializedName("hint")
        private String _hint = "";
    }
}
