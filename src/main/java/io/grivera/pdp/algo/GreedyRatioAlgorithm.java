package io.grivera.pdp.algo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import io.grivera.pdp.network.Network;
import io.grivera.pdp.network.node.DataNode;
import io.grivera.pdp.network.node.SensorNode;
import io.grivera.pdp.network.node.StorageNode;
import io.grivera.pdp.util.Tuple;

public class GreedyRatioAlgorithm extends NetworkAlgorithm {
    private Map<SensorNode, List<Tuple<StorageNode, Long, List<SensorNode>>>> routes;
    private long totalValue;
    private long totalCost;
    private long totalProfit;
    private long totalPackets;

    public GreedyRatioAlgorithm(Network network) {
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
        this.routes = new LinkedHashMap<>();

        Network network = this.getNetwork();

        long runTime = -System.nanoTime();

        while (true) {
            DataNode bestDn = null;
            StorageNode bestSn = null;
            List<SensorNode> bestPath = null;
            double bestRatio = -1;
            long bestCost = 0;

            for (DataNode dn : network.getDataNodes()) {
                if (!dn.hasEnergy() || dn.isEmpty()) {
                    continue;
                }
                for (StorageNode sn : network.getStorageNodes()) {
                    if (sn.isFull()) {
                        continue;
                    }
                    List<SensorNode> path = network.getMinCostPath(dn, sn);
                    if (path == null || path.size() < 2) {
                        continue;
                    }
                    if (!network.canSendPacketsAlong(path, 1)) {
                        continue;
                    }
                    long cost = network.calculateCostOfPath(path);
                    if (cost <= 0) {
                        continue;
                    }
                    double ratio = (double) dn.getOverflowPacketValue() / cost;
                    if (ratio > bestRatio) {
                        bestRatio = ratio;
                        bestDn = dn;
                        bestSn = sn;
                        bestPath = path;
                        bestCost = cost;
                    }
                }
            }

            if (bestDn == null) {
                break;
            }

            long packetsToSend = Math.min(bestDn.getPacketsLeft(), bestSn.getSpaceLeft());
            while (!network.canSendPacketsAlong(bestPath, packetsToSend)) {
                packetsToSend--;
                if (packetsToSend <= 0) {
                    break;
                }
            }
            if (packetsToSend <= 0) {
                continue;
            }

            this.totalValue += bestDn.getOverflowPacketValue() * packetsToSend;
            this.totalCost += bestCost * packetsToSend;
            this.routes.putIfAbsent(bestDn, new ArrayList<>());
            this.routes.get(bestDn).add(Tuple.of(bestSn, (Long) packetsToSend, bestPath));

            network.sendPacketsAlong(bestPath, packetsToSend);
        }

        this.totalProfit = this.totalValue - this.totalCost;

        for (StorageNode storageNode : network.getStorageNodes()) {
            this.totalPackets += storageNode.getUsedSpace();
        }

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
        this.routes = new LinkedHashMap<>();

        Network network = this.getNetwork();

        long runTime = -System.nanoTime();

        while (true) {
            DataNode bestDn = null;
            StorageNode bestSn = null;
            List<SensorNode> bestPath = null;
            double bestRatio = -1;
            long bestCost = 0;

            for (DataNode dn : network.getDataNodes()) {
                if (!dn.hasEnergy() || dn.isEmpty()) {
                    continue;
                }
                for (StorageNode sn : network.getStorageNodes()) {
                    if (sn.isFull()) {
                        continue;
                    }
                    List<SensorNode> path = network.getMinCostPath(dn, sn);
                    if (path == null || path.size() < 2) {
                        continue;
                    }
                    if (!network.canSendPacketsAlong(path, 1)) {
                        continue;
                    }
                    long cost = network.calculateCostOfPath(path);
                    if (cost <= 0) {
                        continue;
                    }
                    double ratio = (double) dn.getOverflowPacketValue() / cost;
                    if (ratio > bestRatio) {
                        bestRatio = ratio;
                        bestDn = dn;
                        bestSn = sn;
                        bestPath = path;
                        bestCost = cost;
                    }
                }
            }

            if (bestDn == null) {
                break;
            }

            long packetsToSend = Math.min(bestDn.getPacketsLeft(), bestSn.getSpaceLeft());
            while (!network.canSendPacketsAlong(bestPath, packetsToSend)) {
                packetsToSend--;
                if (packetsToSend <= 0) {
                    break;
                }
            }
            if (packetsToSend <= 0) {
                continue;
            }

            this.totalValue += bestDn.getOverflowPacketValue() * packetsToSend;
            this.totalCost += bestCost * packetsToSend;
            this.routes.putIfAbsent(bestDn, new ArrayList<>());
            this.routes.get(bestDn).add(Tuple.of(bestSn, (Long) packetsToSend, bestPath));

            network.sendPacketsAlong(bestPath, packetsToSend);
        }

        this.totalProfit = this.totalValue - this.totalCost;

        for (StorageNode storageNode : network.getStorageNodes()) {
            this.totalPackets += storageNode.getUsedSpace();
        }

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

    public void printRoute() {
        StringJoiner str;

        for (Map.Entry<SensorNode, List<Tuple<StorageNode, Long, List<SensorNode>>>> entry : this.routes.entrySet()) {
            for (Tuple<StorageNode, Long, List<SensorNode>> route : entry.getValue()) {
                str = new StringJoiner(" -> ", "[", "]");
                System.out.printf("%s -> %s (flow = %d)\n", entry.getKey().getName(), route.first().getName(), route.second());
                for (SensorNode node : route.third()) {
                    str.add(node.getName());
                }
                System.out.printf("\t%s\n", str);
            }
        }

    }
}
