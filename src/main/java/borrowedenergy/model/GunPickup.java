package borrowedenergy.model;

public final class GunPickup extends Entity implements Interactable {
    private boolean collected;
    public GunPickup(double x,double y) { super(x,y); }
    public boolean collected() { return collected; }
    public String label() { return "PULSE BLASTER"; }
    public String hint() { return "PICK UP BLASTER / HOLD F OR CLICK TO FIRE"; }
    void collect() { collected=true; }
}
