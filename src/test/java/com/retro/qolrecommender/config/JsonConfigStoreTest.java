package com.retro.qolrecommender.config;

import com.retro.qolrecommender.RecommenderException;
import com.retro.qolrecommender.config.model.DependencyData;
import com.retro.qolrecommender.config.model.RecommendedModData;
import com.retro.qolrecommender.config.model.RecommenderConfigData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class JsonConfigStoreTest
{
    @TempDir
    Path _tempDir;

    @Test
    void createsDefaultConfigFileWhenMissing() throws RecommenderException
    {
        IConfigStore store = ConfigStoreFactory.createInDirectory(_tempDir);
        RecommenderConfigData loaded = store.loadOrCreateDefault();

        assertTrue(Files.isRegularFile(store.configPath()));
        assertTrue(loaded.enabled());
        assertTrue(loaded.showOnFirstLaunchOnly());
        assertFalse(loaded.dismissed());
        assertEquals(RecommenderConfigData.DefaultWindowTitle, loaded.windowTitle());
        assertTrue(loaded.entries().isEmpty());
    }

    @Test
    void roundTripsEntriesAndDependenciesWithExpectedJsonKeys() throws IOException, RecommenderException
    {
        IConfigStore store = ConfigStoreFactory.createInDirectory(_tempDir);
        RecommendedModData entry = new RecommendedModData(
                "Iris Shaders",
                "Shader support",
                "https://modrinth.com/mod/iris",
                "https://cdn.modrinth.com/data/a/versions/b/iris-1.8.jar",
                "iris",
                List.of(new DependencyData("https://cdn.modrinth.com/data/a/versions/b/sodium-0.6.jar", "sodium"))
        );
        RecommenderConfigData original = new RecommenderConfigData(
                true,
                true,
                true,
                "Pack QoL Mods",
                List.of(entry)
        );

        store.save(original);
        String rawJson = Files.readString(store.configPath(), StandardCharsets.UTF_8);
        assertTrue(rawJson.contains("\"showOnFirstLaunchOnly\""));
        assertTrue(rawJson.contains("\"filenameHint\""));
        assertTrue(rawJson.contains("\"dependencies\""));

        RecommenderConfigData reloaded = store.loadOrCreateDefault();
        assertEquals(original, reloaded);
        assertFalse(reloaded.shouldShowPopup());
        assertTrue(reloaded.updatedDismissed(false).shouldShowPopup());
    }

    @Test
    void throwsRecommenderExceptionOnCorruptJson() throws IOException
    {
        IConfigStore store = ConfigStoreFactory.createInDirectory(_tempDir);
        Files.writeString(store.configPath(), "{ not valid json", StandardCharsets.UTF_8);

        RecommenderException error = assertThrows(RecommenderException.class, store::loadOrCreateDefault);
        assertTrue(error.getMessage().contains("qolrecommender.json"));
    }

    @Test
    void throwsRecommenderExceptionWhenSaveTargetIsDirectory() throws IOException
    {
        Path blockedPath = _tempDir.resolve("blocked-config-dir");
        Files.createDirectories(blockedPath);
        IConfigStore store = ConfigStoreFactory.create(new ConfigStoreBlueprint(blockedPath));

        RecommenderException error = assertThrows(
                RecommenderException.class,
                () -> store.save(RecommenderConfigData.defaults())
        );
        assertTrue(error.getMessage().contains("blocked-config-dir"));
    }
}
