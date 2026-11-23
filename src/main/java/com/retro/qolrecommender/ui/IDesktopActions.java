package com.retro.qolrecommender.ui;

import com.retro.qolrecommender.RecommenderException;

public interface IDesktopActions
{
    void openModsFolder() throws RecommenderException;

    void openUrl(String rawUrl) throws RecommenderException;
}
