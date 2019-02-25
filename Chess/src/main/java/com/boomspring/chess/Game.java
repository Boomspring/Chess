package com.boomspring.chess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;

public final class Game
{
    private final ImmutableList<Player> players = ImmutableList.of(new Player.Human(Colour.BLACK), new Player.Human(Colour.WHITE));
    private final ArrayList<Turn> turns = new ArrayList<>(List.of(new Turn()));
    private final AtomicReference<String> status = new AtomicReference<>();
    private final AtomicBoolean ended = new AtomicBoolean();

    public final void makeMove(final int positionFrom, final int positionTo) {
        this.getCurrentTurn().board.get(positionFrom).getPiece().map(Piece::getPlayer)
            .filter(getCurrentPlayer()::equals)
            .flatMap(i -> this.getCurrentTurn().getPotentialTurns(positionFrom).filter(t -> t.getPositionTo().equals(positionTo)).findFirst())
            .map(i -> new Turn(positionFrom, positionTo, true))
            .ifPresentOrElse(i -> {
                turns.add(i);
                final var kingPosition = getCurrentTurn().getPosition(Piece.King.class, getCurrentPlayer()).orElseThrow();

                    // CHECK / CHECKMATE / STALEMATE
                    switch((int) getCurrentTurn().getPotentialTurns(getNextPlayer()).mapToInt(Game.Turn::getPositionTo).filter(position -> position == kingPosition).count()) {
                        case 0:
                            getCurrentTurn().getPotentialTurns(getCurrentPlayer()).findAny().ifPresentOrElse(x -> {
                                status.set("Turn " + (turns.size() - 1) + ": Successful");
                            }, () -> {
                                status.set("Turn " + (turns.size() - 1) + ": Successful");
                            });
                            break;
                        case 1:
                            getCurrentTurn().getPotentialTurns(getCurrentPlayer()).findAny().ifPresentOrElse(x -> {
                                status.set("Turn " + (turns.size() - 1) + ": Player " + getCurrentPlayer().getColour() + " is in Check");
                            }, () -> {
                                status.set("Turn " + (turns.size() - 1) + ": Player " + getNextPlayer().getColour() + " Wins");
                                ended.set(true);
                            });
                            break;
                        default:
                            getCurrentTurn().getPotentialTurns(kingPosition).findAny().ifPresentOrElse(x -> {
                                status.set("Turn " + (turns.size() - 1) + ": Player " + getCurrentPlayer().getColour() + " is in Check");
                            }, () -> {
                                status.set("Turn " + (turns.size() - 1) + ": Player " + getNextPlayer().getColour() + " Wins");
                                ended.set(true);
                            });
                    }
            }, IllegalArgumentException::new);
    }

    public final ImmutableList<Player> getPlayers()
    {
        return players;
    }

    public final Player getCurrentPlayer()
    {
        return players.get(turns.size() % 2);
    }

    public final Player getNextPlayer()
    {
        return players.get((turns.size() + 1) % 2);
    }

    public final ImmutableList<Turn> getTurns()
    {
        return ImmutableList.copyOf(turns);
    }

    public final Turn getCurrentTurn()
    {
        return turns.get(turns.size() - 1);
    }

    public final Boolean hasEnded()
    {
        return ended.get();
    }

    public final String getStatus()
    {
        return status.get();
    }

    public final class Turn
    {
        private final Integer positionFrom;
        private final Integer positionTo;
        private final ImmutableList<Tile> board;

        public Turn()
        {
            this.positionFrom = null;
            this.positionTo = null;
            this.board = IntStream.range(0, 64).mapToObj(i ->
            {
                switch(i)
                {
                    case 0: case 7: return new Piece.Rook(players.get(0));
                    case 1: case 6: return new Piece.Knight(players.get(0));
                    case 2: case 5: return new Piece.Bishop(players.get(0));
                    case 3: return new Piece.Queen(players.get(0));
                    case 4: return new Piece.King(players.get(0));
                    case 8: case 9: case 10: case 11: case 12: case 13: case 14: case 15: return new Piece.Pawn(players.get(0));
                    case 48: case 49: case 50: case 51: case 52: case 53: case 54: case 55: return new Piece.Pawn(players.get(1));
                    case 56: case 63: return new Piece.Rook(players.get(1));
                    case 57: case 62: return new Piece.Knight(players.get(1));
                    case 58: case 61: return new Piece.Bishop(players.get(1));
                    case 59: return new Piece.Queen(players.get(1));
                    case 60: return new Piece.King(players.get(1));
                    default: return null;
                }
            }).map(Tile::new).collect(ImmutableList.toImmutableList());
        }

