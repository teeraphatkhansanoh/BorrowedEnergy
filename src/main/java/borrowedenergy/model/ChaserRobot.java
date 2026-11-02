package borrowedenergy.model;

import java.util.ArrayDeque;

/** Ground pursuer uses a tile BFS so it cannot take shortcuts through bulkheads. */
public final class ChaserRobot extends Entity {
    private static final double SPEED=1.85;
    private int health=3;
    private double heading,hitFlash,repath,targetX,targetY;
    public ChaserRobot(double x,double y) { super(x,y);targetX=x;targetY=y; }
    public int health() { return health; }
    public int maxHealth() { return 3; }
    public boolean alive() { return health>0; }
    public double radius() { return .33; }
    public double heading() { return heading; }
    public double hitFlash() { return hitFlash; }
    void hit() { if(alive()) { health--;hitFlash=.16; } }
    void update(double dt,GameModel model) {
        hitFlash=Math.max(0,hitFlash-dt);
        if(!alive()) return;
        repath-=dt;
        // Finish the current tile-center leg before turning; early replans cut corners.
        if(repath<=0 && Math.hypot(targetX-x,targetY-y)<.025) { findNext(model);repath=.15; }
        double dx=targetX-x,dy=targetY-y,d=Math.hypot(dx,dy);
        if(d<.01) return;
        heading=Math.atan2(dy,dx);double distance=Math.min(d,SPEED*dt);
        double nx=x+dx/d*distance,ny=y+dy/d*distance;
        if(valid(model,nx,ny)) { x=nx;y=ny;if(distance>=d-.0001)repath=0; }
        else { targetX=x;targetY=y;repath=0; }
    }
    private boolean valid(GameModel m,double x,double y) {
        return m.level().tileAt((int)Math.floor(x),(int)Math.floor(y))=='.' && !m.collisionAt(x,y,radius());
    }
    private void findNext(GameModel model) {
        int sx=(int)Math.floor(x),sy=(int)Math.floor(y),gx=(int)Math.floor(model.player().x()),gy=(int)Math.floor(model.player().y());
        targetX=x;targetY=y;
        if(sx==gx && sy==gy) { targetX=model.player().x();targetY=model.player().y();return; }
        int w=Level.WIDTH,h=Level.HEIGHT;int[] previous=new int[w*h];java.util.Arrays.fill(previous,-1);
        ArrayDeque<Integer> queue=new ArrayDeque<>();int start=sy*w+sx,goal=gy*w+gx;
        if(start<0||start>=previous.length||goal<0||goal>=previous.length)return;
        queue.add(start);previous[start]=start;
        int[][] directions={{1,0},{-1,0},{0,1},{0,-1}};
        while(!queue.isEmpty() && previous[goal]<0) {
            int cell=queue.remove(),cx=cell%w,cy=cell/w;
            for(int[] direction:directions) {
                int nx=cx+direction[0],ny=cy+direction[1],next=ny*w+nx;
                if(nx<0||ny<0||nx>=w||ny>=h||previous[next]>=0 || !valid(model,nx+.5,ny+.5))continue;
                previous[next]=cell;queue.add(next);
            }
        }
        if(previous[goal]<0)return;
        int next=goal;while(previous[next]!=start && previous[next]!=next)next=previous[next];
        targetX=next%w+.5;targetY=next/w+.5;
    }
}
