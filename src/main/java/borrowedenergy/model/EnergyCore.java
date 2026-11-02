package borrowedenergy.model;

public final class EnergyCore extends Entity implements Interactable {
    public enum State { GROUND, CARRIED, SOCKETED }
    private State state = State.GROUND;
    private PowerSocket socket;
    public EnergyCore(double x, double y) { super(x, y); }
    public State state() { return state; }
    public PowerSocket socket() { return socket; }
    public String label() { return "ION BATTERY"; }
    public String hint() { return "PICK UP ION BATTERY"; }
    void carry() { state = State.CARRIED; socket = null; }
    void insert(PowerSocket target) { state = State.SOCKETED; socket = target; x = target.x(); y = target.y(); }
    void follow(Player player) { if (state == State.CARRIED) { x = player.x(); y = player.y(); } }
    void place(double x, double y) { state = State.GROUND; socket = null; this.x = x; this.y = y; }
}
