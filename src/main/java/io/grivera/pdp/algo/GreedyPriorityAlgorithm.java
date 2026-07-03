package io.grivera.pdp.algo;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import io.grivera.pdp.cli.ProgressBars;
import io.grivera.pdp.network.Network;
import io.grivera.pdp.network.node.DataNode;
import io.grivera.pdp.network.node.SensorNode;
import io.grivera.pdp.network.node.StorageNode;

public class GreedyPriorityAlgorithm extends NetworkAlgorithm {
    // private Map<SensorNode, List<Tuple<StorageNode, Long, List<SensorNode>>>> routes;
    private long totalValue;
    private long totalCost;
    private long totalProfit;
    private long totalPackets;

    public GreedyPriorityAlgorithm(Network network) {
        super(network);
    }

    public void run(int episodes) {
        System.out.println("Warning: Ignoring episodes count; defaulting to 1...");
        this.run();
    }

    @Override
    public RunDetails silentRun() {
        super.silentRun();
        this.totalValue = 0;
        this.totalCost = 0;
        this.totalProfit = 0;
        this.totalPackets = 0;
        
        Network network = this.getNetwork();

        long runTime = -System.nanoTime();
        List<DataNode> sortedDns = network.getDataNodes()
            .parallelStream()
            .sorted(Comparator.comparingLong(DataNode::getOverflowPacketValue).reversed()
                .thenComparingInt(DataNode::getId))
            .toList();

        // Process DataNodes sequentially
        for (DataNode dn : sortedDns) {
            // Sort by cost (energy independent)
            Map<SensorNode, Long> minCostsFromDn = network.getMinCostFrom(dn);
            List<StorageNode> sortedSns = network.getStorageNodes()
                .stream()
                .filter(minCostsFromDn::containsKey)
                .sorted(Comparator.comparing((StorageNode sn) -> minCostsFromDn.get(sn))
                    .thenComparingInt(StorageNode::getId))
                .toList();
            for (StorageNode sn : sortedSns) {
                if (!dn.hasEnergy() || dn.isEmpty()) {
                    break;
                }
                if (sn.isFull()) {
                    continue;
                }

                // Try to send as many packets as possible, decrementing until feasible
                long packetsToSend = Math.min(dn.getPacketsLeft(), sn.getSpaceLeft());
                if (packetsToSend <= 0) {
                    continue;
                }

                List<SensorNode> path = network.getMinCostPath(dn, sn);
                if (path.size() < 2) {
                    continue;
                }
                while (!network.canSendPacketsAlong(path, packetsToSend)) {
                    packetsToSend--;
                    if (packetsToSend <= 0) {
                        break;
                    }
                }
                if (packetsToSend <= 0) {
                    continue;
                }

                long minCost = minCostsFromDn.get(sn);
                this.totalValue += dn.getOverflowPacketValue() * packetsToSend;
                this.totalCost += minCost * packetsToSend;

                network.sendPacketsAlong(path, packetsToSend);
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
            .sorted(Comparator.comparingLong(DataNode::getOverflowPacketValue).reversed()
                .thenComparingInt(DataNode::getId))
            .toList();

        // Process DataNodes sequentially
        System.out.println("Greedy Run Progress Bar:");
        for (DataNode dn : ProgressBars.wrapped(sortedDns)) {
            // Costs are distance-based (energy-independent); compute once per DN for sorting
            Map<SensorNode, Long> minCostsFromDn = network.getMinCostFrom(dn);
            List<StorageNode> sortedSns = network.getStorageNodes()
                .parallelStream()
                .filter(minCostsFromDn::containsKey)
                .sorted(Comparator.comparing((StorageNode sn) -> minCostsFromDn.get(sn))
                    .thenComparingInt(SensorNode::getId))
                .toList();
            for (StorageNode sn : sortedSns) {
                if (!dn.hasEnergy() || dn.isEmpty()) {
                    break;
                }
                if (sn.isFull()) {
                    continue;
                }

                // Try to send as many packets as possible, decrementing until feasible
                long packetsToSend = Math.min(dn.getPacketsLeft(), sn.getSpaceLeft());
                if (packetsToSend <= 0) {
                    continue;
                }

                // Lazily compute the path at send time (may reroute around depleted relays)
                List<SensorNode> path = network.getMinCostPath(dn, sn);
                if (path.size() < 2) {
                    continue;
                }
                while (!network.canSendPacketsAlong(path, packetsToSend)) {
                    packetsToSend--;
                    if (packetsToSend <= 0) {
                        break;
                    }
                }
                if (packetsToSend <= 0) {
                    continue;
                }

                long minCost = minCostsFromDn.get(sn);
                this.totalValue += dn.getOverflowPacketValue() * packetsToSend;
                this.totalCost += minCost * packetsToSend;

                network.sendPacketsAlong(path, packetsToSend);
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
