package borrowedenergy.model;

/** Predictable shuttle patrol; never chases or blocks the player. */
public final class PatrolDrone extends Entity {
    private final double ax,ay,bx,by,period,offset;
    private double heading;
    public PatrolDrone(double ax,double ay,double bx,double by,double period,double offset) {
        super(ax,ay);this.ax=ax;this.ay=ay;this.bx=bx;this.by=by;this.period=period;this.offset=offset;update(0);
    }
    public double radius() { return .42; }
    public double heading() { return heading; }
    public double startX() { return ax; }
    public double startY() { return ay; }
    public double endX() { return bx; }
    public double endY() { return by; }
    void update(double time) {
        double angle=time/period*Math.PI*2+offset,t=(Math.sin(angle)+1)/2;
        x=ax+(bx-ax)*t;y=ay+(by-ay)*t;
        heading=Math.atan2(by-ay,bx-ax)+(Math.cos(angle)<0 ? Math.PI : 0);
    }
}
