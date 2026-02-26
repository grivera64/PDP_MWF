package io.grivera.pdp.algo;

import java.util.List;
import java.util.Map;

import com.google.ortools.Loader;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;

import io.grivera.pdp.network.Network;
import io.grivera.pdp.network.node.DataNode;
import io.grivera.pdp.network.node.SensorNode;
import io.grivera.pdp.network.node.StorageNode;
import io.grivera.pdp.util.Pair;

public class MWFAlgorithm extends NetworkAlgorithm {

    static {
        Loader.loadNativeLibraries();
    }
    private long[][] cachedX;
    private long cachedObjective;
    private long totalCost;
    private long totalPackets;
    
    private Map<SensorNode, List<Pair<List<SensorNode>, Long>>> history;

    public MWFAlgorithm(Network network) {
        super(network);
    }

    public void silentRun() {
        super.silentRun();
        this.solveIlp();
        this.parseIlp();
    }

    @Override
    public RunDetails run() {
        super.run();
        long runTime = this.solveIlp();
        long parseTime = this.parseIlp();
        return new RunDetails(runTime, parseTime);
    }

    @Override
    public long getTotalValue() {
        super.getTotalValue();
        return this.cachedObjective;
    }

    @Override
    public long getTotalCost() {
        super.getTotalCost();
        return this.totalCost;
    }

    @Override
    public long getTotalProfit() {
        super.getTotalProfit();
        return this.cachedObjective;
    }

    @Override
    public long getTotalPackets() {
        super.getTotalPackets();
        return this.totalPackets;
    }

    private long solveIlp() {
        Network network = this.getNetwork();

        List<DataNode> dNodes = network.getDataNodes();
        List<StorageNode> sNodes = network.getStorageNodes();
        List<SensorNode> nodes = network.getSensorNodes();

        int infinity = Integer.MAX_VALUE;
        int n = network.getSensorNodes().size();
        int sourceIndex = 0;
        int sinkIndex = 2 * n + 1;
        MPSolver solver = MPSolver.createSolver("GLOP");
        MPVariable[][] x = new MPVariable[2 * n + 2][2 * n + 2];

        // create 2d array of decision variable x
        // x_i_j value represents number of flows from i to j

        for (int fromIndex = 0; fromIndex < x.length; fromIndex++) {
            for (int toIndex = 0; toIndex < x[fromIndex].length; toIndex++) {
                x[fromIndex][toIndex] = solver.makeIntVar(0, infinity, String.format("x_%d_%d", fromIndex, toIndex));
            }
        }
        // if i has no edge to j, x_i_j=0
        makeMaxFlowEdges(x, solver);

        // constraint (4):
        // indicates the maximum number of packets data node i can offload, di.
        // the initial number of data packets data node i has
        MPConstraint[] four = new MPConstraint[dNodes.size()];
        int constraintIndex = 0;
        for (DataNode dn : dNodes) {
            four[constraintIndex] = solver.makeConstraint(-infinity, dn.getOverflowPackets(),
                    String.format("%s_in", dn.getName()));
            four[constraintIndex].setCoefficient(x[sourceIndex][dn.getUuid()], 1);
            constraintIndex++;
        }

        // constraint (5):
        // indicates the maximum number of packets storage node i can store is mi, the
        // storage capacity of storage node i
        MPConstraint[] five = new MPConstraint[sNodes.size()];
        constraintIndex = 0;
        for (StorageNode sn : sNodes) {
            five[constraintIndex] = solver.makeConstraint(-infinity, sn.getCapacity(),
                    String.format("%s_out", sn.getName()));
            five[constraintIndex].setCoefficient(x[sn.getUuid() + n][sinkIndex], 1);
            constraintIndex++;
        }

        // constraint (6):
        // the flow conservation for data nodes, where the number of its own data
        // packets offloaded plus the number of data packets it relays for other data
        // nodes equals the number of data packets it transmits.
        //
        // x_si' + sum(x_j"i') - sum(x_i"j') == 0
        MPConstraint[] six = new MPConstraint[dNodes.size()];
        constraintIndex = 0;
        for (DataNode dn : dNodes) {
            six[constraintIndex] = solver.makeConstraint(0, 0);
            six[constraintIndex].setCoefficient(x[sourceIndex][dn.getUuid()], 1);
            for (SensorNode sn : nodes) {
                six[constraintIndex].setCoefficient(x[sn.getUuid() + n][dn.getUuid()], 1);
                six[constraintIndex].setCoefficient(x[dn.getUuid() + n][sn.getUuid()], -1);
            }
            constraintIndex++;
        }

        // constraint (7):
        // the flow conservation for storage nodes, which says that data packets a
        // storage node receives are either relayed to other nodes or stored by this
        // storage node
        //
        // sum(x_j"i') - sum(x_i"j') - x_i"t == 0
        MPConstraint[] seven = new MPConstraint[sNodes.size()];
        constraintIndex = 0;
        for (StorageNode sn : sNodes) {
            seven[constraintIndex] = solver.makeConstraint(0, 0);
            seven[constraintIndex].setCoefficient(x[sn.getUuid() + n][sinkIndex], -1);
            for (SensorNode snp : nodes) {
                seven[constraintIndex].setCoefficient(x[snp.getUuid() + n][sn.getUuid()], 1);

                seven[constraintIndex].setCoefficient(x[sn.getUuid() + n][snp.getUuid()], -1);
            }
            constraintIndex++;
        }

        // constraint(8):
        // (8) and (9) represents the energy constraints for data nodes and storage
        // nodes respectively
        // in our work we don't consider the storage cost, so its just one constraint
        //
        // Er_i + sum(x_j"i') + Et_i * sum(x_i"j') <= E_i
        MPConstraint[] eight = new MPConstraint[nodes.size()];
        constraintIndex = 0;
        for (SensorNode sn : nodes) {
            eight[constraintIndex] = solver.makeConstraint(-infinity, sn.getEnergy());
            for (SensorNode snp : network.getNeighbors(sn)) {
                eight[constraintIndex].setCoefficient(x[snp.getUuid() + n][sn.getUuid()], sn.calculateReceivingCost());
                eight[constraintIndex].setCoefficient(x[sn.getUuid() + n][snp.getUuid()],
                        sn.calculateTransmissionCost(snp));

            }
            constraintIndex++;
        }

        // set Objective, (maximize flow from source to data in nodes)
        MPObjective objective = solver.objective();
        for (DataNode dn : dNodes) {
            objective.setCoefficient(x[sourceIndex][dn.getUuid()], dn.getOverflowPacketValue());
        }
        objective.setMaximization();

        // solve
        long runTime = -System.nanoTime();
        final MPSolver.ResultStatus resultStatus = solver.solve();
        runTime += System.nanoTime();
        switch (resultStatus) {
            case OPTIMAL:
                break;
            default:
                throw new IllegalStateException("The network is not ILP (Weighted) feasible!");
        }

        // Cache the variables used
        this.cachedX = new long[x.length][x[x.length - 1].length];
        for (int row = 0; row < this.cachedX.length; row++) {
            for (int col = 0; col < this.cachedX[row].length; col++) {
                this.cachedX[row][col] = Math.round(x[row][col].solutionValue());
            }
        }
        this.cachedObjective = (long) objective.value();
        return runTime;
    }

