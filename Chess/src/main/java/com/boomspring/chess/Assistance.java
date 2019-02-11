package com.boomspring.chess;

import java.util.stream.IntStream;

import com.google.common.base.Predicates;

@FunctionalInterface
interface Assistance {
    abstract boolean boardException(final int position, final int vector);

    default QuadPredicate<Game.Turn, Integer, Integer, Integer> noCollide() {
        return (turn, positionFrom, counter, vector) -> {
            final var piece = turn.getState().get(positionFrom).getPiece().orElseThrow();

            return turn.getState().get(positionFrom + (vector * (counter + 1))).getPiece().map(Piece::getPlayer)
                    .filter(piece.getPlayer()::equals).or(() -> turn.getState().get(positionFrom + (vector * counter))
                            .getPiece().filter(Predicates.not(piece::equals)).map(Piece::getPlayer))
                    .isEmpty();
        };
    }

    default QuadPredicate<Game.Turn, Integer, Integer, Integer> noCheck() {
        return (turn, positionFrom, counter, vector) -> {
            final var piece = turn.getState().get(positionFrom).getPiece().orElseThrow();

            if (piece.getPlayer().equals(turn.getGame().getPlayers().get(turn.getGame().getTurns().size() % 2))) {
                final var testing = turn.getGame().new Turn(positionFrom, positionFrom + (vector * (counter + 1)), false);
                final var kingPosition = IntStream.range(0, 64)
                        .filter(i -> turn.getState().get(i).getPiece().filter(Piece.King.class::isInstance)
                                .map(Piece::getPlayer).filter(piece.getPlayer()::equals).isPresent())
                        .findFirst().orElseThrow();
                return turn.getGame()
                        .getPlayerMoves(testing,
                                turn.getGame().getPlayers().get((turn.getGame().getTurns().size() + 1) % 2))
                        .noneMatch(i -> i == kingPosition);
            } else {
                return true;
            }
        };
    }

    default QuadPredicate<Game.Turn, Integer, Integer, Integer> noError() {
        return (turn, positionFrom, counter, vector) -> {
            return counter < turn.getState().get(positionFrom).getPiece().flatMap(Piece::getRange).orElse(7);
        };
    }
}