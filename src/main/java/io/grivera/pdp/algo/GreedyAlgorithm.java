package io.grivera.pdp.algo;

import java.util.List;
import java.util.Map;

import io.grivera.pdp.cli.ProgressBars;
import io.grivera.pdp.network.Network;
import io.grivera.pdp.network.node.DataNode;
import io.grivera.pdp.network.node.StorageNode;

public class GreedyAlgorithm extends NetworkAlgorithm {
    // private Map<SensorNode, List<Tuple<StorageNode, Long, List<SensorNode>>>> routes;
    private long totalValue;
    private long totalCost;
    private long totalProfit;
    private long totalPackets;

    public GreedyAlgorithm(Network network) {
        super(network);
    }

    public void run(int episodes) {
        System.out.println("Warning: Ignoring episodes count; defaulting to 1...");
        this.run();
    }

    public void silentRun() {
        super.silentRun();
        this.totalValue = 0;
        this.totalCost = 0;
        this.totalProfit = 0;
        this.totalPackets = 0;
        
        Network network = this.getNetwork();

        List<DataNode> sortedDns = network.getDataNodes()
            .parallelStream()
            .sorted((dn1, dn2) -> -Long.compare(dn1.getOverflowPacketValue(), dn2.getOverflowPacketValue()))
            .toList();

        // Process DataNodes sequentially
        for (DataNode dn : sortedDns) {
            List<Map.Entry<StorageNode, Long>> sortedSns = network.getStorageNodes()
                .parallelStream()
                .map(sn -> Map.entry(sn, network.calculateMinCost(dn, sn)))
                .sorted(Map.Entry.comparingByValue())
                .toList();
            for (Map.Entry<StorageNode, Long> pair : sortedSns) {
                StorageNode sn = pair.getKey();
                long minCost = pair.getValue();

                if (!dn.hasEnergy() || dn.isEmpty()) {
                    break;
                }
                if (sn.isFull()) {
                    continue;
                }

                long packetsToSend = Math.min(dn.getPacketsLeft(), sn.getSpaceLeft());
                if (packetsToSend <= 0 || !network.canSendPackets(dn, sn, packetsToSend)) {
                    continue;
                }

                this.totalValue += dn.getOverflowPacketValue() * packetsToSend;
                this.totalCost += minCost * packetsToSend;

                network.sendPackets(dn, sn, packetsToSend);
            }
        }

        this.totalProfit = this.totalValue - this.totalCost;

        this.totalPackets = network.getStorageNodes()
            .parallelStream()
            .mapToLong(StorageNode::getUsedSpace)
            .sum();
    }

    @Override
    public RunDetails run() {
        super.run();
        this.totalValue = 0;
        this.totalCost = 0;
        this.totalProfit = 0;
        this.totalPackets = 0;
        
        Network network = this.getNetwork();

        long runTime = -System.nanoTime();
        List<DataNode> sortedDns = network.getDataNodes()
            .parallelStream()
            .sorted((dn1, dn2) -> -Long.compare(dn1.getOverflowPacketValue(), dn2.getOverflowPacketValue()))
            .toList();

        // Process DataNodes sequentially
        System.out.println("Greedy Run Progress Bar:");
        for (DataNode dn : ProgressBars.wrapped(sortedDns)) {
            List<Map.Entry<StorageNode, Long>> sortedSns = network.getStorageNodes()
                .parallelStream()
                .map(sn -> Map.entry(sn, network.calculateMinCost(dn, sn)))
                .sorted(Map.Entry.comparingByValue())
                .toList();
            for (Map.Entry<StorageNode, Long> pair : sortedSns) {
                StorageNode sn = pair.getKey();
                long minCost = pair.getValue();

                if (!dn.hasEnergy() || dn.isEmpty()) {
                    break;
                }
                if (sn.isFull()) {
                    continue;
                }

                long packetsToSend = Math.min(dn.getPacketsLeft(), sn.getSpaceLeft());
                if (packetsToSend <= 0 || !network.canSendPackets(dn, sn, packetsToSend)) {
                    continue;
                }

                this.totalValue += dn.getOverflowPacketValue() * packetsToSend;
                this.totalCost += minCost * packetsToSend;

                network.sendPackets(dn, sn, packetsToSend);
            }
        }

        this.totalProfit = this.totalValue - this.totalCost;

        this.totalPackets = network.getStorageNodes()
            .parallelStream()
            .mapToLong(StorageNode::getUsedSpace)
            .sum();

        runTime += System.nanoTime();

        return new RunDetails(runTime, -1);
    }
    
    @Override
    public long getTotalValue() {
        super.getTotalValue();
        return this.totalValue;
    }

    @Override
    public long getTotalCost() {
        super.getTotalCost();
        return this.totalCost;
    }

    @Override
    public long getTotalProfit() {
        super.getTotalProfit();
        return this.totalProfit;
    }

    @Override
    public long getTotalPackets() {
        super.getTotalPackets();
        return this.totalPackets;
    }
}
