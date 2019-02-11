package com.boomspring.chess;

import java.util.Objects;

@FunctionalInterface
interface QuadPredicate<A, B, C, D> {
    default QuadPredicate<A, B, C, D> and(final QuadPredicate<? super A, ? super B, ? super C, ? super D> other) {
        Objects.requireNonNull(other);
        return (final A a, final B b, final C c, final D d) -> test(a, b, c, d) && other.test(a, b, c, d);
    }

    default QuadPredicate<A, B, C, D> negate() {
        return (final A a, final B b, final C c, final D d) -> !test(a, b, c, d);
    }

    default QuadPredicate<A, B, C, D> or(final QuadPredicate<? super A, ? super B, ? super C, ? super D> other) {
        return (final A a, final B b, final C c, final D d) -> test(a, b, c, d) || other.test(a, b, c, d);
    }

    abstract boolean test(final A a, final B b, final C c, final D d);
}