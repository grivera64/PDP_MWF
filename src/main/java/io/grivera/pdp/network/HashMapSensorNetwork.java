package io.grivera.pdp.network;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Gatherers;

import io.grivera.pdp.algo.Algorithm;
import io.grivera.pdp.algo.MFAlgorithm;

import io.grivera.pdp.network.node.DataNode;
import io.grivera.pdp.network.node.SensorNode;
import io.grivera.pdp.network.node.StorageNode;
import io.grivera.pdp.network.node.TransitionNode;

import io.grivera.pdp.util.Tuple;

/**
 * An implementation of a Network that contains Data and
 * Storage Sensor Nodes
 *
 * @see Network
 */
public class HashMapSensorNetwork implements Network {

    private List<SensorNode> nodes;
    private List<DataNode> dNodes;
    private List<StorageNode> sNodes;
    private List<TransitionNode> tNodes;
    private Map<SensorNode, Set<SensorNode>> graph;

    private final double width, length;
    private long dataPacketCount;
    private long storageCapacity;
    private final double transmissionRange;
    private long batteryCapacity;
    private final long minPacketValue;
    private final long maxPacketValue;

    /**
     * Constructor to create a Sensor Network
     *
     * @param x  the width of the network (in meters)
     * @param y  the length of the network (in meters)
     * @param N  the number of nodes
     * @param tr the transmission range of the nodes (in meters)
     * @param p  the number of Data Nodes in the network
     * @param q  the number of data packets each Data Node has
     * @param s  the number of Storage Nodes in the network
     * @param m  the storage capacity each Storage nodes has
     * @param c  the battery capacity of each Sensor node (in micro Joules)
     * @param Vl the minimum value of a data packet (inclusive)
     * @param Vh the maximum value of a data packet (inclusive)
     */
    public HashMapSensorNetwork(double x, double y, int N, double tr, int p, long q, int s, long m, long c, long Vl,
            long Vh) {
        this.width = x;
        this.length = y;
        this.dataPacketCount = q;
        this.storageCapacity = m;
        this.transmissionRange = tr;
        this.batteryCapacity = c;

        /* Used to separate each type of node for later use and retrieval */
        if (p + s > N)
            throw new IllegalArgumentException("Invalid SensorNetwork constructor parameters");

        this.dNodes = new ArrayList<>(p);
        this.sNodes = new ArrayList<>(s);
        this.tNodes = new ArrayList<>(N - s - p);

        /*
         * Init the Sensor Network to allow basic operations on it
         */
        this.minPacketValue = Vl;
        this.maxPacketValue = Vh;
        this.nodes = this.initNodes(N, p, s, Vl, Vh);
        this.graph = this.initGraph(this.nodes);
    }

    /**
     * Copy constructor to create a Sensor Network from an .sn
     * file.
     *
     * <p>
     * </p>
     *
     * The file must follow the following format:
     * <p>
     * </p>
     * width length transmission_range
     * <p>
     * data_packets_per_node storage_capacity_per_node
     * <p>
     * total_nodes battery_capacity_per_node
     * <p>
     * (d/s) id x y
     * <p>
     * ...
     *
     * @param fileName the path to the .sn file
     */
    public HashMapSensorNetwork(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            throw new IllegalArgumentException(String.format("File \"%s\" doesn't exist!", fileName));
        }

