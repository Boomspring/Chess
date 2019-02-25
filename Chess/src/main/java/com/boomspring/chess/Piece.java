package com.boomspring.chess;

import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Range;

public abstract class Piece
{
    private final Player player;
    private final String string;
    private final Integer range;
    private final ImmutableList<Integer> vectors;

    private Piece(final Player player, final String string, final Integer range, final Stream<Integer> vectors)
    {
        this.player = player;
        this.string = player.getColour().name().charAt(0) + string;
        this.range = range;
        this.vectors = vectors.collect(ImmutableList.toImmutableList());
    }

    public final Player getPlayer()
    {
        return player;
    }

    public final String toString()
    {
        return string;
    }

    public final Optional<Integer> getRange()
    {
        return Optional.ofNullable(range);
    }

    public final Stream<Integer> getVectors()
    {
        return vectors.stream();
    }

    public boolean validVectors(final int position, final int vector)
    {
        return !ImmutableMultimap.<Integer, Integer>builder().putAll(0, 7, -1, -9).putAll(7, 9, 1, -7).build().containsEntry(UI.getColumn(position), vector);
    }

    public boolean noCollide(final Game.Turn turn, final int positionInitial, final int positionCurrent, final int vector)
    {
        return turn.getBoard().get(positionCurrent + vector).getPiece().map(Piece::getPlayer).filter(this.getPlayer()::equals)
                .or(() -> turn.getBoard().get(positionCurrent).getPiece().filter(Predicates.not(this::equals)).map(Piece::getPlayer))
                .isEmpty();
    }

    public boolean noError(final Game.Turn turn, final int positionInitial, final int positionCurrent, final int vector)
    {
        return Math.floorDiv(positionCurrent - positionInitial, vector) < turn.getBoard().get(positionInitial).getPiece().flatMap(Piece::getRange).orElse(7);
    }

    public boolean noCheckBeforeMoving(final Game.Turn turn, final int positionInitial, final int positionCurrent, final int vector) {
        if (this.getPlayer().equals(turn.getGame().getCurrentPlayer()))
        {
            return !((positionCurrent - positionInitial == vector) && Piece.King.class.isInstance(this) && turn.getPotentialTurns(turn.getGame().getNextPlayer()).mapToInt(Game.Turn::getPositionTo).anyMatch(i -> positionInitial == i));
        } else return true;
    }

    public boolean noCheckAfterMoving(final Game.Turn turn) {
        if (this.getPlayer().equals(turn.getGame().getCurrentPlayer()))
        {
            return turn.getPotentialTurns(turn.getGame().getNextPlayer()).mapToInt(Game.Turn::getPositionTo).noneMatch(x -> x == turn.getPosition(Piece.King.class, turn.getGame().getCurrentPlayer()).orElseThrow());
        } else return true;
    }

    public static final class Rook extends Piece
    {
        public Rook(final Player player)
        {
            super(player, "R", null, Stream.of(8, 1, -1, -8));
        }

        @Override
        public final boolean validVectors(final int position, final int vector)
        {
            return !ImmutableMultimap.<Integer, Integer>builder().put(0, -1).put(7, 1).build().containsEntry(UI.getColumn(position), vector);
        }
    }

    public static final class Knight extends Piece
    {
        public Knight(final Player player)
        {
            super(player, "N", 1, Stream.of(17, 15, 10, 6, -6, -10, -15, -17));
        }

        @Override
        public final boolean validVectors(final int position, final int vector)
        {
            return !ImmutableMultimap.<Integer, Integer>builder().putAll(0, 15, 6, -10, -17).putAll(1, 6, -10).putAll(6, 10, -6).putAll(7, 17, 10, -6, -15).build().containsEntry(UI.getColumn(position), vector);
        }
    }

    public static final class Bishop extends Piece
    {
        public Bishop(final Player player)
        {
            super(player, "B", null, Stream.of(9, 7, -7, -9));
        }

        @Override
        public final boolean validVectors(final int position, final int vector)
        {
            return !ImmutableMultimap.<Integer, Integer>builder().putAll(0, 7, -9).putAll(7, 9, -7).build().containsEntry(UI.getColumn(position), vector);
        }
    }

    public static final class Queen extends Piece
    {
        public Queen(final Player player)
        {
            super(player, "Q", null, Stream.of(9, 8, 7, 1, -1, -7, -8, -9));
        }
    }

    public static final class King extends Piece
    {
        public King(final Player player)
        {
            super(player, "K", 1, Stream.of(9, 8, 7, 1, -1, -7, -8, -9));
        }

        @Override
        public final boolean noError(final Game.Turn turn, final int positionInitial, final int positionCurrent, final int vector)
        {
            if (turn.getBoard().get(positionInitial).getCounter().isEmpty())
            {
                // CASTLING CHECK
                if (Math.abs(vector) == 1)
                {
                    final var tile = turn.getBoard().get(positionInitial + ((7 * vector - 1) / 2));

                    if (tile.getPiece().filter(Piece.Rook.class::isInstance).map(Piece::getPlayer).filter(this.getPlayer()::equals).isPresent() && tile.getCounter().isEmpty())
                    {
                        if (IntStream.iterate(positionInitial + vector , Range.open(0, 7)::contains, i -> i + vector).allMatch(i -> turn.getBoard().get(i).getPiece().isEmpty()))
                        {
                            return Math.floorDiv(positionCurrent - positionInitial, vector) < 2;
                        }
                    }
                }
            }

            return Math.floorDiv(positionCurrent - positionInitial, vector) < 1;
        }
    }

    public static final class Pawn extends Piece
    {
        public Pawn(final Player player)
        {
            super(player, "P", 1, Stream.of(9, 8, 7).map(player.getColour()::setDirection));
        }

        @Override
        public final boolean validVectors(final int position, final int vector)
        {
            return !ImmutableMultimap.<Integer, Integer>builder().putAll(0, 7, -9).putAll(7, 9, -7).build().containsEntry(UI.getColumn(position), vector);
        }

        @Override
        public boolean noCollide(final Game.Turn turn, final int positionInitial, final int positionCurrent, final int vector)
        {
                if (UI.getColumn(vector) == 0) {
                    return turn.getBoard().get(positionCurrent + vector).getPiece().isEmpty();
                } else return turn.getBoard().get(positionCurrent + vector).getPiece().map(Piece::getPlayer).or(() -> {
                    return Optional.of(turn.getBoard().get(positionCurrent + vector + Map.of(9, -8, 7, -8, -7, 8, -9, 8).get(vector)))
                            .filter(i -> Math.abs(turn.getPositionTo() - turn.getPositionFrom()) == 16)
                            .filter(i -> i.getCounter().filter(x -> turn.getGame().getTurns().size() - x == 1).isPresent())
                            .flatMap(x -> x.getPiece()).filter(Piece.Pawn.class::isInstance)
                            .map(Piece::getPlayer).filter(i -> UI.getRow(positionInitial) == i.getColour().ordinal() + 3);
                }).filter(Predicates.not(this.getPlayer()::equals)).isPresent();
        }

        @Override
        public boolean noError(final Game.Turn turn, final int positionInitial, final int positionCurrent, final int vector)
        {
            return Math.floorDiv(positionCurrent - positionInitial, vector) < (turn.getBoard().get(positionInitial).getCounter().isEmpty() && UI.getColumn(vector) == 0 ? 2 : 1);
        }
    }
}