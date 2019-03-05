package com.boomspring.chess;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class Player implements Callable<Game.Turn>
{
    private final Colour colour;

    private Player(final Colour colour)
    {
        this.colour = colour;
    }

    protected final Colour getColour()
    {
        return colour;
    }

    protected static class Human extends Player
    {
        protected Human(final Colour colour)
        {
            super(colour);
        }

        public synchronized final Game.Turn call() throws InterruptedException
        {
            UI.getInstance().getBoard().setEnabled(true);
            wait();
            return UI.getInstance().useTurn();
        }
    }

    protected static final class AI extends Player
    {
        protected AI(final Colour colour)
        {
            super(colour);
        }

        public synchronized final Game.Turn call() throws InterruptedException
        {
            wait(100);
            //return aggressive(() -> UI.getInstance().getGame().getCurrentTurn().getPotentialTurns(this));
            return reactive(3, UI.getInstance().getGame().getCurrentTurn(), this).orElseThrow(InterruptedException::new);
        }

        private final Game.Turn random(final List<Game.Turn> turns)
        {
            return turns.get(new Random().nextInt(turns.size()));
        }

        private final Game.Turn aggressive(final Supplier<Stream<Game.Turn>> turns)
        {
            final var max = turns.get().mapToInt(Game.Turn::getValue).max().orElse(0);
            return random(turns.get().filter(t -> t.getValue() == max).collect(Collectors.toList()));
        }

        // REACTIVE --> DEPTH 1 == AGGRESSIVE METHOD
        private final Optional<Game.Turn> reactive(final int depth, final Game.Turn turn, final Player player)
        {
            if (depth == 0)
            {
                return Optional.of(turn);
            }
            else
            {

                if (this.equals(player))
                {
                    return turn.getPotentialTurns(player).reduce((a, b) -> {

                        final var valA = reactive(depth - 1, a, UI.getInstance().getGame().getNextPlayer()).map(Game.Turn::getValue).orElse(0);
                        final var valB = reactive(depth - 1, b, UI.getInstance().getGame().getNextPlayer()).map(Game.Turn::getValue).orElse(0);

                        if (valA < valB)
                        {
                            return b;
                        }
                        else if (valB < valA)
                        {
                            return a;
                        }
                        else
                        {
                            return random(List.of(a, b));
                        }

                    });
                }
                else
                {
                    return turn.getPotentialTurns(player).reduce((a, b) -> {

                        final var valA = reactive(depth - 1, a, this).map(Game.Turn::getValue).orElse(0);
                        final var valB = reactive(depth - 1, b, this).map(Game.Turn::getValue).orElse(0);

                        if (valA < valB)
                        {
                            return a;
                        }
                        else if (valB < valA)
                        {
                            return b;
                        }
                        else
                        {
                            return random(List.of(a, b));
                        }

                    });
                }
            }
        }
    }
}