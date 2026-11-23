package com.retro.qolrecommender.config.model;

import java.util.List;
import java.util.Objects;

public record RecommendedModData(
        String name,
        String description,
        String pageUrl,
        String downloadUrl,
        String filenameHint,
        List<DependencyData> dependencies
)
{
    public RecommendedModData
    {
        name = cleanText(name);
        description = cleanText(description);
        pageUrl = cleanText(pageUrl);
        downloadUrl = cleanText(downloadUrl);
        filenameHint = cleanText(filenameHint);
        dependencies = dependencies == null
                ? List.of()
                : dependencies.stream().filter(Objects::nonNull).toList();
    }

    public boolean hasDependencies()
    {
        return !dependencies.isEmpty();
    }

    public String preferredPageUrl()
    {
        // Fall back to downloadUrl when no separate project pageUrl is set.
        return pageUrl.isEmpty() ? downloadUrl : pageUrl;
    }

    private static String cleanText(String value)
    {
        return value == null ? "" : value.trim();
    }
}
