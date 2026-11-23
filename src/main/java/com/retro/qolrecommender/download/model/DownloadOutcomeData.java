package com.retro.qolrecommender.download.model;

public record DownloadOutcomeData(boolean succeeded, String summary)
{
    public DownloadOutcomeData
    {
        summary = summary == null ? "" : summary;
    }

    public static DownloadOutcomeData success(String summary)
    {
        return new DownloadOutcomeData(true, summary);
    }

    public static DownloadOutcomeData error(String summary)
    {
        return new DownloadOutcomeData(false, summary);
    }
}
