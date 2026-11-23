package com.retro.qolrecommender;

import com.retro.qolrecommender.config.ConfigStoreFactory;
import com.retro.qolrecommender.config.IConfigStore;
import com.retro.qolrecommender.config.model.RecommenderConfigData;
import com.retro.qolrecommender.download.IModDownloader;
import com.retro.qolrecommender.download.transfer.ModDownloaderFactory;
import com.retro.qolrecommender.ui.DesktopLauncher;
import com.retro.qolrecommender.ui.IDesktopActions;
import com.retro.qolrecommender.ui.PopupLauncher;
import com.retro.qolrecommender.ui.RecommenderWindowBlueprint;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

@Mod(value = QoLRecommenderMod.ModId, dist = Dist.CLIENT)
public final class QoLRecommenderMod
{
    public static final String ModId = "qolrecommender";

    private static final Logger Log = LoggerFactory.getLogger(QoLRecommenderMod.class);

    private final IConfigStore _configStore;
    private final IModDownloader _modDownloader;
    private final IDesktopActions _desktopActions;
    private final PopupLauncher _popupLauncher;
    private boolean _shownThisSession;

    public QoLRecommenderMod()
    {
        Path configDirectory = FMLPaths.CONFIGDIR.get();
        Path modsDirectory = FMLPaths.MODSDIR.get();

        _configStore = ConfigStoreFactory.createInDirectory(configDirectory);
        _modDownloader = ModDownloaderFactory.createForDirectory(modsDirectory);
        _desktopActions = new DesktopLauncher(modsDirectory);
        _popupLauncher = new PopupLauncher();
        _shownThisSession = false;

        NeoForge.EVENT_BUS.addListener(this::onClientTick);
    }

    private void onClientTick(ClientTickEvent.Post event)
    {
        // Wait until Minecraft creates its window so the Swing popup does not open during early mod loading.
        if (_shownThisSession || !_popupLauncher.isMinecraftReady())
        {
            return;
        }

        _shownThisSession = true;
        RecommenderConfigData config = loadConfigAtTickBoundary();
        if (!config.shouldShowPopup())
        {
            return;
        }

        RecommenderWindowBlueprint blueprint = new RecommenderWindowBlueprint(
                config,
                _configStore,
                _modDownloader,
                _desktopActions
        );
        _popupLauncher.showPopup(blueprint);
    }

    private RecommenderConfigData loadConfigAtTickBoundary()
    {
        try
        {
            return _configStore.loadOrCreateDefault();
        }
        catch (RecommenderException ex)
        {
            Log.error("Failed loading QoL Recommender config; showing fallback window.", ex);
            return RecommenderConfigData.errorFallback();
        }
    }
}
