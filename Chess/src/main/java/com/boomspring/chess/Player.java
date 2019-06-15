package com.boomspring.chess;

import java.util.concurrent.Callable;

public abstract class Player implements Callable<Object>
{
    private final Colour colour;

    private Player(final Colour colour)
    {
        this.colour = colour;
    }

    protected final Colour getColour()
    {
        return colour;
    }

    protected static final class Human extends Player
    {
        protected Human(final Colour colour)
        {
            super(colour);
        }

        @Override
        public synchronized final Object call() throws InterruptedException
        {
            return new Object();
        }
    }

    protected static final class AI extends Player
    {
        protected AI(final Colour colour)
        {
            super(colour);
        }

        public synchronized final Object call() throws InterruptedException
        {
            return new Object();
        }
    }
}