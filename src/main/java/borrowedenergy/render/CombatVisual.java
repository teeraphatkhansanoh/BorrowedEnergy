package borrowedenergy.render;

import java.awt.*;
import java.awt.geom.*;

/** Readable combat silhouettes drawn in the same illuminated style as the station. */
public final class CombatVisual {
    private static final Color CYAN = new Color(108, 243, 255);
    private static final Color RED = new Color(255, 83, 112);
    private static final Color AMBER = new Color(255, 201, 100);
    private CombatVisual() { }

    public static void insect(Graphics2D graphics, double x, double y, double heading, double time) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setColor(new Color(0, 0, 0, 110)); g.fill(new Ellipse2D.Double(x-14, y+10, 28, 10));
        g.translate(x, y-8+Math.sin(time*6+x)*2); g.rotate(heading);
        glow(g, 0, 0, 33, new Color(237, 80, 174, 24));
        double flap=.5+.5*Math.sin(time*34+x);
        for(int side : new int[]{-1,1}) {
            Graphics2D wing=(Graphics2D)g.create(); wing.scale(1,side);
            wing.rotate(-.35+flap*.55);
            wing.setColor(new Color(197, 166, 250, 90));
            Path2D shape=new Path2D.Double();shape.moveTo(-2,2);shape.curveTo(-13,5,-27,17,-20,21);shape.curveTo(-8,24,9,10,6,4);shape.closePath();
            wing.fill(shape);wing.setColor(new Color(226,204,255,165));wing.setStroke(new BasicStroke(1));wing.draw(shape);
            wing.draw(new Line2D.Double(-1,6,-18,19));wing.draw(new Line2D.Double(-1,6,-9,19));wing.dispose();
        }
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(92,48,103));
        for(int side : new int[]{-1,1})for(int i=0;i<3;i++) {
            double sx=-8+i*6,sy=side*5,ex=sx-5,ey=side*(12+Math.sin(time*13+i)*2);
            g.draw(new Line2D.Double(sx,sy,ex,ey));g.draw(new Line2D.Double(ex,ey,ex+5,ey+side*3));
        }
        g.setPaint(new GradientPaint(-12,-5,new Color(93,40,91),6,7,new Color(29,24,51)));
        g.fill(new Ellipse2D.Double(-14,-7,23,14));g.setColor(new Color(160,60,136));g.draw(new Ellipse2D.Double(-14,-7,23,14));
        g.setColor(new Color(198,83,163));g.draw(new Arc2D.Double(-12,-6,9,12,-70,140,Arc2D.OPEN));
        g.setColor(new Color(52,28,62));g.fill(new Ellipse2D.Double(3,-8,15,16));
        g.setColor(new Color(211,107,171));g.draw(new Ellipse2D.Double(3,-8,15,16));
        g.setColor(new Color(255,196,100));g.fill(new Ellipse2D.Double(10,-6,5,4));g.fill(new Ellipse2D.Double(10,2,5,4));
        g.setColor(new Color(255,244,211));g.fill(new Ellipse2D.Double(13,-5,2,2));g.fill(new Ellipse2D.Double(13,3,2,2));
        g.setColor(new Color(219,160,220));g.draw(new Line2D.Double(12,-7,18,-14));g.draw(new Line2D.Double(12,7,18,14));
        g.setColor(new Color(255,112,150));g.fill(new Ellipse2D.Double(17,-15,3,3));g.fill(new Ellipse2D.Double(17,12,3,3));
        Path2D mandible=new Path2D.Double();mandible.moveTo(17,-3);mandible.lineTo(23,-5);mandible.lineTo(20,0);mandible.lineTo(23,5);mandible.lineTo(17,3);
        g.setColor(new Color(219,182,205));g.draw(mandible);g.dispose();
    }

    public static void leak(Graphics2D graphics,double x,double y,double w,double h,boolean active,boolean warning,double time) {
        Graphics2D g=(Graphics2D)graphics.create();
        Color color=active?CYAN:warning?AMBER:new Color(76,135,165);
        Shape zone=new RoundRectangle2D.Double(x-w/2,y-h/2,w,h,7,7);
        g.setColor(new Color(7,15,30,140));g.fill(zone);
        g.setColor(alpha(color,active?43:warning?25:8));g.fill(zone);
        g.setStroke(new BasicStroke(1,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,10,new float[]{4,5},0));
        g.setColor(alpha(color,active?180:105));g.draw(zone);g.setStroke(new BasicStroke(1));
        for(int i=0;i<3;i++) {
            double yy=y-h*.28+i*h*.28;
            g.setColor(new Color(12,29,44));g.fill(new RoundRectangle2D.Double(x-w*.3,yy-3,w*.6,6,2,2));
            g.setColor(alpha(color,90));g.draw(new Line2D.Double(x-w*.24,yy,x-w*.12,yy));
        }
        if(active || warning)for(int arc=0;arc<3;arc++) {
            Path2D bolt=new Path2D.Double();
            double yy=y-h*.3+arc*h*.3, sx=x-w*.42;
            bolt.moveTo(sx,yy);
            for(int i=1;i<=7;i++)bolt.lineTo(sx+i*w*.84/7,yy+Math.sin(time*21+arc*2+i*1.9)*h*.16);
            g.setStroke(new BasicStroke(active?7:3,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.setColor(alpha(color,active?45:15));g.draw(bolt);
            g.setStroke(new BasicStroke(active?1.5f:1,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.setColor(alpha(color,active?245:105));g.draw(bolt);
            if(active&&arc==1) {
                Path2D branch=new Path2D.Double();branch.moveTo(x,yy);branch.lineTo(x+w*.1,yy-h*.2);branch.lineTo(x-w*.04,yy-h*.3);
                g.setColor(new Color(223,255,255));g.draw(branch);
            }
        }
        g.setStroke(new BasicStroke(1));g.setColor(alpha(color,180));
        Path2D symbol=new Path2D.Double();symbol.moveTo(x+3,y-9);symbol.lineTo(x-5,y+1);symbol.lineTo(x+1,y+1);symbol.lineTo(x-3,y+9);symbol.lineTo(x+6,y-2);symbol.lineTo(x,y-2);symbol.closePath();g.fill(symbol);
        g.dispose();
    }

    public static void saucer(Graphics2D graphics,double x,double y,boolean warning,boolean firing,double time) {
        Graphics2D g=(Graphics2D)graphics.create();
        g.setColor(new Color(0,0,0,100));g.fill(new Ellipse2D.Double(x-24,y+12,48,13));
        g.translate(x,y-9+Math.sin(time*3+x)*2);
        Color signal=firing?RED:warning?AMBER:new Color(157,144,255);
        glow(g,0,5,39,alpha(signal,35));
        g.setColor(new Color(104,164,224,55));g.fill(new Ellipse2D.Double(-17,-27,34,34));
        g.setColor(new Color(135,221,255,170));g.setStroke(new BasicStroke(1.3f));g.draw(new Arc2D.Double(-17,-27,34,34,0,180,Arc2D.OPEN));
        // The green pilot is deliberately visible through the dome at gameplay scale.
        g.setColor(new Color(76,156,104));g.fill(new Ellipse2D.Double(-9,-24,18,22));
        g.setColor(new Color(155,245,142));g.fill(new Ellipse2D.Double(-9,-25,18,18));
        g.setColor(new Color(9,32,44));
        g.fill(new Ellipse2D.Double(-6,-20,4,7));g.fill(new Ellipse2D.Double(2,-20,4,7));
        g.setColor(new Color(171,255,204));g.fillOval(-5,-20,1,2);g.fillOval(3,-20,1,2);
        g.setColor(new Color(23,58,58));g.drawLine(-2,-10,2,-10);
        g.setPaint(new GradientPaint(-25,-6,new Color(167,180,218),20,13,new Color(49,60,111)));
        g.fill(new Ellipse2D.Double(-29,-6,58,23));
        g.setColor(new Color(30,45,77));g.fill(new Ellipse2D.Double(-25,2,50,12));
        g.setColor(new Color(191,211,244));g.setStroke(new BasicStroke(1));g.draw(new Ellipse2D.Double(-29,-6,58,23));
        g.setColor(signal);g.draw(new Ellipse2D.Double(-31,-1,62,14));
        for(int i=0;i<5;i++) {
            double a=Math.PI*.12+i*Math.PI*.19;
            double lx=Math.cos(a)*24,ly=5+Math.sin(a)*6;
            g.setColor(i%2==0?new Color(154,247,227):signal);g.fill(new Ellipse2D.Double(lx-2,ly-2,4,4));
        }
        g.setColor(new Color(15,29,53));g.fillRoundRect(-7,10,14,9,3,3);
        g.setColor(signal);g.fillRoundRect(-4,14,8,5,2,2);
        if(firing)glow(g,0,18,24,alpha(RED,90));
        g.dispose();
    }

    public static void beam(Graphics2D graphics,double x1,double y1,double x2,double y2,boolean active,boolean warning,double time) {
        if(!active&&!warning)return;
        Graphics2D g=(Graphics2D)graphics.create();Line2D beam=new Line2D.Double(x1,y1,x2,y2);
        if(warning) {
            g.setStroke(new BasicStroke(1.6f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,10,new float[]{5,5},(float)(time*10%10)));
            g.setColor(alpha(AMBER,(int)(125+65*Math.sin(time*18))));g.draw(beam);
            g.setColor(alpha(AMBER,150));g.setStroke(new BasicStroke(1));g.draw(new Ellipse2D.Double(x2-9,y2-9,18,18));
        } else {
            g.setStroke(new BasicStroke(17,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.setColor(alpha(RED,28));g.draw(beam);
            g.setStroke(new BasicStroke(7,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.setColor(alpha(RED,100));g.draw(beam);
            g.setStroke(new BasicStroke(3,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.setColor(new Color(255,95,160));g.draw(beam);
            g.setStroke(new BasicStroke(1));g.setColor(new Color(255,235,245));g.draw(beam);
            glow(g,x2,y2,21,new Color(255,99,149,90));
        }
        g.dispose();
    }

    public static void robot(Graphics2D graphics,double x,double y,double heading,int health,int maxHealth,double hitFlash,double time) {
        Graphics2D g=(Graphics2D)graphics.create();
        if(health<=0) { wreck(g,x,y);g.dispose();return; }
        double step=Math.sin(time*11+x),lookX=Math.cos(heading)*2,lookY=Math.sin(heading)*.7;
        g.setColor(new Color(0,0,0,125));g.fill(new Ellipse2D.Double(x-16,y+8,32,11));
        glow(g,x,y-12,34,new Color(255,87,105,20));
        g.setColor(new Color(25,30,46));
        g.fill(new RoundRectangle2D.Double(x-12,y-1+step*2,10,14,3,3));g.fill(new RoundRectangle2D.Double(x+2,y-1-step*2,10,14,3,3));
        g.setColor(new Color(93,108,131));g.fill(new RoundRectangle2D.Double(x-14,y+6+step*2,13,7,2,2));g.fill(new RoundRectangle2D.Double(x+1,y+6-step*2,13,7,2,2));
        g.setPaint(new GradientPaint((float)x-12,(float)y-27,new Color(133,150,171),(float)x+12,(float)y+5,new Color(43,54,78)));
        g.fill(new RoundRectangle2D.Double(x-12,y-24,24,27,5,5));
        g.setColor(new Color(29,39,60));g.fill(new RoundRectangle2D.Double(x-8,y-16,16,13,3,3));
        g.setColor(RED);g.fill(new RoundRectangle2D.Double(x-4,y-13,8,7,2,2));
        for(int side:new int[]{-1,1}) {
            g.setColor(new Color(71,85,107));g.fill(new RoundRectangle2D.Double(x+side*14-4,y-23,9,20,3,3));
            g.setColor(new Color(159,96,110));g.fill(new RoundRectangle2D.Double(x+side*14-5,y-26,11,7,3,3));
            g.setColor(new Color(35,43,63));g.fill(new RoundRectangle2D.Double(x+side*14-4,y-6,9,9,2,2));
        }
        g.setPaint(new GradientPaint((float)x-11,(float)y-42,new Color(124,144,166),(float)x+9,(float)y-22,new Color(49,62,85)));
        g.fill(new RoundRectangle2D.Double(x-11,y-41,22,19,4,4));
        g.setColor(new Color(15,26,43));g.fill(new RoundRectangle2D.Double(x-9+lookX,y-36+lookY,18,7,2,2));
        g.setColor(RED);g.fill(new RoundRectangle2D.Double(x-7+lookX,y-34+lookY,14,3,1,1));
        g.setColor(new Color(255,227,219));g.fill(new Rectangle2D.Double(x-4+lookX,y-34+lookY,3,2));
        g.setColor(new Color(61,76,97));g.fill(new Rectangle2D.Double(x-5,y-42,10,3));
        g.setStroke(new BasicStroke(1));g.setColor(new Color(233,105,118,175));g.draw(new RoundRectangle2D.Double(x-11,y-41,22,19,4,4));
        if(hitFlash>0) {
            g.setColor(new Color(255,239,218,(int)Math.min(130,hitFlash*400)));g.fill(new RoundRectangle2D.Double(x-12,y-40,24,43,4,4));
            glow(g,x,y-13,35,new Color(255,205,139,40));
        }
        int width=32;
        g.setColor(new Color(4,12,24,210));g.fillRoundRect((int)x-width/2-2,(int)y-51,width+4,6,2,2);
        for(int i=0;i<maxHealth;i++) {
            g.setColor(i<health?RED:new Color(64,53,69));
            g.fillRect((int)x-width/2+i*(width/maxHealth)+1,(int)y-50,width/maxHealth-2,4);
        }
        g.dispose();
    }

    private static void wreck(Graphics2D g,double x,double y) {
        g.setColor(new Color(3,8,18,100));g.fill(new Ellipse2D.Double(x-19,y-6,38,19));
        g.setColor(new Color(55,65,81));g.fill(new RoundRectangle2D.Double(x-13,y-7,23,13,3,3));
        g.setColor(new Color(86,93,111));g.fill(new RoundRectangle2D.Double(x+7,y-12,12,10,3,3));
        g.setColor(new Color(43,51,69));g.fill(new Rectangle2D.Double(x-19,y+2,10,5));g.fill(new Rectangle2D.Double(x+6,y+7,10,5));
        g.setColor(new Color(109,75,87));g.fillRect((int)x+11,(int)y-8,5,2);
        g.setColor(new Color(99,192,185,140));g.draw(new Line2D.Double(x-5,y-1,x+1,y+3));g.draw(new Line2D.Double(x+1,y-1,x-5,y+3));
    }

    /** A compact pulse blaster, facing along angle; useful for pickup and equipped art. */
    public static void blaster(Graphics2D graphics,double x,double y,double angle,double scale,boolean flash,double time) {
        Graphics2D g=(Graphics2D)graphics.create();g.translate(x,y);g.rotate(angle);g.scale(scale,scale);
        g.setColor(new Color(15,30,47));g.fill(new RoundRectangle2D.Double(-5,-6,27,12,3,3));
        g.setPaint(new GradientPaint(-3,-6,new Color(185,213,230),16,5,new Color(61,92,116)));
        g.fill(new RoundRectangle2D.Double(-7,-7,24,11,3,3));
        g.setColor(new Color(48,67,90));g.fill(new RoundRectangle2D.Double(-4,2,7,10,2,2));
        g.setColor(new Color(16,40,54));g.fill(new RoundRectangle2D.Double(-1,-5,10,7,2,2));
        g.setColor(CYAN);g.fill(new RoundRectangle2D.Double(1,-3,7,3,1,1));
        g.setColor(new Color(220,242,248));g.fillRect(15,-4,7,6);
        g.setColor(new Color(26,67,83));g.fillRect(20,-3,4,4);
        g.setColor(CYAN);g.fillRect(23,-2,2,2);
        g.setColor(new Color(236,205,122));g.fillRect(-7,-3,3,3);
        if(flash) {
            glow(g,26,0,20,new Color(93,247,255,120));
            Path2D flare=new Path2D.Double();flare.moveTo(23,0);flare.lineTo(31,-5);flare.lineTo(29,-1);flare.lineTo(36,0);flare.lineTo(29,1);flare.lineTo(31,5);flare.closePath();
            g.setColor(new Color(218,255,255));g.fill(flare);
        }
        g.dispose();
    }

    public static void bolt(Graphics2D graphics,double x,double y,double dx,double dy,double time) {
        Graphics2D g=(Graphics2D)graphics.create();
        Line2D trail=new Line2D.Double(x-dx*10,y-dy*10,x+dx*3,y+dy*3);
        glow(g,x,y,15,new Color(67,240,255,65));
        g.setStroke(new BasicStroke(6,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.setColor(new Color(57,228,255,55));g.draw(trail);
        g.setStroke(new BasicStroke(2.3f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.setColor(CYAN);g.draw(trail);
        g.setColor(new Color(230,255,255));g.fill(new Ellipse2D.Double(x-2,y-2,4,4));g.dispose();
    }

    private static Color alpha(Color color,int opacity) {
        return new Color(color.getRed(),color.getGreen(),color.getBlue(),Math.max(0,Math.min(255,opacity)));
    }
    private static void glow(Graphics2D g,double x,double y,float r,Color color) {
        g.setPaint(new RadialGradientPaint(new Point2D.Double(x,y),r,new float[]{0,1},new Color[]{color,alpha(color,0)}));
        g.fill(new Ellipse2D.Double(x-r,y-r,r*2,r*2));
    }
}
