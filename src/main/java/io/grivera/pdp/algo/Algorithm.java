package io.grivera.pdp.algo;

public interface Algorithm {
    RunDetails run();
    RunDetails silentRun();
    long getTotalValue();
    long getTotalCost();
    long getTotalProfit();
    long getTotalPackets();
}
