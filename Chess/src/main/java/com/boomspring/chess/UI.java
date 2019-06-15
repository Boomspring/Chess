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
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;

import com.google.common.collect.Streams;

@SuppressWarnings("serial")
public final class UI extends JFrame implements Callable<Object>
{
    private static final UI i = new UI();
    private final AtomicReference<Game> game = new AtomicReference<>(new Game());
    private final AtomicReference<JButton> stored = new AtomicReference<>();
    private final AtomicReference<Object> ply = new AtomicReference<>();

    public static final void main(final String... args)
    {
        i.game.get().start();
    }

    private UI()
    {
        super("Chess");
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setLayout(new GridLayout(8, 8));
        this.setMinimumSize(new Dimension(350, 350));
        this.setLocationRelativeTo(null);

        this.setJMenuBar(new JMenuBar());
        this.getJMenuBar().add(new JMenu("Options"));
        this.getJMenuBar().getMenu(0).add("Promote [Beta]");
        this.getJMenuBar().getMenu(0).getItem(0).addActionListener(new ActionListener()
        {
            @Override
            public final void actionPerformed(final ActionEvent e)
            {
                final var piece = UI.this.promote(new Player.Human(Colour.BLACK)).call();
                System.out.println(piece.getClass().getSimpleName());
            }
        });

        IntStream.range(0, 64).mapToObj(String::valueOf).map(JButton::new).peek(this::add).forEach(button ->
        {
            button.setHorizontalAlignment(0);
            button.setVerticalAlignment(0);
            button.setFocusPainted(false);
            button.setMargin(new Insets(0, 0, 0, 0));
            button.setContentAreaFilled(false);
            button.setOpaque(true);

            button.addActionListener(new ActionListener()
            {
                @Override
                public final void actionPerformed(final ActionEvent e)
                {
                    if (Objects.isNull(stored.get()))
                    {
                        stored.set(button);
                    }
                    else if (stored.get().equals(button))
                    {
                        stored.set(null);
                    }
                    else if (button.getBackground().equals(Color.RED))
                    {

                    }
                }
            });

            button.addMouseListener(new MouseAdapter()
            {
                @Override
                public final void mouseEntered(final MouseEvent e)
                {
                    if (Objects.isNull(stored.get()))
                    {
                        button.setBackground(Color.PINK);
                        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    }
                }

                @Override
                public final void mouseExited(final MouseEvent e)
                {
                    UI.this.refresh();
                }
            });
        });

        this.refresh();
        this.setVisible(true);
    }

    protected static final UI get()
    {
        return i;
    }

    protected static final int getColumn(final int i)
    {
        return i % 8;
    }

    protected static final int getRow(final int i)
    {
        return i / 8;
    }

    protected final Game getGame()
    {
        return game.get();
    }

    @Override
    public final Object call()
    {
        return ply.getAndSet(null);
    }

    protected final void refresh()
    {
        if (Objects.isNull(stored.get()))
        {
            Streams.forEachPair(IntStream.range(0, 64).boxed(), Arrays.stream(this.getContentPane().getComponents()).map(JButton.class::cast), (i, button) ->
            {
                button.setCursor(Cursor.getDefaultCursor());
                button.setBackground(switch (Math.floorMod(UI.getRow(i) + i, 2)) {
                    case 0 -> Color.WHITE;
                    default -> null;
                });
            });
        }
    }

    protected final Promotion promote(final Player player)
    {
        return new Promotion(player);
    }

    private final class Promotion extends JDialog implements Callable<Piece>
    {
        private final AtomicReference<Piece> piece = new AtomicReference<>();

        private Promotion(final Player player)
        {
            super(i, "Promotion", true);
            this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            this.setLayout(new GridLayout(2, 2));
            this.setSize(350, 200);
            this.setResizable(false);
            this.setLocationRelativeTo(null);

            Stream.of("Queen", "Rook", "Bishop", "Knight").map(JButton::new).peek(this::add).forEach(button ->
            {
                button.addActionListener(new ActionListener()
                {
                    @Override
                    public final void actionPerformed(final ActionEvent e)
                    {
                        try
                        {
                            piece.set(Piece.class.cast(Class.forName(Piece.class.getCanonicalName() + "$" + button.getText()).getDeclaredConstructor(Player.class).newInstance(player)));
                        }
                        catch(final Exception x)
                        {
                            piece.set(new Piece.Queen(player));
                        }
                        finally
                        {
                            Promotion.this.dispose();
                        }
                    }
                });
            });

            this.setVisible(true);
        }

        @Override
        public final Piece call()
        {
            return piece.getAndSet(null);
        }
    }
}