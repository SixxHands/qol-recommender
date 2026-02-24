package com.retro.qolrecommender;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Client-only mod entrypoint.
 */
@Mod(value = "qolrecommender", dist = Dist.CLIENT)
public final class QoLRecommenderMod {
    private static boolean shownThisSession = false;

    public QoLRecommenderMod() {
        NeoForge.EVENT_BUS.addListener(QoLRecommenderMod::onClientTick);
    }

    private static void onClientTick(final ClientTickEvent.Post event) {
        if (shownThisSession) return;
        if (!PopupGate.isMinecraftReady()) return;

        shownThisSession = true;

        RecommenderConfig cfg = RecommenderConfig.loadOrCreateDefault();
        if (!cfg.enabled) return;
        if (cfg.showOnFirstLaunchOnly && cfg.dismissed) return;

        PopupGate.showPopup(cfg);
    }
}
