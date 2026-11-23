package com.retro.qolrecommender.config.model;

import java.util.List;
import java.util.Objects;

public record RecommenderConfigData(
        boolean enabled,
        boolean showOnFirstLaunchOnly,
        boolean dismissed,
        String windowTitle,
        List<RecommendedModData> entries
)
{
    public static final String DefaultWindowTitle = "Optional Client QoL Mods";
    public static final String FallbackWindowTitle = "Optional Client Mods";
    public static final String ErrorWindowTitle = "Optional Client QoL Mods (config parse error)";

    public RecommenderConfigData
    {
        windowTitle = windowTitle == null ? "" : windowTitle.trim();
        entries = entries == null
                ? List.of()
                : entries.stream().filter(Objects::nonNull).toList();
    }

    public static RecommenderConfigData defaults()
    {
        // Default entries are intentionally blank so pack authors can paste direct Modrinth URLs.
        return new RecommenderConfigData(true, true, false, DefaultWindowTitle, List.of());
    }

    public static RecommenderConfigData errorFallback()
    {
        return new RecommenderConfigData(true, true, false, ErrorWindowTitle, List.of());
    }

    public String effectiveWindowTitle()
    {
        return windowTitle.isEmpty() ? FallbackWindowTitle : windowTitle;
    }

    public boolean shouldShowPopup()
    {
        if (!enabled)
        {
            return false;
        }
        return !showOnFirstLaunchOnly || !dismissed;
    }

    public RecommenderConfigData updatedDismissed(boolean nextDismissed)
    {
        return new RecommenderConfigData(
                enabled,
                showOnFirstLaunchOnly,
                nextDismissed,
                windowTitle,
                entries
        );
    }
}
