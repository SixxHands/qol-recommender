package com.retro.qolrecommender.download.transfer;

import com.retro.qolrecommender.RecommenderException;
import com.retro.qolrecommender.config.model.DependencyData;
import com.retro.qolrecommender.config.model.RecommendedModData;
import com.retro.qolrecommender.download.IModDownloader;
import com.retro.qolrecommender.download.IProgressListener;
import com.retro.qolrecommender.download.model.DownloadOutcomeData;
import com.retro.qolrecommender.download.model.DownloadPolicyData;
import com.retro.qolrecommender.download.model.DownloadTargetData;
import com.retro.qolrecommender.download.model.InstallStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class ModrinthJarDownloader implements IModDownloader
{
    private static final Logger Log = LoggerFactory.getLogger(ModrinthJarDownloader.class);
    private static final String PartSuffix = ".part";
    private static final int HttpOkMin = 200;
    private static final int HttpOkExclusiveMax = 300;

    private final Path _modsDirectory;
    private final DownloadPolicyData _policy;
    private final DownloadUrlValidator _validator;
    private final InstalledModScanner _scanner;
    private final HttpClient _httpClient;

    public ModrinthJarDownloader(
            Path modsDirectory,
            DownloadPolicyData policy,
            DownloadUrlValidator validator,
            InstalledModScanner scanner,
            HttpClient httpClient
    )
    {
        _modsDirectory = Objects.requireNonNull(modsDirectory, "modsDirectory must not be null");
        _policy = Objects.requireNonNull(policy, "policy must not be null");
        _validator = Objects.requireNonNull(validator, "validator must not be null");
        _scanner = Objects.requireNonNull(scanner, "scanner must not be null");
        _httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
    }

    @Override
    public InstallStatus checkStatus(RecommendedModData entry) throws RecommenderException
    {
        Objects.requireNonNull(entry, "entry must not be null");
        if (_scanner.isEntryInstalled(entry))
        {
            return InstallStatus.INSTALLED;
        }
        if (_scanner.hasMissingDependencies(entry))
        {
            return InstallStatus.MISSING_DEPENDENCIES;
        }
        return InstallStatus.NOT_INSTALLED;
    }

    @Override
    public List<DownloadTargetData> buildDownloadQueue(RecommendedModData entry) throws RecommenderException
    {
        Objects.requireNonNull(entry, "entry must not be null");
        List<DownloadTargetData> queue = new ArrayList<>();

        // Queue dependencies before the main jar so required libraries land first.
        for (DependencyData dependency : entry.dependencies())
        {
            if (dependency.url().isEmpty())
            {
                continue;
            }
            queue.add(_validator.validateTarget(dependency.url(), dependency.hint()));
        }

        if (!entry.downloadUrl().isEmpty())
        {
            queue.add(_validator.validateTarget(entry.downloadUrl(), entry.filenameHint()));
        }

        if (queue.isEmpty())
        {
            throw new RecommenderException("No downloadUrl (or dependencies) configured.");
        }
        return List.copyOf(queue);
    }

    @Override
    public void downloadEntry(RecommendedModData entry, IProgressListener progressListener) throws RecommenderException
    {
        List<DownloadTargetData> queue = buildDownloadQueue(entry);
        for (int index = 0; index < queue.size(); index++)
        {
            DownloadTargetData target = queue.get(index);
            if (_scanner.isJarInstalled(target.uri().toString(), target.filenameHint()))
            {
                notifyProgress(progressListener, "Already installed: " + target.fileName());
                continue;
            }

            notifyProgress(
                    progressListener,
                    "Downloading: " + target.fileName() + " (" + (index + 1) + "/" + queue.size() + ")"
            );
            downloadSingleJar(target);
        }
    }

    @Override
    public CompletableFuture<DownloadOutcomeData> downloadEntryAsync(
            RecommendedModData entry,
            IProgressListener progressListener
    )
    {
        return CompletableFuture.supplyAsync(() -> runDownloadBoundary(entry, progressListener));
    }

    private DownloadOutcomeData runDownloadBoundary(RecommendedModData entry, IProgressListener progressListener)
    {
        try
        {
            downloadEntry(entry, progressListener);
            return DownloadOutcomeData.success("Downloaded all (restart Minecraft).");
        }
        catch (RecommenderException ex)
        {
            return DownloadOutcomeData.error(ex.getMessage());
        }
    }

    private void downloadSingleJar(DownloadTargetData target) throws RecommenderException
    {
        Path partFile = _modsDirectory.resolve(target.fileName() + PartSuffix);
        Path finalFile = _modsDirectory.resolve(target.fileName());

        try
        {
            Files.createDirectories(_modsDirectory);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(target.uri())
                    .timeout(_policy.requestTimeout())
                    .GET()
                    .build();

            HttpResponse<InputStream> response = _httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            writeAndPromoteJar(target, response, partFile, finalFile);
        }
        catch (InterruptedException ex)
        {
            Thread.currentThread().interrupt();
            deleteIncompletePart(partFile);
            throw new RecommenderException("Download interrupted: " + target.fileName(), ex);
        }
        catch (IOException ex)
        {
            deleteIncompletePart(partFile);
            throw new RecommenderException("Failed downloading " + target.fileName() + ": " + ex.getMessage(), ex);
        }
    }

    private void writeAndPromoteJar(
            DownloadTargetData target,
            HttpResponse<InputStream> response,
            Path partFile,
            Path finalFile
    ) throws IOException, RecommenderException
    {
        // Write to .part first and reject tiny files so HTML error pages or cut-off transfers never become jars in mods/.
        int statusCode = response.statusCode();
        try (InputStream body = response.body())
        {
            if (statusCode < HttpOkMin || statusCode >= HttpOkExclusiveMax)
            {
                throw new RecommenderException("HTTP " + statusCode + " downloading " + target.fileName());
            }
            Files.deleteIfExists(partFile);
            Files.copy(body, partFile, StandardCopyOption.REPLACE_EXISTING);
        }

        long downloadedBytes = Files.size(partFile);
        if (downloadedBytes < _policy.minimumJarBytes())
        {
            Files.deleteIfExists(partFile);
            throw new RecommenderException(
                    "Downloaded file is suspiciously small (" + downloadedBytes + " bytes): " + target.fileName()
            );
        }

        Files.move(partFile, finalFile, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void deleteIncompletePart(Path partFile)
    {
        if (Files.exists(partFile) && !partFile.toFile().delete())
        {
            Log.warn("Could not delete partial download file: {}", partFile);
        }
    }

    private static void notifyProgress(IProgressListener listener, String message)
    {
        if (listener != null)
        {
            listener.reportProgress(message);
        }
    }
}
