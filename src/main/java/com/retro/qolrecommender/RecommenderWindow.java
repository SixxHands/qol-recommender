package com.retro.qolrecommender;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public final class RecommenderWindow extends JFrame {
    private final RecommenderConfig cfg;
    private final JCheckBox dontShowAgain;

    public RecommenderWindow(RecommenderConfig cfg) {
        super(cfg.windowTitle == null || cfg.windowTitle.isBlank() ? "Optional Client Mods" : cfg.windowTitle);
        try {
            Image icon = Toolkit.getDefaultToolkit().getImage(
                    RecommenderWindow.class.getResource("/assets/qolrecommender/icon.png")
            );
            setIconImage(icon);
        } catch (Exception ignored) {}
        this.cfg = cfg;

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(740, 520);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(12, 12, 12, 12));
        setContentPane(root);

        JLabel header = new JLabel("<html><h2 style='margin:0'>Optional Client / QoL Mods</h2>" +
                "<div style='margin-top:4px'>Download any you want. You'll need to restart Minecraft after installing.</div></html>");
        root.add(header, BorderLayout.NORTH);

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        List<RecommenderConfig.Entry> entries = cfg.entries;
        if (entries == null || entries.isEmpty()) {
            JLabel empty = new JLabel("<html><i>No entries in config.</i><br/>Edit: config/qolrecommender.json</html>");
            empty.setBorder(new EmptyBorder(16, 4, 16, 4));
            listPanel.add(empty);
        } else {
            for (RecommenderConfig.Entry entry : entries) {
                listPanel.add(new EntryRow(entry));
                listPanel.add(Box.createVerticalStrut(10));
            }
        }

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        root.add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(8, 8));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton openMods = new JButton("Open mods folder");
        openMods.addActionListener(e -> PopupGate.openModsFolder());
        left.add(openMods);

        dontShowAgain = new JCheckBox("Don't show this again");
        dontShowAgain.setSelected(cfg.dismissed);
        left.add(dontShowAgain);
        bottom.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton close = new JButton("Close");
        close.addActionListener(e -> onClose());
        right.add(close);
        bottom.add(right, BorderLayout.EAST);

        root.add(bottom, BorderLayout.SOUTH);
    }

    private void onClose() {
        boolean checked = dontShowAgain.isSelected();
        cfg.dismissed = checked;
        RecommenderConfig.save(cfg);
        dispose();
    }

    private static final class EntryRow extends JPanel {
        private final JLabel status;
        private final JButton downloadBtn;

        EntryRow(RecommenderConfig.Entry entry) {
            super(new BorderLayout(10, 6));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(220,220,220)),
                    new EmptyBorder(10, 10, 10, 10)
            ));

            String titleHtml = "<html><b style='font-size:12pt'>" + esc(entry.name) + "</b>"
                    + (entry.description == null || entry.description.isBlank() ? "" :
                    "<div style='margin-top:3px'>" + esc(entry.description) + "</div>")
                    + "</html>";

            JLabel title = new JLabel(titleHtml);
            add(title, BorderLayout.CENTER);

            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));

            status = new JLabel();
            status.setBorder(new EmptyBorder(0, 0, 0, 6));
            actions.add(status);

            JButton open = new JButton("Open page");
            open.addActionListener(e -> {
                if (entry.pageUrl != null && !entry.pageUrl.isBlank()) {
                    PopupGate.openUrl(entry.pageUrl);
                } else {
                    PopupGate.openUrl(entry.downloadUrl);
                }
            });
            actions.add(open);

            downloadBtn = new JButton("Download");
            downloadBtn.addActionListener(e -> startDownload(entry));
            actions.add(downloadBtn);

            add(actions, BorderLayout.EAST);

            refreshStatus(entry);
        }

        private void refreshStatus(RecommenderConfig.Entry entry) {
            boolean installed = ModIO.isEntryInstalled(entry);

            boolean hasDeps = entry.dependencies != null && !entry.dependencies.isEmpty();
            boolean missingDeps = hasDeps && ModIO.hasMissingDependencies(entry);

            if (installed) {
                status.setText("Installed");
                downloadBtn.setEnabled(false);
            } else if (missingDeps) {
                status.setText("Missing dependencies");
                downloadBtn.setEnabled(true);
            } else {
                status.setText("Not installed");
                downloadBtn.setEnabled(true);
            }

            // Optional: make the button label reflect deps
            if (!installed && hasDeps) {
                downloadBtn.setText("Download (deps)");
            } else {
                downloadBtn.setText("Download");
            }
        }

        private void startDownload(RecommenderConfig.Entry entry) {
            downloadBtn.setEnabled(false);
            status.setText("Downloading...");

            ModIO.downloadWithDependencies(entry,
                    msg -> SwingUtilities.invokeLater(() -> status.setText(msg)),
                    (ok, message) -> SwingUtilities.invokeLater(() -> {
                        if (ok) {
                            status.setText("Downloaded (restart)");
                        } else {
                            status.setText("Failed");
                            downloadBtn.setEnabled(true);
                            JOptionPane.showMessageDialog(this,
                                    "Download failed:\n" + message,
                                    "QoL Recommender",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    })
            );
        }

        private static String esc(String s) {
            if (s == null) return "";
            return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }
    }
}