        int lineNumber = 1;
        try (Scanner fileScanner = new Scanner(file)) {
            if (!fileScanner.hasNext()) {
                throw new IllegalArgumentException(String.format("File \"%s\" is empty!", fileName));
            }

            this.width = fileScanner.nextDouble();
            this.length = fileScanner.nextDouble();
            this.transmissionRange = fileScanner.nextDouble();
            fileScanner.nextLine();
            lineNumber++;

            this.dataPacketCount = fileScanner.nextLong();
            this.storageCapacity = fileScanner.nextLong();
            fileScanner.nextLine();
            lineNumber++;

            String[] tokens = fileScanner.nextLine().split(" ");
            int N = Integer.parseInt(tokens[0]);
            this.batteryCapacity = Long.parseLong(tokens[1]);
            lineNumber++;

            SensorNode.resetCounter();
            StorageNode.resetCounter();
            DataNode.resetCounter();
            TransitionNode.resetCounter();

            this.nodes = new ArrayList<>();
            this.sNodes = new ArrayList<>();
            this.dNodes = new ArrayList<>();
            this.tNodes = new ArrayList<>();

            String[] lineArgs;
            double x, y;
            SensorNode node;
            for (int i = 0; i < N; i++) {
                lineArgs = fileScanner.nextLine().split(" ");
                if (lineArgs.length < 3 || lineArgs.length > 4) {
                    throw new IOException(
                            String.format("Invalid Line %d: %s!", lineNumber, String.join(" ", lineArgs)));
                }

                x = Double.parseDouble(lineArgs[1]);
                y = Double.parseDouble(lineArgs[2]);

                // Requires JDK 12+
                node = switch (lineArgs[0]) {
                    case "d" ->
                        new DataNode(x, y, this.transmissionRange, this.batteryCapacity, this.dataPacketCount,
                                Long.parseLong(lineArgs[3]));
                    case "s" ->
                        new StorageNode(x, y, this.transmissionRange, this.batteryCapacity, this.storageCapacity);
                    case "t" ->
                        new TransitionNode(x, y, this.transmissionRange, this.batteryCapacity);
                    default ->
                        throw new IOException();
                };

                this.nodes.add(node);
                if (node instanceof DataNode) {
                    this.dNodes.add((DataNode) node);
                } else if (node instanceof StorageNode) {
                    this.sNodes.add((StorageNode) node);
                } else {
                    this.tNodes.add((TransitionNode) node);
                }
                lineNumber++;
            }
            this.graph = this.initGraph(this.nodes);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid file provided: " + e.getMessage());
        }

