package borrowedenergy;

import borrowedenergy.input.InputState;
import borrowedenergy.model.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** Regression checks for campaign progression and every rule needed to finish it. */
public final class ModelTest {
    private static int checks;
    private static final InputState IDLE=new InputState();
    public static void main(String[] args) {
        initialCampaign();smoothMovementAndCollision();ownershipAndCharging();hazardsAndRecall();
        fullCampaignAndLayout();restartAndReset();livesAndCombat();alienPacingAndDodge();
        System.out.println("PASS: "+checks+" model checks (headless)");
    }
    private static void initialCampaign() {
        GameModel m=new GameModel();
        check(m.stageIndex()==0 && m.stageNumber()==1,"campaign starts on easy stage");
        check(m.difficulty().equals("EASY"),"easy label");
        check(m.lives()==5 && m.maxLives()==5 && !m.stageFailed(),"each stage begins with five lives");
        check(!m.won() && !m.stageComplete() && !m.extractionReady(),"campaign begins uncompleted");
        check(!m.nextStage() && m.stageIndex()==0,"next stage cannot skip an unfinished map");
        check(m.core().state()==EnergyCore.State.GROUND,"battery starts as a pickup");
        check(m.interactionTarget()==null,"battery cannot be picked up from spawn");
        m.interact();ownership(m);
        check(m.core().state()==EnergyCore.State.GROUND,"distant interaction does nothing");
        m.player().setPosition(m.exitX(),m.exitY());m.update(.05,IDLE);
        check(!m.stageComplete(),"exit cannot bypass circuit puzzles");
        double t=m.stageTime();m.update(Double.NaN,IDLE);m.update(-1,IDLE);
        near(m.stageTime(),t,"invalid and negative dt cannot corrupt time");
    }
    private static void smoothMovementAndCollision() {
        GameModel straight=new GameModel();InputState right=input(InputState.Action.RIGHT);
        double x=straight.player().x(),y=straight.player().y();
        straight.update(.05,right);
        check(straight.player().speed()>0 && straight.player().speed()<Player.MAX_SPEED,"walking accelerates gradually");
        check(straight.player().walkPhase()>0,"gait follows actual movement");
        for(int i=0;i<8;i++) straight.update(.05,right);
        near(straight.player().speed(),Player.MAX_SPEED,"walking reaches target speed");
        double straightDistance=Math.hypot(straight.player().x()-x,straight.player().y()-y);
        GameModel diagonal=new GameModel();InputState downRight=input(InputState.Action.RIGHT,InputState.Action.DOWN);
        for(int i=0;i<9;i++) diagonal.update(.05,downRight);
        near(Math.hypot(diagonal.player().x()-x,diagonal.player().y()-y),straightDistance,"diagonal acceleration is normalized");
        straight.update(.05,IDLE);
        check(straight.player().speed()>0 && straight.player().speed()<Player.MAX_SPEED,"release decelerates naturally");
        for(int i=0;i<4;i++) straight.update(.05,IDLE);
        near(straight.player().speed(),0,"release comes to rest quickly");
        double phase=straight.player().walkPhase();straight.update(.05,IDLE);near(straight.player().walkPhase(),phase,"idle animation phase does not walk");
        GameModel wall=new GameModel();wall.player().setPosition(1.3,10.5);
        for(int i=0;i<40;i++) wall.update(.05,input(InputState.Action.LEFT));
        check(wall.player().x()>=1+wall.player().radius(),"outer wall blocks player");
        check(!wall.player().walking(),"pressing into wall does not walk in place");
        var door=wall.doors().get(0);
        check(wall.collisionAt(door.x(),door.y(),.24),"closed bulkhead blocks player");
        GameModel delayed=new GameModel();delayed.update(999,right);
        check(delayed.player().x()-3.5<.2,"delayed frame cannot teleport through walls");
    }
    private static void ownershipAndCharging() {
        GameModel m=new GameModel();acquire(m);PowerCircuit circuit=m.circuits().get(0);
        use(m,circuit.socket());ownership(m);
        safeAdvance(m,100);near(circuit.progress(),0,"misaligned relays cannot charge");
        Relay relay=circuit.relays().get(0);int before=relay.orientation();use(m,relay);
        check(relay.orientation()==(before+1)%4,"E rotates a relay");
        align(m,circuit);safeAdvance(m,10);
        check(circuit.progress()>0 && circuit.progress()<1,"charging takes time");
        use(m,circuit.socket());safeAdvance(m,30);
        near(circuit.progress(),0,"interrupted charge decays");
        check(!m.doors().get(0).open(),"partial charge cannot open gate");
        use(m,circuit.socket());safeAdvance(m,100);
        check(m.doors().get(0).open(),"full charge opens bulkhead");
        use(m,circuit.socket());relay.rotate();safeAdvance(m,80);
        check(m.doors().get(0).open(),"bulkhead latches after battery removal and relay rotation");
        check(!m.collisionAt(m.doors().get(0).x(),m.doors().get(0).y(),.24),"open gate is traversable");ownership(m);
    }
    private static void hazardsAndRecall() {
        GameModel m=new GameModel();int[] pit=findTile(m,'~');
        double cx=m.core().x(),cy=m.core().y();m.player().setPosition(pit[0]+.5,pit[1]+.5);m.update(.05,IDLE);
        check(m.lives()==4 && m.deaths()==0,"void costs exactly one life and recalls without dying");
        check(m.core().state()==EnergyCore.State.GROUND && !m.player().holdingCore(),"fall before pickup never grants battery");
        near(m.core().x(),cx,"unclaimed battery stays in original place x");near(m.core().y(),cy,"unclaimed battery stays in original place y");
        m.player().setPosition(pit[0]+.5,pit[1]+.5);m.update(.05,IDLE);
        check(m.lives()==3,"shield never allows walking on void");
        acquire(m);var c=m.circuits().get(0);align(m,c);use(m,c.socket());safeAdvance(m,100);use(m,c.socket());
        var cp=m.checkpoints().get(1);m.player().setPosition(cp.x(),cp.y());m.update(.05,IDLE);
        check(cp.active() && m.checkpoint()==1,"safe pads activate on arrival");
        use(m,m.circuits().get(1).socket());m.player().setPosition(pit[0]+.5,pit[1]+.5);m.update(.05,IDLE);
        near(m.player().x(),cp.x(),"recall uses latest pad");
        check(m.core().state()==EnergyCore.State.CARRIED,"recall retrieves claimed battery from remote socket");
        check(!m.circuits().get(1).socket().hasCore(),"remote socket is emptied on recall");
        check(c.complete(),"recall preserves latched puzzles");ownership(m);
        GameModel laserGame=new GameModel();LaserHazard laser=laserGame.lasers().get(0);
        check(laser.active(),"tutorial laser initially visible active");
        laserGame.player().setPosition(laser.x(),laser.y());laserGame.update(.05,IDLE);
        check(laserGame.lives()==4 && laserGame.player().invulnerability()>0,"active laser removes one life and grants short hazard shield");
        near(laserGame.player().x(),laser.x(),"ordinary beam damage does not teleport player");
        laserGame.player().setPosition(laser.x(),laser.y());laserGame.update(.05,IDLE);
        check(laserGame.lives()==4,"shield prevents frame-by-frame beam damage");
        boolean warning=false,active=false,safe=false;
        laserGame.player().setPosition(3.5,10.5);
        for(int i=0;i<180;i++) { laserGame.update(.05,IDLE);warning|=laser.warning();active|=laser.active();safe|=!laser.warning()&&!laser.active(); }
        check(warning && active && safe,"lasers have warning active and safe phases");
    }
    private static void fullCampaignAndLayout() {
        GameModel m=new GameModel();String previousMap="";
        for(int stage=0;stage<3;stage++) {
            check(m.stageIndex()==stage,"stages progress in order");
            check(m.difficulty().equals(new String[]{"EASY","MEDIUM","HARD"}[stage]),"difficulty rises stage by stage");
            check(m.circuits().size()==(stage==0 ? 2 : 3),"stage puzzle count");
            String map=mapSignature(m);check(!map.equals(previousMap),"each stage is a different full map");previousMap=map;
            allEntitiesOnFloor(m);
            if(stage==2) clearRobots(m);
            boolean[][] initial=flood(m);
            check(initial[(int)m.core().y()][(int)m.core().x()],"battery reachable from spawn");
            acquire(m);
            for(PowerCircuit circuit:m.circuits()) {
                boolean[][] accessible=flood(m);
                check(accessible[(int)circuit.socket().y()][(int)circuit.socket().x()],"next socket reachable after prior gate");
                for(Relay relay:circuit.relays()) check(accessible[(int)relay.y()][(int)relay.x()],"next relay reachable without crossing void or closed gates");
                if(circuit.id()<m.circuits().size()) {
                    PowerSocket later=m.circuits().get(circuit.id()).socket();
                    check(!accessible[(int)later.y()][(int)later.x()],"next room stays behind current powered bulkhead");
                }
                align(m,circuit);use(m,circuit.socket());safeAdvance(m,100);
                check(circuit.complete(),"circuit completes in finite charging time");
                if(circuit.id()<m.circuits().size())use(m,circuit.socket());
                ownership(m);
            }
            check(m.extractionReady() && !m.stageComplete(),"all circuits unlock extraction but do not skip map exit");
            check(m.core().state()==EnergyCore.State.SOCKETED,"regression setup leaves battery connected in last socket");
            boolean[][] accessible=flood(m);
            check(accessible[(int)m.exitY()][(int)m.exitX()],"final exit reachable after all gates");
            for(DataChip chip:m.chips()) {
                check(accessible[(int)chip.y()][(int)chip.x()],"optional chip accessible");use(m,chip);check(chip.collected(),"E collects archive chip");
            }
            check(m.chipsCollected()==m.totalChips(),"all stage archives counted");
            m.player().setPosition(m.exitX(),m.exitY());m.update(.05,IDLE);
            check(m.stageComplete(),"powered EXIT clears stage even when the battery was still connected");
            check(m.player().holdingCore() && m.core().socket()==null,"extraction automatically recovers the battery");ownership(m);
            check(m.won()==(stage==2),"only final stage wins campaign");
            near(m.player().speed(),0,"stage clear stops player motion");
            double time=m.stageTime(),laserPhase=m.lasers().get(0).phase();m.update(.05,input(InputState.Action.LEFT));
            near(m.stageTime(),time,"stage clear freezes timer");near(m.lasers().get(0).phase(),laserPhase,"stage clear freezes hazards");
            if(stage<2) {
                int data=m.totalChipsCollected();check(m.nextStage(),"cleared stage can advance");
                check(!m.stageComplete() && !m.won(),"next deployment starts fresh");
                check(m.totalChipsCollected()==data,"earlier collected archives persist");
            } else check(!m.nextStage(),"there is no fourth stage");
        }
        check(m.totalChipsCollected()==m.campaignTotalChips(),"campaign archive total sums all stages");
    }
    private static void restartAndReset() {
        GameModel m=new GameModel();solveStage(m);m.nextStage();acquire(m);use(m,m.chips().get(0));
        int previous=m.totalChipsCollected()-m.chipsCollected();double elapsed=m.elapsedSeconds();
        m.restartStage();check(m.stageIndex()==1,"restart retries current difficulty");
        check(m.chipsCollected()==0 && m.totalChipsCollected()==previous,"restart resets current archives only");
        near(m.elapsedSeconds(),elapsed,"campaign clock preserves failed attempt time");near(m.stageTime(),0,"restart resets stage timer");
        check(m.core().state()==EnergyCore.State.GROUND && !m.extractionReady(),"restart restores battery and puzzle state");
        m.reset();check(m.stageIndex()==0 && m.deaths()==0 && m.totalChipsCollected()==0,"campaign reset clears all run statistics");near(m.elapsedSeconds(),0,"campaign reset clears timer");
    }
    private static void solveStage(GameModel m) {
        if(m.stageIndex()==2)clearRobots(m);
        acquire(m);
        for(PowerCircuit c:m.circuits()) { align(m,c);use(m,c.socket());safeAdvance(m,100);use(m,c.socket()); }
        m.player().setPosition(m.exitX(),m.exitY());m.update(.05,IDLE);check(m.stageComplete(),"test solve reaches extraction");
    }
    private static void allEntitiesOnFloor(GameModel m) {
        List<Entity> entities=new ArrayList<>();entities.add(m.core());entities.add(m.player());entities.add(m.generator());entities.addAll(m.chips());entities.addAll(m.checkpoints());
        if(m.gun()!=null)entities.add(m.gun());entities.addAll(m.robots());
        for(PowerCircuit c:m.circuits()) { entities.add(c.socket());entities.addAll(c.relays()); }
        for(Entity entity:entities) check(m.level().tileAt((int)entity.x(),(int)entity.y())=='.',"entity on floor: "+entity.getClass().getSimpleName()+" at "+entity.x()+","+entity.y());
        check(m.level().tileAt((int)m.exitX(),(int)m.exitY())=='.',"exit pad on floor");
    }
    private static void livesAndCombat() {
        GameModel hit=new GameModel();
        check(hit.insects().size()==2,"garden includes flying demon insects");
        for(int life=4;life>=0;life--) {
            if(life<4)safeAdvance(hit,30);
            DemonInsect insect=hit.insects().get(0);
            hit.player().setPosition(insect.x(),insect.y());hit.update(.01,IDLE);
            check(hit.lives()==life,"each distinct insect collision removes exactly one life");
        }
        check(hit.stageFailed() && hit.deaths()==1 && hit.lives()==0,"fifth hit ends this level attempt");
        double time=hit.stageTime(),x=hit.player().x();hit.update(.05,input(InputState.Action.LEFT));
        near(hit.stageTime(),time,"failed stage freezes clock");near(hit.player().x(),x,"failed stage freezes movement");
        check(!hit.nextStage(),"failed stage cannot advance");hit.restartStage();
        check(hit.stageIndex()==0 && hit.lives()==5 && !hit.stageFailed(),"retry restores five lives in the same level");

        GameModel transit=stage(1);check(transit.leaks().size()==3 && transit.aliens().size()==2,"transit includes leaking floors and alien saucers");
        ElectricLeak leak=transit.leaks().get(0);transit.player().setPosition(leak.x(),leak.y());transit.update(.05,IDLE);
        check(leak.warning() && transit.lives()==5,"warning electricity is safe");
        for(int i=0;i<18;i++)transit.update(.05,IDLE);
        check(leak.active() && transit.lives()==4,"active electric leak removes one life");
        for(int i=0;i<10;i++)transit.update(.05,IDLE);
        check(transit.lives()==4,"electric leak respects damage cooldown");
        transit.player().setPosition(3.5,3.5);transit.update(.05,IDLE);
        for(AlienSaucer alien:transit.aliens())for(int i=1;i<50;i++) {
            double t=i/50.0;
            check(!transit.collisionAt(alien.beamStartX()+(alien.beamEndX()-alien.beamStartX())*t,
                    alien.beamStartY()+(alien.beamEndY()-alien.beamStartY())*t,.01),"alien beams stop at walls and closed bulkheads");
        }

        GameModel vault=stage(2);check(vault.gun()!=null && vault.totalRobots()==3,"vault contains blaster and three pursuers");
        boolean[][] reachable=flood(vault);check(reachable[(int)vault.gun().y()][(int)vault.gun().x()],"blaster reachable from spawn before opening any gate");
        InputState fire=new InputState();fire.setFiring(true);vault.update(.05,fire);
        check(vault.projectiles().isEmpty(),"cannot fire before collecting the gun");
        use(vault,vault.gun());check(vault.player().hasGun() && vault.gun().collected(),"E equips the collected blaster");
        ChaserRobot first=vault.robots().get(0);double distance=Math.hypot(first.x()-vault.player().x(),first.y()-vault.player().y());
        // Follow the actual corridor around the damaged floor using only normal robot updates.
        for(int i=0;i<120;i++) {
            vault.update(.05,IDLE);
            check(!vault.collisionAt(first.x(),first.y(),first.radius()),"chaser never overlaps a wall or closed gate");
            check(vault.level().tileAt((int)first.x(),(int)first.y())=='.',"chaser never walks on void");
            check(vault.robots().get(1).x()>12.8,"closed access gate keeps the outer-room robot outside");
        }
        check(Math.hypot(first.x()-vault.player().x(),first.y()-vault.player().y())<distance-2,"chaser pursues around walls and pits rather than getting stuck");

        GameModel wall=stage(2);use(wall,wall.gun());wall.player().setPosition(10.5,9.5);wall.setAim(15,9.5);
        wall.update(.01,fire);check(wall.projectiles().size()==1,"first gun press creates a projectile");
        wall.update(.01,fire);check(wall.projectiles().size()<=1,"weapon has a shot cooldown");
        for(int i=0;i<20;i++) {
            wall.update(.05,fire);
            for(Projectile p:wall.projectiles())check(p.x()<12,"projectiles cannot pass through closed walls");
        }
        check(wall.robots().get(1).health()==3,"bullets never damage a robot through a bulkhead");
        clearRobots(wall);check(wall.robotsRemaining()==0 && wall.robotsDestroyed()==3,"pulse shots destroy all robots");
        wall.restartStage();check(wall.stageIndex()==2 && wall.lives()==5 && wall.robotsRemaining()==3 && !wall.player().hasGun(),"retry vault restores robots, gun pickup and five lives");
    }
    private static void alienPacingAndDodge() {
        GameModel transit=stage(1);
        AlienSaucer saucer=transit.aliens().get(0);
        // Stay at the safe arrival pad while the saucer establishes its aim line.
        for(int i=0;i<28;i++)transit.update(.05,IDLE);
        check(saucer.beamWarning() && !saucer.beamActive(),"saucer still warns after 1.4 seconds");
        double sx=saucer.beamStartX(),sy=saucer.beamStartY(),ex=saucer.beamEndX(),ey=saucer.beamEndY();
        transit.player().setPosition(4.5,5.5);
        for(int i=0;i<11;i++)transit.update(.05,IDLE);
        check(saucer.beamWarning() && !saucer.beamActive(),"saucer gives nearly two seconds warning before firing");
        near(saucer.beamStartX(),sx,"warning keeps the saucer origin still");
        near(saucer.beamStartY(),sy,"warning keeps the saucer origin y still");
        near(saucer.beamEndX(),ex,"last half-second of warning locks aim despite player movement");
        near(saucer.beamEndY(),ey,"locked warning line leaves time to walk aside");
        transit.update(.05,IDLE);
        check(saucer.beamActive(),"saucer fires after the complete two-second warning");
        transit.player().setPosition(3.5,3.5);
        for(int i=0;i<13;i++)transit.update(.05,IDLE);
        check(saucer.beamActive(),"beam remains visible through its short firing window");
        near(saucer.beamStartX(),sx,"firing saucer keeps telegraphed origin x");
        near(saucer.beamStartY(),sy,"firing saucer keeps telegraphed origin y");
        near(saucer.beamEndX(),ex,"active beam does not track player x");
        near(saucer.beamEndY(),ey,"active beam does not track player y");
        for(int i=0;i<3;i++)transit.update(.05,IDLE);
        check(!saucer.beamActive() && !saucer.beamWarning(),"beam is off within 0.8 seconds of firing");
        for(int i=0;i<104;i++)transit.update(.05,IDLE);
        check(!saucer.beamActive() && !saucer.beamWarning(),"saucer leaves more than five safe seconds between attacks");
        for(int i=0;i<6;i++)transit.update(.05,IDLE);
        check(saucer.beamWarning() && !saucer.beamActive(),"next attack starts with another warning instead of immediate fire");
        check(transit.lives()==5,"slower attack cycle leaves arrival and retreat positions safe");
    }
    private static GameModel stage(int index) {
        GameModel m=new GameModel();while(m.stageIndex()<index){solveStage(m);check(m.nextStage(),"fixture advances only after extraction");}return m;
    }
    private static void clearRobots(GameModel m) {
        double px=m.player().x(),py=m.player().y();
        if(!m.player().hasGun())use(m,m.gun());
        InputState fire=new InputState();fire.setFiring(true);
        for(ChaserRobot robot:m.robots()) {
            int attempts=0;
            while(robot.alive() && attempts++<6) {
                boolean found=false;
                for(double[] delta:new double[][]{{-2,0},{2,0},{0,-2},{0,2}}) {
                    double x=robot.x()+delta[0],y=robot.y()+delta[1];
                    if(m.level().tileAt((int)x,(int)y)!='.' || m.collisionAt(x,y,.25))continue;
                    boolean clear=true;for(int s=1;s<20;s++)if(m.collisionAt(x+(robot.x()-x)*s/20,y+(robot.y()-y)*s/20,.06)){clear=false;break;}
                    if(!clear)continue;m.player().setPosition(x,y);found=true;break;
                }
                check(found,"test firing lane exists");m.setAim(robot.x(),robot.y());
                for(int i=0;i<15 && robot.alive() && !m.stageFailed();i++)m.update(.05,fire);
            }
            check(!robot.alive() && !m.stageFailed(),"real projectiles destroy a pursuer before contact kills player");
        }
        m.clearAim();m.player().setPosition(px,py);
    }
    private static String mapSignature(GameModel m) { StringBuilder s=new StringBuilder();for(int y=0;y<Level.HEIGHT;y++) for(int x=0;x<Level.WIDTH;x++) s.append(m.level().tileAt(x,y));return s.toString(); }
    private static int[] findTile(GameModel m,char tile) { for(int y=0;y<Level.HEIGHT;y++) for(int x=0;x<Level.WIDTH;x++) if(m.level().tileAt(x,y)==tile) return new int[]{x,y};throw new AssertionError("tile missing"); }
    private static InputState input(InputState.Action... actions) { InputState input=new InputState();for(var action:actions) input.set(action,true);return input; }
    private static void acquire(GameModel m) { use(m,m.core());check(m.player().holdingCore(),"battery can be acquired");ownership(m); }
    private static void use(GameModel m,Entity entity) { m.player().setPosition(entity.x(),entity.y());check(m.interactionTarget()==entity,"nearest object selected: "+entity.getClass().getSimpleName());m.interact(); }
    private static void align(GameModel m,PowerCircuit c) { for(Relay r:c.relays()) { int attempts=0;while(!r.aligned() && attempts++<4) use(m,r);check(r.aligned(),"relay reaches required route"); } }
    private static void safeAdvance(GameModel m,int frames) { var pad=m.checkpoints().get(0);m.player().setPosition(pad.x(),pad.y());for(int i=0;i<frames;i++) m.update(.05,IDLE); }
    private static boolean[][] flood(GameModel m) {
        boolean[][] visited=new boolean[Level.HEIGHT][Level.WIDTH];ArrayDeque<int[]> queue=new ArrayDeque<>();
        int sx=(int)m.player().x(),sy=(int)m.player().y();queue.add(new int[]{sx,sy});visited[sy][sx]=true;
        for(;!queue.isEmpty();) { int[] p=queue.remove();for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
            int x=p[0]+d[0],y=p[1]+d[1];if(x<0||y<0||x>=Level.WIDTH||y>=Level.HEIGHT||visited[y][x]) continue;
            if(m.level().tileAt(x,y)!='.' || m.collisionAt(x+.5,y+.5,.24)) continue;visited[y][x]=true;queue.add(new int[]{x,y});
        }}return visited;
    }
    private static void ownership(GameModel m) {
        long occupied=m.circuits().stream().filter(c->c.socket().hasCore()).count();
        check(m.player().holdingCore()==(m.core().state()==EnergyCore.State.CARRIED),"player and battery ownership agree");
        check(occupied==(m.core().state()==EnergyCore.State.SOCKETED ? 1 : 0),"one battery occupies at most one socket");
        check((m.core().socket()!=null)==(m.core().state()==EnergyCore.State.SOCKETED),"battery socket reference agrees with state");
        if(m.core().socket()!=null) check(m.core().socket().hasCore(),"battery owning socket is occupied");
    }
    private static void near(double actual,double expected,String message) { check(Math.abs(actual-expected)<1e-8,message+" actual="+actual+" expected="+expected); }
    private static void check(boolean condition,String message) { checks++;if(!condition) throw new AssertionError(message); }
}
