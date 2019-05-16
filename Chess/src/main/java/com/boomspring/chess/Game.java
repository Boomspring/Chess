package com.boomspring.chess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;

public final class Game extends Thread
{
    private final ImmutableList<Player> players;
    private final ArrayList<Turn> turns;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    protected Game(final String title)
    {
        super(title);
        this.players = ImmutableList.of(new Player.AI(Colour.BLACK, 3), new Player.AI(Colour.WHITE, 3));
        this.turns = Lists.newArrayList(new Turn());
    }

    protected Game(final String title, final ImmutableList<Player> players)
    {
        super(title);
        this.players = players;
        this.turns = Lists.newArrayList(new Turn());
    }

    public static final void main(final String... args) throws Exception
    {
        new UI("Chess");
        UI.getGame().start();
    }

    public final Player getCurrentPlayer()
    {
        return players.get(turns.size() % 2);
    }

    public final Player getNextPlayer()
    {
        return players.get((turns.size() + 1) % 2);
    }

    public final ImmutableList<Player> getPlayers()
    {
        return players;
    }

    public final ImmutableList<Turn> getTurns()
    {
        return ImmutableList.copyOf(turns);
    }

    public final Turn getCurrentTurn()
    {
        return Iterables.getLast(turns);
    }

    public synchronized final void run()
    {
        try {
            while(!isInterrupted()) {
                final Turn turn = executor.submit(getCurrentPlayer()).get();

                this.getCurrentTurn().getBoard().get(turn.positionFrom).getPiece().map(Piece::getPlayer)
                    .filter(getCurrentPlayer()::equals)
                    .flatMap(i -> getCurrentTurn().getPotentialTurns(turn.positionFrom).filter(t -> t.getPositionTo().filter(turn.positionTo::equals).isPresent()).findFirst())
                    .map(Turn::new)
                    .stream().peek(turns::add)
                    .findFirst().ifPresentOrElse(x -> System.out.println("Move Successful"), IllegalArgumentException::new);

                UI.refreshBoard();
            }
        } catch(final Exception e) {
            e.printStackTrace();
            executor.shutdownNow();
        }
    }

    public interface Rules
    {
        public default long calculateLimit(final Turn turn, final int positionFrom, final int vector)
        {
            return ((Piece) this).getRange().orElse(7);
        }

        public abstract boolean validVector(final int positionCurrent, final int vector);

        public default boolean noCollide(final Turn turn, final int positionFrom, final int positionCurrent, final int vector)
        {
            return turn.getBoard().get(positionCurrent + vector).getPiece().map(Piece::getPlayer).filter(((Piece) this).getPlayer()::equals)
                .or(() -> turn.getBoard().get(positionCurrent).getPiece().filter(Predicates.not(((Piece) this)::equals)).map(Piece::getPlayer))
                .isEmpty();
        }

        public default boolean noCheckBeforeMoving(final Turn turn, final int positionFrom, final int positionCurrent, final int vector)
        {
            if (((Piece) this).getPlayer().equals(UI.getGame().getCurrentPlayer()))
            {
                return !((positionCurrent - positionFrom == vector) && Piece.King.class.isInstance(this) && turn.getPotentialTurns(UI.getGame().getNextPlayer()).mapToInt(x -> x.getPositionTo().orElse(0)).anyMatch(i -> positionFrom == i));
            }
            else return true;
        }

        public default boolean noCheckAfterMoving(final Turn turn, final int positionFrom)
        {
            if (((Piece) this).getPlayer().equals(UI.getGame().getCurrentPlayer()))
            {
                return turn.getPotentialTurns(UI.getGame().getNextPlayer()).mapToInt(x -> x.getPositionTo().orElse(0)).noneMatch(x -> x == turn.getPosition(Piece.King.class, UI.getGame().getCurrentPlayer()).orElse(-1));
            }
            else return true;
        }
    }

    public final class Turn
    {
        private final Integer positionFrom;
        private final Integer positionTo;
        private final ImmutableList<Tile> board;

        private Turn()
        {
            this.positionFrom = null;
            this.positionTo = null;
            this.board = IntStream.range(0, 64).mapToObj(i -> { switch(i) {
                case 0:  case 7: return new Piece.Rook(players.get(0));
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
            }}).map(Tile::new).collect(ImmutableList.toImmutableList());
        }

