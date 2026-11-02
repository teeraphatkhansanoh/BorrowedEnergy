package borrowedenergy.model;

/** A damaged floor conduit warns before discharging, leaving a safe crossing interval. */
public final class ElectricLeak extends Entity {
    private final double width,height,offset;
    private double cycle;
    public ElectricLeak(double x,double y,double width,double height,double offset) {
        super(x,y);this.width=width;this.height=height;this.offset=offset;update(0);
    }
    public double width() { return width; }
    public double height() { return height; }
    public boolean warning() { return cycle<.85; }
    public boolean active() { return cycle>=.85 && cycle<2.4; }
    public double phase() { return cycle/5.2; }
    void update(double time) { cycle=(time+offset)%5.2; }
}
