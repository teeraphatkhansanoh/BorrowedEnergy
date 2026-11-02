package borrowedenergy.model;

import borrowedenergy.input.InputState;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Campaign, movement and puzzle rules, independent of Swing or screen pixels. */
public final class GameModel {
    public static final int STAGE_COUNT=3;
    private static final String[] NAMES={"ORBITAL GARDEN","NEON TRANSIT","SINGULARITY VAULT"};
    private static final String[] DIFFICULTIES={"EASY","MEDIUM","HARD"};
    private static final String[] DESCRIPTIONS={
        "Dodge flying demon insects while restoring the orbital habitat.",
        "Avoid electric leaks and alien saucers firing charged beams.",
        "Collect the pulse blaster, destroy the chasing robots and restore the vault."};
    private Level level;
    private Player player;
    private EnergyCore core;
    private List<PowerCircuit> circuits;
    private List<ElectricDoor> doors;
    private MainGenerator generator;
    private List<LaserHazard> lasers;
    private List<PatrolDrone> drones;
    private List<DataChip> chips;
    private List<Checkpoint> checkpoints;
    private List<DemonInsect> insects;
    private List<ElectricLeak> leaks;
    private List<AlienSaucer> aliens;
    private List<ChaserRobot> robots;
    private GunPickup gun;
    private final List<Projectile> projectiles=new ArrayList<>();
    private final List<Particle> particles=new ArrayList<>();
    private final Random random=new Random(42);
    private String message;
    private double messageTime,elapsedSeconds,stageTime,particleClock,fallFlash,shotCooldown;
    private double aimTileX=Double.NaN,aimTileY=Double.NaN;
    private int stageIndex,deaths,checkpoint,previousChips,lives;
    private boolean stageComplete,stageFailed;

