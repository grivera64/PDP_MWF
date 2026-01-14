package io.grivera.pdp.network;

import io.grivera.pdp.network.node.DataNode;
import io.grivera.pdp.network.node.SensorNode;
import io.grivera.pdp.network.node.StorageNode;
import io.grivera.pdp.network.node.TransitionNode;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents the basis of a Network with Data and Storage
 * Nodes
 */
public interface Network {
    double getWidth();

    double getLength();

    List<SensorNode> getSensorNodes();

    int getSensorNodeCount();

    List<DataNode> getDataNodes();

    int getDataNodeCount();

    List<StorageNode> getStorageNodes();

    int getStorageNodeCount();

    List<TransitionNode> getTransitionNodes();

    int getTransitionNodeCount();

    /**
     * Tests whether all the nodes are directly or indirectly connected with each
     * other.
     *
     * @return true if and only if all the nodes are directly or indirectly
     *         connected
     *         with each other; otherwise false
     */
    boolean isConnected();

    /**
     * Tests whether there is enough storage for all the overflow packets in the
     * network.
     *
     * @return true if and only if there is enough storage for all the overflow
     *         packets in the network;
     *         otherwise false
     */
    boolean isFeasible();

    boolean isMaxFeasible();

    boolean isStoreFeasible();

    Map<SensorNode, Set<SensorNode>> getAdjacencyList(); // Returns the connection of nodes (using ID)

    long calculateMinCost(SensorNode from, SensorNode to);

    /**
     * Returns the sensor nodes in the min-cost path between the from and to sensor
     * nodes
     *
     * @param from the starting sensor node
     * @param to   the ending sensor node
     * @return a list of the sensor nodes in the min-cost path between the from and
     *         to sensor nodes
     */
    List<SensorNode> getMinCostPath(SensorNode from, SensorNode to);

    /**
     * Calculates the cost of a given path.
     *
     * @param path the path between two sensor nodes
     * @return the cost of the given path
     */
    long calculateCostOfPath(List<SensorNode> path);

    /**
     * Saves the network into a .sn file format.
     *
     * @param fileName the path to the file to save to
     */
    void save(String fileName);

    Set<SensorNode> getNeighbors(SensorNode node);

    boolean isConnected(SensorNode sensorNode1, SensorNode sensorNode2);

    /**
     * Saves the network in the <b>DIMAC</b> format
     * that can be used for the min-cost flow program
     * <a href="https://github.com/iveney/cs2">CS2</a>.
     *
     * @param fileName the path to the file to save to
     */
    void saveAsCsInp(String fileName);

    void setOverflowPackets(long overflowPackets);

    void setStorageCapacity(long storageCapacity);

    void setBatteryCapacity(long batteryCapacity);

    long getBatteryCapacity();

    boolean canSendPackets(DataNode dn, StorageNode sn, long packets);

    boolean canSendPacketsAlong(List<SensorNode> path, long packets);

    void sendPackets(DataNode dn, StorageNode sn, long packets);

    void sendPacketsAlong(List<SensorNode> path, long packets);

    void resetPackets();

    void resetEnergy();

    long calculateProfitOf(DataNode from, StorageNode to);

    SensorNode getSensorNodeByUuid(int uuid);

    DataNode getDataNodeById(int id);

    StorageNode getStorageNodeById(int id);

    TransitionNode getTransitionNodeById(int id);
}