    private void makeMaxFlowEdges(MPVariable[][] x, MPSolver solver) {
        Network network = this.getNetwork();

        List<DataNode> dNodes = network.getDataNodes();
        List<StorageNode> sNodes = network.getStorageNodes();
        List<SensorNode> nodes = network.getSensorNodes();

        // make all edges from the source node to every node that is NOT a data in-node
        // capacity 0
        int n = nodes.size();
        int sourceIndex = 0;
        int sinkIndex = 2 * n + 1;
        MPConstraint c = solver.makeConstraint(0.0, 0.0, "no Edge from i to j");
        for (SensorNode sn : nodes) {
            if (!(sn instanceof DataNode)) {
                // storage in-node
                c.setCoefficient(x[sourceIndex][sn.getUuid()], 1);
                // storage out-node
                c.setCoefficient(x[sourceIndex][sn.getUuid() + n], 1);
            } else {
                // data out-node
                c.setCoefficient(x[sourceIndex][sn.getUuid() + n], 1);

            }
        }
        // sink node
        c.setCoefficient(x[sourceIndex][sinkIndex], 1);

        // make all edges from the data in-nodes to every node that is NOT its
        // respective out-node capacity 0

        for (DataNode dn : dNodes) {

            // source node
            c.setCoefficient(x[dn.getUuid()][0], 1);

            // other data in-nodes
            for (DataNode dnp : dNodes) {
                if (dnp.equals(dn)) {
                    continue;
                }
                c.setCoefficient(x[dn.getUuid()][dnp.getUuid()], 1);
                // all other data out-nodes
                c.setCoefficient(x[dn.getUuid()][dnp.getUuid() + n], 1);
            }

            // storage nodes
            for (StorageNode sn : sNodes) {
                // in-node
                c.setCoefficient(x[dn.getUuid()][sn.getUuid()], 1);
                // out-node
                c.setCoefficient(x[dn.getUuid()][sn.getUuid() + n], 1);
            }
            // sink node
            c.setCoefficient(x[dn.getUuid()][sinkIndex], 1);
        }

        // make all edges from data out-node to:
        // source capacity 0 (1)
        // its respective in-node capacity 0 (2)
        // any data in-node it is NOT directly connected to capacity 0 (3)
        // any other data out-node capacity 0 (4)
        // any storage in- node it is not directly connected to capacity 0 (5)
        // every storage out-node capacity 0 (6)
        // sink capacity 0 (7)
        for (DataNode dn : dNodes) {
            // (1)
            c.setCoefficient(x[dn.getUuid() + n][0], 1);
            // (2)
            c.setCoefficient(x[dn.getUuid() + n][dn.getUuid()], 1);
            for (DataNode dnp : dNodes) {
                if (dn.getUuid() == dnp.getUuid()) {
                    continue;
                }
                if (!network.isConnected(dnp, dn)) {
                    // (3)
                    c.setCoefficient(x[dn.getUuid() + n][dnp.getUuid()], 1);
                }
                // (4)
                c.setCoefficient(x[dn.getUuid() + n][dnp.getUuid() + n], 1);
            }
            for (StorageNode sn : sNodes) {
                if (!network.isConnected(sn, dn)) {
                    // (5)
                    c.setCoefficient(x[dn.getUuid() + n][sn.getUuid()], 1);
                }
                // (6)
                c.setCoefficient(x[dn.getUuid() + n][sn.getUuid() + n], 1);
            }
            // (7)
            c.setCoefficient(x[dn.getUuid() + n][sinkIndex], 1);
        }

        // make all edges from storage in-nodes to any other node that is not its
        // respective out-node capacity 0
        for (StorageNode sn : sNodes) {
            // source node
            c.setCoefficient(x[sn.getUuid()][0], 1);

            for (DataNode dn : dNodes) {
                // data nodes
                // in-nodes
                c.setCoefficient(x[sn.getUuid()][dn.getUuid()], 1);
                // out-nodes
                c.setCoefficient(x[sn.getUuid()][dn.getUuid() + n], 1);
            }

            for (StorageNode snp : sNodes) {
                if (snp.getUuid() == sn.getUuid()) {
                    continue;
                }
                // other storage in-nodes
                c.setCoefficient(x[sn.getUuid()][snp.getUuid()], 1);
                // all other storage out-nodes
                c.setCoefficient(x[sn.getUuid()][snp.getUuid() + n], 1);
            }
            // sink
            c.setCoefficient(x[sn.getUuid()][sinkIndex], 1);
        }
        // make all edges from storage out-nodes to
        // source node capacity 0
        // data in-nodes that it is not directly connected to capacity 0
        // data out-nodes capacity 0
        // its respective storage in-node capcity 0
        // all other storage in-nodes it is not connected to capacity 0
        // all other other storage out-nodes capacity 0
        for (StorageNode sn : sNodes) {
            // source
            c.setCoefficient(x[sn.getUuid() + n][0], 1);
            for (DataNode dn : dNodes) {
                if (!network.isConnected(dn, sn)) {
                    // data in-nodes it is not connected to directly
                    c.setCoefficient(x[sn.getUuid() + n][dn.getUuid()], 1);
                }
                // data out-nodes
                c.setCoefficient(x[sn.getUuid() + n][dn.getUuid() + n], 1);
            }
            // its respective in-node
            c.setCoefficient(x[sn.getUuid() + n][sn.getUuid()], 1);

            for (StorageNode snp : sNodes) {
                if (snp.getUuid() == sn.getUuid()) {
                    continue;
                }
                if (!network.isConnected(sn, snp)) {
                    // storage in-node it is not directly connected to
                    c.setCoefficient(x[sn.getUuid() + n][snp.getUuid()], 1);
                }
                // all other storage out-nodes
                c.setCoefficient(x[sn.getUuid() + n][snp.getUuid() + n], 1);
            }
        }
        // make all edges from the sink node to any other node capacity 0
        // source
        c.setCoefficient(x[sinkIndex][0], 1);
        // sensor in and out-nodes
        for (SensorNode sn : nodes) {
            c.setCoefficient(x[sinkIndex][sn.getUuid()], 1);
            c.setCoefficient(x[sinkIndex][sn.getUuid() + n], 1);
        }
        // finally if i==j, x[i][j]=0
        for (int i = 0; i < x.length; i++) {
            c.setCoefficient(x[i][i], 1);
        }

    }