        // Unknown at this time (cross compatibility)
        this.minPacketValue = Integer.MIN_VALUE;
        this.maxPacketValue = Integer.MAX_VALUE;
    }

    /**
     * Wrapped constructor to create a Sensor Network
     *
     * @param x  the width of the network (in meters)
     * @param y  the length of the network (in meters)
     * @param N  the number of nodes
     * @param tr the transmission range of the nodes (in meters)
     * @param p  the number of Data Nodes in the network
     * @param q  the number of data packets each Data Node has
     * @param s  the number of Storage Nodes in the network
     * @param m  the storage capacity each Storage nodes has
     * @param c  the battery capacity of each Sensor node (in micro Joules)
     * @param Vl the minimum value of a data packet (inclusive)
     * @param Vh the maximum value of a data packet (inclusive)
     */
    public static HashMapSensorNetwork of(double x, double y, int N, double tr, int p, long q, int s, long m, long c,
            long Vl,
            long Vh) {
        HashMapSensorNetwork network;
        int attempts = 0;
        final int maxAttempts = N * 5_000;
        do {
            network = new HashMapSensorNetwork(x, y, N, tr, p, q, s, m, c, Vl, Vh);

            /* Checks if the parameters in the program are feasible */
            if (!network.isFeasible()) {
                System.err.println("Invalid network parameters! Please re-run the program.");
                System.err.println("Exiting the program...");
                System.exit(1);
            }

            /*
             * Checks if we were able to find a valid network within a reasonable range of
             * attempts
             */
            if (attempts > maxAttempts) {
                System.err.printf("Failed to create a connected network after %d tries! Please re-run the program.\n",
                        maxAttempts);
                System.err.println("Exiting the program...");
                System.exit(1);
            }
            attempts++;

        } while (!network.isConnected());

        return network;
    }

    /**
     * Wrapped copy constructor to create a Sensor Network from an .sn
     * file.
     *
     * <p>
     * </p>
     *
     * The file must follow the following format:
     * <p>
     * </p>
     * width length transmission_range
     * <p>
     * data_packets_per_node storage_capacity_per_node
     * <p>
     * total_nodes battery_capacity_per_node
     * <p>
     * (d/s) id x y
     * <p>
     * ...
     *
     * @param fileName the path to the .sn file
     */
    public static HashMapSensorNetwork from(String fileName) {
        return new HashMapSensorNetwork(fileName);
    }

    /**
     * Copy constructor to create a Sensor Network from an .sn
     * file with new overflow packet and storage capacity counts.
     *
     * <p>
     * </p>
     *
     * The file must follow the following format:
     * <p>
     * </p>
     * width length transmission_range
     * <p>
     * data_packets_per_node storage_capacity_per_node
     * <p>
     * total_nodes battery_capacity_per_node
     * <p>
     * (d/s) id x y
     * <p>
     * ...
     *
     * @param fileName        the path to the .sn file
     * @param overflowPackets the number of packets each Data Node has
     * @param storageCapacity the number of storage spaces each Storage Node has
     */
    public static HashMapSensorNetwork from(String fileName, long overflowPackets, long storageCapacity) {
        HashMapSensorNetwork network = new HashMapSensorNetwork(fileName);
        network.setOverflowPackets(overflowPackets);
        network.setStorageCapacity(storageCapacity);
        return network;
    }

    private List<SensorNode> initNodes(int nodeCount, int p, int s, long Vl, long Vh) {
        List<SensorNode> nodes = new ArrayList<>(nodeCount);
        Random rand = new Random();

        /* Reset Counters (This is a temporary fix) */
        SensorNode.resetCounter();
        StorageNode.resetCounter();
        DataNode.resetCounter();
        TransitionNode.resetCounter();

        /* Choose p random nodes to be Generator Nodes, the rest are Storage Nodes */
        int choice;
        double x, y;
        SensorNode sensorNode;
        long packetValue;
        for (int index = 0; index < nodeCount; index++) {
            choice = rand.nextInt(1, 11);
            x = this.width * rand.nextDouble();
            y = this.length * rand.nextDouble();

            packetValue = rand.nextLong(Vh - Vl + 1) + Vl;

            if ((choice < 4 && p > 0) || nodeCount - index <= p) {
                sensorNode = new DataNode(x, y, this.transmissionRange, this.batteryCapacity, this.dataPacketCount,
                        packetValue);
                this.dNodes.add((DataNode) sensorNode);
                p--;
            } else if ((choice < 8 && s > 0) || nodeCount - index - p - s <= 0) {
                sensorNode = new StorageNode(x, y, this.transmissionRange, this.batteryCapacity, this.storageCapacity);
                this.sNodes.add((StorageNode) sensorNode);
                s--;
            } else {
                sensorNode = new TransitionNode(x, y, this.transmissionRange, this.batteryCapacity);
                this.tNodes.add((TransitionNode) sensorNode);
            }
            nodes.add(sensorNode);
        }
        return nodes;
    }

    private Map<SensorNode, Set<SensorNode>> initGraph(List<SensorNode> nodes) {
        Map<SensorNode, Set<SensorNode>> graph = new HashMap<>();

        /* Create the adjacency graph */
        SensorNode node1;
        SensorNode node2;
        for (int index1 = 0; index1 < nodes.size(); index1++) {
            node1 = nodes.get(index1);

            /* Populate the graph with adjacent nodes */
            graph.putIfAbsent(node1, new HashSet<>());
            for (int index2 = index1 + 1; index2 < nodes.size(); index2++) {
                node2 = nodes.get(index2);
                graph.putIfAbsent(node2, new HashSet<>());
                if (node1.inRangeOf(node2)) {
                    graph.get(node1).add(node2);
                    graph.get(node2).add(node1); // This makes the graph a non-directed graph
                }
            }
        }
        return graph;
    }

    @Override
    public double getWidth() {
        return this.width;
    }

    @Override
    public double getLength() {
        return this.length;
    }

    @Override
    public List<SensorNode> getSensorNodes() {
        return Collections.unmodifiableList(this.nodes);
    }

    @Override
    public int getSensorNodeCount() {
        return this.nodes.size();
    }

    @Override
    public List<DataNode> getDataNodes() {
        return Collections.unmodifiableList(this.dNodes);
    }

    @Override
    public int getDataNodeCount() {
        return this.dNodes.size();
    }

    @Override
    public List<StorageNode> getStorageNodes() {
        return Collections.unmodifiableList(this.sNodes);
    }

    @Override
    public int getStorageNodeCount() {
        return this.sNodes.size();
    }

    @Override
    public List<TransitionNode> getTransitionNodes() {
        return Collections.unmodifiableList(this.tNodes);
    }

    @Override
    public int getTransitionNodeCount() {
        return this.tNodes.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isConnected() {
        return isConnectedComponent(this.nodes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isFeasible() {
        int p = this.dNodes.size();
        return p * this.dataPacketCount <= (this.nodes.size() - p) * this.storageCapacity;
    }

    public boolean isMaxFeasible() {
        Algorithm mf = new MFAlgorithm(this);
        try {
            mf.silentRun();

            long totalPackets = this.dNodes.parallelStream()
                    .mapToLong(DataNode::getOverflowPackets)
                    .sum();
            return mf.getTotalPackets() == totalPackets;
        } catch (IllegalStateException e) {
            return false;
        } finally {
            this.resetEnergy();
            this.resetPackets();
        }
    }

    public boolean isStoreFeasible() {
        Algorithm mf = new MFAlgorithm(this);
        try {
            mf.run();

            long totalPackets = this.sNodes.parallelStream()
                    .mapToLong(StorageNode::getCapacity)
                    .sum();
            return mf.getTotalPackets() == totalPackets;
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return false;
        } finally {
            this.resetEnergy();
            this.resetPackets();
        }
    }

    @Override
    public Map<SensorNode, Set<SensorNode>> getAdjacencyList() {
        return Collections.unmodifiableMap(this.graph);
    }

    @Override
    public long calculateMinCost(SensorNode from, SensorNode to) {
        return this.calculateCostOfPath(this.getMinCostPath(from, to));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SensorNode> getMinCostPath(SensorNode from, SensorNode to) {
        return getMinCostPath(this.graph, from, to);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long calculateCostOfPath(List<SensorNode> path) {
        return path.parallelStream()
                .gather(Gatherers.windowSliding(2))
                .filter(l -> l.size() == 2)
                .mapToLong(l -> this.getCost(l.get(0), l.get(1)))
                .sum();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(String fileName) {
        File file = new File(fileName);

        try (PrintWriter pw = new PrintWriter(file)) {
            pw.printf("%f %f %f\n", this.getWidth(), this.getLength(), this.transmissionRange); // X, Y, Tr
            pw.printf("%d %d\n", this.dataPacketCount, this.storageCapacity); // q m
            pw.printf("%d %d\n", this.nodes.size(), this.batteryCapacity); // N c

            for (SensorNode n : this.nodes) {
                if (n instanceof DataNode dn) { // JDK 15+ feature
                    pw.printf("%c %f %f %d\n", 'd', dn.getX(), dn.getY(), dn.getOverflowPacketValue());
                } else if (n instanceof StorageNode) {
                    pw.printf("%c %f %f\n", 's', n.getX(), n.getY());
                } else {
                    pw.printf("%c %f %f\n", 't', n.getX(), n.getY());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private boolean isConnectedComponent(List<SensorNode> nodes) {
        Stack<SensorNode> stack = new Stack<>();
        Set<SensorNode> seen = new HashSet<>();
        stack.push(nodes.get(0));

        SensorNode curr;
        while (!stack.isEmpty()) {
            curr = stack.pop();
            seen.add(curr);

            for (SensorNode neighbor : this.getNeighbors(curr)) {
                if (!seen.contains(neighbor)) {
                    stack.push(neighbor);
                }
            }
        }
        return seen.size() == nodes.size();
    }

    public Set<SensorNode> getNeighbors(SensorNode node) {
        return this.graph.getOrDefault(node, Set.of());
    }

    public boolean isConnected(SensorNode sensorNode1, SensorNode sensorNode2) {
        return this.getNeighbors(sensorNode1).contains(sensorNode2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveAsCsInp(String fileName) {
        final long supply = this.dataPacketCount * this.dNodes.size();
        final long demand = -supply;

        final int totalNodes = this.dNodes.size() + this.sNodes.size() + 3;
        final int totalEdges = this.getEdgeCount();

        File file = new File(fileName);
        try (PrintWriter writer = new PrintWriter(file)) {
            /* Header */
            writer.printf("c Min-Cost flow problem with %d nodes and %d arcs (edges)\n", totalNodes, totalEdges);
            writer.printf("p min %d %d\n", totalNodes, totalEdges);
            writer.println();

            /* Set s (source) and t (sink) nodes */
            writer.printf("c Supply of %d at node %d (\"Source\")\n", supply, 0);
            writer.printf("n %d %d\n", 0, supply);
            writer.println();

            writer.printf("c Demand of %d at node %d (\"Sink\")\n", demand, totalNodes - 1);
            writer.printf("n %d %d\n", totalNodes - 1, demand);
            writer.println();

            /* Arcs */
            writer.println("c arc list follows");
            writer.println("c arc has <tail> <head> <capacity l.b.> <capacity u.b> <cost>");

            /* Path from Source to DN is always 0 cost (not represented in the network) */
            for (DataNode dn : this.dNodes) {
                writer.printf("c Source -> %s\n", dn.getName());
                writer.printf("a %d %d %d %d %d\n", 0, dn.getUuid(), 0, this.dataPacketCount, 0);
            }
            writer.println();

            /* Find all paths from DN# -> SN#, Dummy */
            long profit;
            for (DataNode dn : this.dNodes) {
                for (StorageNode sn : this.sNodes) {
                    writer.printf("c %s -> %s\n", dn.getName(), sn.getName());
                    profit = this.calculateProfitOf(dn, sn);
                    writer.printf("a %d %d %d %d %d\n", dn.getUuid(), sn.getUuid(),
                            0, this.dataPacketCount, -profit);
                }
                writer.printf("c %s to Dummy Node\n", dn.getName());
                writer.printf("a %d %d %d %d %d\n", dn.getUuid(), totalNodes - 2, 0, this.dataPacketCount, 0);
                writer.println();
            }

            /*
             * Path from SN, Dummy -> Sink is always 0 cost (not represented in the network)
             */
            writer.println("c SNs to Sink");
            for (SensorNode sn : this.sNodes) {
                writer.printf("a %d %d %d %d %d\n",
                        sn.getUuid(), totalNodes - 1, 0, this.storageCapacity, 0);
            }
            writer.println("c Dummy to Sink");
            writer.printf("a %d %d %d %d %d\n", totalNodes - 2, totalNodes - 1, 0, supply, 0);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private List<SensorNode> getMinCostPath(Map<SensorNode, Set<SensorNode>> graph, SensorNode start, SensorNode end) {
        Queue<Tuple<SensorNode, Long, SensorNode>> q = new PriorityQueue<>(Comparator.comparing(Tuple::second));
        Map<SensorNode, SensorNode> backPointers = new HashMap<>();
        for (SensorNode neighbor : graph.getOrDefault(start, Set.of())) {
            if (!(start.canTransmitTo(neighbor, 1) && neighbor.canReceiveFrom(start, 1))) {
                continue;
            }
            backPointers.put(start, null);
            q.offer(Tuple.of(neighbor, this.getCost(start, neighbor), start));
        }

        Tuple<SensorNode, Long, SensorNode> currPair;
        SensorNode currNode;
        SensorNode prevNode;
        long currCost;
        while (!q.isEmpty()) {
            currPair = q.poll();
            currNode = currPair.first();
            currCost = currPair.second();
            prevNode = currPair.third();

            if (backPointers.containsKey(currNode)) {
                continue;
            }

            if (!(prevNode.canTransmitTo(currNode, 1) && currNode.canReceiveFrom(prevNode, 1))) {
                continue;
            }

            backPointers.put(currNode, prevNode);
            for (SensorNode neighbor : graph.getOrDefault(currNode, Set.of())) {
                q.offer(Tuple.of(neighbor, currCost + this.getCost(currNode, neighbor), currNode));
            }

            if (currNode.equals(end)) {
                break;
            }
        }

        LinkedList<SensorNode> deque = new LinkedList<>();
        currNode = end;
        while (currNode != null) {
            deque.push(currNode);
            currNode = backPointers.getOrDefault(currNode, null);
        }
        return deque;
    }

    private long getCost(SensorNode from, SensorNode to) {
        return from.calculateTransmissionCost(to) + to.calculateReceivingCost();
    }

    private int getEdgeCount() {
        return this.dNodes.size() + ((this.sNodes.size() + 1) * this.dNodes.size()) + (this.sNodes.size() + 1);
    }

    public void setOverflowPackets(long overflowPackets) {
        this.dataPacketCount = overflowPackets;

        for (DataNode dn : this.dNodes) {
            dn.setOverflowPackets(overflowPackets);
        }
    }

    public void setStorageCapacity(long storageCapacity) {
        this.storageCapacity = storageCapacity;

        for (StorageNode sn : this.sNodes) {
            sn.setCapacity(storageCapacity);
        }
    }

    public void setBatteryCapacity(long batteryCapacity) {
        this.batteryCapacity = batteryCapacity;
        for (SensorNode sn : this.nodes) {
            sn.setBatteryCapacity(batteryCapacity);
        }
    }

    public long getBatteryCapacity() {
        return this.batteryCapacity;
    }

    @Override
    public boolean canSendPackets(DataNode dn, StorageNode sn, long packets) {
        List<SensorNode> path = this.getMinCostPath(dn, sn);
        return this.canSendPacketsAlong(path, packets);
    }

    @Override
    public boolean canSendPacketsAlong(List<SensorNode> path, long packets) {
        if (path.size() < 2) {
            return false;
        }

        long[] costDp = new long[path.size()];
        for (int index = 0; index < path.size(); index++) {
            costDp[index] = path.get(index).getEnergy();
        }

        SensorNode from, to;
        int fromIndex, toIndex;
        for (int index = 0; index < path.size() - 1; index++) {
            fromIndex = index;
            toIndex = index + 1;

            from = path.get(fromIndex);
            to = path.get(toIndex);

            costDp[fromIndex] -= from.calculateTransmissionCost(to) * packets;
            costDp[toIndex] -= to.calculateReceivingCost() * packets;
        }

        for (int index = 0; index < path.size(); index++) {
            if (costDp[index] < 0) {
                return false;
            }
        }

        return path.getLast().canStoreFrom(path.get(path.size() - 2), packets);
    }

    @Override
    public void sendPackets(DataNode dn, StorageNode sn, long packets) {
        if (!this.canSendPackets(dn, sn, packets)) {
            throw new IllegalArgumentException(
                    String.format("Cannot send %d packets from %s (%d/%d packets left) -> %s (%d/%d space left)\n",
                            packets, dn.getName(), dn.getPacketsLeft(), this.dataPacketCount,
                            sn.getName(), sn.getSpaceLeft(), this.storageCapacity));
        }

        List<SensorNode> path = this.getMinCostPath(dn, sn);
        this.sendPacketsAlong(path, packets);
    }

    @Override
    public void sendPacketsAlong(List<SensorNode> path, long packets) {
        if (!canSendPacketsAlong(path, packets)) {
            throw new IllegalArgumentException(
                    String.format("Cannot send %d packets along path %s\n", packets, path));
        }

        SensorNode tmpFrom;
        SensorNode tmpTo;
        for (int index = 0; index < path.size() - 1; index++) {
            tmpFrom = path.get(index);
            tmpTo = path.get(index + 1);

            if (index == 0) {
                tmpFrom.offloadTo(tmpTo, packets);
            } else {
                tmpFrom.transmitTo(tmpTo, packets);
            }

            if (index == path.size() - 2) {
                tmpTo.storeFrom(tmpFrom, packets);
            } else {
                tmpTo.receiveFrom(tmpFrom, packets);
            }
        }
    }

    @Override
    public void resetPackets() {
        for (SensorNode node : this.nodes) {
            node.resetPackets();
        }
    }

    @Override
    public void resetEnergy() {
        for (SensorNode node : this.nodes) {
            node.resetEnergy();
        }
    }

    @Override
    public long calculateProfitOf(DataNode from, StorageNode to) {
        long cost = this.calculateMinCost(from, to);
        return from.getOverflowPacketValue() - cost;
    }

    public SensorNode getSensorNodeByUuid(int uuid) {
        if (uuid < 1 || uuid > this.nodes.size()) {
            throw new IndexOutOfBoundsException(String.format("Invalid SensorNode ID %d", uuid));
        }
        return this.nodes.get(uuid - 1);
    }

    @Override
    public DataNode getDataNodeById(int id) {
        if (id < 1 || id > this.getDataNodeCount()) {
            throw new IndexOutOfBoundsException(String.format("Invalid DN ID %d", id));
        }
        return this.dNodes.get(id - 1);
    }

    @Override
    public StorageNode getStorageNodeById(int id) {
        if (id < 1 || id > this.getStorageNodeCount()) {
            throw new IndexOutOfBoundsException(String.format("Invalid SN ID %d", id));
        }
        return this.sNodes.get(id - 1);
    }

    @Override
    public TransitionNode getTransitionNodeById(int id) {
        if (id < 1 || id > this.getTransitionNodeCount()) {
            throw new IndexOutOfBoundsException(String.format("Invalid TN ID %d", id));
        }
        return this.tNodes.get(id - 1);
    }

    public String toString() {
        String packetValueRange = (this.minPacketValue == Integer.MIN_VALUE && this.maxPacketValue == Integer.MAX_VALUE)
                ? "<UNKNOWN>"
                : String.format("[%d, %d]", this.minPacketValue, this.maxPacketValue);

        return String.format(
                """
                        Sensor Network Configuration:
                        =============================
                        %,f m x %,f m Area
                        %,f Transmission Range
                        %,d Battery Capacity

                        %d total Sensor Nodes
                        - %d Data Nodes (%,d packets each, values between range %s)
                        - %d Storage Nodes (%,d storage capacity each)
                        """,

                this.length, this.width,
                this.transmissionRange,
                this.batteryCapacity,

                this.nodes.size(),
                this.dNodes.size(), this.dataPacketCount, packetValueRange,
                this.sNodes.size(), this.storageCapacity);
    }
}
