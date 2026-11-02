package borrowedenergy.model;

/** A rotatable output arrow. Its engraved marker specifies the required route. */
public final class Relay extends Entity implements Interactable {
    private final String label;
    private final int requiredOrientation;
    private int orientation;
    public Relay(double x, double y, String label, int required, int initial) {
        super(x, y); this.label = label; requiredOrientation = required; orientation = initial;
    }
    public int orientation() { return orientation; }
    public int requiredOrientation() { return requiredOrientation; }
    public boolean aligned() { return orientation == requiredOrientation; }
    public void rotate() { orientation = (orientation + 1) % 4; }
    public String label() { return label; }
    public String hint() { return "ROTATE RELAY  /  MATCH THE SMALL MARKER"; }
}
