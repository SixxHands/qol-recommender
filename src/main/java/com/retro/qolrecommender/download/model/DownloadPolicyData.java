package com.retro.qolrecommender.download.model;

import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record DownloadPolicyData(
        Set<String> allowedHosts,
        long minimumJarBytes,
        Duration connectTimeout,
        Duration requestTimeout
)
{
    public static final String DefaultModrinthHost = "cdn.modrinth.com";
    public static final long DefaultMinimumJarBytes = 50_000L;
    public static final Duration DefaultConnectTimeout = Duration.ofSeconds(20);
    public static final Duration DefaultRequestTimeout = Duration.ofMinutes(2);

    public DownloadPolicyData
    {
        Objects.requireNonNull(allowedHosts, "allowedHosts must not be null");
        Objects.requireNonNull(connectTimeout, "connectTimeout must not be null");
        Objects.requireNonNull(requestTimeout, "requestTimeout must not be null");
        if (minimumJarBytes < 0)
        {
            throw new IllegalArgumentException("minimumJarBytes must not be negative");
        }
        allowedHosts = allowedHosts.stream()
                .filter(Objects::nonNull)
                .map(host -> host.trim().toLowerCase(Locale.ROOT))
                .filter(host -> !host.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public static DownloadPolicyData defaults()
    {
        return new DownloadPolicyData(
                Set.of(DefaultModrinthHost),
                DefaultMinimumJarBytes,
                DefaultConnectTimeout,
                DefaultRequestTimeout
        );
    }
}
