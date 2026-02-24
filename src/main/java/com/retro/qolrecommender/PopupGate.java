package com.retro.qolrecommender;

import net.minecraft.client.Minecraft;

import javax.swing.*;
import java.awt.*;

public final class PopupGate {
    private PopupGate() {}

    public static boolean isMinecraftReady() {
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.getWindow() != null;
    }

    public static void showPopup(RecommenderConfig cfg) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            RecommenderWindow window = new RecommenderWindow(cfg);
            window.setVisible(true);
        });
    }

    public static void openModsFolder() {
        try {
            Desktop.getDesktop().open(ModIO.getModsDir().toFile());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Couldn't open mods folder:\n" + e.getMessage(),
                    "QoL Recommender",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void openUrl(String url) {
        try {
            Desktop.getDesktop().browse(java.net.URI.create(url));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Couldn't open link:\n" + e.getMessage(),
                    "QoL Recommender",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
