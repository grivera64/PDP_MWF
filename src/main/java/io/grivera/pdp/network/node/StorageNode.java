package io.grivera.pdp.network.node;

/**
 * Represents a Sensor Node in a Network that has storage
 * space for overflow data packets.
 *
 * @see SensorNode
 */
public class StorageNode extends SensorNode {

    private static final double E_store = 100e-9;

    private static int idCounter = 1;
    private int id;
    private long capacity;
    private long usedSpace;

    public StorageNode(double x, double y, double tr, long power, long capacity) {
        super(x, y, tr, String.format("SN%02d", idCounter), power);
        this.id = idCounter++;
        this.setCapacity(capacity);
    }

    public void setCapacity(long capacity) {
        this.capacity = capacity;
        this.usedSpace = 0;
    }

    public long getCapacity() {
        return this.capacity;
    }

    public long getUsedSpace() {
        return this.usedSpace;
    }

    public long getSpaceLeft() {
        return this.capacity - this.usedSpace;
    }

    public boolean isFull() {
        return this.usedSpace >= this.capacity;
    }

    @Override
    public boolean canStoreFrom(SensorNode senderNode, long packets) {
        return this.canReceiveFrom(senderNode, packets) && this.usedSpace + packets <= this.capacity;
    }

    @Override
    public void storeFrom(SensorNode senderNode, long packets) {
        if (!this.canStoreFrom(senderNode, packets)) {
            throw new IllegalArgumentException(String.format("%s with %d spaces left cannot store %d packets",
                    this.getName(), this.getSpaceLeft(), packets));
        }

        super.receiveFrom(senderNode, packets);
        this.usedSpace += packets;
    }

    @Override
    public void resetPackets() {
        this.usedSpace = 0;
    }

    @Override
    public int getId() {
        return this.id;
    }

    public long calculateStorageCost() {
        double cost = this.usedSpace * BITS_PER_PACKET * E_store;
        return Math.round(cost * Math.pow(10, 6));
    }

    public static void resetCounter() {
        idCounter = 1;
    }

    @Override
    public boolean canOffloadTo(SensorNode receiverNode, long packets) {
        return false;
    }

    @Override
    public void offloadTo(SensorNode receiverNode, long packets) {
        throw new UnsupportedOperationException("Storage Nodes cannot offload packets");
    }
}
