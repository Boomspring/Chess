package com.boomspring.chess;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JToolBar;

public final class UI extends JFrame
{
    private static final long serialVersionUID = 1L;
    private final JPanel board = new JPanel(new GridLayout(8, 8));
    private final JLabel message = new JLabel();
    private final AtomicReference<Game> game = new AtomicReference<>(new Game());
    private final AtomicReference<JLabel> stored = new AtomicReference<>();

    public static void main(final String... args) {
        new UI("Chess");
    }

    private UI(final String title)
    {
        super(title);

        IntStream.range(0, 64).mapToObj(x -> game.get().getCurrentTurn().getBoard().get(x).getPiece().map(Piece::toString).orElse(null)).map(JLabel::new).peek(board::add).forEach(x ->
        {
            x.setOpaque(true);
            x.setHorizontalAlignment(0);
            x.setVerticalAlignment(0);
            x.setBorder(BorderFactory.createLoweredBevelBorder());
            x.addMouseListener(new MouseAdapter()
            {
                public final void mouseEntered(final MouseEvent e)
                {
                    final var list = Arrays.asList(board.getComponents());

                    if (Objects.equals(stored.get(), null) && Optional.of(list.indexOf(x))
                        .filter(i -> game.get().getCurrentTurn().getBoard().get(i).getPiece().map(Piece::getPlayer).filter(game.get().getCurrentPlayer()::equals).isPresent())
                        .map(game.get().getCurrentTurn()::getPotentialTurns)
                        .orElse(Stream.empty())
                        .mapToInt(Game.Turn::getPositionTo)
                        .mapToObj(board::getComponent)
                        .peek(x ->
                        {
                            x.setBackground(Color.RED);
                            x.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        }).count() > 0)
                        {
                            x.setBackground(Color.PINK);
                            x.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        };
                }

                public final void mouseExited(final MouseEvent e)
                {
                    UI.this.resetBoard();
                }

                public final void mouseClicked(final MouseEvent e)
                {
                    if (Objects.equals(stored.get(), null) && x.getBackground().equals(Color.PINK)) // NO VALUE
                    {
                        stored.set(x);
                    } else if (x.equals(stored.get())) // RESET VALUE
                    {
                        stored.set(null);
                    } else if (x.getBackground().equals(Color.RED)) // NEW MOVE
                    {
                        try
                        {
                            final var list = Arrays.asList(board.getComponents());
                            game.get().makeMove(list.indexOf(stored.getAndSet(null)), list.indexOf(x));
                        } finally
                        {
                            UI.this.resetBoard();
                        }
                    }
                }
            });
        });

        final var menu = new JMenuBar();
        final var options = new JMenu("Options");

        menu.add(options);
        menu.add(Box.createHorizontalGlue());
        menu.add(message);
        menu.add(new JToolBar.Separator());

        options.add("New Game");
        options.getItem(0).addActionListener(new ActionListener()
        {
			@Override
			public void actionPerformed(ActionEvent e) {
                game.set(new Game());
                UI.this.resetBoard();
			}
        });

        this.setMinimumSize(new Dimension(350, 350));
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setJMenuBar(menu);
        this.resetBoard();
        this.add(board);
        this.setVisible(true);
    }

    private final void resetBoard()
    {
        message.setText(game.get().getStatus());

        if (Objects.equals(stored.get(), null))
        {
            for(int i = 0; i < 64; i++)
            {
                final var component = JLabel.class.cast(board.getComponent(i));
                component.setCursor(Cursor.getDefaultCursor());
                component.setText(game.get().getCurrentTurn().getBoard().get(i).getPiece().map(Piece::toString).orElse(null));

                switch (Math.floorMod(getRow(i) + i, 2))
                {
                    case 0:
                        component.setBackground(Color.WHITE);
                        break;
                    case 1:
                        component.setBackground(null);
                        break;
                }
            }
        }

        if (game.get().hasEnded())
        {
            board.setEnabled(false);
        }
    }

    public static final int getRow(final int i)
    {
        return i / 8;
    }

    public static final int getColumn(final int i)
    {
        return i % 8;
    }
}