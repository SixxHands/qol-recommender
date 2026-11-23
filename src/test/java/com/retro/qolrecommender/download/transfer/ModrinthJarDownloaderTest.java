package com.retro.qolrecommender.download.transfer;

import com.retro.qolrecommender.RecommenderException;
import com.retro.qolrecommender.config.model.DependencyData;
import com.retro.qolrecommender.config.model.RecommendedModData;
import com.retro.qolrecommender.download.IModDownloader;
import com.retro.qolrecommender.download.ModDownloaderBlueprint;
import com.retro.qolrecommender.download.model.DownloadOutcomeData;
import com.retro.qolrecommender.download.model.DownloadPolicyData;
import com.retro.qolrecommender.download.model.InstallStatus;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ModrinthJarDownloaderTest
{
    private static final int ValidJarSize = 60_000;
    private static final int TinyJarSize = 256;

    @TempDir
    Path _tempDir;

    private HttpServer _server;
    private String _baseUrl;

    @BeforeEach
    void startLocalServer() throws IOException
    {
        _server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        byte[] validBytes = filledBytes(ValidJarSize, (byte) 0x50);
        byte[] tinyBytes = filledBytes(TinyJarSize, (byte) 0x50);

        _server.createContext("/mods/dep-1.0.jar", exchange -> sendResponse(exchange, 200, validBytes));
        _server.createContext("/mods/main-1.0.jar", exchange -> sendResponse(exchange, 200, validBytes));
        _server.createContext("/mods/tiny-1.0.jar", exchange -> sendResponse(exchange, 200, tinyBytes));
        _server.createContext("/mods/missing-1.0.jar", exchange -> sendResponse(exchange, 404, new byte[0]));
        _server.start();

        _baseUrl = "http://127.0.0.1:" + _server.getAddress().getPort();
    }

    @AfterEach
    void stopLocalServer()
    {
        if (_server != null)
        {
            _server.stop(0);
        }
    }

    @Test
    void downloadsDependenciesFirstThenMainJarAndUpdatesStatus() throws RecommenderException
    {
        IModDownloader downloader = createLocalDownloader();
        RecommendedModData entry = new RecommendedModData(
                "Main Mod",
                "Test mod",
                "",
                _baseUrl + "/mods/main-1.0.jar",
                "main",
                List.of(new DependencyData(_baseUrl + "/mods/dep-1.0.jar", "dep"))
        );

        assertEquals(InstallStatus.MISSING_DEPENDENCIES, downloader.checkStatus(entry));

        List<String> progressMessages = new ArrayList<>();
        downloader.downloadEntry(entry, progressMessages::add);

        assertTrue(Files.isRegularFile(_tempDir.resolve("dep-1.0.jar")));
        assertTrue(Files.isRegularFile(_tempDir.resolve("main-1.0.jar")));
        assertFalse(Files.exists(_tempDir.resolve("dep-1.0.jar.part")));
        assertFalse(Files.exists(_tempDir.resolve("main-1.0.jar.part")));
        assertEquals(2, progressMessages.size());
        assertTrue(progressMessages.get(0).contains("dep-1.0.jar (1/2)"));
        assertTrue(progressMessages.get(1).contains("main-1.0.jar (2/2)"));
        assertEquals(InstallStatus.INSTALLED, downloader.checkStatus(entry));
    }

    @Test
    void skipsAlreadyInstalledDependency() throws IOException, RecommenderException
    {
        Files.write(_tempDir.resolve("dep-1.0.jar"), filledBytes(ValidJarSize, (byte) 0x50));
        IModDownloader downloader = createLocalDownloader();
        RecommendedModData entry = new RecommendedModData(
                "Main Mod",
                "",
                "",
                _baseUrl + "/mods/main-1.0.jar",
                "main",
                List.of(new DependencyData(_baseUrl + "/mods/dep-1.0.jar", "dep"))
        );

        assertEquals(InstallStatus.NOT_INSTALLED, downloader.checkStatus(entry));

        List<String> progressMessages = new ArrayList<>();
        downloader.downloadEntry(entry, progressMessages::add);

        assertTrue(progressMessages.get(0).startsWith("Already installed: dep-1.0.jar"));
        assertTrue(Files.isRegularFile(_tempDir.resolve("main-1.0.jar")));
    }

    @Test
    void rejectsSuspiciouslySmallDownloadAndCleansUpPartFile()
    {
        IModDownloader downloader = createLocalDownloader();
        RecommendedModData entry = new RecommendedModData(
                "Tiny Mod",
                "",
                "",
                _baseUrl + "/mods/tiny-1.0.jar",
                "tiny",
                List.of()
        );

        RecommenderException error = assertThrows(
                RecommenderException.class,
                () -> downloader.downloadEntry(entry, null)
        );

        assertTrue(error.getMessage().contains("suspiciously small"));
        assertFalse(Files.exists(_tempDir.resolve("tiny-1.0.jar")));
        assertFalse(Files.exists(_tempDir.resolve("tiny-1.0.jar.part")));
    }

    @Test
    void reportsHttpFailureViaAsyncBoundary()
    {
        IModDownloader downloader = createLocalDownloader();
        RecommendedModData entry = new RecommendedModData(
                "Missing Mod",
                "",
                "",
                _baseUrl + "/mods/missing-1.0.jar",
                "missing",
                List.of()
        );

        DownloadOutcomeData outcome = downloader.downloadEntryAsync(entry, null).join();

        assertFalse(outcome.succeeded());
        assertTrue(outcome.summary().contains("HTTP 404"));
        assertFalse(Files.exists(_tempDir.resolve("missing-1.0.jar")));
    }

    @Test
    void throwsWhenEntryHasNoDownloadUrls()
    {
        IModDownloader downloader = createLocalDownloader();
        RecommendedModData emptyEntry = new RecommendedModData("Empty", "", "", "", "", List.of());

        RecommenderException error = assertThrows(
                RecommenderException.class,
                () -> downloader.buildDownloadQueue(emptyEntry)
        );

        assertTrue(error.getMessage().contains("No downloadUrl"));
    }

    private IModDownloader createLocalDownloader()
    {
        DownloadPolicyData localPolicy = new DownloadPolicyData(
                Set.of("127.0.0.1"),
                DownloadPolicyData.DefaultMinimumJarBytes,
                Duration.ofSeconds(5),
                Duration.ofSeconds(10)
        );
        ModDownloaderBlueprint blueprint = new ModDownloaderBlueprint(_tempDir).policy(localPolicy);
        return ModDownloaderFactory.create(blueprint);
    }

    private static void sendResponse(
            HttpExchange exchange,
            int statusCode,
            byte[] responseBytes
    ) throws IOException
    {
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream out = exchange.getResponseBody())
        {
            out.write(responseBytes);
        }
    }

    private static byte[] filledBytes(int length, byte fill)
    {
        byte[] bytes = new byte[length];
        Arrays.fill(bytes, fill);
        return bytes;
    }
}
