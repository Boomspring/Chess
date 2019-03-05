package com.boomspring.chess;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JToolBar;

@SuppressWarnings("serial")
public final class UI extends JFrame {
    private static final UI INTERFACE = new UI("Chess");

    private final AtomicReference<Game> game = new AtomicReference<>(new Game(Player.AI.class, Player.Human.class));
    private final AtomicReference<Game.Turn> turn = new AtomicReference<>();
    private final AtomicReference<JButton> stored = new AtomicReference<>();

    private final JLabel message = new JLabel();
    private final JPanel board = new JPanel(new GridLayout(8, 8));

    public static final void main(final String... args) {
        UI.getInstance().game.get().start();
    }

    private UI() {
    }

    private UI(final String title) {
        super(title);

        final var menu = new JMenuBar();
        final var options = new JMenu("Options");
        final var reset = new JMenuItem("New Game");

        menu.add(options);
        menu.add(Box.createHorizontalGlue());
        menu.add(message);
        menu.add(new JToolBar.Separator());
        options.add(reset);

        reset.addActionListener(new ActionListener() {
            @Override
            public final void actionPerformed(final ActionEvent e) {
                stored.set(null);
                game.getAndSet(game.get().replay()).finish();
                game.get().start();
                UI.this.resetBoard();
            }
        });

        IntStream.range(0, 64)
                .mapToObj(
                        x -> game.get().getCurrentTurn().getBoard().get(x).getPiece().map(Piece::toString).orElse(null))
                .map(JButton::new).peek(board::add).forEach(button -> {
                    button.setHorizontalAlignment(0);
                    button.setVerticalAlignment(0);
                    button.setFocusPainted(false);
                    button.setMargin(new Insets(0, 0, 0, 0));
                    button.setContentAreaFilled(false);
                    button.setOpaque(true);

                    button.addActionListener(new ActionListener() {
                        @Override
                        public final void actionPerformed(final ActionEvent e) {
                            if (Objects.equals(stored.get(), null) && button.getBackground().equals(Color.PINK)) // NO VALUE
                            {
                                stored.set(button);
                            } else if (button.equals(stored.get())) // RESET VALUE
                            {
                                stored.set(null);
                            } else if (button.getBackground().equals(Color.RED)) // NEW MOVE
                            {
                                final var list = Arrays.asList(board.getComponents());
                                turn.set(game.get().new Turn(list.indexOf(stored.getAndSet(null)), list.indexOf(button), false));

                                synchronized (game.get().getCurrentPlayer()) {
                                    game.get().getCurrentPlayer().notify();
                                }
                            }
                        }
                    });

                    button.addMouseListener(new MouseAdapter() {
                        @Override
                        public final void mouseEntered(final MouseEvent e) {
                            if (Objects.equals(stored.get(), null) && board.isEnabled()
                                    && Optional.of(Arrays.asList(board.getComponents()).indexOf(button))
                                            .filter(i -> game.get().getCurrentTurn().getBoard().get(i).getPiece()
                                                    .map(Piece::getPlayer).filter(game.get().getCurrentPlayer()::equals)
                                                    .isPresent())
                                            .map(game.get().getCurrentTurn()::getPotentialTurns).orElse(Stream.empty())
                                            .mapToInt(x -> x.getPositionTo().orElse(0)).mapToObj(board::getComponent)
                                            .peek(x -> {
                                                x.setBackground(Color.RED);
                                                x.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                                            }).count() > 0) {
                                button.setBackground(Color.PINK);
                                button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                            }
                            ;
                        }

                        @Override
                        public final void mouseExited(final MouseEvent e) {
                            UI.this.resetBoard();
                        }
                    });
                });

        UI.this.resetBoard();

        this.setMinimumSize(new Dimension(350, 350));
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setJMenuBar(menu);
        this.add(board);
        this.pack();
        this.setVisible(true);
    }

    protected static final UI getInstance() {
        return INTERFACE;
    }

    protected final JPanel getBoard() {
        return board;
    }

    protected final void resetBoard() {
        message.setText(game.get().getStatus());

        if (Objects.equals(stored.get(), null)) {
            for (Integer i = 0; i < 64; i++) {
                final var component = JButton.class.cast(board.getComponent(i));
                component.setCursor(Cursor.getDefaultCursor());
                component.setText(game.get().getCurrentTurn().getBoard().get(i).getPiece().map(Piece::toString).orElse(null));

                if (game.get().getCurrentTurn().getPositionFrom().filter(i::equals).isPresent()) {
                    component.setBackground(Color.CYAN);
                } else if (game.get().getCurrentTurn().getPositionTo().filter(i::equals).isPresent()) {
                    component.setBackground(Color.YELLOW);
                } else switch (Math.floorMod(UI.getRow(i) + i, 2)) {
                    case 0:
                        component.setBackground(Color.WHITE);
                        break;
                    case 1:
                        component.setBackground(null);
                        break;
                }
            }
        }
    }

    protected final Game getGame() {
        return game.get();
    }

    protected final Game.Turn useTurn() {
        return turn.getAndSet(null);
    }

    protected static final int getRow(final int i) {
        return i / 8;
    }

    protected static final int getColumn(final int i) {
        return i % 8;
    }

    protected static final class Promotion extends JDialog implements Callable<Piece> {
        private final AtomicReference<Piece> piece = new AtomicReference<>();

        protected Promotion(final Player player) {
            super(UI.getInstance(), "Promotion", true);

            this.setLayout(new GridLayout(2, 2));
            this.setSize(350, 200);
            this.setResizable(false);
            this.setLocationRelativeTo(null);
            this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

            Stream.of("Rook", "Bishop", "Knight", "Queen").map(JButton::new).peek(this::add).forEach(button -> {
                button.setHorizontalAlignment(0);
                button.setVerticalAlignment(0);
                button.setFocusPainted(false);
                button.setMargin(new Insets(0, 0, 0, 0));
                button.setContentAreaFilled(false);
                button.setOpaque(true);

                button.addActionListener(new ActionListener()
                {
                    @Override
                    public final void actionPerformed(final ActionEvent e) {
                        try {
                            piece.set((Piece) Class.forName("com.boomspring.chess.Piece$" + button.getText()).getDeclaredConstructor(Player.class).newInstance(player));
                        } catch (final Exception x) {
                            piece.set(new Piece.Queen(player));
                        }
                        Promotion.this.dispose();
                    }
                });
            });

            this.setVisible(true);
        }

        @Override
        public final Piece call() {
            return piece.get();
        }
    }
}