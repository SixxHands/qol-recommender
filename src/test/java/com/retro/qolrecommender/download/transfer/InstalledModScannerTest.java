package com.retro.qolrecommender.download.transfer;

import com.retro.qolrecommender.RecommenderException;
import com.retro.qolrecommender.config.model.DependencyData;
import com.retro.qolrecommender.config.model.RecommendedModData;
import com.retro.qolrecommender.download.model.DownloadPolicyData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class InstalledModScannerTest
{
    @TempDir
    Path _tempDir;

    @Test
    void returnsFalseWhenModsDirectoryDoesNotExist() throws RecommenderException
    {
        Path missingMods = _tempDir.resolve("missing-mods");
        InstalledModScanner scanner = createScanner(missingMods);

        assertFalse(scanner.isJarInstalled("https://cdn.modrinth.com/data/a/versions/b/sodium.jar", "sodium"));
    }

    @Test
    void matchesJarCaseInsensitivelyByHintAndIgnoresNonJars() throws IOException, RecommenderException
    {
        Files.writeString(_tempDir.resolve("Sodium-Notes.txt"), "not a jar");
        InstalledModScanner scanner = createScanner(_tempDir);
        assertFalse(scanner.isJarInstalled("https://cdn.modrinth.com/data/a/versions/b/sodium.jar", "SODIUM"));

        Files.writeString(_tempDir.resolve("Sodium-NeoForge-0.6.jar"), "jar content");
        assertTrue(scanner.isJarInstalled("https://cdn.modrinth.com/data/a/versions/b/other.jar", "SODIUM"));
    }

    @Test
    void fallsBackToExactUrlFileNameWhenHintIsBlank() throws IOException, RecommenderException
    {
        Files.writeString(_tempDir.resolve("iris-neoforge-1.8.jar"), "jar content");
        InstalledModScanner scanner = createScanner(_tempDir);

        assertTrue(scanner.isJarInstalled(
                "https://cdn.modrinth.com/data/a/versions/b/iris-neoforge-1.8.jar",
                ""
        ));
        assertFalse(scanner.isJarInstalled(
                "https://cdn.modrinth.com/data/a/versions/b/other-1.0.jar",
                ""
        ));
    }

    @Test
    void detectsMissingAndSatisfiedDependencies() throws IOException, RecommenderException
    {
        InstalledModScanner scanner = createScanner(_tempDir);
        RecommendedModData entry = new RecommendedModData(
                "Iris",
                "Shaders",
                "",
                "https://cdn.modrinth.com/data/a/versions/b/iris-1.8.jar",
                "iris",
                List.of(new DependencyData("https://cdn.modrinth.com/data/a/versions/b/sodium-0.6.jar", "sodium"))
        );

        assertTrue(scanner.hasMissingDependencies(entry));

        Files.writeString(_tempDir.resolve("sodium-neoforge-0.6.jar"), "jar content");
        assertFalse(scanner.hasMissingDependencies(entry));
    }

    private static InstalledModScanner createScanner(Path modsDirectory)
    {
        DownloadUrlValidator validator = new DownloadUrlValidator(DownloadPolicyData.defaults());
        return new InstalledModScanner(modsDirectory, validator);
    }
}
