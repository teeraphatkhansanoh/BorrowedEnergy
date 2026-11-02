package borrowedenergy.model;

/** Positions are in map tiles, independent of screen resolution. */
public abstract class Entity {
    protected double x;
    protected double y;
    protected Entity(double x, double y) { this.x = x; this.y = y; }
    public double x() { return x; }
    public double y() { return y; }
}
