package com.retro.qolrecommender.download;

import com.retro.qolrecommender.RecommenderException;
import com.retro.qolrecommender.config.model.RecommendedModData;
import com.retro.qolrecommender.download.model.DownloadOutcomeData;
import com.retro.qolrecommender.download.model.DownloadTargetData;
import com.retro.qolrecommender.download.model.InstallStatus;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface IModDownloader
{
    InstallStatus checkStatus(RecommendedModData entry) throws RecommenderException;

    List<DownloadTargetData> buildDownloadQueue(RecommendedModData entry) throws RecommenderException;

    void downloadEntry(RecommendedModData entry, IProgressListener progressListener) throws RecommenderException;

    CompletableFuture<DownloadOutcomeData> downloadEntryAsync(
            RecommendedModData entry,
            IProgressListener progressListener
    );
}
