package com.boomspring.chess;

import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.google.common.primitives.ImmutableIntArray;

/**
 * The essential class in the program.
 */
final class Game {
    public static void main(final String... args) {
        final var game = new Game(ImmutableIntArray.of());
    }

    private final ImmutableList<Player> players = ImmutableList.of(new Player(Colour.BLACK), new Player(Colour.WHITE));
    private final ArrayList<Turn> turns = Lists.newArrayList(new Turn());
    private final UI ui = new UI("Chess");

    /**
     * Starts the {@link Game} with predetermined {@link Game.Turn Turns}
     *
     * @see ImmutableIntArray
     */
    private Game(final ImmutableIntArray values) {
        Lists.partition(values.subArray(0, values.length() & ~(1 << 0)).asList(), 2).forEach(move -> {
            this.makeMove(move.get(0), move.get(1));
        });
    }

    /**
     * Attempts to perform a legal Chess move
     *
     * @param positionFrom - Original Location
     * @param positionTo   - New Location
     */
    private final void makeMove(final int positionFrom, final int positionTo) {
        getCurrentTurn().state.get(positionFrom).getPiece().map(Piece::getPlayer)
                .filter(getCurrentPlayer()::equals)
                .filter(i -> getMoves(getCurrentTurn(), positionFrom).anyMatch(Integer.valueOf(positionTo)::equals))
                .ifPresentOrElse(i -> {
                    turns.add(new Turn(positionFrom, positionTo, true));
                    ui.reset.accept(ui.getComponents(ui.getContentPane(), JLabel.class));

                    final var kingPosition = getCurrentTurn().getPosition(Piece.King.class, getCurrentPlayer()).orElseThrow();

                    // CHECK / CHECKMATE / STALEMATE
                    switch((int) getPlayerMoves(getCurrentTurn(), getNextPlayer()).filter(position -> position == kingPosition).count()) {
                        case 0:
                            if (getPlayerMoves(getCurrentTurn(), getCurrentPlayer()).count() == 0) {
                                // STALEMATE
                                System.out.println("STALEMATE!");
                            }
                            break;
                        case 1:
                            if (getPlayerMoves(getCurrentTurn(), getCurrentPlayer()).count() == 0) {
                                // OLD PLAYER WINS
                                System.out.println("PLAYER " + getNextPlayer().getColor() + " WINS!");
                            } else {
                                // NEW PLAYER IS IN CHECK
                                System.out.println("PLAYER " + getCurrentPlayer().getColor() + " IS IN CHECK!");
                            }
                            break;
                        default:
                            if (getMoves(getCurrentTurn(), kingPosition).count() == 0) {
                                // OLD PLAYER WINS
                                System.out.println("PLAYER " + getNextPlayer().getColor() + " WINS!");
                            } else {
                                // NEW PLAYER IS IN CHECK
                                System.out.println("PLAYER " + getCurrentPlayer().getColor() + " IS IN CHECK!");
                            }

                    }

                }, () -> {
                    System.out.println("Cannot Perform Move");
                });
    }

    final Player getCurrentPlayer() {
        return players.get(turns.size() % 2);
    }

    final Player getNextPlayer() {
        return players.get((turns.size() + 1) % 2);
    }

    final ImmutableList<Turn> getTurns() {
        return ImmutableList.copyOf(turns);
    }

    final Turn getCurrentTurn() {
        return Iterables.getLast(turns);
    }

    /**
     * Obtains all the positions a {@link Player} can move to
     *
     * @param turn   - Includes temporary
     * @param player - Person
     * @return - All possible positions
     */
    final IntStream getPlayerMoves(final Turn turn, final Player player) {
        return IntStream.range(0, 64)
                .filter(i -> turn.state.get(i).getPiece().map(Piece::getPlayer).filter(player::equals).isPresent())
                .flatMap(i -> this.getMoves(turn, i));
    }

    /**
     * Obtains all the positions a {@link Piece} can move to
     *
     * @param turn - Includes temporary
     * @param positionFrom - Original location
     * @return - All possible positions
     */
    final IntStream getMoves(final Turn turn, final int positionFrom) {
        return turn.state.get(positionFrom).getPiece().map(piece -> {
            return piece.getVectors().flatMap(vector -> {
                final var counter = new AtomicInteger();

                return IntStream.iterate(positionFrom + vector, Range.closedOpen(0, 64)::contains, x -> x + vector)
                        .takeWhile(i -> piece.boardException(i - vector, vector))
                        .takeWhile(i -> piece.noCollide().and(piece.noError()).and(piece.noCheck()).test(turn, positionFrom, counter.getAndIncrement(), vector));
            }).sorted().distinct();
        }).orElse(IntStream.empty());
    }

    /**
     * The visual elements of the {@link Game}.
     */
    private final class UI extends JFrame {
        private static final long serialVersionUID = 1L;
        private final AtomicReference<JComponent> stored = new AtomicReference<>();

