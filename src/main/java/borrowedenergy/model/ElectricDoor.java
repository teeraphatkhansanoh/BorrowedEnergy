package borrowedenergy.model;

public final class ElectricDoor extends Entity {
    private final PowerCircuit circuit;
    private final double width,height;
    ElectricDoor(double x,double y,PowerCircuit circuit) { this(x,y,1,2,circuit); }
    ElectricDoor(double x,double y,double width,double height,PowerCircuit circuit) {
        super(x,y);this.width=width;this.height=height;this.circuit=circuit;
    }
    public PowerCircuit circuit() { return circuit; }
    public boolean open() { return circuit.complete(); }
    public double width() { return width; }
    public double height() { return height; }
}
