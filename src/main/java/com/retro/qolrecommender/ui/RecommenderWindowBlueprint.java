package com.retro.qolrecommender.ui;

import com.retro.qolrecommender.config.IConfigStore;
import com.retro.qolrecommender.config.model.RecommenderConfigData;
import com.retro.qolrecommender.download.IModDownloader;

import java.util.Objects;
import java.util.Optional;

public final class RecommenderWindowBlueprint
{
    public static final String DefaultIconResourcePath = "/assets/qolrecommender/icon.png";

    private RecommenderConfigData _config;
    private IConfigStore _configStore;
    private IModDownloader _modDownloader;
    private IDesktopActions _desktopActions;
    private Optional<String> _iconResourcePath;

    public RecommenderWindowBlueprint(
            RecommenderConfigData config,
            IConfigStore configStore,
            IModDownloader modDownloader,
            IDesktopActions desktopActions
    )
    {
        _config = Objects.requireNonNull(config, "config must not be null");
        _configStore = Objects.requireNonNull(configStore, "configStore must not be null");
        _modDownloader = Objects.requireNonNull(modDownloader, "modDownloader must not be null");
        _desktopActions = Objects.requireNonNull(desktopActions, "desktopActions must not be null");
        _iconResourcePath = Optional.of(DefaultIconResourcePath);
    }

    public RecommenderConfigData config()
    {
        return _config;
    }

    public IConfigStore configStore()
    {
        return _configStore;
    }

    public IModDownloader modDownloader()
    {
        return _modDownloader;
    }

    public IDesktopActions desktopActions()
    {
        return _desktopActions;
    }

    public Optional<String> iconResourcePath()
    {
        return _iconResourcePath;
    }

    public RecommenderWindowBlueprint iconResourcePath(Optional<String> iconResourcePath)
    {
        _iconResourcePath = Objects.requireNonNull(iconResourcePath, "iconResourcePath must not be null");
        return this;
    }
}
