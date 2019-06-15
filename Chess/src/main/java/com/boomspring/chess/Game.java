package com.boomspring.chess;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

public final class Game extends Thread
{
    private final ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("Ply").build());

    protected Game()
    {
        super("Game");
    }

    @Override
    public final void run()
    {
        try
        {
            while(true)
            {
                
            }
        }
        catch(final Exception e)
        {

        }

        executor.shutdownNow().forEach(System.out::println);
    }
}