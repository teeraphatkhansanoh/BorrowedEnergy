package borrowedenergy.input;

import java.util.EnumSet;

/** Held movement actions. Interaction is handled separately on the key-down edge. */
public final class InputState {
    public enum Action { UP, DOWN, LEFT, RIGHT }
    private final EnumSet<Action> held = EnumSet.noneOf(Action.class);
    private boolean firing;
    public void set(Action action, boolean pressed) {
        if (pressed) held.add(action); else held.remove(action);
    }
    public double dx() { return (held.contains(Action.RIGHT) ? 1 : 0) - (held.contains(Action.LEFT) ? 1 : 0); }
    public double dy() { return (held.contains(Action.DOWN) ? 1 : 0) - (held.contains(Action.UP) ? 1 : 0); }
    public void setFiring(boolean pressed) { firing = pressed; }
    public boolean firing() { return firing; }
    public void clear() { held.clear(); firing = false; }
}
