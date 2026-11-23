package com.retro.qolrecommender.download.transfer;

import com.retro.qolrecommender.RecommenderException;
import com.retro.qolrecommender.download.model.DownloadPolicyData;
import com.retro.qolrecommender.download.model.DownloadTargetData;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DownloadUrlValidatorTest
{
    private final DownloadUrlValidator _validator = new DownloadUrlValidator(DownloadPolicyData.defaults());

    @Test
    void acceptsValidModrinthJarUrl() throws RecommenderException
    {
        String url = "https://cdn.modrinth.com/data/abc123/versions/v1/sodium-neoforge-0.6.jar";
        DownloadTargetData target = _validator.validateTarget(url, "sodium");

        assertEquals("sodium-neoforge-0.6.jar", target.fileName());
        assertEquals("sodium", target.filenameHint());
        assertEquals("cdn.modrinth.com", target.uri().getHost());
    }

    @Test
    void stripsQueryAndFragmentWhenExtractingFileName()
    {
        Optional<String> extracted = _validator.extractFileName(
                "https://cdn.modrinth.com/data/a/versions/b/iris-1.8.jar?download=1#top"
        );

        assertEquals(Optional.of("iris-1.8.jar"), extracted);
    }

    @Test
    void returnsEmptyFileNameForInvalidOrPathTraversalInputs()
    {
        assertTrue(_validator.extractFileName("").isEmpty());
        assertTrue(_validator.extractFileName("https://cdn.modrinth.com/").isEmpty());
        assertTrue(_validator.extractFileName("https://cdn.modrinth.com/..evil.jar").isEmpty());
    }

    @Test
    void rejectsBlankUrl()
    {
        RecommenderException error = assertThrows(
                RecommenderException.class,
                () -> _validator.validateTarget("   ", "hint")
        );

        assertTrue(error.getMessage().contains("No downloadUrl"));
    }

    @Test
    void rejectsBlockedHost()
    {
        RecommenderException error = assertThrows(
                RecommenderException.class,
                () -> _validator.validateTarget("https://example.com/mods/test-1.0.jar", "test")
        );

        assertTrue(error.getMessage().contains("Blocked URL host"));
    }

    @Test
    void rejectsUnsupportedScheme()
    {
        RecommenderException error = assertThrows(
                RecommenderException.class,
                () -> _validator.validateTarget("file:///C:/mods/test-1.0.jar", "test")
        );

        assertTrue(error.getMessage().contains("Unsupported URL scheme"));
    }

    @Test
    void rejectsNonJarFileName()
    {
        RecommenderException error = assertThrows(
                RecommenderException.class,
                () -> _validator.validateTarget("https://cdn.modrinth.com/data/a/versions/b/readme.zip", "readme")
        );

        assertTrue(error.getMessage().contains(".jar"));
    }

    @Test
    void rejectsMalformedUri()
    {
        RecommenderException error = assertThrows(
                RecommenderException.class,
                () -> _validator.validateTarget("https://cdn.modrinth.com/bad url.jar", "bad")
        );

        assertTrue(error.getMessage().contains("Invalid download URL"));
    }
}