        /**
         * Loads the interface
         */
        private UI(final String title) {
            super(title);
            this.setLayout(new GridLayout(8, 8));
            this.setMinimumSize(new Dimension(350, 350));
            this.setLocationRelativeTo(null);
            this.setAlwaysOnTop(true);
            this.setDefaultCloseOperation(EXIT_ON_CLOSE);

            IntStream.range(0, 64)
                    .mapToObj(i -> getCurrentTurn().state.get(i).getPiece().map(Piece::toString).orElse("--"))
                    .map(JLabel::new).peek(this::add).forEach(label -> {
                        label.setHorizontalAlignment(SwingConstants.CENTER);
                        label.setVerticalAlignment(SwingConstants.CENTER);
                        label.addMouseListener(new MouseAdapter() {
                            public final void mouseEntered(final MouseEvent e) {
                                reset.andThen(list -> {

                                    if (getCurrentTurn().state.get(list.indexOf(label)).getPiece()
                                            .map(Piece::getPlayer).filter(getCurrentPlayer()::equals)
                                            .filter(x -> Objects.equals(stored.get(), null))
                                            .map(x -> Game.this.getMoves(getCurrentTurn(), list.indexOf(label)))
                                            .orElse(IntStream.empty())
                                            .mapToObj(list::get)
                                            .peek(label -> { // VALID MOVES

                                                label.setOpaque(true);
                                                label.setBackground(Color.RED);
                                                label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                                            })
                                            .count() > 0) { // HOVER

                                                label.setOpaque(true);
                                                label.setBackground(Color.PINK);
                                                label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                                    };

                                }).accept(UI.this.getComponents(getContentPane(), JLabel.class));
                            }

                            public final void mouseExited(final MouseEvent e) {
                                reset.accept(UI.this.getComponents(getContentPane(), JLabel.class));
                            }

                            public final void mouseClicked(final MouseEvent e) {
                                if (label.isOpaque()) {
                                    if (Objects.equals(stored.get(), null)) { // NO VALUE
                                        stored.set(label);
                                    } else if (label.equals(stored.get())) { // RESET VALUE
                                        stored.set(null);
                                    } else { // NEW MOVE
                                        final var list = UI.this.getComponents(getContentPane(), JLabel.class);
                                        Game.this.makeMove(list.indexOf(stored.getAndSet(null)), list.indexOf(label));
                                    }
                                }
                            }
                        });
                    });

            this.pack();
            this.setVisible(true);
        }

        /**
         * Resets the interface
         *
         * Now limits resetting to only opaque regions
         */
        private final Consumer<ImmutableList<JLabel>> reset = list -> {
            if (Objects.equals(stored.get(), null)) {
                list.stream().filter(JLabel::isOpaque).forEach(label -> {
                    label.setOpaque(false);
                    label.setBackground(null);
                    label.setCursor(Cursor.getDefaultCursor());
                    label.setText(getCurrentTurn().state.get(list.indexOf(label)).getPiece().map(Piece::toString).orElse("--"));
                    label.repaint();
                });
            }
        };

        /**
         * Obtains all the {@link JLabel}, each representing a {@link Game.Tile Tile}
         * @param container - Location
         * @param component - Type
         * @return {@link ImmutableList}<{@link Game.Tile Tile}>
         */
        private final <T extends JComponent> ImmutableList<T> getComponents(final Container container, final Class<T> component) {
            return Arrays.stream(UI.this.getContentPane().getComponents()).filter(component::isInstance).map(component::cast).collect(ImmutableList.toImmutableList());
        }
    }

    /**
     * A move that can be evaluated.
     */
    final class Turn {
        private final ImmutableList<Tile> state;
        private final Integer positionFrom;
        private final Integer positionTo;

        /**
         * Initial
         */
        private Turn() {
            this.positionFrom = null;
            this.positionTo = null;
            final var map = ImmutableMultimap.<Supplier<Piece>, Integer>builder()
                    .putAll(() -> new Piece.Rook(players.get(0)), 0, 7)
                    .putAll(() -> new Piece.Knight(players.get(0)), 1, 6)
                    .putAll(() -> new Piece.Bishop(players.get(0)), 2, 5)
                    .put(() -> new Piece.Queen(players.get(0)), 3)
                    .put(() -> new Piece.King(players.get(0)), 4)
                    .putAll(() -> new Piece.Pawn(players.get(0)), 8, 9, 10, 11, 12, 13, 14, 15)
                    .putAll(() -> new Piece.Pawn(players.get(1)), 48, 49, 50, 51, 52, 53, 54, 55)
                    .putAll(() -> new Piece.Rook(players.get(1)), 56, 63)
                    .putAll(() -> new Piece.Knight(players.get(1)), 57, 62)
                    .putAll(() -> new Piece.Bishop(players.get(1)), 58, 61)
                    .put(() -> new Piece.Queen(players.get(1)), 59)
                    .put(() -> new Piece.King(players.get(1)), 60)
                    .build().inverse();

            this.state = IntStream.range(0, 64).mapToObj(i -> Iterables.getFirst(map.get(i), () -> null).get()).map(Tile::new).collect(ImmutableList.toImmutableList());
        }

