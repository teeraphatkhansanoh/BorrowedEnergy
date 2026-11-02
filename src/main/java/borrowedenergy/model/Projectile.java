package borrowedenergy.model;

public final class Projectile extends Entity {
    private final double dx,dy;
    private double life=1.5;
    Projectile(double x,double y,double dx,double dy) { super(x,y);this.dx=dx;this.dy=dy; }
    public double dx() { return dx; }
    public double dy() { return dy; }
    public double life() { return life; }
    void move(double dt) { x+=dx*13*dt;y+=dy*13*dt;life-=dt; }
}
