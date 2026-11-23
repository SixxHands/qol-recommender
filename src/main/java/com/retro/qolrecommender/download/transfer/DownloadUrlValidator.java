package com.retro.qolrecommender.download.transfer;

import com.retro.qolrecommender.RecommenderException;
import com.retro.qolrecommender.download.model.DownloadPolicyData;
import com.retro.qolrecommender.download.model.DownloadTargetData;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class DownloadUrlValidator
{
    private static final String JarExtension = ".jar";

    private final DownloadPolicyData _policy;

    public DownloadUrlValidator(DownloadPolicyData policy)
    {
        _policy = Objects.requireNonNull(policy, "policy must not be null");
    }

    public DownloadTargetData validateTarget(String rawUrl, String filenameHint) throws RecommenderException
    {
        String trimmedUrl = rawUrl == null ? "" : rawUrl.trim();
        if (trimmedUrl.isEmpty())
        {
            throw new RecommenderException("No downloadUrl set for this entry.");
        }

        URI uri = readUri(trimmedUrl);
        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("https") && !scheme.equalsIgnoreCase("http")))
        {
            throw new RecommenderException("Unsupported URL scheme: " + trimmedUrl);
        }

        String host = uri.getHost();
        if (host == null || !_policy.allowedHosts().contains(host.toLowerCase(Locale.ROOT)))
        {
            throw new RecommenderException("Blocked URL host. Only allowed hosts may be downloaded: " + trimmedUrl);
        }

        Optional<String> extractedName = extractFileName(trimmedUrl);
        if (extractedName.isEmpty() || !extractedName.get().toLowerCase(Locale.ROOT).endsWith(JarExtension))
        {
            throw new RecommenderException("URL does not look like a .jar: " + trimmedUrl);
        }

        return new DownloadTargetData(uri, extractedName.get(), filenameHint);
    }

    public Optional<String> extractFileName(String rawUrl)
    {
        // Strip query/fragment parts and reject path traversal before using the URL tail as a filename in mods/.
        if (rawUrl == null || rawUrl.isBlank())
        {
            return Optional.empty();
        }

        String cleaned = rawUrl.trim();
        int queryStart = cleaned.indexOf('?');
        if (queryStart >= 0)
        {
            cleaned = cleaned.substring(0, queryStart);
        }

        int fragmentStart = cleaned.indexOf('#');
        if (fragmentStart >= 0)
        {
            cleaned = cleaned.substring(0, fragmentStart);
        }

        int lastSlash = cleaned.lastIndexOf('/');
        if (lastSlash < 0 || lastSlash + 1 >= cleaned.length())
        {
            return Optional.empty();
        }

        String fileName = cleaned.substring(lastSlash + 1);
        if (fileName.isBlank() || fileName.contains(" ") || fileName.contains(".."))
        {
            return Optional.empty();
        }
        return Optional.of(fileName);
    }

    private static URI readUri(String rawUrl) throws RecommenderException
    {
        try
        {
            return new URI(rawUrl);
        }
        catch (URISyntaxException ex)
        {
            throw new RecommenderException("Invalid download URL: " + rawUrl, ex);
        }
    }
}
