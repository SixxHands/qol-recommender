package com.retro.qolrecommender.ui;

import com.retro.qolrecommender.ui.window.RecommenderWindow;
import com.retro.qolrecommender.ui.window.RecommenderWindowFactory;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import java.util.Objects;

public final class PopupLauncher
{
    private static final Logger Log = LoggerFactory.getLogger(PopupLauncher.class);

    public boolean isMinecraftReady()
    {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null && minecraft.getWindow() != null;
    }

    public void showPopup(RecommenderWindowBlueprint blueprint)
    {
        Objects.requireNonNull(blueprint, "blueprint must not be null");
        SwingUtilities.invokeLater(() -> openOnSwingThread(blueprint));
    }

    private void openOnSwingThread(RecommenderWindowBlueprint blueprint)
    {
        applySystemLookAndFeel();
        RecommenderWindow window = RecommenderWindowFactory.create(blueprint);
        window.setVisible(true);
    }

    private static void applySystemLookAndFeel()
    {
        try
        {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (ClassNotFoundException
               | InstantiationException
               | IllegalAccessException
               | UnsupportedLookAndFeelException ex)
        {
            Log.warn("Could not apply system LookAndFeel; falling back to Swing default.", ex);
        }
    }
}
