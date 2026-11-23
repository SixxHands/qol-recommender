package com.retro.qolrecommender.download.transfer;

import com.retro.qolrecommender.RecommenderException;
import com.retro.qolrecommender.config.model.DependencyData;
import com.retro.qolrecommender.config.model.RecommendedModData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public final class InstalledModScanner
{
    private static final String JarExtension = ".jar";

    private final Path _modsDirectory;
    private final DownloadUrlValidator _validator;

    public InstalledModScanner(Path modsDirectory, DownloadUrlValidator validator)
    {
        _modsDirectory = Objects.requireNonNull(modsDirectory, "modsDirectory must not be null");
        _validator = Objects.requireNonNull(validator, "validator must not be null");
    }

    public boolean isEntryInstalled(RecommendedModData entry) throws RecommenderException
    {
        Objects.requireNonNull(entry, "entry must not be null");
        return isJarInstalled(entry.downloadUrl(), entry.filenameHint());
    }

    public boolean hasMissingDependencies(RecommendedModData entry) throws RecommenderException
    {
        Objects.requireNonNull(entry, "entry must not be null");
        for (DependencyData dependency : entry.dependencies())
        {
            if (dependency.url().isEmpty())
            {
                continue;
            }
            if (!isJarInstalled(dependency.url(), dependency.hint()))
            {
                return true;
            }
        }
        return false;
    }

    public boolean isJarInstalled(String rawUrl, String filenameHint) throws RecommenderException
    {
        if (!Files.isDirectory(_modsDirectory))
        {
            return false;
        }

        // Match any jar containing filenameHint first (whatever case) then fall back to the exact filename from the URL.
        String trimmedHint = filenameHint == null ? "" : filenameHint.trim();
        if (!trimmedHint.isEmpty())
        {
            return matchesHintInModsFolder(trimmedHint.toLowerCase(Locale.ROOT));
        }

        Optional<String> fileName = _validator.extractFileName(rawUrl);
        if (fileName.isEmpty())
        {
            return false;
        }
        return Files.isRegularFile(_modsDirectory.resolve(fileName.get()));
    }

    private boolean matchesHintInModsFolder(String lowerHint) throws RecommenderException
    {
        try (Stream<Path> entries = Files.list(_modsDirectory))
        {
            return entries
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString().toLowerCase(Locale.ROOT))
                    .anyMatch(name -> name.endsWith(JarExtension) && name.contains(lowerHint));
        }
        catch (IOException ex)
        {
            throw new RecommenderException("Failed scanning mods folder: " + _modsDirectory, ex);
        }
    }
}