    private long parseIlp() {
        long[][] oldX = new long[this.cachedX.length][this.cachedX[this.cachedX.length - 1].length];
        for (int row = 0; row < this.cachedX.length; row++) {
            System.arraycopy(this.cachedX[row], 0, oldX[row], 0, this.cachedX[row].length);
        }

        final Network network = this.getNetwork();
        final List<StorageNode> sns = network.getStorageNodes();
        final List<SensorNode> nodes = network.getSensorNodes();
        final int n = network.getSensorNodeCount();
        // Source index is 0, since is 2 * n + 1
        final int sinkIndex = 2 * n + 1;

        long parseTime = -System.nanoTime();
        this.totalPackets = sns.parallelStream()
                .mapToLong(sn -> this.cachedX[sn.getUuid() + n][sinkIndex])
                .sum();

        this.totalCost = nodes.parallelStream()
                .flatMapToLong(node1 -> nodes.stream()
                        .filter(node2 -> !node1.equals(node2))
                        .filter(node2 -> this.cachedX[node1.getUuid() + n][node2.getUuid()] != 0)
                        .mapToLong(node2 -> this.cachedX[node1.getUuid() + n][node2.getUuid()]
                                * network.calculateMinCost(node1, node2)))
                .sum();

        parseTime += System.nanoTime();

        this.cachedX = oldX;
        return parseTime;
    }
}
