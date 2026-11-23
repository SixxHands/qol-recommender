package com.retro.qolrecommender.config.model;

public record DependencyData(String url, String hint)
{
    public DependencyData
    {
        url = cleanText(url);
        hint = cleanText(hint);
    }

    private static String cleanText(String value)
    {
        return value == null ? "" : value.trim();
    }
}
