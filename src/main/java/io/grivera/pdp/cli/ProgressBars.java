package io.grivera.pdp.cli;

import java.util.Collection;

public class ProgressBars {
    public static RangeProgressBar range(final long start, final long end, final long step) {
        return new RangeProgressBar(start, end, step);
    }
    public static <T> WrappedProgressBar<T> wrapped(Collection<T> collection) {
        return new WrappedProgressBar<T>(collection);
    }
}
