package com.boomspring.chess;

import java.util.ArrayList;

import com.google.common.collect.ImmutableList;

public abstract class Player
{
    private final Colour colour;
    private final ArrayList<Piece> pieces = new ArrayList<>();

    private Player(final Colour colour)
    {
        this.colour = colour;
    }

    public final Colour getColour()
    {
        return colour;
    }

    public final ImmutableList<Piece> getPieces()
    {
        return ImmutableList.copyOf(pieces);
    }

    public final void takePiece(final Piece piece)
    {
        pieces.add(piece);
    }

    public static final class Human extends Player
    {
        public Human(final Colour colour)
        {
            super(colour);
        }
    }

    public static final class AI extends Player
    {
        public AI(final Colour colour)
        {
            super(colour);
        }
    }
}