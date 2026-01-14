package io.grivera.pdp.network.node;

/**
 * Represents the basic form of a Sensor Node in a Sensor
 * Network
 */
public abstract class SensorNode implements Comparable<SensorNode> {

    private static int uuidCounter = 1;

    protected static int BITS_PER_PACKET = 3200;
    protected static final double E_elec = 100e-9;
    protected static final double E_amp = 100e-12;

    private int uuid;
    private final double x, y, tr;
    private long batteryCapacity;
    private long energy;
    private String name;

    public SensorNode(double x, double y, double tr, String name, long c) {
        this.x = x;
        this.y = y;
        this.tr = tr;
        this.name = name;
        this.uuid = uuidCounter++;
        this.setBatteryCapacity(c);
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public int getUuid() {
        return this.uuid;
    }

    /**
     * Tests if this Sensor Node is within range of the specified Sensor Node
     *
     * @param o the specified Sensor Node
     * @return true if and only if this Sensor Node is within transmission range
     *         of the specified Sensor Node; otherwise false
     */
    public boolean inRangeOf(SensorNode o) {
        return this.distanceTo(o) <= Math.min(this.tr, o.tr) + 0.0001;
    }

    /**
     * Calculates the distance between this Sensor Node and the specified Sensor
     * Node
     *
     * @param o the specified Sensor Node
     * @return the distance between the two nodes (in meters)
     */
    public double distanceTo(SensorNode o) {
        return Math.sqrt(Math.pow(this.x - o.x, 2) + Math.pow(this.y - o.y, 2));
    }

    @Override
    public String toString() {
        return String.format("%-14s(%.6f, %.6f) [%d]", this.getName(), this.getX(), this.getY(), this.getUuid());
    }

    public String getName() {
        return this.name;
    }

    public void setBatteryCapacity(long batteryCapacity) {
        this.batteryCapacity = batteryCapacity;
        this.resetEnergy();
    }

    public long getEnergy() {
        return this.energy;
    }

    public boolean hasEnergy() {
        return this.energy > 0;
    }

    public void resetEnergy() {
        this.energy = this.batteryCapacity;
    }

    public boolean canTransmitTo(SensorNode receiverNode, long packets) {
        return this.calculateTransmissionCost(receiverNode) * packets <= this.energy;
    }

    public void transmitTo(SensorNode receiverNode, long packets) {
        if (!this.canTransmitTo(receiverNode, packets)) {
            throw new IllegalArgumentException(
                    String.format("%s cannot transmit %d packets to %s (%d microJ > %d/%d microJ left)", this.getName(),
                            packets, receiverNode.getName(), this.calculateTransmissionCost(receiverNode) * packets,
                            this.energy, this.batteryCapacity));
        }
        if (receiverNode.equals(this)) {
            return;
        }
        this.energy -= this.calculateTransmissionCost(receiverNode) * packets;
    }

    /**
     * Calculates the cost to transmit/relay a data packet from this Sensor Node to
     * a specified receiver Sensor Node
     *
     * @param receiverNode the Sensor Node that this Sensor Node would
     *                     transmit/relay one data packet to
     * @return the cost to transmit/relay one data packet from this Sensor Node to
     *         the receiver Sensor Node
     */
    public long calculateTransmissionCost(SensorNode receiverNode) {
        double cost = BITS_PER_PACKET * (E_elec + E_amp * Math.pow(this.distanceTo(receiverNode), 2));
        return Math.round(cost * Math.pow(10, 6));
    }

    public boolean canReceiveFrom(SensorNode senderNode, long packets) {
        return this.calculateReceivingCost() * packets <= this.energy;
    }

    public void receiveFrom(SensorNode senderNode, long packets) {
        if (!this.canReceiveFrom(senderNode, packets)) {
            throw new IllegalArgumentException(String.format(
                    "%s cannot receive %d packets from %s (%d > %d/%d microJ left)", this.getName(), packets,
                    senderNode.getName(), this.calculateReceivingCost() * packets, this.energy, this.batteryCapacity));
        }

        if (senderNode.equals(this)) {
            return;
        }
        this.energy -= this.calculateReceivingCost() * packets;
    }

    /**
     * Calculates the cost for this Sensor Node to receive one transmitted/relayed
     * data packet
     *
     * @return the cost for this Sensor Node to receive a transmitted/relayed data
     *         packet
     */
    public long calculateReceivingCost() {
        double cost = BITS_PER_PACKET * E_elec;
        return Math.round(cost * Math.pow(10, 6));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SensorNode sn)) {
            return false;
        }
        return this.getUuid() == sn.getUuid();
    }

    @Override
    public int compareTo(SensorNode sn) {
        return Integer.compare(this.getId(), sn.getId());
    }

    @Override
    public int hashCode() {
        return this.uuid;
    }

    public static void resetCounter() {
        uuidCounter = 1;
    }

    public static int getBitsPerPacket() {
        return BITS_PER_PACKET;
    }

    public static void setBitsPerPacket(int bitsPerPacket) {
        BITS_PER_PACKET = bitsPerPacket;
    }

    public abstract void resetPackets();

    public abstract int getId();

    public abstract boolean canOffloadTo(SensorNode receiverNode, long packets);

    public abstract void offloadTo(SensorNode receiverNode, long packets);

    public abstract boolean canStoreFrom(SensorNode senderNode, long packets);

    public abstract void storeFrom(SensorNode senderNode, long packets);
}
