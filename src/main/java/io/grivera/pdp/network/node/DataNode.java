package io.grivera.pdp.network.node;

/**
 * Represents a Sensor Node in a Network that has overflow
 * data packets to store.
 *
 * @see SensorNode
 */
public class DataNode extends SensorNode {

    private static int idCounter = 1;
    private int id;
    private long overflowPackets;
    private long overflowPacketsValue;
    private long packetsLeft;

    public DataNode(double x, double y, double tr, long power, long overflowPackets, long overflowPacketsValue) {
        super(x, y, tr, String.format("DN%02d", idCounter), power);
        this.id = idCounter++;
        this.setOverflowPackets(overflowPackets);
        this.overflowPacketsValue = overflowPacketsValue;
    }

    public void setOverflowPackets(long overflowPackets) {
        this.overflowPackets = overflowPackets;
        this.packetsLeft = overflowPackets;
    }

    public long getOverflowPackets() {
        return this.overflowPackets;
    }

    public boolean isEmpty() {
        return this.packetsLeft < 1;
    }

    @Override
    public boolean canOffloadTo(SensorNode receiver, long deltaPackets) {
        return this.canTransmitTo(receiver, deltaPackets) && this.packetsLeft - deltaPackets >= 0;
    }

    @Override
    public void offloadTo(SensorNode receiver, long packets) {
        if (!this.canOffloadTo(receiver, packets)) {
            throw new IllegalArgumentException(String.format("%s with %d packets cannot offload %d packets",
                    this.getName(), this.packetsLeft, packets));
        }
        this.transmitTo(receiver, packets);
        this.packetsLeft -= packets;
    }

    public void discardPackets(int packets) {
        if (packets > this.packetsLeft) {
            throw new IllegalArgumentException(String.format("%s with %d packets cannot discard %d packets",
                    this.getName(), this.packetsLeft, packets));
        }
        this.overflowPackets -= packets;
    }

    @Override
    public boolean canStoreFrom(SensorNode senderNode, long packets) {
        return false;
    }

    @Override
    public void storeFrom(SensorNode senderNode, long packets) {
        throw new UnsupportedOperationException("Data Nodes cannot store packets");
    }

    @Override
    public void resetPackets() {
        this.packetsLeft = this.overflowPackets;
    }

    @Override
    public int getId() {
        return this.id;
    }

    public long getPacketsLeft() {
        return this.packetsLeft;
    }

    public long getOverflowPacketValue() {
        return this.overflowPacketsValue;
    }

    public static void resetCounter() {
        idCounter = 1;
    }

}
