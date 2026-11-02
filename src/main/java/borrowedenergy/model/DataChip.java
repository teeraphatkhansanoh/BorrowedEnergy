package borrowedenergy.model;

/** Optional archive collectible; never required for stage completion. */
public final class DataChip extends Entity implements Interactable {
    private final String label;
    private boolean collected;
    public DataChip(double x,double y,String label) { super(x,y);this.label=label; }
    public boolean collected() { return collected; }
    public String label() { return label; }
    public String hint() { return "RECOVER DATA CHIP"; }
    void collect() { collected=true; }
}