    public GameModel() { reset(); }
    public void reset() { stageIndex=0;deaths=0;elapsedSeconds=0;previousChips=0;loadStage(); }
    public void restartStage() { loadStage(); }
    public boolean nextStage() {
        if(!stageComplete || stageIndex>=STAGE_COUNT-1) return false;
        previousChips+=chipsCollected();stageIndex++;loadStage();return true;
    }
    private void loadStage() {
        level=new Level(stageIndex);stageComplete=false;stageFailed=false;lives=maxLives();stageTime=0;checkpoint=0;particleClock=0;fallFlash=0;shotCooldown=0;
        particles.clear();projectiles.clear();clearAim();
        if(stageIndex==0) garden();else if(stageIndex==1) transit();else vault();
        if(stageIndex==0) {
            insects=List.of(new DemonInsect(11.5,9.0,3.2,1.6,6.4,0),new DemonInsect(28,12,3,1.2,5.8,2.3));
            leaks=List.of();aliens=List.of();robots=List.of();gun=null;
        } else if(stageIndex==1) {
            insects=List.of();
            leaks=List.of(new ElectricLeak(12,4.5,2,1.4,0),new ElectricLeak(24.5,10,2,1.5,1.7),new ElectricLeak(20.5,17.5,3,1.1,3.1));
            aliens=List.of(new AlienSaucer(23,27,2.0,0),new AlienSaucer(27.5,32,9.4,2.7));
            robots=List.of();gun=null;drones=List.of();
        } else {
            insects=List.of();leaks=List.of();aliens=List.of();drones=List.of();
            robots=List.of(new ChaserRobot(9.5,4.5),new ChaserRobot(27.5,3.5),new ChaserRobot(31.5,16.5));
            gun=new GunPickup(5.5,17.5);
        }
        announce("STAGE "+stageNumber()+" / "+difficulty()+"  -  "+stageDescription(),7);
    }
    private PowerCircuit circuit(int id,String name,double sx,double sy,List<Relay> relays,double tx,double ty,double seconds) {
        return new PowerCircuit(id,name,sx,sy,relays,tx,ty,seconds);
    }
    private Relay relay(double x,double y,String label,int required,int initial) { return new Relay(x,y,label,required,initial); }
    private void garden() {
        player=new Player(3.5,10.5);core=new EnergyCore(6.5,10.5);
        var a=circuit(1,"HABITAT GATE",8.5,5.5,List.of(relay(13.5,5.5,"RELAY A",0,2)),18.5,10,1.6);
        var b=circuit(2,"HABITAT REACTOR",25.5,13.5,List.of(relay(28.5,10.5,"RELAY B",3,1)),30.5,4.5,2.2);
        circuits=List.of(a,b);doors=List.of(new ElectricDoor(18.5,10,a));generator=new MainGenerator(30.5,4.5,b);
        lasers=List.of(new LaserHazard(24.5,10,.16,4,.9,1.5,2.8,1.2));drones=List.of();
        chips=List.of(new DataChip(4.5,17.5,"HABITAT LOG"),new DataChip(31.5,8.5,"ORBITAL LOG"));
        checkpoints=List.of(new Checkpoint(3.5,10.5,"ARRIVAL PAD",true),new Checkpoint(20.5,10.5,"HABITAT PAD",false));
    }
    private void transit() {
        player=new Player(3.5,3.5);core=new EnergyCore(5.5,4.5);
        var a=circuit(1,"UPPER BULKHEAD",14.5,4.5,List.of(relay(20.5,3.5,"RELAY A",0,2),relay(25.5,5.5,"RELAY B",2,3)),10,7.5,1.9);
        var b=circuit(2,"LOWER BULKHEAD",4.5,9.5,List.of(relay(15.5,10.5,"RELAY C",0,1),relay(22.5,11.5,"RELAY D",0,2)),27,13.5,2.3);
        var c=circuit(3,"TRANSIT REACTOR",22.5,16.5,List.of(relay(10.5,16.5,"RELAY E",2,0),relay(7.5,16.5,"RELAY F",2,1)),4.5,16.5,2.7);
        circuits=List.of(a,b,c);doors=List.of(new ElectricDoor(10,7.5,2,1,a),new ElectricDoor(27,13.5,2,1,b));generator=new MainGenerator(4.5,16.5,c);
        lasers=List.of(new LaserHazard(18.5,4,.16,4,.8,1.8,2.4,0),new LaserHazard(13.5,10.5,.16,5,.8,1.7,2.2,1.6));
        drones=List.of(new PatrolDrone(17.5,16,28.0,16,8.5,0));
        chips=List.of(new DataChip(32.5,5.5,"TRANSIT LOG"),new DataChip(31.5,10.5,"PATROL LOG"),new DataChip(2.5,17.5,"FREIGHT LOG"));
        checkpoints=List.of(new Checkpoint(3.5,3.5,"ARRIVAL PAD",true),new Checkpoint(10.5,9.5,"UPPER PAD",false),new Checkpoint(27.5,14.8,"LOWER PAD",false));
    }
    private void vault() {
        player=new Player(3.5,16.5);core=new EnergyCore(8.5,16.5);
        var a=circuit(1,"VAULT ACCESS",3.5,4.5,List.of(relay(8.5,5.5,"RELAY A",1,2),relay(6.5,9.5,"RELAY B",0,3)),12.5,6,2.2);
        var b=circuit(2,"INNER SHIELD",16.5,9.5,List.of(relay(18.5,4.5,"RELAY C",0,1),relay(26.5,4.5,"RELAY D",1,3),relay(31.5,9.5,"RELAY E",2,0)),29,12.5,2.7);
        var c=circuit(3,"SINGULARITY CORE",18.5,16.5,List.of(relay(22.5,15.5,"RELAY F",0,1),relay(28.5,17.5,"RELAY G",3,0),relay(32.5,14.5,"RELAY H",1,2)),30.5,14.5,3.2);
        circuits=List.of(a,b,c);doors=List.of(new ElectricDoor(12.5,6,a),new ElectricDoor(29,12.5,2,1,b));generator=new MainGenerator(30.5,14.5,c);
        lasers=List.of(new LaserHazard(24.5,7.5,.18,7,.85,2.25,2.1,0),new LaserHazard(24.5,16,.18,5,.85,2.4,2.1,2.1));
        drones=List.of(new PatrolDrone(16.5,10.8,31.5,10.8,8,0),new PatrolDrone(20.5,16,31.5,16,6.8,Math.PI));
        chips=List.of(new DataChip(9.5,3.5,"VAULT LOG"),new DataChip(32.5,4.5,"SECURITY LOG"),new DataChip(14.5,17.5,"SINGULARITY LOG"));
        checkpoints=List.of(new Checkpoint(3.5,16.5,"ARRIVAL PAD",true),new Checkpoint(14.5,6.5,"OUTER PAD",false),new Checkpoint(27.5,14.5,"INNER PAD",false));
    }
    public Level level() { return level; }
    public Player player() { return player; }
    public EnergyCore core() { return core; }
    public List<PowerCircuit> circuits() { return circuits; }
    public List<ElectricDoor> doors() { return doors; }
    public MainGenerator generator() { return generator; }
    public List<LaserHazard> lasers() { return lasers; }
    public List<PatrolDrone> drones() { return drones; }
    public List<DataChip> chips() { return chips; }
    public List<Checkpoint> checkpoints() { return checkpoints; }
    public List<DemonInsect> insects() { return insects; }
    public List<ElectricLeak> leaks() { return leaks; }
    public List<AlienSaucer> aliens() { return aliens; }
    public List<ChaserRobot> robots() { return robots; }
    public GunPickup gun() { return gun; }
    public List<Projectile> projectiles() { return List.copyOf(projectiles); }
    public int robotsRemaining() { return (int)robots.stream().filter(ChaserRobot::alive).count(); }
    public int totalRobots() { return robots.size(); }
    public int robotsDestroyed() { return totalRobots()-robotsRemaining(); }
    public int lives() { return lives; }
    public int maxLives() { return 5; }
    public boolean stageFailed() { return stageFailed; }
    public double damageFlash() { return fallFlash; }
    public double shotFlash() { return player.muzzleFlash(); }
    public void setAim(double x,double y) { if(Double.isFinite(x)&&Double.isFinite(y)){aimTileX=x;aimTileY=y;} }
    public void clearAim() { aimTileX=aimTileY=Double.NaN; }
    public double aimX() { return Double.isFinite(aimTileX)?aimTileX:player.x()+player.facingX()*8; }
    public double aimY() { return Double.isFinite(aimTileY)?aimTileY:player.y()+player.facingY()*8; }
    public List<Particle> particles() { return List.copyOf(particles); }
    public String message() { return messageTime>0 ? message : ""; }
    public int stageIndex() { return stageIndex; }
    public int stageNumber() { return stageIndex+1; }
    public String stageName() { return NAMES[stageIndex]; }
    public String difficulty() { return DIFFICULTIES[stageIndex]; }
    public String stageDescription() { return DESCRIPTIONS[stageIndex]; }
    public boolean stageComplete() { return stageComplete; }
    public boolean won() { return stageComplete && stageIndex==STAGE_COUNT-1; }
    public boolean circuitsComplete() { return circuits.stream().allMatch(PowerCircuit::complete); }
    public boolean extractionReady() { return circuitsComplete() && (stageIndex!=2 || (player.hasGun() && robotsRemaining()==0)); }
    public double exitX() { return 33; }
    public double exitY() { return 17; }
    public int deaths() { return deaths; }
    public int checkpoint() { return checkpoint; }
    public double elapsedSeconds() { return elapsedSeconds; }
    public double stageTime() { return stageTime; }
    public double fallFlash() { return fallFlash; }
    public int completedCircuits() { return (int)circuits.stream().filter(PowerCircuit::complete).count(); }
    public int chipsCollected() { return (int)chips.stream().filter(DataChip::collected).count(); }
    public int totalChips() { return chips.size(); }
    public int totalChipsCollected() { return previousChips+chipsCollected(); }
    public int campaignTotalChips() { return 8; }

