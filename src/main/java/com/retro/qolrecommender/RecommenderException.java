package com.retro.qolrecommender;

public final class RecommenderException extends Exception
{
    private static final long serialVersionUID = 1L;

    public RecommenderException(String message)
    {
        super(message);
    }

    public RecommenderException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
