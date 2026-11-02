package borrowedenergy.model;

/** A flying creature follows a readable looping flight path in its habitat. */
public final class DemonInsect extends Entity {
    private final double cx,cy,rx,ry,period,offset;
    private double heading;
    public DemonInsect(double cx,double cy,double rx,double ry,double period,double offset) {
        super(cx,cy);this.cx=cx;this.cy=cy;this.rx=rx;this.ry=ry;this.period=period;this.offset=offset;update(0);
    }
    public double radius() { return .38; }
    public double heading() { return heading; }
    void update(double time) {
        double angle=time/period*Math.PI*2+offset;
        x=cx+Math.cos(angle)*rx;y=cy+Math.sin(angle)*ry;
        heading=Math.atan2(Math.cos(angle)*ry,-Math.sin(angle)*rx);
    }
}
