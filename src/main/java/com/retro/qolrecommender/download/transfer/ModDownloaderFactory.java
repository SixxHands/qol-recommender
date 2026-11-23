package com.retro.qolrecommender.download.transfer;

import com.retro.qolrecommender.download.IModDownloader;
import com.retro.qolrecommender.download.ModDownloaderBlueprint;
import com.retro.qolrecommender.download.model.DownloadPolicyData;

import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.Objects;

public final class ModDownloaderFactory
{
    private ModDownloaderFactory()
    {
    }

    public static IModDownloader create(ModDownloaderBlueprint blueprint)
    {
        Objects.requireNonNull(blueprint, "blueprint must not be null");
        DownloadPolicyData policy = blueprint.policy();
        DownloadUrlValidator validator = new DownloadUrlValidator(policy);
        InstalledModScanner scanner = new InstalledModScanner(blueprint.modsDirectory(), validator);
        HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(policy.connectTimeout())
                .build();

        return new ModrinthJarDownloader(blueprint.modsDirectory(), policy, validator, scanner, httpClient);
    }

    public static IModDownloader createForDirectory(Path modsDirectory)
    {
        return create(new ModDownloaderBlueprint(modsDirectory));
    }
}
