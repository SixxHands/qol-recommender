package com.retro.qolrecommender.config;

import java.nio.file.Path;
import java.util.Objects;

public final class ConfigStoreBlueprint
{
    public static final String DefaultFileName = "qolrecommender.json";

    private Path _configPath;

    public ConfigStoreBlueprint(Path configPath)
    {
        _configPath = Objects.requireNonNull(configPath, "configPath must not be null");
    }

    public Path configPath()
    {
        return _configPath;
    }

    public ConfigStoreBlueprint configPath(Path configPath)
    {
        _configPath = Objects.requireNonNull(configPath, "configPath must not be null");
        return this;
    }
}
