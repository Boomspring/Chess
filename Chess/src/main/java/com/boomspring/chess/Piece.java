package com.boomspring.chess;

import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Range;

public abstract class Piece implements Game.Rules
{
    private final String name;
    private final int value;
    private final Player player;
    private final Integer range;
    private final ImmutableList<Integer> vectors;

    public Piece(final String name, final int value, final Player player, final Integer range, final Integer... vectors)
    {
        this.name = name;
        this.value = value;
        this.player = player;
        this.range = range;
        this.vectors = ImmutableList.copyOf(vectors);
    }

    @Override
    public final String toString()
    {
        return player.getColour().name().substring(0, 1).concat(name);
    }

    public final Player getPlayer()
    {
        return player;
    }

    public final Optional<Integer> getValue()
    {
        return Optional.ofNullable(value);
    }

    public final Optional<Integer> getRange()
    {
        return Optional.ofNullable(range);
    }

    public final Stream<Integer> getVectors()
    {
        return vectors.stream();
    }

    public static final class Rook extends Piece
    {
        protected Rook(final Player player)
        {
            super("R", 5, player, null, 8, 1, -1, -8);
        }

        @Override
        public final boolean validVector(final int currentPosition, final int vector)
        {
            return !ImmutableMultimap.<Integer, Integer>builder().put(0, -1).put(7, 1).build().containsEntry(UI.getColumn(currentPosition), vector);
        }
    }

    public static final class Knight extends Piece
    {
        protected Knight(final Player player)
        {
            super("N", 3, player, 1, 17, 15, 10, 6, -6, -10, -15, -17);
        }

        @Override
        public final boolean validVector(final int currentPosition, final int vector)
        {
            return !ImmutableMultimap.<Integer, Integer>builder().putAll(0, 15, 6, -10, -17).putAll(1, 6, -10).putAll(6, 10, -6).putAll(7, 17, 10, -6, -15).build().containsEntry(UI.getColumn(currentPosition), vector);
        }
    }

    public static final class Bishop extends Piece
    {
        protected Bishop(final Player player)
        {
            super("B", 3, player, null, 9, 7, -7, -9);
        }

        @Override
        public final boolean validVector(final int currentPosition, final int vector)
        {
            return !ImmutableMultimap.<Integer, Integer>builder().putAll(0, 7, -9).putAll(7, 9, -7).build().containsEntry(UI.getColumn(currentPosition), vector);
        }
    }

    public static final class Queen extends Piece
    {
        protected Queen(final Player player)
        {
            super("Q", 9, player, null, 9, 8, 7, 1, -1, -7, -8, -9);
        }

        @Override
        public final boolean validVector(final int currentPosition, final int vector)
        {
            return !ImmutableMultimap.<Integer, Integer>builder().putAll(0, 7, -1, -9).putAll(7, 9, 1, -7).build().containsEntry(UI.getColumn(currentPosition), vector);
        }
    }

    public static final class King extends Piece
    {
        protected King(final Player player)
        {
            super("K", 100, player, 1, 9, 8, 7, 1, -1, -7, -8, -9);
        }

        @Override
        public final long calculateLimit(final Game.Turn turn, final int positionFrom, final int vector)
        {
            if (turn.getBoard().get(positionFrom).getCounter().isEmpty())
            {
                if (Math.abs(vector) == 1)
                {
                    final Tile tile = turn.getBoard().get(positionFrom + ((7 * vector - 1) / 2));

                    if (tile.getPiece().filter(Piece.Rook.class::isInstance).map(Piece::getPlayer).filter(this.getPlayer()::equals).isPresent() && tile.getCounter().isEmpty())
                    {
                        if (IntStream.iterate(positionFrom + vector , Range.open(positionFrom - 4, positionFrom + 3)::contains, i -> i + vector).allMatch(i -> turn.getBoard().get(i).getPiece().isEmpty()))
                        {
                            return 2;
                        }
                    }
                }
            }

            return 1;
        }

        @Override
        public final boolean validVector(final int currentPosition, final int vector)
        {
            return !ImmutableMultimap.<Integer, Integer>builder().putAll(0, 7, -1, -9).putAll(7, 9, 1, -7).build().containsEntry(UI.getColumn(currentPosition), vector);
        }
    }

    public static final class Pawn extends Piece
    {
        protected Pawn(final Player player)
        {
            super("P", 1, player, 1, Stream.of(7, 8, 9).map(player.getColour()::setDirection).toArray(Integer[]::new));
        }

        @Override
        public final long calculateLimit(final Game.Turn turn, final int positionFrom, final int vector)
        {
            return turn.getBoard().get(positionFrom).getCounter().isEmpty() && UI.getColumn(vector) == 0 ? 2 : 1;
        }

        @Override
        public final boolean validVector(final int positionCurrent, final int vector)
        {
            return !ImmutableMultimap.<Integer, Integer>builder().putAll(0, 7, -9).putAll(7, 9, -7).build().containsEntry(UI.getColumn(positionCurrent), vector);
        }

        public final boolean noCollide(final Game.Turn turn, final int positionFrom, final int positionCurrent, final int vector)
        {
            if (UI.getColumn(vector) == 0) {
                return turn.getBoard().get(positionCurrent + vector).getPiece().isEmpty();
            } else return turn.getBoard().get(positionCurrent + vector).getPiece().map(Piece::getPlayer).or(() -> {
                return Optional.of(turn.getBoard().get(positionCurrent + vector + Map.of(9, -8, 7, -8, -7, 8, -9, 8).get(vector)))
                        .filter(i -> Math.abs(turn.getPositionTo().orElse(0) - turn.getPositionFrom().orElse(0)) == 16)
                        .filter(i -> i.getCounter().filter(x -> UI.getGame().getTurns().size() - x == 1).isPresent())
                        .flatMap(x -> x.getPiece()).filter(Piece.Pawn.class::isInstance)
                        .map(Piece::getPlayer).filter(i -> UI.getRow(positionFrom) == i.getColour().ordinal() + 3);
            }).filter(Predicates.not(this.getPlayer()::equals)).isPresent();
        }
    }
}