        /**
         * Temporary
         */
        private Turn(final int positionFrom, final int positionTo, final boolean permanentMove) {
            this.positionFrom = positionFrom;
            this.positionTo = positionTo;

            final var copy = Lists.newArrayList(getCurrentTurn().state);

            if (copy.get(positionFrom).getPiece().filter(Piece.Pawn.class::isInstance).isPresent() && (positionTo - positionFrom) % 8 != 0) { // DIAGONAL PAWN MOVE
                final var passant = positionTo + Map.of(9, -8, 7, -8, -7, 8, -9, 8).getOrDefault(positionTo - positionFrom, 0);

                if (copy.get(positionTo).getPiece().map(Piece::getPlayer).isPresent()) { // NORMAL DIAGONAL
                    if (permanentMove) copy.get(positionFrom).getPiece().map(Piece::getPlayer).ifPresent(x -> x.takePiece(copy.get(positionTo).getPiece().orElseThrow()));
                    copy.set(positionTo, removePiece.andThen(updateCounter).apply(copy.get(positionTo)));
                } else if (Optional.of(copy.get(passant))
                        .filter(i -> Math.abs(getCurrentTurn().getPositionTo().orElse(0) - getCurrentTurn().getPositionFrom().orElse(0)) == 16)
                        .filter(i -> i.getCounter().filter(x -> turns.size() - x == 1).isPresent())
                        .flatMap(Game.Tile::getPiece)
                        .filter(Piece.Pawn.class::isInstance).map(Piece::getPlayer)
                        .filter(i -> positionFrom / 8 == i.getColor().ordinal() + 3)
                        .filter(getNextPlayer()::equals).isPresent()) { // ENPASSANT DIAGONAL

                    ui.getComponents(ui.getContentPane(), JLabel.class).get(passant).setOpaque(true);
                    if (permanentMove) copy.get(positionFrom).getPiece().map(Piece::getPlayer).ifPresent(x -> x.takePiece(copy.get(passant).getPiece().orElseThrow()));
                    copy.set(passant, removePiece.andThen(updateCounter).apply(copy.get(passant)));
                }
            } else { // OTHER MOVES
                if (permanentMove) { // REAL MOVE
                    if (copy.get(positionFrom).getPiece().filter(Piece.King.class::isInstance).isPresent()) { // KING CASTLING
                        switch(positionTo - positionFrom) {
                            case 2: // RIGHT SIDE
                                copy.set(positionTo + 1, updateCounter.apply(copy.get(positionTo + 1)));
                                ui.getComponents(ui.getContentPane(), JLabel.class).get(positionTo + 1).setOpaque(true);
                                Collections.swap(copy, positionTo + 1, positionTo - 1);
                                break;
                            case -2: // LEFT SIDE
                                copy.set(positionTo - 2, updateCounter.apply(copy.get(positionTo - 2)));
                                ui.getComponents(ui.getContentPane(), JLabel.class).get(positionTo -2).setOpaque(true);
                                Collections.swap(copy, positionTo - 2, positionTo + 1);
                                break;
                        }
                    }

                    copy.get(positionFrom).getPiece().map(Piece::getPlayer).ifPresent(playerFrom -> { // ADD PIECE (IF PRESENT) TO TAKEN
                        copy.get(positionTo).getPiece().ifPresent(playerFrom::takePiece);
                    });
                }

                copy.set(positionTo, removePiece.andThen(updateCounter).apply(copy.get(positionTo)));
            }

            copy.set(positionFrom, updateCounter.apply(copy.get(positionFrom)));
            Collections.swap(copy, positionFrom, positionTo);

            this.state = ImmutableList.copyOf(copy);
        }

        public final ImmutableList<Tile> getState() {
            return state;
        }

        final Optional<Integer> getPositionFrom() {
            return Optional.ofNullable(positionFrom);
        }

        final Optional<Integer> getPositionTo() {
            return Optional.ofNullable(positionTo);
        }

        final OptionalInt getPosition(final Class<? extends Piece> piece, final Player player) {
            return IntStream.range(0, 64).filter(x -> this.state.get(x).getPiece().filter(piece::isInstance).map(Piece::getPlayer).filter(player::equals).isPresent()).findFirst();
        }

        final Game getGame() {
            return Game.this;
        }

        final Turn temporaryTurn(final int positionFrom, final int positionTo) {
            return new Turn(positionFrom, positionTo, false);
        }

        private final Function<Tile, Tile> removePiece = tile -> new Tile(null, tile.counter);

        private final Function<Tile, Tile> updateCounter = tile -> new Tile(tile.piece, turns.size());
;
    }

    /**
     * A section of the board, holding a {@link Piece}.
     */
    final class Tile {
        private final Piece piece;
        private final Integer counter;

        private Tile(final Piece piece) {
            this.piece = piece;
            this.counter = null;
        }

        private Tile(final Piece piece, final Integer counter) {
            this.piece = piece;
            this.counter = counter;
        }

        final Optional<Piece> getPiece() {
            return Optional.ofNullable(piece);
        }

        final Optional<Integer> getCounter() {
            return Optional.ofNullable(counter);
        }
    }
}