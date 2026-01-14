package io.grivera.pdp.algo;

public record RunDetails(long runTimeNS, long parseTimeNS) {
    public long runTimeMS() {
        return runTimeNS() / 1_000_000;
    }

    public long parseTimeMS() {
        return runTimeNS() / 1_000_000;
    }
}
