package com.boomspring.chess;

import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JFrame;

@SuppressWarnings("serial")
public final class UI extends JFrame
{
    private static final UI i = new UI();
    private final AtomicReference<Game> game = new AtomicReference<>(new Game());

    public static final void main(final String... args)
    {
        i.getGame().start();
    }

    protected static final UI get()
    {
        return i;
    }

    protected final Game getGame()
    {
        return game.get();
    }
}