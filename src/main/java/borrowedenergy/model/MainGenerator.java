package borrowedenergy.model;

public final class MainGenerator extends Entity {
    private final PowerCircuit circuit;
    MainGenerator(double x, double y, PowerCircuit circuit) { super(x, y); this.circuit = circuit; }
    public PowerCircuit circuit() { return circuit; }
    public boolean online() { return circuit.complete(); }
}
