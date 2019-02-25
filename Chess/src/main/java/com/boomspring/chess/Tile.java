package com.boomspring.chess;

import java.util.Optional;

public final class Tile
{
    private final Piece piece;
    private final Integer counter;

    public Tile(final Piece piece)
    {
        this.piece = piece;
        this.counter = null;
    }

    private Tile(final Piece piece, final Integer counter)
    {
        this.piece = piece;
        this.counter = counter;
    }

    public final Optional<Piece> getPiece()
    {
        return Optional.ofNullable(piece);
    }

    public final Optional<Integer> getCounter()
    {
        return Optional.ofNullable(counter);
    }

    public final Tile updateCounter(final int counter)
    {
        return new Tile(this.piece, counter);
    }

    public final Tile updatePiece(final Piece piece)
    {
        return new Tile(piece, this.counter);
    }
}