package com.retro.qolrecommender.download.model;

import java.net.URI;
import java.util.Objects;

public record DownloadTargetData(URI uri, String fileName, String filenameHint)
{
    public DownloadTargetData
    {
        Objects.requireNonNull(uri, "uri must not be null");
        if (fileName == null || fileName.isBlank())
        {
            throw new IllegalArgumentException("fileName must not be blank");
        }
        filenameHint = filenameHint == null ? "" : filenameHint.trim();
    }
}