        private Turn(final int positionFrom, final int positionTo, final boolean permanent)
        {
            this.positionFrom = positionFrom;
            this.positionTo = positionTo;

            final var copy = new ArrayList<>(getCurrentTurn().board);

            if (copy.get(positionFrom).getPiece().filter(Piece.Pawn.class::isInstance).isPresent() && UI.getColumn(positionTo - positionFrom) != 0) { // DIAGONAL PAWN MOVE
                final var passant = positionTo + Map.of(9, -8, 7, -8, -7, 8, -9, 8).getOrDefault(positionTo - positionFrom, 0);

                if (copy.get(positionTo).getPiece().map(Piece::getPlayer).isPresent()) { // NORMAL DIAGONAL
                    if (permanent) copy.get(positionFrom).getPiece().map(Piece::getPlayer).ifPresent(x -> x.takePiece(copy.get(positionTo).getPiece().orElseThrow()));
                    copy.set(positionTo, copy.get(positionTo).updatePiece(null).updateCounter(turns.size()));
                } else if (Optional.of(copy.get(passant))
                        .filter(i -> Math.abs(getCurrentTurn().getPositionTo() - getCurrentTurn().getPositionFrom()) == 16)
                        .filter(i -> i.getCounter().filter(x -> turns.size() - x == 1).isPresent())
                        .flatMap(x -> x.getPiece())
                        .filter(Piece.Pawn.class::isInstance).map(Piece::getPlayer)
                        .filter(i -> UI.getRow(positionFrom) == i.getColour().ordinal() + 3)
                        .filter(getNextPlayer()::equals).isPresent()) { // ENPASSANT DIAGONAL

                    //ui.getComponents(ui.board, JLabel.class).get(passant).setOpaque(true);
                    if (permanent) copy.get(positionFrom).getPiece().map(Piece::getPlayer).ifPresent(x -> x.takePiece(copy.get(passant).getPiece().orElseThrow()));
                    copy.set(passant, copy.get(passant).updatePiece(null).updateCounter(turns.size()));
                }
            } else { // OTHER MOVES
                if (permanent) { // REAL MOVE
                    if (copy.get(positionFrom).getPiece().filter(Piece.King.class::isInstance).isPresent()) { // KING CASTLING
                        switch(positionTo - positionFrom) {
                            case 2: // RIGHT SIDE
                                copy.set(positionTo + 1, copy.get(positionTo + 1).updateCounter(turns.size()));
                                //ui.getComponents(ui.board, JLabel.class).get(positionTo + 1).setOpaque(true);
                                Collections.swap(copy, positionTo + 1, positionTo - 1);
                                break;
                            case -2: // LEFT SIDE
                                copy.set(positionTo - 2, copy.get(positionTo - 2).updateCounter(turns.size()));
                                //ui.getComponents(ui.board, JLabel.class).get(positionTo -2).setOpaque(true);
                                Collections.swap(copy, positionTo - 2, positionTo + 1);
                                break;
                        }
                    }

                    copy.get(positionFrom).getPiece().map(Piece::getPlayer).ifPresent(playerFrom -> { // ADD PIECE (IF PRESENT) TO TAKEN
                        copy.get(positionTo).getPiece().ifPresent(playerFrom::takePiece);
                    });
                }

                copy.set(positionTo, copy.get(positionTo).updatePiece(null).updateCounter(turns.size()));
            }

            copy.set(positionFrom, copy.get(positionFrom).updateCounter(turns.size()));
            Collections.swap(copy, positionFrom, positionTo);

            // PAWN PROMOTION
            Optional.of(positionTo).filter(p -> permanent).filter(p -> UI.getRow(p) == Map.of(Colour.BLACK, 7, Colour.WHITE, 0).get(getCurrentPlayer().getColour())).map(copy::get)
                    .flatMap(Tile::getPiece).filter(Piece.Pawn.class::isInstance)
                    .map(Piece::getPlayer).filter(getCurrentPlayer()::equals).ifPresent(player -> {

                copy.set(positionTo, copy.get(positionTo).updatePiece(new Promotion(player).getStored()));
            });


            this.board = ImmutableList.copyOf(copy);
        }

        public final Game getGame()
        {
            return Game.this;
        }

        public final Integer getPositionFrom()
        {
            return Optional.ofNullable(positionFrom).orElse(0);
        }

        public final Integer getPositionTo()
        {
            return Optional.ofNullable(positionTo).orElse(0);
        }

        final OptionalInt getPosition(final Class<? extends Piece> piece, final Player player) {
            return IntStream.range(0, 64).filter(x -> this.board.get(x).getPiece().filter(piece::isInstance).map(Piece::getPlayer).filter(player::equals).isPresent()).findFirst();
        }

        public final ImmutableList<Tile> getBoard()
        {
            return board;
        }

        public final IntStream getPiecePositions(final Player player)
        {
            return board.stream().filter(x -> x.getPiece().map(Piece::getPlayer).filter(player::equals).isPresent()).mapToInt(board::indexOf);
        }

        public final Stream<Turn> getPotentialTurns(final Player player)
        {
            return this.getPiecePositions(player).boxed().flatMap(this::getPotentialTurns);
        }

        public final Stream<Turn> getPotentialTurns(final int positionFrom)
        {
            return board.get(positionFrom).getPiece().map(piece -> {
                return piece.getVectors().flatMapToInt(vector -> {
                    return IntStream.iterate(positionFrom + vector, Range.closedOpen(0, 64)::contains, i -> i + vector)
                        .takeWhile(i -> piece.validVectors(i - vector, vector))
                        .takeWhile(i -> piece.noCollide(this, positionFrom, i - vector, vector))
                        .takeWhile(i -> piece.noError(this, positionFrom, i - vector, vector))
                        .takeWhile(i -> piece.noCheckBeforeMoving(this, positionFrom, i - vector, vector));
                })
                .mapToObj(i -> new Turn(positionFrom, i, false))
                .filter(piece::noCheckAfterMoving);
            }).orElse(Stream.empty());
        }
    }
}