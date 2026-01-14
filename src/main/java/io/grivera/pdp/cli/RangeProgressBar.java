package io.grivera.pdp.cli;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class RangeProgressBar implements ProgressBar<Long> {

    private long start, end, step;

    protected RangeProgressBar(long start, long end, long step) {
        if (start >= end) {
            if (step >= 0) {
                throw new IllegalArgumentException("Step must be less than 0.");
            } 
        } else {
            if (step <= 0) {
                throw new IllegalArgumentException("Step must be greater than 0.");
            }
        }

        this.start = start;
        this.end = end;
        this.step = step;
    }

    public Iterator<Long> iterator() {
        return new Iterator<Long>() {
            private long current = start;
            private long totalRange = end - start;

            @Override
            public boolean hasNext() {
                return current < end;
            }

            @Override
            public Long next() {
                if (current >= end) {
                    throw new NoSuchElementException();
                }

                long progress = current - start;
                ProgressBar.renderBar(progress, totalRange);
                long prevCurrent = current;
                current += step;

                if (current >= end) { // Ensure 100% is rendered at the end
                    ProgressBar.renderBar(totalRange, totalRange);
                }
                return prevCurrent;
            }
        };
    }
}
