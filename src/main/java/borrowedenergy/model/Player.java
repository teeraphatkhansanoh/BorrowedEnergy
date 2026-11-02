package borrowedenergy.model;

/** Velocity and gait progress are simulated independently of rendering. */
public final class Player extends Entity {
    public static final double MAX_SPEED=3.75;
    private double facingX=1,facingY,velocityX,velocityY,walkPhase,invulnerability,aimX=1,aimY,muzzleFlash;
    private boolean walking,holdingCore,hasGun;
    public Player(double x,double y) { super(x,y); }
    public double radius() { return .24; }
    public double facingX() { return facingX; }
    public double facingY() { return facingY; }
    public double velocityX() { return velocityX; }
    public double velocityY() { return velocityY; }
    public double speed() { return Math.hypot(velocityX,velocityY); }
    public double walkPhase() { return walkPhase; }
    public double invulnerability() { return invulnerability; }
    public boolean walking() { return walking; }
    public boolean holdingCore() { return holdingCore; }
    public boolean hasGun() { return hasGun; }
    public double aimX() { return aimX; }
    public double aimY() { return aimY; }
    public double muzzleFlash() { return muzzleFlash; }
    public void setPosition(double x,double y) { this.x=x;this.y=y;stop(); }
    void moveTo(double x,double y) { this.x=x;this.y=y; }
    void accelerate(double dx,double dy,double dt) {
        double length=Math.hypot(dx,dy);
        if(length>0) { dx/=length;dy/=length;facingX=dx;facingY=dy; }
        double tx=dx*MAX_SPEED,ty=dy*MAX_SPEED,vx=tx-velocityX,vy=ty-velocityY,change=Math.hypot(vx,vy);
        double maxChange=(length>0 ? 18 : 26)*dt;
        if(change<=maxChange) { velocityX=tx;velocityY=ty; }
        else { velocityX+=vx/change*maxChange;velocityY+=vy/change*maxChange; }
    }
    void recordDisplacement(double distance) { walking=distance>.0001;if(walking) walkPhase=(walkPhase+distance*5.5)%(Math.PI*2); }
    void blockX() { velocityX=0; }
    void blockY() { velocityY=0; }
    void stop() { velocityX=velocityY=0;walking=false; }
    void setMotion(double dx,double dy) { if(dx==0 && dy==0) stop(); }
    void setHoldingCore(boolean value) { holdingCore=value; }
    void equipGun() { hasGun=true; }
    void aim(double dx,double dy) {
        double distance=Math.hypot(dx,dy);
        if(distance>.001) { aimX=dx/distance;aimY=dy/distance; }
    }
    void fire() { muzzleFlash=.1; }
    void protect(double seconds) { invulnerability=seconds; }
    void updateProtection(double dt) { invulnerability=Math.max(0,invulnerability-dt);muzzleFlash=Math.max(0,muzzleFlash-dt); }
}
