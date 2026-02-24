package com.retro.qolrecommender;

import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

public final class ModIO {
    private ModIO() {}

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    public static Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    public static Path getModsDir() {
        return FMLPaths.MODSDIR.get();
    }

    public static boolean isEntryInstalled(RecommenderConfig.Entry entry) {
        String hint = entry.filenameHint == null ? "" : entry.filenameHint.trim();
        if (hint.isEmpty()) {
            String url = entry.downloadUrl == null ? "" : entry.downloadUrl.trim();
            String fname = filenameFromUrl(url);
            if (fname.isEmpty()) return false;
            return Files.exists(getModsDir().resolve(fname));
        }

        String hintLower = hint.toLowerCase(Locale.ROOT);

        try (Stream<Path> s = Files.list(getModsDir())) {
            return s
                    .filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString().toLowerCase(Locale.ROOT))
                    .anyMatch(name -> name.endsWith(".jar") && name.contains(hintLower));
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean isJarInstalledByUrlOrHint(String url, String hint) {
        // 1) If hint exists, use hint scan (most flexible)
        if (hint != null && !hint.trim().isEmpty()) {
            String h = hint.trim().toLowerCase(Locale.ROOT);
            try (Stream<Path> s = Files.list(getModsDir())) {
                return s.filter(Files::isRegularFile)
                        .map(p -> p.getFileName().toString().toLowerCase(Locale.ROOT))
                        .anyMatch(name -> name.endsWith(".jar") && name.contains(h));
            } catch (IOException ignored) {}
        }

        // 2) Fallback: exact filename from URL
        String fileName = filenameFromUrl(url);
        if (fileName.isEmpty()) return false;
        return Files.exists(getModsDir().resolve(fileName));
    }

    public static void downloadToMods(RecommenderConfig.Entry entry, BiConsumer<Boolean, String> callback) {
        String url = entry.downloadUrl == null ? "" : entry.downloadUrl.trim();
        if (url.isEmpty()) {
            callback.accept(false, "No downloadUrl set for this entry.");
            return;
        }

        // Direct Modrinth CDN URL. Still enforce basic safety.
        if (!isAllowedUrl(url)) {
            callback.accept(false, "Blocked URL host. For safety, only cdn.modrinth.com is allowed by default.");
            return;
        }

        String fileName = filenameFromUrl(url);
        if (fileName.isEmpty() || !fileName.toLowerCase(Locale.ROOT).endsWith(".jar")) {
            callback.accept(false, "Download URL does not look like a .jar file.");
            return;
        }

        Path modsDir = getModsDir();
        Path tmp = modsDir.resolve(fileName + ".part");
        Path out = modsDir.resolve(fileName);

        if (Files.exists(out)) {
            callback.accept(true, "Already installed.");
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                Files.createDirectories(modsDir);

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofMinutes(2))
                        .GET()
                        .build();

                HttpResponse<InputStream> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofInputStream());
                int code = resp.statusCode();
                if (code < 200 || code >= 300) {
                    callback.accept(false, "HTTP " + code);
                    return;
                }

                try (InputStream in = resp.body()) {
                    Files.deleteIfExists(tmp);
                    Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
                }

                long size = Files.size(tmp);
                if (size < 50_000) {
                    Files.deleteIfExists(tmp);
                    callback.accept(false, "Downloaded file is suspiciously small (" + size + " bytes).");
                    return;
                }

                try {
                    Files.move(tmp, out, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(tmp, out, StandardCopyOption.REPLACE_EXISTING);
                }

                callback.accept(true, "Downloaded to mods/" + fileName);
            } catch (Exception e) {
                try { Files.deleteIfExists(tmp); } catch (Exception ignored) {}
                callback.accept(false, e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        });
    }

    private static boolean isAllowedUrl(String url) {
        try {
            URI u = URI.create(url);
            String host = u.getHost();
            if (host == null) return false;
            host = host.toLowerCase(Locale.ROOT);
            return host.equals("cdn.modrinth.com");
        } catch (Exception e) {
            return false;
        }
    }

