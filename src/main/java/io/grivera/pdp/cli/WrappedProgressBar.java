package io.grivera.pdp.cli;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class WrappedProgressBar<T> implements ProgressBar<T> {
    private long start;
    private long end;
    private long step;
    private final Collection<T> wrappedIterable;

    public WrappedProgressBar(Collection<T> wrappedIterable) {
        this.wrappedIterable = wrappedIterable;
    }

    @Override
    public Iterator<T> iterator() {
        long start = 0;
        long end = this.wrappedIterable.size();
        long step = 1;
        Iterator<T> iterator = this.wrappedIterable.iterator();
        return new Iterator<T>() {
            private long current = start;
            private long totalRange = end;

            @Override
            public boolean hasNext() {
                return current < end;
            }

            @Override
            public T next() {
                if (current >= end) {
                    throw new NoSuchElementException();
                }

                long progress = this.current - start;
                ProgressBar.renderBar(progress, this.totalRange);
                T prevCurrent = iterator.next();
                this.current += step;

                if (current >= end) {
                    ProgressBar.renderBar(this.totalRange, this.totalRange);
                }
                return prevCurrent;
            }
        };
    }
}
