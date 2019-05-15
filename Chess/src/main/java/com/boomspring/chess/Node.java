package com.boomspring.chess;

import com.google.common.collect.ImmutableList;

public final class Node
{
    private final Game.Turn currentState;
    private final ImmutableList<Player> players = ImmutableList.of(UI.getGame().getCurrentPlayer(), UI.getGame().getNextPlayer());
    private final ImmutableList<Node> children;
    private final int currentDepth;
    private final int requiredDepth;

    public Node(final Game game, final int requiredDepth)
    {
        this.currentState = game.getCurrentTurn();
        this.currentDepth = 0;
        this.requiredDepth = requiredDepth;
        this.children = calculateChildren();
    }

    private Node(final Game.Turn turn, final int currentDepth, final int requiredDepth)
    {
        this.currentState = turn;
        this.currentDepth = currentDepth;
        this.requiredDepth = requiredDepth;
        this.children = calculateChildren();
    }

    protected final Game.Turn getCurrentState()
    {
        return currentState;
    }

    protected final ImmutableList<Node> getChildren()
    {
        return children;
    }

    private final ImmutableList<Node> calculateChildren()
    {
        if (currentDepth < requiredDepth)
        {
            return currentState.getPotentialTurns(players.get(currentDepth % 2)).map(x -> {
                return new Node(x, currentDepth + 1, requiredDepth);
            }).collect(ImmutableList.toImmutableList());
        } else return ImmutableList.of();
    }

    protected final int miniMax(int alpha, int beta)
    {
        int value;

        if (currentDepth == requiredDepth || children.isEmpty())
        {
            return (players.get(0).getColour().equals(Colour.WHITE) ? 1 : -1) * currentState.calculateValue();
        }
        else
        {
            if (currentDepth % 2 == 0)
            {
                value = -10000;

                for (final Node child : children)
                {
                    value = Math.max(value, child.miniMax(alpha, beta));
                    alpha = Math.max(alpha, value);
                    if (alpha >= beta) break;
                }

                return value;
            }
            else
            {
                value = 10000;

                for (final Node child : children)
                {
                    value = Math.min(value, child.miniMax(alpha, beta));
                    beta = Math.min(beta, value);
                    if (alpha >= beta) break;
                }

                return value;
            }
        }
    }

    protected final int miniMax()
    {
        int value;

        if (currentDepth == requiredDepth || children.isEmpty())
        {
            return currentState.calculateValue();
        }
        else
        {
            if (currentDepth % 2 == 0)
            {
                value = -10000;

                for (final Node child : children)
                {
                    value = Math.max(value, child.miniMax());
                }

                return value;
            }
            else
            {
                value = 10000;

                for (final Node child : children)
                {
                    value = Math.min(value, child.miniMax());
                }

                return value;
            }
        }
    }
}