    public String objective() {
        if(stageFailed) return "NO LIVES LEFT / RETRY THIS LEVEL";
        if(won()) return "ALL SYSTEMS ONLINE / EXPEDITION COMPLETE";
        if(stageComplete) return "STAGE CLEARED / READY FOR NEXT DEPLOYMENT";
        if(extractionReady()) return "MISSION COMPLETE / WALK TO EXIT - BATTERY TRANSFERS AUTOMATICALLY";
        if(stageIndex==2 && !player.hasGun()) return "Collect the pulse blaster near the arrival pad [E]";
        if(stageIndex==2 && circuitsComplete() && robotsRemaining()>0) return "Destroy the remaining robots: "+robotsRemaining()+" [HOLD F / CLICK]";
        if(core.state()==EnergyCore.State.GROUND) return "Find and collect the ion battery";
        PowerCircuit target=circuits.stream().filter(c -> !c.complete()).findFirst().orElseThrow();
        if(core.socket()!=null && core.socket().circuit().complete()) return "Retrieve the battery from the completed socket";
        if(!target.aligned()) return "Align the relay arrows for circuit "+target.id();
        if(!target.socket().hasCore()) return "Insert the battery into socket "+target.id();
        return "Charging "+target.name().toLowerCase()+"...";
    }
    public void update(double dt,InputState input) {
        if(!Double.isFinite(dt)) return;
        dt=Math.max(0,Math.min(.05,dt));
        if(stageComplete || stageFailed) { animateParticles(dt);return; }
        elapsedSeconds+=dt;stageTime+=dt;messageTime-=dt;fallFlash=Math.max(0,fallFlash-dt*2);
        player.updateProtection(dt);player.accelerate(input.dx(),input.dy(),dt);
        for(LaserHazard laser:lasers) laser.update(stageTime);
        for(PatrolDrone drone:drones) drone.update(stageTime);
        for(DemonInsect insect:insects) insect.update(stageTime);
        for(ElectricLeak leak:leaks) leak.update(stageTime);
        for(AlienSaucer alien:aliens) alien.update(stageTime,this);
        for(ChaserRobot robot:robots) robot.update(dt,this);
        player.aim(aimX()-player.x(),aimY()-player.y());
        shotCooldown=Math.max(0,shotCooldown-dt);
        if(input.firing() && player.hasGun() && shotCooldown<=0) shoot();
        updateProjectiles(dt);
        double ox=player.x(),oy=player.y();
        boolean recalled=false;
        for(int step=0;step<4;step++) {
            double nx=player.x()+player.velocityX()*dt/4;
            if(!collisionAt(nx,player.y(),player.radius())) player.moveTo(nx,player.y());else player.blockX();
            double ny=player.y()+player.velocityY()*dt/4;
            if(!collisionAt(player.x(),ny,player.radius())) player.moveTo(player.x(),ny);else player.blockY();
            boolean pit=level.tileAt((int)Math.floor(player.x()),(int)Math.floor(player.y()))=='~';
            if(pit) { damage(true);recalled=true;break; }
            if(player.invulnerability()<=0 && dangerAt(player.x(),player.y(),player.radius())) { damage(false);break; }
        }
        if(stageFailed) return;
        if(!recalled) player.recordDisplacement(Math.hypot(player.x()-ox,player.y()-oy));
        core.follow(player);
        for(int i=checkpoint+1;i<checkpoints.size();i++) {
            Checkpoint pad=checkpoints.get(i);
            if(Math.hypot(player.x()-pad.x(),player.y()-pad.y())<.8) {
                checkpoint=i;pad.activate();announce("Recall point synchronized. Your battery is safe.",4);burst(pad.x(),pad.y(),15);
            }
        }
        for(PowerCircuit circuit:circuits) {
            boolean before=circuit.complete();circuit.update(dt);
            if(!before && circuit.complete()) {
                announce(circuit==generator.circuit() ? "All circuits online. Reach EXIT; the battery transfers automatically." : "Bulkhead latched open. Retrieve the battery to power the next circuit.",6);
                var receiver=circuit.waypoints().get(circuit.waypoints().size()-1);burst(receiver.x,receiver.y,40);
            }
        }
        if(extractionReady() && Math.hypot(player.x()-exitX(),player.y()-exitY())<.9) {
            recoverBattery();
            stageComplete=true;player.stop();announce(won() ? "All three stages restored. Welcome to tomorrow." : "Stage clear. The next full-map deployment is ready.",10);burst(exitX(),exitY(),60);
        }
        particleClock+=dt;
        if(particleClock>.08) { particleClock=0;if(core.state()!=EnergyCore.State.GROUND) burst(core.x(),core.y(),1); }
        animateParticles(dt);
    }
    public boolean collisionAt(double x,double y,double radius) {
        for(int ty=(int)Math.floor(y-radius);ty<=Math.floor(y+radius);ty++)
            for(int tx=(int)Math.floor(x-radius);tx<=Math.floor(x+radius);tx++)
                if(level.tileAt(tx,ty)=='#' && circleRect(x,y,radius,tx,ty,1,1)) return true;
        for(ElectricDoor door:doors) if(!door.open() && circleRect(x,y,radius,
                door.x()-door.width()/2,door.y()-door.height()/2,door.width(),door.height())) return true;
        return false;
    }
    public boolean dangerAt(double x,double y,double radius) {
        if(level.tileAt((int)Math.floor(x),(int)Math.floor(y))=='~') return true;
        for(LaserHazard laser:lasers) if(laser.active() && circleRect(x,y,radius,
                laser.x()-laser.width()/2,laser.y()-laser.height()/2,laser.width(),laser.height())) return true;
        for(PatrolDrone drone:drones) if(Math.hypot(x-drone.x(),y-drone.y())<radius+drone.radius()) return true;
        for(DemonInsect insect:insects) if(Math.hypot(x-insect.x(),y-insect.y())<radius+insect.radius()) return true;
        for(ElectricLeak leak:leaks) if(leak.active() && circleRect(x,y,radius,leak.x()-leak.width()/2,leak.y()-leak.height()/2,leak.width(),leak.height()))return true;
        for(AlienSaucer alien:aliens) {
            if(Math.hypot(x-alien.x(),y-alien.y())<radius+alien.radius())return true;
            if(alien.beamActive() && java.awt.geom.Line2D.ptSegDist(alien.beamStartX(),alien.beamStartY(),alien.beamEndX(),alien.beamEndY(),x,y)<radius+.08)return true;
        }
        for(ChaserRobot robot:robots) if(robot.alive() && Math.hypot(x-robot.x(),y-robot.y())<radius+robot.radius())return true;
        return false;
    }
    private boolean circleRect(double x,double y,double r,double rx,double ry,double w,double h) {
        double cx=Math.max(rx,Math.min(x,rx+w)),cy=Math.max(ry,Math.min(y,ry+h));return Math.hypot(x-cx,y-cy)<r;
    }
    public Interactable interactionTarget() {
        List<Interactable> targets=new ArrayList<>();
        if(core.state()==EnergyCore.State.GROUND) targets.add(core);
        for(PowerCircuit circuit:circuits) { targets.add(circuit.socket());targets.addAll(circuit.relays()); }
        for(DataChip chip:chips) if(!chip.collected()) targets.add(chip);
        if(gun!=null && !gun.collected())targets.add(gun);
        Interactable nearest=null;double best=1.3;
        for(Interactable target:targets) {
            double d=Math.hypot(player.x()-target.x(),player.y()-target.y());
            if(d<best && clearLine(player.x(),player.y(),target.x(),target.y())) { nearest=target;best=d; }
        }
        return nearest;
    }
    private boolean clearLine(double x1,double y1,double x2,double y2) {
        for(int i=1;i<=16;i++) if(collisionAt(x1+(x2-x1)*i/16,y1+(y2-y1)*i/16,.02)) return false;return true;
    }
    public void interact() {
        if(stageComplete || stageFailed) return;
        Interactable target=interactionTarget();
        if(target instanceof EnergyCore) {
            core.carry();player.setHoldingCore(true);core.follow(player);announce("Ion battery acquired. One cell powers every system.",5);
        } else if(target instanceof PowerSocket socket) {
            if(socket.hasCore()) {
                socket.setHasCore(false);core.carry();player.setHoldingCore(true);core.follow(player);
                announce(socket.circuit().complete() ? "Battery retrieved. The powered bulkhead stays open." : "Battery retrieved. Charge interrupted.",4);
            } else if(player.holdingCore()) {
                core.insert(socket);player.setHoldingCore(false);socket.setHasCore(true);
                announce(socket.circuit().aligned() ? "Power route stable. Charging..." : "Battery connected. Align each relay arrow with its marker.",5);
            } else announce("Retrieve the ion battery from its current location first.",4);
        } else if(target instanceof Relay relay) {
            relay.rotate();announce(relay.aligned() ? relay.label()+" aligned." : "Rotate the large arrow to match the small direction marker.",3);
        } else if(target instanceof DataChip chip) {
            chip.collect();announce("Archive recovered: "+chip.label()+". Optional data "+chipsCollected()+" / "+totalChips()+".",5);burst(chip.x(),chip.y(),12);
        } else if(target instanceof GunPickup pickup) {
            pickup.collect();player.equipGun();announce("Pulse blaster equipped. Aim with the mouse; hold F or left click to fire. Destroy all robots.",7);
        } else announce("Approach a battery, socket, relay or data chip, then press E.",3);
    }
    private void damage(boolean pit) {
        if(stageFailed || stageComplete || (!pit && player.invulnerability()>0))return;
        lives--;fallFlash=1;player.protect(1.4);player.stop();
        if(lives<=0) {
            lives=0;stageFailed=true;deaths++;projectiles.clear();
            announce("No lives left. Retry this level with five lives; earlier levels stay completed.",10);return;
        }
        if(pit) {
            Checkpoint pad=checkpoints.get(checkpoint);player.setPosition(pad.x(),pad.y());recoverBattery();burst(pad.x(),pad.y(),18);
        }
        announce((pit?"Safety recall. ":"Shield hit. ")+lives+" / 5 lives remaining.",3);
    }
    private void recoverBattery() {
        if(core.state()!=EnergyCore.State.GROUND) {
            if(core.socket()!=null) core.socket().setHasCore(false);
            core.carry();player.setHoldingCore(true);core.follow(player);
        }
    }
    private void shoot() {
        double dx=player.aimX(),dy=player.aimY();
        projectiles.add(new Projectile(player.x()+dx*.32,player.y()+dy*.32,dx,dy));
        player.fire();shotCooldown=.22;
    }
    private void updateProjectiles(double dt) {
        for(int i=projectiles.size()-1;i>=0;i--) {
            Projectile projectile=projectiles.get(i);boolean remove=false;
            int steps=Math.max(1,(int)Math.ceil(13*dt/.08));
            for(int step=0;step<steps && !remove;step++) {
                projectile.move(dt/steps);
                if(projectile.life()<=0 || collisionAt(projectile.x(),projectile.y(),.06)) { remove=true;break; }
                for(ChaserRobot robot:robots) if(robot.alive() && Math.hypot(robot.x()-projectile.x(),robot.y()-projectile.y())<robot.radius()+.1) {
                    robot.hit();remove=true;burst(robot.x(),robot.y(),robot.alive()?5:24);
                    if(!robot.alive())announce("Pursuer destroyed. "+robotsRemaining()+" remaining.",3);
                    break;
                }
            }
            if(remove)projectiles.remove(i);
        }
    }
    private void announce(String text,double seconds) { message=text;messageTime=seconds; }
    private void burst(double x,double y,int count) {
        for(int i=0;i<count;i++) {
            double a=random.nextDouble()*Math.PI*2,v=.1+random.nextDouble()*.7,life=.4+random.nextDouble()*.7;
            particles.add(new Particle(x,y,Math.cos(a)*v,Math.sin(a)*v,life,life));
        }
    }
    private void animateParticles(double dt) {
        for(int i=particles.size()-1;i>=0;i--) {
            Particle p=particles.get(i).advance(dt);if(p.life()<=0) particles.remove(i);else particles.set(i,p);
        }
    }
}
