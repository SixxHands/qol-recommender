package com.retro.qolrecommender.ui.window;

import com.retro.qolrecommender.ui.RecommenderWindowBlueprint;

import java.util.Objects;

public final class RecommenderWindowFactory
{
    private RecommenderWindowFactory()
    {
    }

    public static RecommenderWindow create(RecommenderWindowBlueprint blueprint)
    {
        Objects.requireNonNull(blueprint, "blueprint must not be null");
        return new RecommenderWindow(
                blueprint.config(),
                blueprint.configStore(),
                blueprint.modDownloader(),
                blueprint.desktopActions(),
                blueprint.iconResourcePath()
        );
    }
}
