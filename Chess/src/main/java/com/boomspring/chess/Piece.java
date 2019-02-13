package com.boomspring.chess;

import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Range;
import com.google.common.primitives.ImmutableIntArray;

abstract class Piece implements Rules {
    private final Player player;
    private final String string;
    private final Integer range;
    private final ImmutableIntArray vectors;

    /**
     * Constructor for the Piece
     */
    private Piece(final Player player, final String letter, final Integer range, final ImmutableIntArray vectors) {
        this.player = player;
        this.string = player.getColor().name().substring(0, 1) + letter;
        this.range = range;
        this.vectors = vectors;
    }

    final Player getPlayer() {
        return player;
    }

    @Override
    public final String toString() {
        return string;
    }

    final Optional<Integer> getRange() {
        return Optional.ofNullable(range);
    }

    final IntStream getVectors() {
        return vectors.stream();
    }

    static final class Rook extends Piece {
        Rook(final Player player) {
            super(player, "R", null, ImmutableIntArray.of(8, 1, -1, -8));
        }

        @Override
        public final boolean boardException(final int position, final int vector) {
            return !ImmutableMultimap.<Integer, Integer>builder().put(0, -1).put(7, 1).build().containsEntry(Game.getFile(position), vector);
        }
    }

    static final class Knight extends Piece {
        Knight(final Player player) {
            super(player, "N", 1, ImmutableIntArray.of(17, 15, 10, 6, -6, -10, -15, -17));
        }

        @Override
        public final boolean boardException(final int position, final int vector) {
            return !ImmutableMultimap.<Integer, Integer>builder().putAll(0, 15, 6, -10, -17).putAll(1, 6, -10).putAll(6, 10, -6).putAll(7, 17, 10, -6, -15).build().containsEntry(Game.getFile(position), vector);
        }
    }

    static final class Bishop extends Piece {
        Bishop(final Player player) {
            super(player, "B", null, ImmutableIntArray.of(9, 7, -7, -9));
        }

        @Override
        public final boolean boardException(final int position, final int vector) {
            return !ImmutableMultimap.<Integer, Integer>builder().putAll(0, 7, -9).putAll(7, 9, -7).build().containsEntry(Game.getFile(position), vector);
        }
    }

    static final class Queen extends Piece {
        Queen(final Player player) {
            super(player, "Q", null, ImmutableIntArray.of(9, 8, 7, 1, -1, -7, -8, -9));
        }

        @Override
        public final boolean boardException(final int position, final int vector) {
            return !ImmutableMultimap.<Integer, Integer>builder().putAll(0, 7, -1, -9).putAll(7, 9, 1, -7).build().containsEntry(Game.getFile(position), vector);
        }
    }

    static final class King extends Piece {
        King(final Player player) {
            super(player, "K", 1, ImmutableIntArray.of(9, 8, 7, 1, -1, -7, -8, -9));
        }

        @Override
        public final boolean boardException(final int position, final int vector) {
            return !ImmutableMultimap.<Integer, Integer>builder().putAll(0, 7, -1, -9).putAll(7, 9, 1, -7).build().containsEntry(Game.getFile(position), vector);
        }

        @Override
        public final QuadPredicate<Game.Turn, Integer, Integer, Integer> noError() {
            return (turn, positionFrom, counter, vector) -> {
                if (turn.getState().get(positionFrom).getCounter().isEmpty()) {
                    // CASTLING CHECK
                    if (Math.abs(vector) == 1) {
                        if (turn.getState().get(positionFrom + ((7 * vector - 1) / 2)).getPiece().filter(Piece.Rook.class::isInstance).map(Piece::getPlayer).filter(this.getPlayer()::equals).isPresent()) {
                            if (turn.getState().get(positionFrom + ((7 * vector - 1) / 2)).getCounter().isEmpty()) {
                                if (IntStream.iterate(positionFrom + vector , Range.open(0, 7)::contains, i -> i + vector).allMatch(i -> turn.getState().get(i).getPiece().isEmpty())) {
                                    return counter < 2;
                                }
                            }
                        }
                    }
                }

                return counter.equals(0);
            };
        }
    }

    static final class Pawn extends Piece {
        Pawn(final Player player) {
            super(player, "P", 1, ImmutableIntArray.copyOf(IntStream.of(-9, -8, -7).map(i -> i * (2 * player.getColor().ordinal() - 1))));
        }

        @Override
        public final boolean boardException(final int position, final int vector) {
            return !ImmutableMultimap.<Integer, Integer>builder().putAll(0, 7, -9).putAll(7, 9, -7).build().containsEntry(Game.getFile(position), vector);
        }

        @Override
        public final QuadPredicate<Game.Turn, Integer, Integer, Integer> noCollide() {
            return (turn, positionFrom, counter, vector) -> {
                if (Game.getFile(vector) == 0) {
                    return turn.getState().get(positionFrom + (vector * (counter + 1))).getPiece().isEmpty();
                } else return turn.getState().get(positionFrom + (vector * (counter + 1))).getPiece().map(Piece::getPlayer).or(() -> {
                    return Optional.of(turn.getState().get((positionFrom + (vector * (counter + 1))) + Map.of(9, -8, 7, -8, -7, 8, -9, 8).get(vector)))
                            .filter(i -> Math.abs(turn.getPositionTo().orElse(0) - turn.getPositionFrom().orElse(0)) == 16)
                            .filter(i -> i.getCounter().filter(x -> turn.getGame().getTurns().size() - x == 1).isPresent())
                            .flatMap(Game.Tile::getPiece).filter(Piece.Pawn.class::isInstance)
                            .map(Piece::getPlayer).filter(i -> positionFrom / 8 == i.getColor().ordinal() + 3);
                }).filter(Predicates.not(this.getPlayer()::equals)).isPresent();
            };
        }

        @Override
        public final QuadPredicate<Game.Turn, Integer, Integer, Integer> noError() {
            return (turn, positionFrom, counter, vector) -> {
                return counter < (turn.getState().get(positionFrom).getCounter().isEmpty() && Game.getFile(vector) == 0 ? 2 : 1);
            };
        }
    }
}