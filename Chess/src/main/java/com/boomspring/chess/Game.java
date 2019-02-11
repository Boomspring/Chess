package com.boomspring.chess;

import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.stream.*;

import javax.swing.*;

import com.google.common.base.Predicates;
import com.google.common.collect.*;
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
        final var trimmed = values.subArray(0, values.length() - (values.length() % 2)).trimmed();

        for (int i = 0; i < trimmed.length(); i += 2) {
            makeMove(trimmed.get(i), trimmed.get(i + 1));
        }
    }

    /**
     * Attempts to perform a legal Chess move
     *
     * @param positionFrom - Original Location
     * @param positionTo   - New Location
     */
    private final void makeMove(final int positionFrom, final int positionTo) {
        Iterables.getLast(turns).state.get(positionFrom).getPiece().map(Piece::getPlayer)
                .filter(players.get(turns.size() % 2)::equals).flatMap(i -> {
                    return this.getMoves(Iterables.getLast(turns), positionFrom)
                            .filter(Integer.valueOf(positionTo)::equals).mapToObj(x -> x).findFirst();
                }).ifPresentOrElse(i -> {
                    turns.add(new Turn(positionFrom, positionTo, true));
                    ui.reset.accept(ui.getComponents(ui.getContentPane(), JLabel.class));
                    System.out.println("Can Perform Move");
                }, () -> {
                    System.out.println("Cannot Perform Move");
                });
    }

    final ImmutableList<Player> getPlayers() {
        return players;
    }

    final ImmutableList<Turn> getTurns() {
        return ImmutableList.copyOf(turns);
    }

    /**
     * Obtains all the positions a {@link Player} can move to
     *
     * @param turn   - Includes temporary
     * @param player
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
     * @param turn         - Includes temporary
     * @param positionFrom - Original location
     * @return - All possible positions
     */
    final IntStream getMoves(final Turn turn, final int positionFrom) {
        return turn.state.get(positionFrom).getPiece().map(piece -> {
            return piece.getVectors().flatMap(vector -> {
                final var counter = new AtomicInteger();
                return IntStream.iterate(positionFrom + vector, Range.closedOpen(0, 64)::contains, x -> x + vector)
                        .takeWhile(i -> piece.boardException(i - vector, vector))
                        .takeWhile(i -> piece.noCollide().and(piece.noError()).and(piece.noCheck()).test(turn,
                                positionFrom, counter.getAndIncrement(), vector));
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
                    .mapToObj(i -> Iterables.getLast(turns).state.get(i).getPiece().map(Piece::toString).orElse("--"))
                    .map(JLabel::new).peek(this::add).forEach(label -> {
                        label.setHorizontalAlignment(SwingConstants.CENTER);
                        label.setVerticalAlignment(SwingConstants.CENTER);
                        label.addMouseListener(new MouseAdapter() {
                            public final void mouseEntered(final MouseEvent e) {
                                reset.andThen(list -> {
                                    if (Objects.equals(stored.get(), null) && Iterables.getLast(turns).state.get(list.indexOf(label)).getPiece()
                                            .map(Piece::getPlayer).filter(players.get(turns.size() % 2)::equals)
                                            .isPresent()) {
                                        Game.this.getMoves(Iterables.getLast(turns), list.indexOf(label))
                                                .mapToObj(list::get).forEach(label -> {
                                                    label.setOpaque(true);
                                                    label.setBackground(Color.RED);
                                                });

                                        label.setOpaque(true);
                                        label.setBackground(Color.PINK);
                                        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                                    }
                                }).accept(UI.this.getComponents(getContentPane(), JLabel.class));
                            }

                            public final void mouseExited(final MouseEvent e) {
                                reset.accept(UI.this.getComponents(getContentPane(), JLabel.class));
                            }

                            public final void mouseClicked(final MouseEvent e) {
                                if (Objects.equals(stored.get(), null)) {
                                    stored.set(label);
                                } else if (stored.get().equals(label)) {
                                    stored.set(null);
                                } else if (label.getBackground().equals(Color.RED)) {
                                    final var list = UI.this.getComponents(getContentPane(), JLabel.class);
                                    Game.this.makeMove(list.indexOf(stored.getAndSet(null)), list.indexOf(label));
                                }
                            }
                        });
                    });

            this.pack();
            this.setVisible(true);
        }

        /**
         * Resets the interface
         */
        private final Consumer<ImmutableList<JLabel>> reset = list -> list.forEach(label -> {
            if (Objects.equals(stored.get(), null)) {
                label.setOpaque(false);
                label.setBackground(null);
                label.setCursor(Cursor.getDefaultCursor());
                label.setText(Iterables.getLast(turns).state.get(list.indexOf(label)).getPiece().map(Piece::toString).orElse("--"));
                label.repaint();
            }
        });

        /**
         * Obtains all the {@link JLabel}, each representing a {@link Game.Tile Tile}         *
         * @param container - Location
         * @param component - Type
         * @return {@link ImmutableList}<{@link Game.Tile Tile}>
         */
        private final <T extends JComponent> ImmutableList<T> getComponents(final Container container,
                final Class<T> component) {
            return Arrays.stream(UI.this.getContentPane().getComponents()).filter(component::isInstance)
                    .map(component::cast).collect(ImmutableList.toImmutableList());
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
                    .putAll(() -> new Piece.Bishop(players.get(0)), 2, 5).put(() -> new Piece.Queen(players.get(0)), 3)
                    .put(() -> new Piece.King(players.get(0)), 4)
                    .putAll(() -> new Piece.Pawn(players.get(0)), 8, 9, 10, 11, 12, 13, 14, 15)
                    .putAll(() -> new Piece.Pawn(players.get(1)), 48, 49, 50, 51, 52, 53, 54, 55)
                    .putAll(() -> new Piece.Rook(players.get(1)), 56, 63)
                    .putAll(() -> new Piece.Knight(players.get(1)), 57, 62)
                    .putAll(() -> new Piece.Bishop(players.get(1)), 58, 61)
                    .put(() -> new Piece.Queen(players.get(1)), 59).put(() -> new Piece.King(players.get(1)), 60)
                    .build().inverse();

            this.state = IntStream.range(0, 64).mapToObj(i -> Iterables.getFirst(map.get(i), () -> null).get())
                    .map(Tile::new).collect(ImmutableList.toImmutableList());
        }

        /**
         * Temporary
         */
        Turn(final int positionFrom, final int positionTo, final boolean permanentMove) {
            this.positionFrom = positionFrom;
            this.positionTo = positionTo;

            final var copy = Lists.newArrayList(Iterables.getLast(turns).state);

            // EN PASSANT CHECK
            if (copy.get(positionFrom).getPiece().filter(Piece.Pawn.class::isInstance).isPresent() && (positionTo - positionFrom) % 8 != 0) {
                final var passant = positionTo + Map.of(9, -8, 7, -8, -7, 8, -9, 8).getOrDefault(positionTo - positionFrom, 0);

                if (copy.get(positionTo).getPiece().map(Piece::getPlayer).isPresent()) {
                    // NORMAL DIAGONAL
                    if (permanentMove) copy.get(positionFrom).getPiece().map(Piece::getPlayer).ifPresent(x -> x.takePiece(copy.get(positionTo).getPiece().orElseThrow()));
                    copy.set(positionTo, removePiece.andThen(updateCounter).apply(copy.get(positionTo)));
                } else if (Optional.of(copy.get(passant))
                        .filter(i -> Math.abs(Iterables.getLast(turns).getPositionTo().orElse(0) - Iterables.getLast(turns).getPositionFrom().orElse(0)) == 16)
                        .filter(i -> i.getCounter().filter(x -> turns.size() - x == 1).isPresent())
                        .flatMap(Game.Tile::getPiece).filter(Piece.Pawn.class::isInstance).map(Piece::getPlayer)
                        .filter(i -> positionFrom / 8 == i.getColor().ordinal() + 3)
                        .filter(Predicates.not(players.get(turns.size() % 2)::equals)).isPresent()) {

                    // ENPASSANT DIAGONAL
                    if (permanentMove) copy.get(positionFrom).getPiece().map(Piece::getPlayer).ifPresent(x -> x.takePiece(copy.get(passant).getPiece().orElseThrow()));
                    copy.set(passant, removePiece.andThen(updateCounter).apply(copy.get(passant)));
                }
            } else if (copy.get(positionFrom).getPiece().filter(Piece.King.class::isInstance).isPresent() && permanentMove) {
                switch(positionTo - positionFrom) {
                    case 2: // RIGHT SIDE
                        copy.set(positionTo + 1, updateCounter.apply(copy.get(positionTo + 1)));
                        Collections.swap(copy, positionTo + 1, positionTo - 1);
                        break;
                    case -2: // LEFT SIDE
                        copy.set(positionTo - 2, updateCounter.apply(copy.get(positionTo - 2)));
                        Collections.swap(copy, positionTo - 2, positionTo + 1);
                        break;
                }
                // DEFAULT KING ACTION
                copy.set(positionTo, removePiece.andThen(updateCounter).apply(copy.get(positionTo)));

            } else {
                if (copy.get(positionTo).getPiece().map(Piece::getPlayer).isPresent() && permanentMove) copy.get(positionFrom).getPiece().map(Piece::getPlayer).ifPresent(x -> x.takePiece(copy.get(positionTo).getPiece().orElseThrow()));
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

        final Game getGame() {
            return Game.this;
        }

        private final Function<Tile, Tile> removePiece = tile -> {
            return new Tile(null, tile.counter);
        };

        private final Function<Tile, Tile> updateCounter = tile -> {
            return new Tile(tile.piece, turns.size());
        };
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