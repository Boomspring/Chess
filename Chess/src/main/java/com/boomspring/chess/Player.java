package com.boomspring.chess;

import java.util.ArrayList;
import java.util.stream.Stream;

final class Player {
    private final Colour colour;
    private final ArrayList<Piece> pieces = new ArrayList<>();

    Player(final Colour colour) {
        this.colour = colour;
    }

    final Colour getColor() {
        return colour;
    }

    final boolean takePiece(final Piece piece) {
        return pieces.add(piece);
    }

    final Stream<Piece> getPieces() {
        return pieces.stream();
    }
}