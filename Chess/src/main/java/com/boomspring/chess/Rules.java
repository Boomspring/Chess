package com.boomspring.chess;

import com.google.common.base.Predicates;

@FunctionalInterface
interface Rules {
    abstract boolean boardException(final int position, final int vector);

    default QuadPredicate<Game.Turn, Integer, Integer, Integer> noCollide() {
        return (turn, positionFrom, counter, vector) -> {
            final var piece = turn.getState().get(positionFrom).getPiece().orElseThrow();

            return turn.getState().get(positionFrom + (vector * (counter + 1))).getPiece().map(Piece::getPlayer)
                    .filter(piece.getPlayer()::equals).or(() -> turn.getState().get(positionFrom + (vector * counter)).getPiece().filter(Predicates.not(piece::equals)).map(Piece::getPlayer))
                    .isEmpty();
        };
    }

    default QuadPredicate<Game.Turn, Integer, Integer, Integer> noCheck() {
        return (turn, positionFrom, counter, vector) -> {
            if (turn.getState().get(positionFrom).getPiece().orElseThrow().getPlayer().equals(turn.getGame().getCurrentPlayer())) {
                final var temporary = turn.temporaryTurn(positionFrom, positionFrom + (vector * (counter + 1)));
                return turn.getGame().getPlayerMoves(temporary, turn.getGame().getNextPlayer()).noneMatch(x -> x == temporary.getPosition(Piece.King.class, turn.getGame().getCurrentPlayer()).orElseThrow());
            } else return true;
        };
    }

    default QuadPredicate<Game.Turn, Integer, Integer, Integer> noError() {
        return (turn, positionFrom, counter, vector) -> {
            return counter < turn.getState().get(positionFrom).getPiece().flatMap(Piece::getRange).orElse(7);
        };
    }
}