package com.retro.qolrecommender.ui.window;

import com.retro.qolrecommender.RecommenderException;
import com.retro.qolrecommender.config.IConfigStore;
import com.retro.qolrecommender.config.model.RecommendedModData;
import com.retro.qolrecommender.config.model.RecommenderConfigData;
import com.retro.qolrecommender.download.IModDownloader;
import com.retro.qolrecommender.ui.IDesktopActions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class RecommenderWindow extends JFrame
{
    private static final long serialVersionUID = 1L;
    private static final Logger Log = LoggerFactory.getLogger(RecommenderWindow.class);
    private static final String DialogTitle = "QoL Recommender";
    private static final int WindowWidth = 740;
    private static final int WindowHeight = 520;
    private static final int ScrollUnitIncrement = 16;

    private final transient RecommenderConfigData _config;
    private final transient IConfigStore _configStore;
    private final transient IModDownloader _modDownloader;
    private final transient IDesktopActions _desktopActions;
    private final JCheckBox _dontShowAgainCheckBox;

    public RecommenderWindow(
            RecommenderConfigData config,
            IConfigStore configStore,
            IModDownloader modDownloader,
            IDesktopActions desktopActions,
            Optional<String> iconResourcePath
    )
    {
        super(Objects.requireNonNull(config, "config must not be null").effectiveWindowTitle());
        _config = config;
        _configStore = Objects.requireNonNull(configStore, "configStore must not be null");
        _modDownloader = Objects.requireNonNull(modDownloader, "modDownloader must not be null");
        _desktopActions = Objects.requireNonNull(desktopActions, "desktopActions must not be null");

        applyWindowIcon(Objects.requireNonNull(iconResourcePath, "iconResourcePath must not be null"));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(WindowWidth, WindowHeight);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(12, 12, 12, 12));
        setContentPane(root);

        root.add(buildHeaderLabel(), BorderLayout.NORTH);
        root.add(buildEntriesScrollPane(), BorderLayout.CENTER);

        _dontShowAgainCheckBox = new JCheckBox("Don't show this again");
        _dontShowAgainCheckBox.setSelected(_config.dismissed());
        root.add(buildBottomBar(), BorderLayout.SOUTH);
    }

    private void applyWindowIcon(Optional<String> iconResourcePath)
    {
        if (iconResourcePath.isEmpty())
        {
            return;
        }
        URL iconUrl = RecommenderWindow.class.getResource(iconResourcePath.get());
        if (iconUrl != null)
        {
            Image icon = Toolkit.getDefaultToolkit().getImage(iconUrl);
            setIconImage(icon);
        }
    }

    private static JLabel buildHeaderLabel()
    {
        return new JLabel("<html><h2 style='margin:0'>Optional Client / QoL Mods</h2>"
                + "<div style='margin-top:4px'>Download any you want. You'll need to restart Minecraft after installing.</div></html>");
    }

    private JScrollPane buildEntriesScrollPane()
    {
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        List<RecommendedModData> entries = _config.entries();
        if (entries.isEmpty())
        {
            JLabel emptyLabel = new JLabel(
                    "<html><i>No entries in config.</i><br/>Edit: config/qolrecommender.json</html>"
            );
            emptyLabel.setBorder(new EmptyBorder(16, 4, 16, 4));
            listPanel.add(emptyLabel);
        }
        else
        {
            for (RecommendedModData entry : entries)
            {
                listPanel.add(new EntryRowPanel(entry, _modDownloader, _desktopActions));
                listPanel.add(Box.createVerticalStrut(10));
            }
        }

        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(ScrollUnitIncrement);
        return scrollPane;
    }

    private JPanel buildBottomBar()
    {
        JPanel bottom = new JPanel(new BorderLayout(8, 8));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton openModsButton = new JButton("Open mods folder");
        openModsButton.addActionListener(event -> openModsFolder());
        left.add(openModsButton);
        left.add(_dontShowAgainCheckBox);
        bottom.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(event -> closeWindow());
        right.add(closeButton);
        bottom.add(right, BorderLayout.EAST);

        return bottom;
    }

    private void openModsFolder()
    {
        try
        {
            _desktopActions.openModsFolder();
        }
        catch (RecommenderException ex)
        {
            Log.warn("Failed opening mods folder", ex);
            JOptionPane.showMessageDialog(
                    this,
                    "Couldn't open mods folder:\n" + ex.getMessage(),
                    DialogTitle,
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void closeWindow()
    {
        RecommenderConfigData updated = _config.updatedDismissed(_dontShowAgainCheckBox.isSelected());
        try
        {
            _configStore.save(updated);
        }
        catch (RecommenderException ex)
        {
            Log.error("Failed saving config on window close", ex);
            JOptionPane.showMessageDialog(
                    this,
                    "Couldn't save config:\n" + ex.getMessage(),
                    DialogTitle,
                    JOptionPane.ERROR_MESSAGE
            );
        }
        dispose();
    }
}
