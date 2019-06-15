package com.boomspring.chess;

public abstract class Piece
{
    private final Player player;

    private Piece(final Player player)
    {
        this.player = player;
    }

    protected final Player getPlayer()
    {
        return player;
    }

    protected static final class Rook extends Piece
    {
        protected Rook(final Player player)
        {
            super(player);
        }
    }

    protected static final class Knight extends Piece
    {
        protected Knight(final Player player)
        {
            super(player);
        }
    }

    protected static final class Bishop extends Piece
    {
        protected Bishop(final Player player)
        {
            super(player);
        }
    }

    protected static final class Queen extends Piece
    {
        protected Queen(final Player player)
        {
            super(player);
        }
    }

    protected static final class King extends Piece
    {
        protected King(final Player player)
        {
            super(player);
        }
    }

    protected static final class Pawn extends Piece
    {
        protected Pawn(final Player player)
        {
            super(player);
        }
    }
}