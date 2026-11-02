package borrowedenergy.render;

import borrowedenergy.model.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.List;

/** The entire current mission is one readable, illuminated science-fiction space. */
public final class WorldRenderer {
    public static final int TILE=32, MAP_X=64, MAP_Y=96;
    private static final int W=36*TILE,H=20*TILE;
    private static final Color MINT=new Color(113,255,213), AMBER=new Color(255,201,108), RED=new Color(255,91,115);
    private static final Font SMALL=new Font(Font.SANS_SERIF,Font.BOLD,9);
    private final BufferedImage darkness=new BufferedImage(W,H,BufferedImage.TYPE_INT_ARGB);
    private Level cachedLevel;
    private BufferedImage architecture;
    private record Theme(Color primary,Color secondary,Color floor,String code) { }
    private static Theme theme(int stage) {
        return switch(stage) {
            case 1 -> new Theme(new Color(135,170,255),new Color(235,112,255),new Color(42,43,69),"NTR / 02");
            case 2 -> new Theme(new Color(255,157,100),new Color(244,102,170),new Color(49,42,59),"SGV / 03");
            default -> new Theme(new Color(83,223,239),new Color(121,255,195),new Color(34,52,65),"ORG / 01");
        };
    }

    public void render(Graphics2D graphics,GameModel model,double time) {
        Theme t=theme(model.stageIndex());
        Graphics2D g=(Graphics2D)graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,RenderingHints.VALUE_STROKE_PURE);
        background(g,t,time);g.translate(MAP_X,MAP_Y);g.clipRect(0,0,W,H);
        if(cachedLevel!=model.level()) {
            cachedLevel=model.level();architecture=new BufferedImage(W,H,BufferedImage.TYPE_INT_ARGB);
            Graphics2D map=architecture.createGraphics();map.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            floor(map,model.level(),t,model.stageIndex());walls(map,model.level(),t,model.stageIndex());map.dispose();
        }
        g.drawImage(architecture,0,0,null);ambient(g,t,time);
        for(PowerCircuit circuit:model.circuits()) cable(g,circuit,t,time);
        for(ElectricLeak leak:model.leaks())
            CombatVisual.leak(g,px(leak.x()),px(leak.y()),px(leak.width()),px(leak.height()),leak.active(),leak.warning(),time);
        for(AlienSaucer alien:model.aliens())
            CombatVisual.beam(g,px(alien.beamStartX()),px(alien.beamStartY()),px(alien.beamEndX()),px(alien.beamEndY()),alien.beamActive(),alien.beamWarning(),time);
        extraction(g,model,time);
        for(Checkpoint checkpoint:model.checkpoints()) checkpoint(g,checkpoint,t);
        for(LaserHazard laser:model.lasers()) laser(g,laser,time);
        for(ElectricDoor door:model.doors()) door(g,door,t);
        for(PowerCircuit circuit:model.circuits()) {
            socket(g,circuit.socket(),t,time);
            for(Relay relay:circuit.relays()) relay(g,relay,time);
        }
        generator(g,model.generator(),t,time);
        for(DataChip chip:model.chips()) if(!chip.collected()) chip(g,chip,time);
        if(model.core().state()==EnergyCore.State.GROUND) {
            double x=px(model.core().x()),y=px(model.core().y());
            g.setColor(new Color(0,0,0,100));g.fill(new Ellipse2D.Double(x-17,y+14,34,9));
            BatteryVisual.draw(g,x,y-7+Math.sin(time*2.6)*2,time,.82);
            ring(g,x,y+15,24,alpha(MINT,130),time*20);label(g,"ION BATTERY",x,y+41,MINT);
        }
        for(PatrolDrone drone:model.drones()) drone(g,drone,time);
        for(DemonInsect insect:model.insects())
            CombatVisual.insect(g,px(insect.x()),px(insect.y()),insect.heading(),time);
        for(AlienSaucer alien:model.aliens())
            CombatVisual.saucer(g,px(alien.x()),px(alien.y()),alien.beamWarning(),alien.beamActive(),time);
        for(ChaserRobot robot:model.robots())
            CombatVisual.robot(g,px(robot.x()),px(robot.y()),robot.heading(),robot.health(),robot.maxHealth(),robot.hitFlash(),time);
        if(model.gun()!=null&&!model.gun().collected()) gunPickup(g,model.gun(),time);
        player(g,model.player(),time);
        for(Projectile projectile:model.projectiles())
            CombatVisual.bolt(g,px(projectile.x()),px(projectile.y()),projectile.dx(),projectile.dy(),time);
        particles(g,model.particles());lighting(g,model);
        deviceLabels(g,model);interaction(g,model.interactionTarget());
        if(model.damageFlash()>0) damageEdges(g,model.damageFlash());
        g.dispose();frame(graphics,t);
    }
    private static double px(double x){return x*TILE;}
    private static Color alpha(Color c,int a){return new Color(c.getRed(),c.getGreen(),c.getBlue(),Math.max(0,Math.min(255,a)));}
    private static void background(Graphics2D g,Theme t,double time) {
        g.setColor(new Color(6,12,23));g.fillRect(0,0,1280,800);
        g.setPaint(new RadialGradientPaint(640,360,790,new float[]{0,1},new Color[]{new Color(23,38,58),new Color(5,10,19)}));g.fillRect(0,0,1280,800);
        g.setColor(new Color(72,106,145,37));for(int x=24;x<1280;x+=32)for(int y=8;y<800;y+=32)g.fillRect(x,y,1,1);
        for(int i=0;i<8;i++){int x=38+(i%2)*1205,y=125+(i/2)*140;g.setColor(alpha(t.primary,80));g.fillRoundRect(x,y,2,55,2,2);g.setColor(alpha(t.primary,35));g.fillRect(x-3,y,8,3);}
        g.setColor(new Color(0,0,0,160));g.fillRoundRect(MAP_X-8,MAP_Y-5,W+16,H+17,15,15);
    }
    private static void floor(Graphics2D g,Level level,Theme t,int stage) {
        g.setColor(new Color(11,23,36));g.fillRect(0,0,W,H);
        for(int y=0;y<Level.HEIGHT;y++)for(int x=0;x<Level.WIDTH;x++) {
            int ox=x*TILE,oy=y*TILE;char tile=level.tileAt(x,y);
            if(tile=='~'){pit(g,level,x,y,t);continue;}if(tile=='#')continue;
            int v=Math.floorMod(x*31+y*19,5);g.setColor(new Color(t.floor.getRed()+v,t.floor.getGreen()+v,t.floor.getBlue()+v));g.fillRect(ox,oy,TILE,TILE);
            g.setColor(new Color(9,19,32,160));g.drawRect(ox,oy,TILE,TILE);
            g.setColor(new Color(136,177,198,34));g.drawLine(ox+2,oy+2,ox+29,oy+2);
            if(x%2==0&&y%2==0){g.setColor(new Color(145,184,209,30));g.fillRect(ox+4,oy+4,2,2);g.setColor(new Color(8,19,31,100));g.fillRoundRect(ox+9,oy+25,14,2,1,1);}
            if((x+3*y)%29==0){g.setColor(new Color(10,22,34,120));g.fillRoundRect(ox+6,oy+7,21,18,3,3);g.setColor(new Color(95,133,161,65));for(int n=0;n<4;n++)g.drawLine(ox+9,oy+10+n*3,ox+24,oy+10+n*3);}
            g.setColor(alpha(t.primary,96));
            if(level.tileAt(x,y-1)=='#')g.fillRect(ox+5,oy+3,22,1);
            if(level.tileAt(x-1,y)=='#')g.fillRect(ox+3,oy+5,1,22);
            if(level.tileAt(x,y+1)=='#')g.fillRect(ox+5,oy+28,22,1);
        }
        g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,42));g.setColor(alpha(t.primary,26));g.drawString(stage==0?"ORBITAL":stage==1?"TRANSIT":"SINGULARITY",61,92);
        g.setFont(new Font(Font.MONOSPACED,Font.PLAIN,10));g.setColor(alpha(t.primary,105));g.drawString("DECK "+(stage+1)+"  /  "+t.code+"  /  AUXILIARY ACCESS",63,111);
        for(int x=5;x<32;x+=9)if(level.tileAt(x,17)=='.'){
            g.setColor(alpha(t.primary,65));for(int n=0;n<3;n++){int sx=x*TILE+n*12;g.drawLine(sx,556,sx+5,561);g.drawLine(sx+5,561,sx,566);}
        }
    }
    private static void pit(Graphics2D g,Level level,int x,int y,Theme t) {
        int ox=x*TILE,oy=y*TILE;g.setColor(new Color(4,8,20));g.fillRect(ox,oy,TILE,TILE);
        g.setColor(alpha(t.primary,28));g.drawLine(ox,oy+30,ox+30,oy);g.drawLine(ox+16,oy+32,ox+32,oy+16);
        g.setColor(new Color(54,79,111,100));g.fillRect(ox+Math.floorMod(x*7,25)+3,oy+11,1,1);
        for(int side=0;side<4;side++) {
            int nx=x+(side==0?-1:side==1?1:0),ny=y+(side==2?-1:side==3?1:0);if(level.tileAt(nx,ny)=='~')continue;
            g.setColor(new Color(86,78,65));if(side<2)g.fillRect(ox+(side==0?0:28),oy,4,32);else g.fillRect(ox,oy+(side==2?0:28),32,4);
            g.setColor(new Color(246,184,85,175));for(int i=3;i<32;i+=9){if(side<2)g.drawLine(ox+(side==0?0:29),oy+i,ox+(side==0?3:32),oy+i+3);else g.drawLine(ox+i,oy+(side==2?0:29),ox+i+3,oy+(side==2?3:32));}
        }
    }
    private static void walls(Graphics2D g,Level level,Theme t,int stage) {
        for(int y=0;y<Level.HEIGHT;y++)for(int x=0;x<Level.WIDTH;x++) {
            if(level.tileAt(x,y)!='#')continue;int ox=x*TILE,oy=y*TILE;boolean bottom=level.tileAt(x,y+1)!='#';
            if(bottom){g.setColor(new Color(0,0,0,88));g.fillRect(ox+3,oy+22,33,18);}
            g.setPaint(new GradientPaint(ox,oy,new Color(71,92,117),ox,oy+32,new Color(35,51,73)));g.fillRect(ox,oy,32,32);
            g.setColor(new Color(16,31,49));g.drawRect(ox+1,oy+1,30,30);g.setColor(new Color(137,169,194,90));g.drawLine(ox+2,oy+2,ox+29,oy+2);
            if(bottom){g.setColor(new Color(21,35,53));g.fillRect(ox,oy+24,32,8);g.setColor(alpha(t.primary,140));g.fillRect(ox+3,oy+24,26,2);}
            if((y==0||y==19)&&x>1&&x<34) {
                g.setColor(new Color(5,15,35));g.fillRoundRect(ox+2,oy+6,28,17,3,3);g.setColor(new Color(42,91,133,110));g.fillRect(ox+4,oy+8,24,2);
                g.setColor(new Color(139,207,237,175));g.fillRect(ox+5+Math.floorMod(x*11,20),oy+12+Math.floorMod(x*7,8),1,1);
                if(x%6==0){g.setColor(alpha(t.primary,100));g.drawLine(ox+21,oy+7,ox+7,oy+21);}
            }else if(x>0&&x<35&&y>0&&y<19) {
                int mode=Math.floorMod(x*3+y*7,4);
                if(stage==0&&mode==0){g.setColor(new Color(13,40,49));g.fillRoundRect(ox+5,oy+5,22,19,5,5);g.setColor(new Color(68,155,115));g.fillOval(ox+10,oy+10,8,12);g.fillOval(ox+16,oy+7,6,10);g.setColor(new Color(117,223,197,100));g.drawRoundRect(ox+5,oy+5,22,19,5,5);}
                else if(mode==1){g.setColor(new Color(13,30,48));g.fillRoundRect(ox+6,oy+5,20,17,3,3);g.setColor(alpha(t.primary,150));g.fillRect(ox+9,oy+8,11,2);g.fillRect(ox+9,oy+13,6,1);g.setColor(alpha(t.secondary,150));g.fillRect(ox+9,oy+17,14,1);}
                else{g.setColor(new Color(16,33,51));for(int i=0;i<4;i++)g.fillRoundRect(ox+7,oy+7+i*4,18,2,1,1);g.setColor(new Color(123,156,178,110));g.fillRect(ox+4,oy+5,2,2);g.fillRect(ox+26,oy+20,2,2);}
            }
        }
    }
    private static void ambient(Graphics2D g,Theme t,double time) {
        g.setStroke(new BasicStroke(1));for(int i=0;i<6;i++){double x=82+i*189,y=39+(time*11+i*91)%550;g.setColor(alpha(t.primary,50));g.drawLine((int)x,(int)y,(int)x+18,(int)y);g.fillRect((int)x+21,(int)y-2,2,2);}
        int sy=(int)((time*20)%H);g.setPaint(new GradientPaint(0,sy-28,new Color(100,230,255,0),0,sy,alpha(t.primary,13)));g.fillRect(32,sy-28,W-64,28);g.setColor(alpha(t.primary,24));g.drawLine(32,sy,W-32,sy);
    }
    private static Path2D cablePath(PowerCircuit circuit) {
        Path2D path=new Path2D.Double();List<Point2D.Double> points=circuit.waypoints();Point2D.Double start=points.get(0);path.moveTo(px(start.x),px(start.y));
        for(int i=1;i<points.size();i++) {
            Point2D.Double a=points.get(i-1),b=points.get(i);Relay relay=i>1&&i-2<circuit.relays().size()?circuit.relays().get(i-2):null;
            if(relay!=null){int d=relay.requiredOrientation();double sign=d<2?1:-1;
                if(d%2==0){double elbow=a.x+sign*Math.max(.8,Math.max(0,(b.x-a.x)*sign)*.5);path.lineTo(px(elbow),px(a.y));path.lineTo(px(elbow),px(b.y));}
                else{double elbow=(b.y-a.y)*sign>=.8?b.y:a.y+sign*.8;path.lineTo(px(a.x),px(elbow));path.lineTo(px(b.x),px(elbow));}
            }else if(a.x!=b.x&&a.y!=b.y){double elbow=(a.x+b.x)*.5;path.lineTo(px(elbow),px(a.y));path.lineTo(px(elbow),px(b.y));}
            path.lineTo(px(b.x),px(b.y));
        }return path;
    }
    private static void cable(Graphics2D g,PowerCircuit circuit,Theme t,double time) {
        Path2D path=cablePath(circuit);boolean live=circuit.powered()||circuit.complete();Color color=circuit.complete()?MINT:live?t.primary:new Color(98,147,171);
        g.setStroke(new BasicStroke(9,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.setColor(new Color(6,18,32,210));g.draw(path);
        if(live){g.setStroke(new BasicStroke(12,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.setColor(alpha(color,28));g.draw(path);}
        g.setStroke(new BasicStroke(3,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.setColor(alpha(color,live?195:115));g.draw(path);
        g.setStroke(new BasicStroke(.9f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND,10,new float[]{5,18},(float)((time*35)%23)));g.setColor(alpha(color,live?255:55));g.draw(path);g.setStroke(new BasicStroke(1));
    }
    private static void door(Graphics2D g,ElectricDoor door,Theme t) {
        Graphics2D d=(Graphics2D)g.create();double w=px(door.width()),h=px(door.height());d.translate(px(door.x()),px(door.y()));
        if(w>h){d.rotate(Math.PI/2);double temp=w;w=h;h=temp;}
        int x=(int)(-w/2),y=(int)(-h/2);Color color=door.open()?MINT:AMBER;
        d.setColor(new Color(9,23,37));d.fillRoundRect(x-4,y-5,(int)w+8,(int)h+10,4,4);d.setColor(new Color(101,136,157));d.fillRect(x-4,y-5,(int)w+8,5);d.fillRect(x-4,y+(int)h,(int)w+8,5);
        if(!door.open()) {
            d.setPaint(new GradientPaint(x,0,new Color(81,103,127),x+(int)w,0,new Color(38,62,88)));d.fillRect(x+2,y+1,(int)w-4,(int)h-2);
            d.setColor(new Color(8,24,40));d.fillRect(-1,y,2,(int)h);d.setColor(alpha(t.primary,90));for(int i=7;i<h-3;i+=12)d.drawLine(x+5,y+i,x+(int)w-6,y+i);
            d.setColor(new Color(10,24,38));d.fillRoundRect(-9,-10,18,20,4,4);d.setColor(color);d.drawRoundRect(-4,-5,8,7,4,4);d.fillRoundRect(-6,0,12,8,2,2);
        }else{d.setColor(alpha(MINT,15));d.fillRect(x,y,(int)w,(int)h);d.setColor(new Color(73,99,122));d.fillRect(x+1,y,3,(int)h);d.fillRect(x+(int)w-4,y,3,(int)h);d.setColor(MINT);for(int i=7;i<h-3;i+=12){d.fillRect(x+1,y+i,2,4);d.fillRect(x+(int)w-3,y+i,2,4);}}
        glow(d,0,y-2,26,alpha(color,44));d.setColor(color);d.fillRoundRect(x+2,y-4,(int)w-4,2,2,2);d.dispose();
    }
    private static void socket(Graphics2D g,PowerSocket socket,Theme t,double time) {
        double x=px(socket.x()),y=px(socket.y());PowerCircuit circuit=socket.circuit();Color color=circuit.complete()?MINT:socket.hasCore()?t.primary:new Color(143,186,203);
        if(socket.hasCore()||circuit.complete())glow(g,x,y,58,alpha(color,42));
        g.setColor(new Color(5,16,28,145));g.fill(new Ellipse2D.Double(x-24,y-12,48,35));Shape shell=hex(x,y,22);
        g.setPaint(new GradientPaint((float)x,(float)y-22,new Color(70,99,123),(float)x,(float)y+22,new Color(28,49,71)));g.fill(shell);g.setColor(alpha(color,180));g.setStroke(new BasicStroke(1.4f));g.draw(shell);
        g.setColor(new Color(9,25,37));g.fillRoundRect((int)x-11,(int)y-16,22,32,5,5);
        if(socket.hasCore())BatteryVisual.draw(g,x,y-1,time,.72);else{g.setColor(alpha(color,145));g.drawRoundRect((int)x-7,(int)y-11,14,22,3,3);g.fillRect((int)x-3,(int)y-14,6,3);g.drawLine((int)x-3,(int)y,(int)x+3,(int)y);g.drawLine((int)x,(int)y-3,(int)x,(int)y+3);}
        for(int i=0;i<3;i++){g.setColor(alpha(color,120));g.fillRect((int)x-25,(int)y-7+i*7,3,3);g.fillRect((int)x+23,(int)y-7+i*7,3,3);}
        if(circuit.powered()&&!circuit.complete()){g.setColor(MINT);g.setStroke(new BasicStroke(3,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.drawArc((int)x-28,(int)y-28,56,56,90,-(int)(360*circuit.progress()));}g.setStroke(new BasicStroke(1));
    }
    private static void relay(Graphics2D g,Relay relay,double time) {
        double x=px(relay.x()),y=px(relay.y());Color color=relay.aligned()?MINT:AMBER;
        glow(g,x,y,34,alpha(color,17));g.setColor(new Color(5,19,32,130));g.fillOval((int)x-20,(int)y-9,40,30);
        Shape shell=hex(x,y,19);g.setColor(new Color(33,57,77));g.fill(shell);g.setColor(alpha(color,130));g.draw(shell);g.setColor(new Color(9,27,42));g.fillOval((int)x-13,(int)y-13,26,26);
        Graphics2D r=(Graphics2D)g.create();r.translate(x,y);r.rotate(relay.orientation()*Math.PI/2);r.setColor(color);r.setStroke(new BasicStroke(2.6f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));r.drawLine(-6,0,7,0);r.drawLine(2,-5,7,0);r.drawLine(7,0,2,5);r.dispose();
        r=(Graphics2D)g.create();r.translate(x,y);r.rotate(relay.requiredOrientation()*Math.PI/2);Path2D marker=new Path2D.Double();marker.moveTo(28,0);marker.lineTo(21,-4);marker.lineTo(21,4);marker.closePath();r.setColor(AMBER);r.fill(marker);r.dispose();g.setStroke(new BasicStroke(1));
        if(relay.aligned()){g.setColor(MINT);g.fillOval((int)x-2,(int)y-23,4,4);}
    }
    private static void generator(Graphics2D g,MainGenerator generator,Theme t,double time) {
        double x=px(generator.x()),y=px(generator.y());boolean online=generator.online();Color color=online?MINT:t.primary;glow(g,x,y,online?112:75,alpha(color,online?72:33));
        g.setColor(new Color(3,15,29,150));g.fillOval((int)x-49,(int)y-25,98,75);Shape shell=hex(x,y,43);
        g.setPaint(new GradientPaint((float)x-30,(float)y-40,new Color(79,110,136),(float)x+30,(float)y+40,new Color(22,42,67)));g.fill(shell);g.setColor(alpha(color,180));g.setStroke(new BasicStroke(1.5f));g.draw(shell);
        g.setColor(new Color(6,22,37));g.fillOval((int)x-31,(int)y-31,62,62);ring(g,x,y,29,alpha(color,155),time*(online?35:8));ring(g,x,y,23,alpha(t.secondary,160),-time*28);
        g.setPaint(new RadialGradientPaint(new Point2D.Double(x,y),17,new float[]{0,1},new Color[]{alpha(color,online?255:170),alpha(color,14)}));g.fillOval((int)x-17,(int)y-17,34,34);g.setColor(new Color(220,255,244,online?235:120));g.fill(hex(x,y,8));
        for(int i=0;i<4;i++){double a=Math.PI/4+i*Math.PI/2;double rx=x+Math.cos(a)*37,ry=y+Math.sin(a)*37;g.setColor(color);g.fill(new Ellipse2D.Double(rx-2,ry-2,4,4));}g.setStroke(new BasicStroke(1));
    }
    private static void extraction(Graphics2D g,GameModel model,double time) {
        double x=px(model.exitX()),y=px(model.exitY());boolean ready=model.extractionReady();Color color=ready?MINT:new Color(99,137,165);
        glow(g,x,y,67,alpha(color,ready?42:15));g.setColor(new Color(10,30,45,150));g.fillOval((int)x-37,(int)y-25,74,50);
        g.setStroke(new BasicStroke(2));g.setColor(alpha(color,180));g.drawOval((int)x-32,(int)y-22,64,44);g.setColor(alpha(color,60));g.drawOval((int)x-25,(int)y-17,50,34);g.setStroke(new BasicStroke(1));
        for(int i=0;i<4;i++){double a=i*Math.PI/2+time*(ready?.5:0);double ax=x+Math.cos(a)*36,ay=y+Math.sin(a)*24;g.setColor(color);g.fill(new Ellipse2D.Double(ax-2,ay-2,4,4));}
        g.setColor(color);g.fillRoundRect((int)x-11,(int)y-4,22,8,2,2);g.drawLine((int)x+6,(int)y-9,(int)x+12,(int)y);g.drawLine((int)x+12,(int)y,(int)x+6,(int)y+9);
        String status=ready?"EXIT / READY":model.stageIndex()==2&&model.robotsRemaining()>0?"CLEAR HOSTILE ROBOTS":"RESTORE ALL CIRCUITS";
        label(g,status,x,y+39,color);
        if(ready) {
            g.setColor(alpha(MINT,150));g.setStroke(new BasicStroke(1.3f));
            double pulse=2+Math.sin(time*3)*2;
            g.drawOval((int)(x-38-pulse),(int)(y-26-pulse),(int)(76+pulse*2),(int)(52+pulse*2));
            g.setStroke(new BasicStroke(1));
        }
    }
    private static void checkpoint(Graphics2D g,Checkpoint pad,Theme t) {
        double x=px(pad.x()),y=px(pad.y());Color color=pad.active()?MINT:t.primary;g.setColor(alpha(color,20));g.fillOval((int)x-20,(int)y-12,40,24);
        g.setColor(alpha(color,pad.active()?180:90));g.setStroke(new BasicStroke(1.5f));g.drawOval((int)x-20,(int)y-12,40,24);g.drawLine((int)x-6,(int)y,(int)x+6,(int)y);g.drawLine((int)x,(int)y-6,(int)x,(int)y+6);g.setStroke(new BasicStroke(1));
        label(g,pad.active()?"RECALL LINK ACTIVE":"RECALL BEACON",x,y+29,alpha(color,185));
    }
    private static void laser(Graphics2D g,LaserHazard laser,double time) {
        double x=px(laser.x()),y=px(laser.y()),w=px(laser.width()),h=px(laser.height());boolean horizontal=w>=h,live=laser.active(),warning=laser.warning();Color color=live?RED:warning?AMBER:new Color(75,138,155);
        Rectangle2D zone=new Rectangle2D.Double(x-w/2,y-h/2,w,h);g.setColor(alpha(color,live?35:warning?25:10));g.fill(zone);
        g.setStroke(new BasicStroke(1,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,10,new float[]{4,6},0));g.setColor(alpha(color,live?155:85));g.draw(zone);
        double x1=horizontal?x-w/2:x,y1=horizontal?y:y-h/2,x2=horizontal?x+w/2:x,y2=horizontal?y:y+h/2;Line2D beam=new Line2D.Double(x1,y1,x2,y2);
        if(live){g.setStroke(new BasicStroke(12));g.setColor(alpha(RED,37));g.draw(beam);g.setStroke(new BasicStroke(4));g.setColor(RED);g.draw(beam);g.setStroke(new BasicStroke(1.2f));g.setColor(new Color(255,237,230));g.draw(beam);}
        else if(warning){g.setStroke(new BasicStroke(2));g.setColor(alpha(AMBER,(int)(110+90*Math.sin(time*19))));g.draw(beam);}else{g.setStroke(new BasicStroke(1));g.setColor(alpha(color,65));g.draw(beam);}g.setStroke(new BasicStroke(1));
        for(int n=0;n<2;n++){double ex=n==0?x1:x2,ey=n==0?y1:y2;g.setColor(new Color(16,32,48));g.fillRoundRect((int)ex-6,(int)ey-7,12,14,4,4);g.setColor(color);g.fillRoundRect((int)ex-2,(int)ey-4,4,8,2,2);glow(g,ex,ey,17,alpha(color,live?60:20));}
        label(g,live?"LASER ACTIVE":warning?"WARNING / PULSE":"SAFE WINDOW",x,y-h/2-9,alpha(color,190));
    }
    private static void drone(Graphics2D g,PatrolDrone drone,double time) {
        double x=px(drone.x()),y=px(drone.y()),bob=Math.sin(time*5+x)*1.5;g.setColor(new Color(0,0,0,100));g.fillOval((int)x-16,(int)y+8,32,12);
        g.setColor(new Color(255,94,118,25));g.fillOval((int)x-21,(int)y-21,42,42);g.setStroke(new BasicStroke(1,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,10,new float[]{3,5},(float)(time*8%8)));g.setColor(alpha(RED,100));g.drawOval((int)x-21,(int)y-21,42,42);g.setStroke(new BasicStroke(1));
        Graphics2D d=(Graphics2D)g.create();d.translate(x,y-5+bob);d.rotate(drone.heading());
        for(int n=-1;n<=1;n+=2){glow(d,-3,n*11,14,new Color(91,181,255,60));d.setColor(new Color(30,52,77));d.fillRoundRect(-10,n*11-4,19,8,4,4);d.setColor(new Color(121,210,255));d.fillRoundRect(-11,n*11-2,4,4,2,2);}
        d.setPaint(new GradientPaint(-12,-10,new Color(137,154,176),12,10,new Color(43,58,82)));d.fill(hex(0,0,14));d.setColor(new Color(11,25,42));d.fillOval(-9,-9,18,18);d.setColor(RED);d.fillRoundRect(3,-4,7,8,3,3);d.setColor(new Color(255,227,217));d.fillOval(6,-2,3,3);d.dispose();
    }
    private static void chip(Graphics2D g,DataChip chip,double time) {
        double x=px(chip.x()),y=px(chip.y())+Math.sin(time*3+chip.x())*2;Color violet=new Color(216,160,255);glow(g,x,y,31,alpha(violet,35));
        g.setColor(new Color(20,28,57));g.fillRoundRect((int)x-9,(int)y-11,18,22,4,4);g.setColor(violet);g.drawRoundRect((int)x-9,(int)y-11,18,22,4,4);g.setColor(new Color(166,124,232));g.fillRect((int)x-4,(int)y-6,8,9);
        for(int i=-1;i<=1;i++){g.setColor(new Color(239,218,255));g.fillRect((int)x-12,(int)y+i*5,3,2);g.fillRect((int)x+10,(int)y+i*5,3,2);}g.setColor(new Color(239,218,255));g.fillRect((int)x-2,(int)y+6,4,1);
    }
    private static void gunPickup(Graphics2D g,GunPickup gun,double time) {
        double x=px(gun.x()),y=px(gun.y()),bob=Math.sin(time*2.5)*2;
        glow(g,x,y-3,42,new Color(90,236,255,45));
        g.setColor(new Color(4,17,29,160));g.fillOval((int)x-24,(int)y+9,48,15);
        ring(g,x,y+10,26,alpha(MINT,170),time*22);
        CombatVisual.blaster(g,x-5,y-4+bob,-.2,1.05,false,time);
        label(g,"PULSE BLASTER / E",x,y+39,MINT);
    }
    private static void player(Graphics2D g,Player player,double time) {
        double x=px(player.x()),y=px(player.y());g.setColor(new Color(0,0,0,125));g.fillOval((int)x-13,(int)y+6,26,11);glow(g,x,y-9,30,new Color(255,212,92,17));
        if(player.invulnerability()<=0||(int)(time*12)%3!=0)RobotSprite.drawWalking(g,x,y+12,54,player.facingX(),player.facingY(),player.walkPhase(),player.speed(),time);
        if(player.holdingCore())BatteryVisual.draw(g,x+(player.facingX()<-.1?-18:18),y-10,time,.43);
        if(player.hasGun()) {
            double angle=Math.atan2(player.aimY(),player.aimX());
            CombatVisual.blaster(g,x+player.aimX()*6,y-8+player.aimY()*4,angle,.72,player.muzzleFlash()>0,time);
        }
    }
    private static void damageEdges(Graphics2D g,double strength) {
        float a=(float)Math.min(1,Math.max(0,strength));
        Color edge=new Color(255,70,107,(int)(a*80)),clear=new Color(255,70,107,0);
        g.setPaint(new GradientPaint(0,0,edge,92,0,clear));g.fillRect(0,0,92,H);
        g.setPaint(new GradientPaint(W,0,edge,W-92,0,clear));g.fillRect(W-92,0,92,H);
        g.setPaint(new GradientPaint(0,0,edge,0,65,clear));g.fillRect(0,0,W,65);
        g.setPaint(new GradientPaint(0,H,edge,0,H-65,clear));g.fillRect(0,H-65,W,65);
    }
    private static void particles(Graphics2D g,List<Particle> particles) {
        for(Particle p:particles){double fade=Math.max(0,Math.min(1,p.life()/p.maxLife()));g.setColor(alpha(MINT,(int)(fade*200)));double s=1+fade*2;g.fill(new Ellipse2D.Double(px(p.x())-s/2,px(p.y())-s/2,s,s));}
    }
    private void lighting(Graphics2D g,GameModel model) {
        Graphics2D mask=darkness.createGraphics();mask.setComposite(AlphaComposite.Src);mask.setColor(new Color(2,7,20,model.extractionReady()?20:55));mask.fillRect(0,0,W,H);mask.setComposite(AlphaComposite.DstOut);
        light(mask,px(model.player().x()),px(model.player().y()),model.player().holdingCore()?255:205,.96f);
        if(model.core().state()==EnergyCore.State.GROUND)light(mask,px(model.core().x()),px(model.core().y()),130,.88f);
        for(PowerCircuit c:model.circuits())light(mask,px(c.socket().x()),px(c.socket().y()),c.complete()?110:65,.65f);
        light(mask,px(model.generator().x()),px(model.generator().y()),115,.65f);mask.dispose();g.drawImage(darkness,0,0,null);
    }
    private static void light(Graphics2D g,double x,double y,float r,float strength) {
        g.setPaint(new RadialGradientPaint(new Point2D.Double(x,y),r,new float[]{0,.5f,1},new Color[]{new Color(0f,0f,0f,strength),new Color(0f,0f,0f,strength*.7f),new Color(0f,0f,0f,0f)}));g.fill(new Ellipse2D.Double(x-r,y-r,r*2,r*2));
    }
    private static void deviceLabels(Graphics2D g,GameModel model) {
        for(PowerCircuit c:model.circuits()){label(g,c.name()+" / "+(c.complete()?"ONLINE":"SOCKET "+c.id()),px(c.socket().x()),px(c.socket().y())+38,c.complete()?MINT:new Color(162,201,216));for(Relay r:c.relays())label(g,r.label(),px(r.x()),px(r.y())+34,r.aligned()?MINT:AMBER);}
        label(g,model.generator().online()?"REACTOR ONLINE":"FUSION REACTOR",px(model.generator().x()),px(model.generator().y())+57,model.generator().online()?MINT:new Color(163,208,229));
    }
    private static void interaction(Graphics2D g,Interactable target) {
        if(target==null)return;double x=px(target.x()),y=px(target.y());g.setColor(new Color(207,255,242,220));g.setStroke(new BasicStroke(1.6f));
        for(int n=0;n<4;n++){int sx=n%2==0?-1:1,sy=n<2?-1:1;g.draw(new Line2D.Double(x+sx*27,y+sy*19,x+sx*27,y+sy*27));g.draw(new Line2D.Double(x+sx*27,y+sy*27,x+sx*19,y+sy*27));}
        g.setStroke(new BasicStroke(1));g.setColor(MINT);g.fillRoundRect((int)x+22,(int)y-39,20,20,5,5);g.setFont(new Font(Font.MONOSPACED,Font.BOLD,12));g.setColor(new Color(11,40,38));g.drawString("E",(int)x+28,(int)y-24);
    }
    private static void label(Graphics2D g,String value,double x,double y,Color color) {
        g.setFont(SMALL);int w=g.getFontMetrics().stringWidth(value);g.setColor(new Color(5,17,31,205));g.fillRoundRect((int)x-w/2-5,(int)y-11,w+10,16,5,5);g.setColor(color);g.drawString(value,(float)x-w/2f,(float)y);
    }
    private static void frame(Graphics2D graphics,Theme t) {
        Graphics2D g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setColor(new Color(61,92,120));g.setStroke(new BasicStroke(1));g.drawRoundRect(MAP_X-1,MAP_Y-1,W+1,H+1,8,8);g.setStroke(new BasicStroke(2));g.setColor(alpha(t.primary,210));
        for(int i=0;i<4;i++){int x=i%2==0?MAP_X-1:MAP_X+W+1,y=i<2?MAP_Y-1:MAP_Y+H+1,dx=i%2==0?15:-15,dy=i<2?15:-15;g.drawLine(x,y+dy,x,y);g.drawLine(x,y,x+dx,y);}g.dispose();
    }
    private static Shape hex(double x,double y,double r) {
        Path2D p=new Path2D.Double();for(int i=0;i<6;i++){double a=Math.PI/3*i+Math.PI/6;if(i==0)p.moveTo(x+Math.cos(a)*r,y+Math.sin(a)*r);else p.lineTo(x+Math.cos(a)*r,y+Math.sin(a)*r);}p.closePath();return p;
    }
    private static void ring(Graphics2D g,double x,double y,int radius,Color color,double angle) {
        g.setStroke(new BasicStroke(1.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.setColor(color);for(int i=0;i<3;i++)g.drawArc((int)x-radius,(int)y-radius,radius*2,radius*2,(int)angle+i*120,80);g.setStroke(new BasicStroke(1));
    }
    private static void glow(Graphics2D g,double x,double y,float r,Color color) {
        g.setPaint(new RadialGradientPaint(new Point2D.Double(x,y),r,new float[]{0,.45f,1},new Color[]{color,alpha(color,color.getAlpha()/2),alpha(color,0)}));g.fill(new Ellipse2D.Double(x-r,y-r,r*2,r*2));
    }
}