        private Turn(final Turn turn)
        {
            this.positionFrom = turn.positionFrom;
            this.positionTo = turn.positionTo;

            final ArrayList<Tile> copy = new ArrayList<>(turn.board);

            // PAWN PROMOTION
            Optional.of(positionTo).filter(p -> UI.getRow(p) == Map.of(Colour.BLACK, 7, Colour.WHITE, 0).get(getCurrentPlayer().getColour())).map(copy::get)
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

        protected Turn(final Turn turn, final int positionFrom, final int positionTo)
        {
            this.positionFrom = positionFrom;
            this.positionTo = positionTo;

            final ArrayList<Tile> copy = new ArrayList<>(turn.board);

            if (copy.get(positionFrom).getPiece().filter(Piece.Pawn.class::isInstance).isPresent()) { // DIAGONAL PAWN MOVE
                final int passant = positionTo + Map.of(9, -8, 7, -8, -7, 8, -9, 8).getOrDefault(positionTo - positionFrom, 0);

                if (copy.get(positionTo).getPiece().map(Piece::getPlayer).isEmpty() && Optional.of(copy.get(passant))
                        .filter(i -> Math.abs(getCurrentTurn().getPositionTo().orElse(0) - getCurrentTurn().getPositionFrom().orElse(0)) == 16)
                        .filter(i -> i.getCounter().filter(x -> turns.size() - x == 1).isPresent())
                        .flatMap(x -> x.getPiece())
                        .filter(Piece.Pawn.class::isInstance).map(Piece::getPlayer)
                        .filter(i -> UI.getRow(positionFrom) == i.getColour().ordinal() + 3)
                        .filter(getNextPlayer()::equals).isPresent()) { // ENPASSANT DIAGONAL

                    copy.set(passant, copy.get(passant).updatePiece(null).updateCounter(turns.size()));
                } else { // NORMAL DIAGONAL OR STRAIGHT

                    copy.set(positionTo, copy.get(positionTo).updatePiece(null).updateCounter(turns.size()));
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

                copy.set(positionTo, copy.get(positionTo).updatePiece(null).updateCounter(turns.size()));
            }

            copy.set(positionFrom, copy.get(positionFrom).updateCounter(turns.size()));
            Collections.swap(copy, positionFrom, positionTo);



            this.board = ImmutableList.copyOf(copy);
        }

        public final Optional<Integer> getPositionFrom()
        {
            return Optional.ofNullable(positionFrom);
        }

        public final Optional<Integer> getPositionTo()
        {
            return Optional.ofNullable(positionTo);
        }

        protected final OptionalInt getPosition(final Class<? extends Piece> piece, final Player player)
        {
            return IntStream.range(0, 64).filter(x -> this.board.get(x).getPiece().filter(piece::isInstance).map(Piece::getPlayer).filter(player::equals).isPresent()).findFirst();
        }

        public final ImmutableList<Tile> getBoard()
        {
            return board;
        }

        protected final Stream<Turn> getPotentialTurns(final Player player)
        {
            return board.stream().filter(x -> x.getPiece().map(Piece::getPlayer).filter(player::equals).isPresent()).mapToInt(board::indexOf).boxed().flatMap(this::getPotentialTurns);
        }

        protected final Stream<Turn> getPotentialTurns(final int positionFrom)
        {
            return board.get(positionFrom).getPiece().map(piece -> {
                return piece.getVectors().flatMapToInt(vector -> {
                    return IntStream.iterate(positionFrom + vector, Range.closedOpen(0, 64)::contains, i -> i + vector)
                        .limit(piece.calculateLimit(this, positionFrom, vector))
                        .takeWhile(i -> piece.validVector(i - vector, vector))
                        .takeWhile(i -> piece.noCollide(this, positionFrom, i - vector, vector))
                        .takeWhile(i -> piece.noCheckBeforeMoving(this, positionFrom, i - vector, vector));
                })
                .sorted().distinct()
                .mapToObj(i -> new Turn(this, positionFrom, i))
                .filter(i -> piece.noCheckAfterMoving(i, positionFrom));
            }).orElse(Stream.empty());
        }

        protected final int calculateValue()
        {
            final int totalWhite = board.stream().map(Tile::getPiece).flatMap(Optional::stream).filter(x -> x.getPlayer().getColour().equals(Colour.WHITE)).mapToInt(x -> x.getValue().orElseThrow()).sum();
            final int totalBlack = board.stream().map(Tile::getPiece).flatMap(Optional::stream).filter(x -> x.getPlayer().getColour().equals(Colour.BLACK)).mapToInt(x -> x.getValue().orElseThrow()).sum();

            return totalWhite - totalBlack;
        }
    }
}