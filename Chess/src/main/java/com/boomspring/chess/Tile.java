package com.boomspring.chess;

import java.util.Optional;

public final class Tile
{
    private final Piece piece;
    private final Integer counter;

    protected Tile(final Piece piece)
    {
        this.piece = piece;
        this.counter = null;
    }

    private Tile(final Piece piece, final Integer counter)
    {
        this.piece = piece;
        this.counter = counter;
    }

    protected final Optional<Piece> getPiece()
    {
        return Optional.ofNullable(piece);
    }

    protected final Optional<Integer> getCounter()
    {
        return Optional.ofNullable(counter);
    }

    protected final Tile updateCounter(final int counter)
    {
        return new Tile(this.piece, counter);
    }

    protected final Tile updatePiece(final Piece piece)
    {
        return new Tile(piece, this.counter);
    }
}