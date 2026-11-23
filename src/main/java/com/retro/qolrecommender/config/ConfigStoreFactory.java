package com.retro.qolrecommender.config;

import java.nio.file.Path;
import java.util.Objects;

public final class ConfigStoreFactory
{
    private ConfigStoreFactory()
    {
    }

    public static IConfigStore create(ConfigStoreBlueprint blueprint)
    {
        Objects.requireNonNull(blueprint, "blueprint must not be null");
        return new JsonConfigStore(blueprint.configPath());
    }

    public static IConfigStore createInDirectory(Path configDirectory)
    {
        Objects.requireNonNull(configDirectory, "configDirectory must not be null");
        Path configFile = configDirectory.resolve(ConfigStoreBlueprint.DefaultFileName);
        return create(new ConfigStoreBlueprint(configFile));
    }
}
