package io.grivera.pdp.algo;

public interface Algorithm {
    RunDetails run();
    void silentRun();
    long getTotalValue();
    long getTotalCost();
    long getTotalProfit();
    long getTotalPackets();
}
