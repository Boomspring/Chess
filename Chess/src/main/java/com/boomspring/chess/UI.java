package com.boomspring.chess;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;

import com.google.common.collect.ImmutableList;

@SuppressWarnings("serial")
public final class UI extends JFrame
{
    private static final AtomicReference<Game> game = new AtomicReference<>(new Game("Chess"));
    private static final JPanel board = new JPanel(new GridLayout(8, 8));
    private static final AtomicReference<JButton> stored = new AtomicReference<>();
    private static final AtomicReference<Game.Turn> turn = new AtomicReference<>();

    protected UI(final String title)
    {
        super(title);
        this.setMinimumSize(new Dimension(350, 350));
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);

        this.setJMenuBar(new JMenuBar());
        this.getJMenuBar().add(new JMenu("Options"));
        this.getJMenuBar().getMenu(0).add("Reset");
        this.getJMenuBar().getMenu(0).add("Human Game");
        this.getJMenuBar().getMenu(0).add("AI Game");
        this.getJMenuBar().getMenu(0).add("Mixed Game");
        this.add(board);

        // RESET GAME BUTTON
        this.getJMenuBar().getMenu(0).getItem(0).addActionListener(changeGame.apply(game.get().getPlayers()));

        // HUMAN GAME BUTTON
        this.getJMenuBar().getMenu(0).getItem(1).addActionListener(changeGame.apply(ImmutableList.of(new Player.Human(Colour.BLACK), new Player.Human(Colour.WHITE))));

        // AI GAME BUTTON
        this.getJMenuBar().getMenu(0).getItem(2).addActionListener(changeGame.apply(ImmutableList.of(new Player.AI(Colour.BLACK, 3), new Player.AI(Colour.WHITE, 3))));

        // MIXED GAME BUTTON
        this.getJMenuBar().getMenu(0).getItem(3).addActionListener(changeGame.apply(ImmutableList.of(new Player.AI(Colour.BLACK, 3), new Player.Human(Colour.WHITE))));

        IntStream.range(0, 64).mapToObj(i -> game.get().getCurrentTurn().getBoard().get(i).getPiece().map(Piece::toString).orElse("--")).map(JButton::new).peek(board::add).forEach(x -> {
            x.setHorizontalAlignment(0);
            x.setVerticalAlignment(0);
            x.setFocusPainted(false);
            x.setMargin(new Insets(0, 0, 0, 0));
            x.setContentAreaFilled(false);
            x.setOpaque(true);


            x.addActionListener(new ActionListener() {
                @Override
                public final void actionPerformed(final ActionEvent e) {
                    if (Objects.equals(stored.get(), null) && x.getBackground().equals(Color.PINK)) // NO VALUE
                    {
                        stored.set(x);
                    }
                    else if (x.equals(stored.get())) // RESET VALUE
                    {
                        stored.set(null);
                    }
                    else if (x.getBackground().equals(Color.RED)) // NEW MOVE
                    {
                        final List<Component> buttons = Arrays.asList(board.getComponents());
                        turn.set(game.get().new Turn(game.get().getCurrentTurn(), buttons.indexOf(stored.getAndSet(null)), buttons.indexOf(x)));

                        synchronized (game.get().getCurrentPlayer()) {
                            game.get().getCurrentPlayer().notify();
                        }
                    }
                }
            });

            x.addMouseListener(new MouseAdapter() {
                @Override
                public final void mouseEntered(final MouseEvent e) {
                    final List<Component> buttons = Arrays.asList(board.getComponents());
                    final int index = buttons.indexOf(x);
                    final boolean correctPlayer = game.get().getCurrentTurn().getBoard().get(index).getPiece().map(Piece::getPlayer).filter(game.get().getCurrentPlayer()::equals).isPresent();

                    if (Objects.equals(stored.get(), null) && correctPlayer && game.get().getCurrentTurn().getPotentialTurns(index).mapToInt(x -> x.getPositionTo().orElseThrow()).mapToObj(buttons::get).peek(i -> {
                        i.setBackground(Color.RED);
                        i.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    }).count() > 0)
                    {
                        x.setBackground(Color.PINK);
                        x.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    }
                }

                @Override
                public final void mouseExited(final MouseEvent e) {
                    UI.refreshBoard();
                }
            });
        });

        UI.refreshBoard();
        this.setVisible(true);
    }

    public static final Game getGame()
    {
        return game.get();
    }

    public static final int getColumn(final int position)
    {
        return position % 8;
    }

    public static final int getRow(final int position)
    {
        return position / 8;
    }

    public static final Game.Turn useTurn()
    {
        return turn.getAndSet(null);
    }

    public static final void refreshBoard()
    {
        if (Objects.equals(stored.get(), null))
        {
            for(Integer i = 0; i < 64; i++)
            {
                final JButton button = JButton.class.cast(board.getComponent(i));
                button.setCursor(Cursor.getDefaultCursor());
                button.setText(game.get().getCurrentTurn().getBoard().get(i).getPiece().map(Piece::toString).orElse("--"));

                if (game.get().getCurrentTurn().getPositionFrom().filter(i::equals).isPresent()) {
                    button.setBackground(Color.CYAN);
                } else if (game.get().getCurrentTurn().getPositionTo().filter(i::equals).isPresent()) {
                    button.setBackground(Color.YELLOW);
                } else switch (Math.floorMod(UI.getRow(i) + i, 2)) {
                    case 0:
                        button.setBackground(Color.WHITE);
                        break;
                    case 1:
                        button.setBackground(null);
                        break;
                }
            }
        }
    }

    private final Function<ImmutableList<Player>, ActionListener> changeGame = players -> new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            stored.set(null);
            game.get().interrupt();

            while(game.get().isAlive())
            {
                // Do NOTHING
            }

            game.set(new Game("Chess", players));
            game.get().start();
            UI.refreshBoard();
        }
    };

    protected static final class Promotion extends JDialog implements Callable<Piece> {
        private final AtomicReference<Piece> piece = new AtomicReference<>();

        protected Promotion(final Player player) {
            super((Frame) null, "Promotion", true);

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
                        piece.set(Map.of("Rook", new Piece.Rook(player), "Bishop", new Piece.Bishop(player), "Knight", new Piece.Knight(player), "Queen", new Piece.Queen(player)).get(button.getText()));
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