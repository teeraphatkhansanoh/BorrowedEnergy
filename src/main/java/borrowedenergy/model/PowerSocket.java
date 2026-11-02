package borrowedenergy.model;

public final class PowerSocket extends Entity implements Interactable {
    private final PowerCircuit circuit;
    private boolean hasCore;
    PowerSocket(double x, double y, PowerCircuit circuit) { super(x, y); this.circuit = circuit; }
    public PowerCircuit circuit() { return circuit; }
    public boolean hasCore() { return hasCore; }
    void setHasCore(boolean value) { hasCore = value; }
    public String label() { return "SOCKET " + String.format("%02d", circuit.id()); }
    public String hint() { return hasCore ? "RETRIEVE BATTERY" : "INSERT BATTERY"; }
}
