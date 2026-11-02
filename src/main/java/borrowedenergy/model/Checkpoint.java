package borrowedenergy.model;

/** A recall pad activates automatically on arrival. */
public final class Checkpoint extends Entity {
    private final String label;
    private boolean active;
    public Checkpoint(double x,double y,String label,boolean active) { super(x,y);this.label=label;this.active=active; }
    public String label() { return label; }
    public boolean active() { return active; }
    void activate() { active=true; }
}
