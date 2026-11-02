package borrowedenergy.model;

import java.awt.geom.Point2D;
import java.util.List;

/** The receiver latches permanently when a correctly routed charge completes. */
public final class PowerCircuit {
    private final int id;
    private final String name;
    private final PowerSocket socket;
    private final List<Relay> relays;
    private final List<Point2D.Double> waypoints;
    private final double chargeSeconds;
    private double progress;
    private boolean complete;
    PowerCircuit(int id, String name, double sx, double sy, List<Relay> relays,
                 double tx, double ty, double chargeSeconds) {
        this.id = id; this.name = name; this.relays = List.copyOf(relays); this.chargeSeconds = chargeSeconds;
        socket = new PowerSocket(sx, sy, this);
        var points = new java.util.ArrayList<Point2D.Double>();
        points.add(new Point2D.Double(sx, sy));
        for (Relay relay : relays) points.add(new Point2D.Double(relay.x(), relay.y()));
        points.add(new Point2D.Double(tx, ty));
        waypoints = List.copyOf(points);
    }
    public int id() { return id; }
    public String name() { return name; }
    public PowerSocket socket() { return socket; }
    public List<Relay> relays() { return relays; }
    public List<Point2D.Double> waypoints() { return waypoints; }
    public boolean aligned() { return relays.stream().allMatch(Relay::aligned); }
    public boolean powered() { return socket.hasCore() && aligned(); }
    public boolean complete() { return complete; }
    public double progress() { return progress; }
    void update(double dt) {
        if (complete) return;
        if (powered()) {
            progress = Math.min(1, progress + dt / chargeSeconds);
            if (progress >= 1 - 1e-9) { progress = 1; complete = true; }
        } else progress = Math.max(0, progress - dt * .6);
    }
}
