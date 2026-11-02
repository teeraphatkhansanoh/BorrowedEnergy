package borrowedenergy.model;

/** A piloted saucer charges a visible aim line, then fires its locked beam. */
public final class AlienSaucer extends Entity {
    private static final double WARNING_SECONDS=2.0;
    private static final double AIM_LOCK_SECONDS=.6;
    private static final double ACTIVE_SECONDS=.75;
    private static final double COOLDOWN_SECONDS=5.5;
    private static final double PERIOD=WARNING_SECONDS+ACTIVE_SECONDS+COOLDOWN_SECONDS;
    private final double ax,bx,cy,offset;
    private double cycle,heading,beamDX,beamDY=1,beamEndX,beamEndY;
    public AlienSaucer(double ax,double bx,double y,double offset) {
        super(ax,y);this.ax=ax;this.bx=bx;this.cy=y;this.offset=offset;
        beamEndX=ax;beamEndY=y+6;
    }
    public double radius() { return .5; }
    public double heading() { return heading; }
    public double beamStartX() { return x; }
    public double beamStartY() { return y+.25; }
    public double beamEndX() { return beamEndX; }
    public double beamEndY() { return beamEndY; }
    public boolean beamWarning() { return cycle<WARNING_SECONDS; }
    public boolean beamActive() { return cycle>=WARNING_SECONDS && cycle<WARNING_SECONDS+ACTIVE_SECONDS; }
    public double beamPhase() { return cycle/PERIOD; }
    void update(double time,GameModel model) {
        cycle=(time+offset)%PERIOD;
        double angle=time*.5+offset;
        // Hold the craft still during warning and firing so the telegraphed line stays fair.
        if(!beamWarning() && !beamActive()) { x=ax+(bx-ax)*(.5+.5*Math.sin(angle));y=cy+Math.sin(time*1.3+offset)*.12; }
        heading=Math.cos(angle)>=0?0:Math.PI;
        // Give the player a fixed aim line for the final part of the warning.
        if(cycle<WARNING_SECONDS-AIM_LOCK_SECONDS) {
            double dx=model.player().x()-beamStartX(),dy=model.player().y()-beamStartY(),d=Math.hypot(dx,dy);
            if(d>.01) { beamDX=dx/d;beamDY=dy/d; }
        }
        // Stop at the first bulkhead; beams do not pass through closed rooms.
        beamEndX=beamStartX();beamEndY=beamStartY();
        for(double d=.12;d<=8;d+=.08) {
            double tx=beamStartX()+beamDX*d,ty=beamStartY()+beamDY*d;
            if(model.collisionAt(tx,ty,.04)) break;
            beamEndX=tx;beamEndY=ty;
        }
    }
}
