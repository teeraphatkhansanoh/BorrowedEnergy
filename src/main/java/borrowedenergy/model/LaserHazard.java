package borrowedenergy.model;

/** Cycles through a visible warning, active beam, and a safe crossing interval. */
public final class LaserHazard extends Entity {
    private final double width,height,warningSeconds,activeSeconds,period,offset;
    private double cycle;
    public LaserHazard(double x,double y,double width,double height,double warningSeconds,double activeSeconds,double safeSeconds,double offset) {
        super(x,y);this.width=width;this.height=height;this.warningSeconds=warningSeconds;
        this.activeSeconds=activeSeconds;period=warningSeconds+activeSeconds+safeSeconds;this.offset=offset;update(0);
    }
    public double width() { return width; }
    public double height() { return height; }
    public boolean warning() { return cycle<warningSeconds; }
    public boolean active() { return cycle>=warningSeconds && cycle<warningSeconds+activeSeconds; }
    public double phase() { return cycle/period; }
    void update(double time) { cycle=(time+offset)%period; }
}
