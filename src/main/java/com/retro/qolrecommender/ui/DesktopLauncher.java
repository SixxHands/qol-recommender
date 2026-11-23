package com.retro.qolrecommender.ui;

import com.retro.qolrecommender.RecommenderException;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class DesktopLauncher implements IDesktopActions
{
    private final Path _modsDirectory;

    public DesktopLauncher(Path modsDirectory)
    {
        _modsDirectory = Objects.requireNonNull(modsDirectory, "modsDirectory must not be null");
    }

    @Override
    public void openModsFolder() throws RecommenderException
    {
        Desktop desktop = requireDesktop(Desktop.Action.OPEN, "opening folders");
        try
        {
            Files.createDirectories(_modsDirectory);
            desktop.open(_modsDirectory.toFile());
        }
        catch (IOException ex)
        {
            throw new RecommenderException("Could not open mods folder: " + _modsDirectory, ex);
        }
    }

    @Override
    public void openUrl(String rawUrl) throws RecommenderException
    {
        String trimmed = rawUrl == null ? "" : rawUrl.trim();
        if (trimmed.isEmpty())
        {
            throw new RecommenderException("No link is configured for this entry.");
        }

        Desktop desktop = requireDesktop(Desktop.Action.BROWSE, "opening links");
        try
        {
            desktop.browse(new URI(trimmed));
        }
        catch (URISyntaxException | IOException ex)
        {
            throw new RecommenderException("Could not open link: " + trimmed, ex);
        }
    }

    private static Desktop requireDesktop(Desktop.Action action, String capabilityName) throws RecommenderException
    {
        if (!Desktop.isDesktopSupported())
        {
            throw new RecommenderException("Desktop actions are not supported in this environment.");
        }

        Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(action))
        {
            throw new RecommenderException("This system does not support " + capabilityName + ".");
        }
        return desktop;
    }
}
