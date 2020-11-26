package com.kebe.sample;

import java.util.function.Supplier;

@FunctionalInterface
public interface ThrowingSupplier<T> extends Supplier<T> {
    @Override
    default T get() {
        try {
            return getThrows();
        } catch (Throwable th) {
            throw new RuntimeException(th);
        }
    }

    T getThrows() throws Throwable;
}
