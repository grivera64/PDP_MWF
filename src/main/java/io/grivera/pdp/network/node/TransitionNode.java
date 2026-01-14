package io.grivera.pdp.network.node;

public class TransitionNode extends SensorNode {
    private static int idCounter = 1;
    private int id;

    public TransitionNode(double x, double y, double tr, long power) {
        super(x, y, tr, String.format("TN%02d", idCounter), power);
        this.id = idCounter++;
    }

    @Override
    public void resetPackets() {
        /* Do Nothing */
    }

    @Override
    public int getId() {
        return this.id;
    }

    public static void resetCounter() {
        idCounter = 1;
    }

    @Override
    public boolean canStoreFrom(SensorNode senderNode, long packets) {
        return false;
    }

    @Override
    public void storeFrom(SensorNode senderNode, long packets) {
        throw new UnsupportedOperationException("Transition Nodes cannot store packets");
    }

    @Override
    public boolean canOffloadTo(SensorNode receiverNode, long packets) {
        return false;
    }

    @Override
    public void offloadTo(SensorNode receiverNode, long packets) {
        throw new UnsupportedOperationException("Transition Nodes cannot offload packets");
    }

}
