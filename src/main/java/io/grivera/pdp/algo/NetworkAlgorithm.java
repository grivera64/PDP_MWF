package io.grivera.pdp.algo;

import io.grivera.pdp.network.Network;
import io.grivera.pdp.network.node.DataNode;

public abstract class NetworkAlgorithm implements Algorithm {
    private final Network network;
    private boolean hasRan;

    public NetworkAlgorithm(Network network) {
        this.network = network;
    }

    public void silentRun() {
        this.hasRan = true;
        this.network.resetPackets();
        this.network.resetEnergy();
    }

    @Override
    public RunDetails run() {
        this.hasRan = true;
        this.network.resetPackets();
        this.network.resetEnergy();

        return null;
    }

    @Override
    public long getTotalValue() {
        if (!this.hasRan) {
            throw new IllegalArgumentException("Cannot get the total value before runing the model!");
        }
        return -1;
    }

    @Override
    public long getTotalCost() {
        if (!this.hasRan) {
            throw new IllegalStateException("Cannot get the total cost before running the model!");
        }
        return -1;
    }

    @Override
    public long getTotalProfit() {
        if (!this.hasRan) {
            throw new IllegalStateException("Cannot get the total profit before running the model!");
        }
        return -1;
    }

    @Override
    public long getTotalPackets() {
        if (!this.hasRan) {
            throw new IllegalStateException("Cannot get the total packets before running the model!");
        }
        return -1;
    }

    public final Network getNetwork() {
        return this.network;
    }

    protected boolean overflowPacketsRemain() {
        for (DataNode dn : this.network.getDataNodes()) {
            if (!dn.isEmpty()) {
                return true;
            }
        }

        return false;
    }

    public boolean hasRan() {
        return this.hasRan;
    }
}
