package com.boomspring.chess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public final class Game extends Thread {
    private final ImmutableList<Player> players;
    private final ArrayList<Turn> turns = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("Move").build());
    private final AtomicReference<Integer> status = new AtomicReference<>(-1);
    private final AtomicBoolean playable = new AtomicBoolean(true);

    protected Game(final Class<? extends Player> black, final Class<? extends Player> white)  {
        super("Chess");
        this.players = assignPlayers(black, white);
        this.turns.add(new Turn());
    }

    private Game(final ImmutableList<Player> players)
    {
        super("Chess");
        this.players = players;
        this.turns.add(new Turn());
    }

    @Override
    public void run() {
        while (playable.get()) {
            try {
                final var turn = executor.submit(this.getCurrentPlayer()).get();

                UI.getInstance().getBoard().setEnabled(false);

                this.getCurrentTurn().getBoard().get(turn.positionFrom).getPiece().map(Piece::getPlayer)
                    .filter(getCurrentPlayer()::equals)
                    .flatMap(i -> getCurrentTurn().getPotentialTurns(turn.positionFrom).filter(t -> t.getPositionTo().filter(turn.positionTo::equals).isPresent()).findFirst())
                    .map(Turn::permanentTurn)
                    .stream().peek(turns::add)
                    .findFirst().ifPresentOrElse(this::setStatus, IllegalArgumentException::new);

                UI.getInstance().resetBoard();
            } catch (final Exception e) {
                playable.set(false);
            }
        }

        executor.shutdown();
    }

    protected final void finish()
    {
        executor.shutdownNow();
        this.interrupt();
    }

    protected final Game replay()
    {
        return new Game(players);
    }

    private final ImmutableList<Player> assignPlayers(final Class<? extends Player> black, final Class<? extends Player> white)
    {
        try {
            return ImmutableList.of(black.getDeclaredConstructor(Colour.class).newInstance(Colour.BLACK), white.getDeclaredConstructor(Colour.class).newInstance(Colour.WHITE));
        } catch (final Exception e) {
            return ImmutableList.of(new Player.Human(Colour.BLACK), new Player.Human(Colour.WHITE));
        }
    }

    protected final Player getCurrentPlayer()
    {
        return players.get(turns.size() % 2);
    }

    protected final Player getNextPlayer()
    {
        return players.get((turns.size() + 1) % 2);
    }


    protected final Turn getCurrentTurn()
    {
        return Iterables.getLast(turns);
    }

    protected final int getTurnCount()
    {
        return turns.size();
    }

    protected final String getStatus()
    {
        switch(status.get()) {
            case -1: // Initial State
                return "";
            case 0: // Normal Turn
                return "Turn " + (turns.size() - 1) + ": Successful";
            case 1: // Stalemate
                return "Turn " + (turns.size() - 1) + ": Stalemate";
            case 2: // Current Player is in Check
                return "Turn " + (turns.size() - 1) + ": Player " + getCurrentPlayer().getColour() + " is in Check";
            case 3: // Next Player Wins
                return "Turn " + (turns.size() - 1) + ": Player " + getNextPlayer().getColour() + " Wins";
            case 4:
                return "Turn " + (turns.size() - 1) + ": Stalemate (100 Turn Rule)";
            default: // Exceptions
                return null;
        }
    }

    protected final void setStatus(final Turn turn)
    {
        // 50 MOVE RULE
        if (turns.size() >= 101 && Lists.partition(turns.subList(turns.size() - 100, turns.size()), 2).stream().filter(x -> {
                return x.get(0).board.get(x.get(1).positionTo).getPiece().or(() -> {
                    return x.get(0).board.get(x.get(0).positionFrom).getPiece().filter(Piece.Pawn.class::isInstance);
                }).isPresent();
            }).count() == 0)
        {
            status.set(4);
            playable.set(false);
            return;
        }

        final var kingPosition = getCurrentTurn().getPosition(Piece.King.class, getCurrentPlayer()).orElseThrow();

        // CHECK / CHECKMATE / STALEMATE
        switch((int) getCurrentTurn().getPotentialTurns(getNextPlayer()).mapToInt(x -> x.positionTo).filter(position -> position == kingPosition).count()) {
            case 0:
                getCurrentTurn().getPotentialTurns(getCurrentPlayer()).findAny().ifPresentOrElse(x -> {
                    status.set(0);
                }, () -> {
                    status.set(1);
                    playable.set(false);
                });
                break;
            case 1:
                getCurrentTurn().getPotentialTurns(getCurrentPlayer()).findAny().ifPresentOrElse(x -> {
                    status.set(2);
                }, () -> {
                    status.set(3);
                    playable.set(false);
                });
                break;
            default:
                getCurrentTurn().getPotentialTurns(kingPosition).findAny().ifPresentOrElse(x -> {
                    status.set(2);
                }, () -> {
                    status.set(3);
                    playable.set(false);
                });
        }
    }

    protected final class Turn
    {
        private final Integer positionFrom;
        private final Integer positionTo;
        private final int value;
        private final ImmutableList<Tile> board;

        private Turn()
        {
            this.positionFrom = null;
            this.positionTo = null;
            this.value = 0;
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

        protected Turn(final int positionFrom, final int positionTo, final boolean permanent)
        {
            this.positionFrom = positionFrom;
            this.positionTo = positionTo;

            final var copy = new ArrayList<>(getCurrentTurn().board);

            if (copy.get(positionFrom).getPiece().filter(Piece.Pawn.class::isInstance).isPresent()) { // DIAGONAL PAWN MOVE
                final var passant = positionTo + Map.of(9, -8, 7, -8, -7, 8, -9, 8).getOrDefault(positionTo - positionFrom, 0);

                if (copy.get(positionTo).getPiece().map(Piece::getPlayer).isEmpty() && Optional.of(copy.get(passant))
                        .filter(i -> Math.abs(getCurrentTurn().getPositionTo().orElse(0) - getCurrentTurn().getPositionFrom().orElse(0)) == 16)
                        .filter(i -> i.getCounter().filter(x -> turns.size() - x == 1).isPresent())
                        .flatMap(x -> x.getPiece())
                        .filter(Piece.Pawn.class::isInstance).map(Piece::getPlayer)
                        .filter(i -> UI.getRow(positionFrom) == i.getColour().ordinal() + 3)
                        .filter(getNextPlayer()::equals).isPresent()) { // ENPASSANT DIAGONAL

                    this.value = assignValue.apply(copy, passant);
                } else { // NORMAL DIAGONAL OR STRAIGHT

                    this.value = assignValue.apply(copy, positionTo);
                }
            } else { // OTHER MOVES
                if (copy.get(positionFrom).getPiece().filter(Piece.King.class::isInstance).isPresent()) { // KING CASTLING
                    switch(positionTo - positionFrom) {
                        case 2: // RIGHT SIDE
                            copy.set(positionTo + 1, copy.get(positionTo + 1).updateCounter(turns.size()));
                            Collections.swap(copy, positionTo + 1, positionTo - 1);
                            break;
                        case -2: // LEFT SIDE
                            copy.set(positionTo - 2, copy.get(positionTo - 2).updateCounter(turns.size()));
                            Collections.swap(copy, positionTo - 2, positionTo + 1);
                            break;
                    }
                }

                this.value = assignValue.apply(copy, positionTo);
            }

            copy.set(positionFrom, copy.get(positionFrom).updateCounter(turns.size()));
            Collections.swap(copy, positionFrom, positionTo);

            // PAWN PROMOTION
            Optional.of(positionTo).filter(p -> permanent).filter(p -> UI.getRow(p) == Map.of(Colour.BLACK, 7, Colour.WHITE, 0).get(getCurrentPlayer().getColour())).map(copy::get)
                    .flatMap(Tile::getPiece).filter(Piece.Pawn.class::isInstance)
                    .map(Piece::getPlayer).filter(getCurrentPlayer()::equals).ifPresent(player -> {
                        if (Player.Human.class.isInstance(player)) {
                            copy.set(positionTo, copy.get(positionTo).updatePiece(new UI.Promotion(player).call()));
                        } else {
                            copy.set(positionTo, copy.get(positionTo).updatePiece(new Piece.Queen(player)));
                        }
            });


            this.board = ImmutableList.copyOf(copy);
        }

        protected final OptionalInt getPosition(final Class<? extends Piece> piece, final Player player) {
            return IntStream.range(0, 64).filter(x -> this.board.get(x).getPiece().filter(piece::isInstance).map(Piece::getPlayer).filter(player::equals).isPresent()).findFirst();
        }

        protected final Optional<Integer> getPositionFrom()
        {
            return Optional.ofNullable(positionFrom);
        }

        protected final Optional<Integer> getPositionTo()
        {
            return Optional.ofNullable(positionTo);
        }

        protected final ImmutableList<Tile> getBoard()
        {
            return board;
        }

        private final IntStream getPiecePositions(final Player player)
        {
            return board.stream().filter(x -> x.getPiece().map(Piece::getPlayer).filter(player::equals).isPresent()).mapToInt(board::indexOf);
        }

        protected final Stream<Turn> getPotentialTurns(final Player player)
        {
            return this.getPiecePositions(player).boxed().flatMap(this::getPotentialTurns);
        }

        protected final Stream<Turn> getPotentialTurns(final int positionFrom)
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

        private final BiFunction<ArrayList<Tile>, Integer, Integer> assignValue = (list, position) ->
        {
            final var value = list.get(position).getPiece().map(Piece::getValue).orElse(0);
            list.set(position, list.get(position).updatePiece(null).updateCounter(turns.size()));
            return value;
        };

        protected final int getValue()
        {
            return value;
        }

        private final Turn permanentTurn()
        {
            return new Turn(this.positionFrom, this.positionTo, true);
        }
    }
}