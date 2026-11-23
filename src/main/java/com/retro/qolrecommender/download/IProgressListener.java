package com.retro.qolrecommender.download;

@FunctionalInterface
public interface IProgressListener
{
    void reportProgress(String message);
}
