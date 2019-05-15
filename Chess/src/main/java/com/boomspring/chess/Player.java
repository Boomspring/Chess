package com.boomspring.chess;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public abstract class Player implements Callable<Game.Turn>
{
    private final Colour colour;

    private Player(final Colour colour)
    {
        this.colour = colour;
    }

    public final Colour getColour()
    {
        return colour;
    }

    public static final class Human extends Player
    {
        protected Human(final Colour colour)
        {
            super(colour);
        }

		@Override
        public synchronized final Game.Turn call() throws InterruptedException
        {
            Thread.currentThread().setName("Human");
            wait();
			return UI.useTurn();
		}
    }

    public static final class AI extends Player
    {
        private final Random random = new Random();
        private final int depth;

        protected AI(final Colour colour, final int depth)
        {
            super(colour);
            this.depth = depth;
        }

        @Override
        public synchronized final Game.Turn call() throws InterruptedException
        {
            Thread.currentThread().setName("AI");

            final Node tree = new Node(UI.getGame(), depth);
            final int value = tree.miniMax(-10000, 10000);
            final List<Node> list = tree.getChildren().stream().filter(n -> n.miniMax(-10000, 10000) == value).collect(Collectors.toList());

            System.out.println("PLAYER: " + UI.getGame().getCurrentPlayer().getColour().name() + ", CURRENT BOARD VALUE: " + value);
            return list.get(random.nextInt(list.size())).getCurrentState();
        }
    }
}