package com.retro.qolrecommender.download;

import com.retro.qolrecommender.download.model.DownloadPolicyData;

import java.nio.file.Path;
import java.util.Objects;

public final class ModDownloaderBlueprint
{
    private Path _modsDirectory;
    private DownloadPolicyData _policy;

    public ModDownloaderBlueprint(Path modsDirectory)
    {
        _modsDirectory = Objects.requireNonNull(modsDirectory, "modsDirectory must not be null");
        _policy = DownloadPolicyData.defaults();
    }

    public Path modsDirectory()
    {
        return _modsDirectory;
    }

    public DownloadPolicyData policy()
    {
        return _policy;
    }

    public ModDownloaderBlueprint modsDirectory(Path modsDirectory)
    {
        _modsDirectory = Objects.requireNonNull(modsDirectory, "modsDirectory must not be null");
        return this;
    }

    public ModDownloaderBlueprint policy(DownloadPolicyData policy)
    {
        _policy = Objects.requireNonNull(policy, "policy must not be null");
        return this;
    }
}