    private static String filenameFromUrl(String url) {
        try {
            URI u = URI.create(url);
            String path = u.getPath();
            if (path == null) return "";
            int slash = path.lastIndexOf('/');
            return (slash >= 0 && slash + 1 < path.length()) ? path.substring(slash + 1) : "";
        } catch (Exception e) {
            return "";
        }
    }
    public interface ProgressCallback {
        void onProgress(String message);
    }
    public static void downloadWithDependencies(
            RecommenderConfig.Entry entry,
            ProgressCallback progress,
            BiConsumer<Boolean, String> callback
    ) {
        // deps first, then main
        java.util.List<RecommenderConfig.Entry.Dep> deps =
                entry.dependencies == null ? java.util.List.of() : entry.dependencies;

        java.util.List<DownloadItem> queue = new java.util.ArrayList<>();

        for (RecommenderConfig.Entry.Dep d : deps) {
            if (d == null) continue;
            String url = d.url == null ? "" : d.url.trim();
            if (url.isEmpty()) continue;

            String hint = d.hint == null ? "" : d.hint.trim();
            queue.add(new DownloadItem(url, hint));
        }

        String mainUrl = entry.downloadUrl == null ? "" : entry.downloadUrl.trim();
        if (!mainUrl.isEmpty()) {
            String mainHint = entry.filenameHint == null ? "" : entry.filenameHint.trim();
            queue.add(new DownloadItem(mainUrl, mainHint));
        }

        if (queue.isEmpty()) {
            callback.accept(false, "No downloadUrl (or dependencies) configured.");
            return;
        }

        downloadQueueSequential(queue, progress, callback);
    }

    private static final class DownloadItem {
        final String url;
        final String hint; // optional

        DownloadItem(String url, String hint) {
            this.url = url;
            this.hint = hint;
        }
    }
    private static void downloadQueueSequential(
            java.util.List<DownloadItem> items,
            ProgressCallback progress,
            BiConsumer<Boolean, String> callback
    ) {
        CompletableFuture.runAsync(() -> {
            try {
                for (int i = 0; i < items.size(); i++) {
                    DownloadItem it = items.get(i);

                    if (!isAllowedUrl(it.url)) {
                        callback.accept(false, "Blocked URL host: " + it.url);
                        return;
                    }

                    String fileName = filenameFromUrl(it.url);
                    if (fileName.isEmpty() || !fileName.toLowerCase(Locale.ROOT).endsWith(".jar")) {
                        callback.accept(false, "URL does not look like a .jar: " + it.url);
                        return;
                    }

                    if (isJarInstalledByUrlOrHint(it.url, it.hint)) {
                        if (progress != null) progress.onProgress("Already installed: " + fileName);
                        continue;
                    }

                    if (progress != null) {
                        progress.onProgress("Downloading: " + fileName + " (" + (i + 1) + "/" + items.size() + ")");
                    }

                    boolean ok = downloadSingleJarUrl(it.url);
                    if (!ok) {
                        callback.accept(false, "Failed downloading: " + fileName);
                        return;
                    }
                }

                callback.accept(true, "Downloaded all (restart Minecraft).");
            } catch (Exception e) {
                callback.accept(false, e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        });
    }
    private static boolean downloadSingleJarUrl(String url) throws Exception {
        String fileName = filenameFromUrl(url);

        Path modsDir = getModsDir();
        Path tmp = modsDir.resolve(fileName + ".part");
        Path out = modsDir.resolve(fileName);

        Files.createDirectories(modsDir);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMinutes(2))
                .GET()
                .build();

        HttpResponse<InputStream> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofInputStream());
        int code = resp.statusCode();
        if (code < 200 || code >= 300) return false;

        try (InputStream in = resp.body()) {
            Files.deleteIfExists(tmp);
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        }

        long size = Files.size(tmp);
        if (size < 50_000) { // same sanity check you had
            Files.deleteIfExists(tmp);
            return false;
        }

        try {
            Files.move(tmp, out, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, out, StandardCopyOption.REPLACE_EXISTING);
        }

        return true;
        }
    public static boolean hasMissingDependencies(RecommenderConfig.Entry entry) {
        if (entry.dependencies == null || entry.dependencies.isEmpty()) return false;

        for (RecommenderConfig.Entry.Dep d : entry.dependencies) {
            if (d == null) continue;

            String url = d.url == null ? "" : d.url.trim();
            if (url.isEmpty()) continue;

            String hint = d.hint == null ? "" : d.hint.trim();

            if (!isJarInstalledByUrlOrHint(url, hint)) {
                return true;
            }
        }
        return false;
    }
}

