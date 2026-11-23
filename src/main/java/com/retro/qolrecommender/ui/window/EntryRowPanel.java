package com.retro.qolrecommender.ui.window;

import com.retro.qolrecommender.RecommenderException;
import com.retro.qolrecommender.config.model.RecommendedModData;
import com.retro.qolrecommender.download.IModDownloader;
import com.retro.qolrecommender.download.model.DownloadOutcomeData;
import com.retro.qolrecommender.download.model.InstallStatus;
import com.retro.qolrecommender.ui.IDesktopActions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.util.Objects;

public final class EntryRowPanel extends JPanel
{
    private static final long serialVersionUID = 1L;
    private static final Logger Log = LoggerFactory.getLogger(EntryRowPanel.class);
    private static final String DialogTitle = "QoL Recommender";
    private static final Color BorderColor = new Color(220, 220, 220);

    private final transient RecommendedModData _entry;
    private final transient IModDownloader _modDownloader;
    private final transient IDesktopActions _desktopActions;
    private final JLabel _statusLabel;
    private final JButton _downloadButton;

    public EntryRowPanel(
            RecommendedModData entry,
            IModDownloader modDownloader,
            IDesktopActions desktopActions
    )
    {
        super(new BorderLayout(10, 6));
        _entry = Objects.requireNonNull(entry, "entry must not be null");
        _modDownloader = Objects.requireNonNull(modDownloader, "modDownloader must not be null");
        _desktopActions = Objects.requireNonNull(desktopActions, "desktopActions must not be null");

        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BorderColor),
                new EmptyBorder(10, 10, 10, 10)
        ));

        add(new JLabel(buildTitleHtml(_entry)), BorderLayout.CENTER);

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));

        _statusLabel = new JLabel();
        _statusLabel.setBorder(new EmptyBorder(0, 0, 0, 6));
        actionsPanel.add(_statusLabel);

        JButton openPageButton = new JButton("Open page");
        openPageButton.addActionListener(event -> openEntryPage());
        actionsPanel.add(openPageButton);

        _downloadButton = new JButton("Download");
        _downloadButton.addActionListener(event -> startDownload());
        actionsPanel.add(_downloadButton);

        add(actionsPanel, BorderLayout.EAST);
        refreshStatus();
    }

    private void refreshStatus()
    {
        try
        {
            InstallStatus status = _modDownloader.checkStatus(_entry);
            applyStatusDisplay(status);
        }
        catch (RecommenderException ex)
        {
            Log.error("Failed checking install status for {}", _entry.name(), ex);
            _statusLabel.setText("Status error");
            _downloadButton.setEnabled(true);
            _downloadButton.setText("Download");
        }
    }

    private void applyStatusDisplay(InstallStatus status)
    {
        if (status == InstallStatus.INSTALLED)
        {
            _statusLabel.setText("Installed");
            _downloadButton.setEnabled(false);
            _downloadButton.setText("Download");
            return;
        }

        if (status == InstallStatus.MISSING_DEPENDENCIES)
        {
            _statusLabel.setText("Missing dependencies");
        }
        else
        {
            _statusLabel.setText("Not installed");
        }

        _downloadButton.setEnabled(true);
        _downloadButton.setText(_entry.hasDependencies() ? "Download (deps)" : "Download");
    }

    private void openEntryPage()
    {
        try
        {
            _desktopActions.openUrl(_entry.preferredPageUrl());
        }
        catch (RecommenderException ex)
        {
            Log.warn("Failed opening entry link for {}", _entry.name(), ex);
            JOptionPane.showMessageDialog(
                    this,
                    "Couldn't open link:\n" + ex.getMessage(),
                    DialogTitle,
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void startDownload()
    {
        _downloadButton.setEnabled(false);
        _statusLabel.setText("Downloading...");

        _modDownloader.downloadEntryAsync(
                _entry,
                message -> SwingUtilities.invokeLater(() -> _statusLabel.setText(message))
        ).thenAccept(outcome -> SwingUtilities.invokeLater(() -> applyDownloadOutcome(outcome)));
    }

    private void applyDownloadOutcome(DownloadOutcomeData outcome)
    {
        if (outcome.succeeded())
        {
            _statusLabel.setText("Downloaded (restart)");
            return;
        }

        _statusLabel.setText("Failed");
        _downloadButton.setEnabled(true);
        JOptionPane.showMessageDialog(
                this,
                "Download failed:\n" + outcome.summary(),
                DialogTitle,
                JOptionPane.ERROR_MESSAGE
        );
    }

    private static String buildTitleHtml(RecommendedModData entry)
    {
        String descriptionBlock = entry.description().isEmpty()
                ? ""
                : "<div style='margin-top:3px'>" + escapeHtml(entry.description()) + "</div>";
        return "<html><b style='font-size:12pt'>" + escapeHtml(entry.name()) + "</b>"
                + descriptionBlock + "</html>";
    }

    private static String escapeHtml(String text)
    {
        if (text == null)
        {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
