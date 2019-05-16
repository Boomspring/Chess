package com.boomspring.chess;

import java.util.Map;

public enum Colour
{
    BLACK, WHITE;

    protected final int setDirection(final int vector)
    {
        return vector * Map.of(BLACK, 1, WHITE, -1).get(this);
    }
}