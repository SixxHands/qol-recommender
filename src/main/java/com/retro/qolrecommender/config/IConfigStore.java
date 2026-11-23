package com.retro.qolrecommender.config;

import com.retro.qolrecommender.RecommenderException;
import com.retro.qolrecommender.config.model.RecommenderConfigData;

import java.nio.file.Path;

public interface IConfigStore
{
    Path configPath();

    RecommenderConfigData loadOrCreateDefault() throws RecommenderException;

    void save(RecommenderConfigData config) throws RecommenderException;
}
