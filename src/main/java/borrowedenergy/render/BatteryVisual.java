package borrowedenergy.render;

import java.awt.*;
import java.awt.geom.*;

/** A reusable ion battery illustration, centered at the supplied position. */
public final class BatteryVisual {
    private BatteryVisual() { }
    public static void draw(Graphics2D graphics, double x, double y, double time, double scale) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.translate(x, y); g.scale(scale, scale);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color cyan = new Color(103, 255, 224);
        g.setPaint(new RadialGradientPaint(0, 0, 47, new float[]{0,1},
                new Color[]{new Color(62,255,220,68), new Color(62,255,220,0)}));
        g.fillOval(-47,-47,94,94);
        g.setColor(new Color(7,18,27,180));g.fillRoundRect(-13,-18,26,39,7,7);
        Path2D shell = new Path2D.Double();
        shell.moveTo(-9,-20);shell.lineTo(9,-20);shell.lineTo(14,-14);shell.lineTo(14,14);
        shell.lineTo(8,21);shell.lineTo(-8,21);shell.lineTo(-14,14);shell.lineTo(-14,-14);shell.closePath();
        g.setPaint(new GradientPaint(-14,0,new Color(198,221,227),14,0,new Color(63,94,110)));
        g.fill(shell);g.setStroke(new BasicStroke(1.3f));g.setColor(new Color(13,29,43));g.draw(shell);
        g.setColor(new Color(18,45,54));g.fillRoundRect(-8,-15,16,31,5,5);
        g.setColor(new Color(43,101,105));g.fillRoundRect(-6,-13,12,26,3,3);
        for (int i=0;i<4;i++) {
            float pulse=(float)(.75+.25*Math.sin(time*2.4-i*.6));
            g.setColor(new Color(.39f,1f,.83f,pulse));g.fillRoundRect(-5,8-i*6,10,4,2,2);
            g.setColor(new Color(222,255,246,160));g.drawLine(-4,8-i*6,3,8-i*6);
        }
        g.setColor(new Color(220,241,238));g.fillRoundRect(-6,-23,12,5,2,2);
        g.setColor(new Color(255,213,108));g.fillRect(-4,-24,3,3);g.fillRect(1,-24,3,3);
        g.setColor(new Color(22,46,58));g.fillRoundRect(-12,-11,3,22,2,2);g.fillRoundRect(9,-11,3,22,2,2);
        g.setColor(cyan);g.fillRect(-12,-7,2,8);g.fillRect(10,2,2,8);
        g.setColor(new Color(226,246,240));g.fillOval(-9,16,2,2);g.fillOval(7,16,2,2);
        g.setStroke(new BasicStroke(.8f));g.setColor(new Color(206,255,243,120));g.drawRoundRect(-16,-25,32,49,8,8);
        g.dispose();
    }
}
