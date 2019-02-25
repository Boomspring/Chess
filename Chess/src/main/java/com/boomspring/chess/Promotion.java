package com.boomspring.chess;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;

public final class Promotion extends JDialog
{
    private static final long serialVersionUID = 1L;
    private final AtomicReference<Piece> stored = new AtomicReference<>();

    public Promotion(final Player player)
    {
        super((Frame) null, "Promotion", true);
        this.setLayout(new GridLayout(2, 2));
        this.setMinimumSize(new Dimension(350, 200));
        this.setLocationRelativeTo(null);
        this.setAlwaysOnTop(true);
        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        IntStream.range(0, 4).mapToObj(i -> {
            return Map.of(0, "ROOK", 1, "KNIGHT", 2, "BISHOP", 3, "QUEEN").getOrDefault(i, "--");
        }).map(JLabel::new).peek(this::add).forEach(label -> {
            label.setOpaque(true);
            label.setHorizontalAlignment(0);
            label.setVerticalAlignment(0);
            label.setBorder(BorderFactory.createLoweredBevelBorder());
            label.addMouseListener(new MouseAdapter() {
                public final void mouseEntered(final MouseEvent e)
                {
                    label.setBackground(Color.PINK);
                    label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }

                public final void mouseExited(final MouseEvent e)
                {
                    Arrays.stream(getContentPane().getComponents()).forEach(label ->
                    {
                        label.setBackground(null);
                        label.setCursor(Cursor.getDefaultCursor());
                    });
                }

                public final void mouseClicked(final MouseEvent e)
                {
                    stored.set(Map.of("ROOK", new Piece.Rook(player), "KNIGHT", new Piece.Knight(player), "BISHOP", new Piece.Bishop(player), "QUEEN", new Piece.Queen(player)).get(label.getText()));
                    Promotion.this.dispose();
                }
            });
        });

        this.pack();
        this.setVisible(true);
    }

    public final Piece getStored()
    {
        return stored.get();
    }
}