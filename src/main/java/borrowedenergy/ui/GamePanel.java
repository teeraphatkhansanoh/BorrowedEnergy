package borrowedenergy.ui;

import borrowedenergy.input.InputState;
import borrowedenergy.model.EnergyCore;
import borrowedenergy.model.GameModel;
import borrowedenergy.model.Interactable;
import borrowedenergy.render.BatteryVisual;
import borrowedenergy.render.RobotSprite;
import borrowedenergy.render.WorldRenderer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.HashSet;
import java.util.Set;
import javax.swing.*;

/** Swing screens and input. Campaign rules and movement stay in GameModel. */
@SuppressWarnings("serial")
public final class GamePanel extends JPanel {
    public static final int WIDTH = 1280, HEIGHT = 800;
    private enum Screen { TITLE, PLAYING, PAUSED, HELP, RESTART, STAGE_CLEAR, WON, GAME_OVER }
    private static final Color INK = new Color(6, 11, 24);
    private static final Color CYAN = new Color(83, 236, 247);
    private static final Color MINT = new Color(119, 251, 189);
    private static final Color VIOLET = new Color(174, 139, 255);
    private static final Color TEXT = new Color(230, 243, 251);
    private static final Color MUTED = new Color(142, 166, 187);
    private static final Color AMBER = new Color(255, 197, 100);
    private static final Color RED = new Color(255, 104, 131);
    private static final String[] NAMES = {"ORBITAL GARDEN", "NEON TRANSIT", "SINGULARITY VAULT"};
    private final GameModel model = new GameModel();
    private final InputState input = new InputState();
    private final WorldRenderer renderer = new WorldRenderer();
    private final Set<String> movementHeld = new HashSet<>();
    private final Set<String> actionHeld = new HashSet<>();
    private Screen screen = Screen.TITLE;
    private Screen helpReturn = Screen.TITLE;
    private final Timer timer;
    private long previous;
    private double time, screenTime;
    private double mouseX = -1, mouseY = -1;
    private boolean fireMouseHeld, fireKeyHeld;

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(INK); setFocusable(true); setDoubleBuffered(true);
        bindMovement("W", "UP", InputState.Action.UP);
        bindMovement("S", "DOWN", InputState.Action.DOWN);
        bindMovement("A", "LEFT", InputState.Action.LEFT);
        bindMovement("D", "RIGHT", InputState.Action.RIGHT);
        bindKey("E", () -> { if (screen == Screen.PLAYING) model.interact(); });
        bindKey("ESCAPE", this::escape); bindKey("P", this::escape);
        bindKey("ENTER", this::enter); bindKey("SPACE", this::enter);
        bindKey("H", this::help);
        bindKey("R", () -> { if (screen == Screen.PLAYING || screen == Screen.PAUSED) setScreen(Screen.RESTART); });
        bind("F", false, () -> { fireKeyHeld = true; syncFiring(); });
        bind("F", true, () -> { fireKeyHeld = false; syncFiring(); });
        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) { updateMouse(e); }
            @Override public void mouseDragged(MouseEvent e) { updateMouse(e); }
            @Override public void mouseExited(MouseEvent e) {
                mouseX = mouseY = -1; fireMouseHeld = false; syncFiring(); model.clearAim();
                setCursor(Cursor.getDefaultCursor());
            }
            @Override public void mousePressed(MouseEvent e) {
                updateMouse(e);
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (screen == Screen.PLAYING && overWorld()) { fireMouseHeld = true; syncFiring(); }
                    else click();
                }
                requestFocusInWindow();
            }
            @Override public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) { fireMouseHeld = false; syncFiring(); }
            }
        };
        addMouseListener(mouse); addMouseMotionListener(mouse);
        timer = new Timer(16, event -> tick()); timer.setCoalesce(true);
    }
    public void start() { previous = System.nanoTime(); timer.start(); }
    public void onFocusLost() {
        if (screen == Screen.PLAYING) setScreen(Screen.PAUSED);
        clearMovement(); actionHeld.clear();
    }
    private void tick() {
        long now = System.nanoTime();
        double dt = Math.min(.05, (now - previous) / 1_000_000_000.0); previous = now;
        time += dt; screenTime += dt;
        if (screen == Screen.PLAYING) {
            model.update(dt, input);
            if (model.stageFailed()) setScreen(Screen.GAME_OVER);
            else if (model.stageComplete()) setScreen(model.won() ? Screen.WON : Screen.STAGE_CLEAR);
        }
        repaint();
    }
    private void bindMovement(String letter, String arrow, InputState.Action action) {
        for (String key : new String[]{letter, arrow}) {
            bind(key, false, () -> { movementHeld.add(key); syncMovement(action, letter, arrow); });
            bind(key, true, () -> { movementHeld.remove(key); syncMovement(action, letter, arrow); });
        }
    }
    private void syncMovement(InputState.Action action, String letter, String arrow) {
        input.set(action, screen == Screen.PLAYING && (movementHeld.contains(letter) || movementHeld.contains(arrow)));
    }
    private void bindKey(String key, Runnable action) {
        bind(key, false, () -> { if (actionHeld.add(key)) action.run(); });
        bind(key, true, () -> actionHeld.remove(key));
    }
    private void bind(String key, boolean release, Runnable action) {
        String id = key + (release ? "-up" : "-down");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke((release ? "released " : "pressed ") + key), id);
        getActionMap().put(id, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { action.run(); }
        });
    }
    private void syncFiring() { input.setFiring(screen == Screen.PLAYING && (fireKeyHeld || fireMouseHeld)); }
    private void clearMovement() {
        movementHeld.clear(); input.clear(); fireMouseHeld = fireKeyHeld = false; model.clearAim();
    }
    private void setScreen(Screen next) {
        screen = next; screenTime = 0; clearMovement();
        setCursor(Cursor.getPredefinedCursor(isButtonHover() ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }
    private void begin() { model.reset(); setScreen(Screen.PLAYING); }
    private void restartStage() { model.restartStage(); setScreen(Screen.PLAYING); }
    private void nextStage() { if (model.nextStage()) setScreen(Screen.PLAYING); }
    private void escape() {
        switch (screen) {
            case PLAYING -> setScreen(Screen.PAUSED);
            case PAUSED, RESTART -> setScreen(Screen.PLAYING);
            case HELP -> setScreen(helpReturn);
            default -> { }
        }
    }
    private void enter() {
        switch (screen) {
            case TITLE -> begin();
            case RESTART, GAME_OVER -> restartStage();
            case STAGE_CLEAR -> nextStage();
            case PAUSED -> setScreen(Screen.PLAYING);
            case HELP -> setScreen(helpReturn);
            default -> { }
        }
    }
    private void help() {
        if (screen == Screen.HELP) setScreen(helpReturn);
        else if (screen == Screen.TITLE || screen == Screen.PAUSED || screen == Screen.PLAYING) {
            helpReturn = screen == Screen.PLAYING ? Screen.PAUSED : screen; setScreen(Screen.HELP);
        }
    }
    private void updateMouse(MouseEvent e) {
        double scale = Math.min(getWidth() / (double) WIDTH, getHeight() / (double) HEIGHT);
        mouseX = (e.getX() - (getWidth() - WIDTH * scale) / 2) / scale;
        mouseY = (e.getY() - (getHeight() - HEIGHT * scale) / 2) / scale;
        if (screen == Screen.PLAYING && overWorld()) {
            model.setAim((mouseX-WorldRenderer.MAP_X)/WorldRenderer.TILE, (mouseY-WorldRenderer.MAP_Y)/WorldRenderer.TILE);
        } else model.clearAim();
        int cursor = isButtonHover() ? Cursor.HAND_CURSOR :
                screen == Screen.PLAYING && model.player().hasGun() && overWorld() ? Cursor.CROSSHAIR_CURSOR : Cursor.DEFAULT_CURSOR;
        setCursor(Cursor.getPredefinedCursor(cursor));
    }
    private boolean overWorld() { return inside(WorldRenderer.MAP_X,WorldRenderer.MAP_Y,36*WorldRenderer.TILE,20*WorldRenderer.TILE); }
    private boolean inside(int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x+w && mouseY >= y && mouseY <= y+h;
    }
    private boolean isButtonHover() {
        return switch (screen) {
            case TITLE -> inside(90, 438, 340, 57) || inside(90, 509, 340, 47);
            case PAUSED -> inside(455, 391, 370, 54) || inside(455, 459, 370, 46) || inside(455, 519, 370, 46);
            case HELP -> inside(455, 664, 370, 48);
            case RESTART -> inside(455, 441, 370, 54) || inside(455, 509, 370, 46);
            case STAGE_CLEAR -> inside(455, 541, 370, 54);
            case WON -> inside(455,548,370,58);
            case GAME_OVER -> inside(455,452,370,54);
            case PLAYING -> inside(1200, 22, 38, 38);
        };
    }
    private void click() {
        switch (screen) {
            case TITLE -> { if (inside(90,438,340,57)) begin(); else if (inside(90,509,340,47)) help(); }
            case PLAYING -> { if (inside(1200,22,38,38)) setScreen(Screen.PAUSED); }
            case PAUSED -> {
                if (inside(455,391,370,54)) setScreen(Screen.PLAYING);
                else if (inside(455,459,370,46)) help();
                else if (inside(455,519,370,46)) setScreen(Screen.RESTART);
            }
            case HELP -> { if (inside(455,664,370,48)) setScreen(helpReturn); }
            case RESTART -> {
                if (inside(455,441,370,54)) restartStage();
                else if (inside(455,509,370,46)) setScreen(Screen.PLAYING);
            }
            case STAGE_CLEAR -> { if (inside(455,541,370,54)) nextStage(); }
            case WON -> { if (inside(455,548,370,58)) setScreen(Screen.TITLE); }
            case GAME_OVER -> { if (inside(455,452,370,54)) restartStage(); }
        }
    }
    @Override protected void paintComponent(Graphics raw) {
        super.paintComponent(raw);
        Graphics2D g = (Graphics2D) raw.create();
        double scale = Math.min(getWidth()/(double)WIDTH, getHeight()/(double)HEIGHT);
        g.translate((getWidth()-WIDTH*scale)/2, (getHeight()-HEIGHT*scale)/2); g.scale(scale,scale);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(INK); g.fillRect(0,0,WIDTH,HEIGHT);
        renderer.render(g,model,time);
        hud(g);
        switch (screen) {
            case TITLE -> title(g);
            case HELP -> helpScreen(g);
            case PAUSED -> pause(g);
            case RESTART -> restart(g);
            case STAGE_CLEAR -> stageClear(g);
            case WON -> victory(g);
            case GAME_OVER -> gameOver(g);
            default -> { }
        }
        if (screen == Screen.PLAYING && screenTime < .6) {
            g.setColor(new Color(6,11,24,(int)(150*(1-screenTime/.6)))); g.fillRect(0,0,WIDTH,HEIGHT);
        }
        g.dispose();
    }
    private Color accent() { return model.stageIndex()==0 ? CYAN : model.stageIndex()==1 ? VIOLET : AMBER; }
    private void hud(Graphics2D g) {
        Color accent = accent();
        g.setPaint(new GradientPaint(0,0,new Color(11,23,42),0,86,new Color(7,14,29)));
        g.fillRect(0,0,WIDTH,86); line(g,40,85,1240,85,new Color(40,66,91));
        BatteryVisual.draw(g,60,43,time,.65);
        text(g,"BORROWED ENERGY",86,37,15,Font.BOLD,TEXT);
        text(g,"HELIOS STATION / YEAR 2186",86,56,9,Font.PLAIN,MUTED);
        line(g,311,20,311,66,new Color(42,65,87));
        text(g,"LEVEL 0"+model.stageNumber()+" / "+model.difficulty(),335,27,10,Font.BOLD,accent);
        text(g,model.stageName(),335,48,18,Font.BOLD,TEXT);
        fit(g,model.objective(),335,68,13,475,MUTED);
        text(g,"LIVES  "+model.lives()+" / "+model.maxLives(),845,17,9,Font.BOLD,model.lives()<=2?RED:MUTED);
        for (int i=0; i<model.maxLives(); i++) heart(g,845+i*24,24,i<model.lives());
        text(g,"CIRCUITS",845,56,8,Font.BOLD,MUTED);
        for (int i=0; i<model.circuits().size(); i++) {
            var circuit = model.circuits().get(i);
            int x = 845+i*54;
            g.setColor(new Color(28,43,64)); g.fillRoundRect(x,63,44,5,2,2);
            g.setColor(circuit.complete()?MINT:accent);
            g.fillRoundRect(x,63,(int)(44*circuit.progress()),5,2,2);
        }
        String battery = model.core().state() == EnergyCore.State.CARRIED ? "CARRYING" :
                model.core().state() == EnergyCore.State.SOCKETED ? "CONNECTED" : "LOCATE CELL";
        text(g,"BATTERY",1030,29,9,Font.BOLD,MUTED);
        text(g,battery,1030,48,11,Font.BOLD,model.player().holdingCore()?MINT:accent);
        text(g,model.stageIndex()==2 ? (model.player().hasGun()?"BLASTER / "+model.robotsRemaining()+" HOSTILES":"FIND THE BLASTER") :
                "DATA "+model.chipsCollected()+"/"+model.totalChips(),1030,65,9,Font.BOLD,model.player().hasGun()?MINT:MUTED);
        g.setColor(inside(1200,22,38,38)?new Color(42,79,96):new Color(24,43,64));
        g.fill(chamfer(1200,22,38,38,6));
        g.setColor(TEXT); g.fillRect(1213,34,3,13); g.fillRect(1221,34,3,13);
        g.setColor(new Color(7,15,29)); g.fillRect(0,744,WIDTH,56);
        line(g,40,744,1240,744,new Color(40,66,91));
        key(g,"WASD / ARROWS",44,760,101); text(g,"MOVE",155,778,10,Font.BOLD,MUTED);
        key(g,"E",211,760,27); text(g,"USE",248,778,10,Font.BOLD,MUTED);
        key(g,"ESC",297,760,39); text(g,"PAUSE",346,778,10,Font.BOLD,MUTED);
        Interactable target = model.interactionTarget();
        String hint=target!=null ? "[E] "+target.hint() : model.player().hasGun() ? "F / LEFT CLICK  SHOOT   ·   MOUSE  AIM" : "H  FIELD MANUAL  /  R  RESTART LEVEL";
        fit(g,hint,437,778,11,540,target!=null?CYAN:MUTED);
        text(g,"RETRIES "+model.deaths(),1000,778,10,Font.PLAIN,MUTED);
        text(g,clock(model.stageTime()),1160,779,13,Font.BOLD,TEXT);
        if (screen==Screen.PLAYING && !model.message().isEmpty()) {
            g.setFont(font(12,Font.PLAIN));
            int w = Math.min(960,g.getFontMetrics().stringWidth(model.message())+50);
            int x=(WIDTH-w)/2;
            glass(g,x,698,w,30,8);fit(g,model.message(),x+22,718,12,w-44,TEXT);
        }
    }
    private void title(Graphics2D g) {
        g.setPaint(new GradientPaint(0,0,new Color(4,10,25,251),1280,0,new Color(8,16,35,242)));
        g.fillRect(0,0,WIDTH,HEIGHT);
        starfield(g);
        glow(g,968,328,366,new Color(55,124,242,30));
        glow(g,982,395,215,new Color(83,236,247,22));
        Graphics2D orbit=(Graphics2D)g.create(); orbit.translate(937,397);
        orbit.setStroke(new BasicStroke(1));
        for(int i=0;i<3;i++) {
            orbit.setColor(new Color(67,154,188,55-i*12));
            orbit.draw(new Ellipse2D.Double(-238-i*28,-188-i*21,476+i*56,376+i*42));
        }
        orbit.setColor(new Color(93,236,247,120));
        orbit.drawArc(-248,-248,496,496,(int)(time*4),72); orbit.dispose();
        text(g,"HELIOS / ORBITAL RESEARCH STATION",90,72,11,Font.BOLD,CYAN);
        text(g,"EMERGENCY TRANSMISSION  //  2186",90,141,10,Font.PLAIN,MUTED);
        text(g,"Borrowed",85,234,78,Font.BOLD,TEXT);
        text(g,"Energy",85,318,78,Font.BOLD,CYAN);
        line(g,90,347,144,347,CYAN);
        text(g,"One battery. Three worlds on the edge of darkness.",90,386,16,Font.PLAIN,MUTED);
        text(g,"Find your path. Outsmart the station. Restore the future.",90,412,13,Font.PLAIN,MUTED);
        button(g,"START EXPEDITION",90,438,340,57,true,"ENTER");
        button(g,"FIELD MANUAL",90,509,340,47,false,"H");
        heroRobot(g,938,383);
        glass(g,824,566,226,40,8);
        centered(g,"B-01 / EXPLORER UNIT",937,591,11,Font.BOLD,TEXT);
        text(g,"MISSION SEQUENCE",90,610,10,Font.BOLD,MUTED);
        String[] difficulties={"EASY","MEDIUM","HARD"};
        Color[] colors={CYAN,VIOLET,AMBER};
        for(int i=0;i<3;i++) {
            int x=90+i*208;
            glass(g,x,628,194,73,9);
            text(g,"0"+(i+1)+"  "+difficulties[i],x+15,651,10,Font.BOLD,colors[i]);
            text(g,NAMES[i],x+15,677,11,Font.BOLD,TEXT);
        }
        text(g,"A SIGNAL REMAINS. FOLLOW IT.",90,753,10,Font.BOLD,MUTED);
        BatteryVisual.draw(g,1128,677,time,2.2);
        text(g,"ION CELL / 01",1078,753,10,Font.BOLD,CYAN);
    }
    private void starfield(Graphics2D g) {
        for (int i=0;i<100;i++) {
            int x=Math.floorMod(i*277+41,1280),y=Math.floorMod(i*193+67,800);
            int a=(int)(18+24*(.5+.5*Math.sin(time*.6+i)));
            g.setColor(new Color(146,190,255,a));g.fillRect(x,y,i%17==0?2:1,i%17==0?2:1);
        }
        g.setColor(new Color(59,95,145,16));
        for(int y=120;y<800;y+=40)g.drawLine(0,y,1280,y);
    }
    private void heroRobot(Graphics2D g,int x,int y) {
        glow(g,x,y+15,190,new Color(255,205,64,18));
        g.setColor(new Color(0,0,0,115));g.fillOval(x-95,y+137,190,25);
        RobotSprite.draw(g,x,y+148+Math.sin(time*1.8)*3,310,false);
        g.setColor(new Color(83,236,247,115));g.setStroke(new BasicStroke(1));
        g.drawOval(x-112,y+131,224,35);g.drawOval(x-98,y+135,196,27);
        text(g,"UNIT SCAN: ONLINE",x-102,y-184,10,Font.PLAIN,CYAN);
    }
    private void veil(Graphics2D g) {
        g.setColor(new Color(3,8,23,220));g.fillRect(0,0,WIDTH,HEIGHT);
        glow(g,640,350,430,new Color(35,108,188,18));
    }
    private void glass(Graphics2D g,int x,int y,int w,int h,int corner) {
        Shape shape=chamfer(x,y,w,h,corner);
        g.setPaint(new GradientPaint(x,y,new Color(19,37,62,245),x,y+h,new Color(9,22,43,245)));
        g.fill(shape);g.setColor(new Color(53,87,120,190));g.setStroke(new BasicStroke(1));g.draw(shape);
    }
    private void card(Graphics2D g,int x,int y,int w,int h) {
        glass(g,x,y,w,h,16);
        line(g,x+25,y+1,x+w/2,y+1,CYAN);
        line(g,x+w-80,y+h-1,x+w-22,y+h-1,VIOLET);
    }
    private void pause(Graphics2D g) {
        veil(g);card(g,395,210,490,408);
        centered(g,"NEURAL LINK / STANDBY",640,255,10,Font.BOLD,CYAN);
        centered(g,"Expedition paused.",640,307,33,Font.BOLD,TEXT);
        centered(g,"LEVEL 0"+model.stageNumber()+" / "+model.stageName(),640,342,12,Font.PLAIN,MUTED);
        button(g,"RESUME",455,391,370,54,true,"ENTER");
        button(g,"FIELD MANUAL",455,459,370,46,false,"H");
        button(g,"RESTART THIS LEVEL",455,519,370,46,false,"R");
    }
    private void restart(Graphics2D g) {
        veil(g);card(g,395,255,490,336);
        centered(g,"RESET LEVEL 0"+model.stageNumber()+"?",640,303,10,Font.BOLD,AMBER);
        centered(g,"Try a different approach.",640,353,28,Font.BOLD,TEXT);
        centered(g,"This level resets. Earlier levels stay completed.",640,394,13,Font.PLAIN,MUTED);
        button(g,"RESTART LEVEL",455,441,370,54,true,"ENTER");
        button(g,"KEEP EXPLORING",455,509,370,46,false,"ESC");
    }
    private void helpScreen(Graphics2D g) {
        veil(g);card(g,260,67,760,670);
        centered(g,"B-01 / FIELD MANUAL",640,108,10,Font.BOLD,CYAN);
        centered(g,"Borrow power. Create a way forward.",640,151,29,Font.BOLD,TEXT);
        helpRow(g,1,193,"FIND THE ION BATTERY","Move with WASD or arrows. Press E near the glowing battery to collect it.");
        helpRow(g,2,274,"CONNECT THE NETWORK","Turn relay arrows to match the small markers. Insert the battery at a socket.");
        helpRow(g,3,355,"REUSE EVERY SPARK","Wait for a circuit to charge. Gates stay open. Retrieve your battery with E.");
        helpRow(g,4,436,"FIVE LIVES PER LEVEL","Dodge demon insects, electrical leaks and alien beams. Each hit costs one life.");
        helpRow(g,5,517,"ARM UP, THEN EXTRACT","In level 3, pick up the blaster with E. Aim with mouse; hold F or left click to fire.");
        fit(g,"Power every circuit, then enter EXIT. Your battery is recovered automatically.",344,601,12,628,MUTED);
        fit(g,"Destroy every pursuing robot in level 3. Zero lives means retrying the current level.",344,627,12,628,MUTED);
        button(g,"LINK READY",455,664,370,48,true,"ENTER");
    }
    private void helpRow(Graphics2D g,int n,int y,String heading,String body) {
        g.setColor(new Color(27,65,87));g.fill(chamfer(301,y-8,28,28,5));
        text(g,"0"+n,307,y+11,11,Font.BOLD,CYAN);
        text(g,heading,349,y+7,12,Font.BOLD,TEXT);
        fit(g,body,349,y+32,12,628,MUTED);
    }
    private void stageClear(Graphics2D g) {
        veil(g);card(g,370,161,540,477);
        BatteryVisual.draw(g,640,224,time,1.3);
        centered(g,"LEVEL 0"+model.stageNumber()+" / EXTRACTION COMPLETE",640,281,10,Font.BOLD,MINT);
        centered(g,model.stageName(),640,330,31,Font.BOLD,TEXT);
        centered(g,"A little more of the station is alive.",640,366,14,Font.PLAIN,MUTED);
        line(g,435,397,845,397,new Color(51,79,106));
        centered(g,"TIME  "+clock(model.stageTime())+"    LIVES  "+model.lives()+"/5    DATA  "+model.chipsCollected()+"/"+model.totalChips(),640,427,12,Font.BOLD,CYAN);
        centered(g,"NEXT / "+NAMES[Math.min(2,model.stageIndex()+1)],640,473,12,Font.BOLD,TEXT);
        centered(g,model.stageIndex()==0?"Electrical leaks and alien saucers await.":"Find a blaster. Destroy the pursuing robots.",640,500,12,Font.PLAIN,MUTED);
        button(g,"CONTINUE TO LEVEL 0"+(model.stageNumber()+1),455,541,370,54,true,"ENTER");
    }
    private void victory(Graphics2D g) {
        veil(g);card(g,370,139,540,555);
        BatteryVisual.draw(g,640,209,time,1.5);
        centered(g,"ALL THREE MISSIONS COMPLETE",640,271,10,Font.BOLD,MINT);
        centered(g,"GAME",640,329,40,Font.BOLD,TEXT);
        centered(g,"COMPLETE",640,375,40,Font.BOLD,CYAN);
        centered(g,"ORBITAL GARDEN  /  NEON TRANSIT  /  SINGULARITY VAULT",640,417,10,Font.PLAIN,MUTED);
        line(g,435,443,845,443,new Color(51,79,106));
        centered(g,"TIME  "+clock(model.elapsedSeconds())+"    RETRIES  "+model.deaths()+"    DATA  "+model.totalChipsCollected()+"/"+model.campaignTotalChips(),640,478,12,Font.BOLD,TEXT);
        centered(g,"One explorer. One battery. An entire station restored.",640,507,12,Font.PLAIN,MUTED);
        button(g,"RETURN HOME",455,548,370,58,true,"");
        centered(g,"Thank you for playing. Return Home whenever you are ready.",640,642,12,Font.PLAIN,MUTED);
    }
    private void gameOver(Graphics2D g) {
        veil(g);card(g,385,235,510,365);
        centered(g,"LIFE SUPPORT / OFFLINE",640,282,10,Font.BOLD,RED);
        centered(g,"No lives left.",640,337,38,Font.BOLD,TEXT);
        centered(g,"LEVEL 0"+model.stageNumber()+" / "+model.stageName(),640,376,12,Font.BOLD,AMBER);
        centered(g,"Restart this level with 5 lives and a fresh route.",640,413,13,Font.PLAIN,MUTED);
        button(g,"RETRY LEVEL 0"+model.stageNumber(),455,452,370,54,true,"ENTER");
        centered(g,"Your previously completed levels stay unlocked.",640,548,12,Font.PLAIN,MUTED);
    }
    private static void heart(Graphics2D g,int x,int y,boolean filled) {
        Path2D p=new Path2D.Double();
        p.moveTo(x+9,y+4);p.curveTo(x+3,y-3,x-4,y+4,x+1,y+10);
        p.lineTo(x+9,y+18);p.lineTo(x+17,y+10);p.curveTo(x+23,y+3,x+15,y-3,x+9,y+4);p.closePath();
        g.setColor(filled?new Color(255,111,142):new Color(33,48,67));g.fill(p);
        g.setStroke(new BasicStroke(1));g.setColor(filled?new Color(255,173,187):new Color(62,76,95));g.draw(p);
    }
    private void button(Graphics2D g,String label,int x,int y,int w,int h,boolean primary,String shortcut) {
        boolean hover=inside(x,y,w,h);
        Shape shape=chamfer(x,y,w,h,9);
        if(primary) {
            g.setPaint(new GradientPaint(x,y,hover?new Color(161,255,249):CYAN,x+w,y,hover?new Color(127,225,255):new Color(94,185,255)));
        } else g.setColor(hover?new Color(30,61,88):new Color(16,34,57));
        g.fill(shape);
        g.setStroke(new BasicStroke(1));g.setColor(primary?new Color(172,255,255,180):new Color(60,94,124));g.draw(shape);
        text(g,label,x+21,y+h/2+5,12,Font.BOLD,primary?new Color(5,30,49):TEXT);
        if(!shortcut.isEmpty()) {
            g.setFont(font(9,Font.BOLD));int sw=g.getFontMetrics().stringWidth(shortcut);
            text(g,shortcut,x+w-21-sw,y+h/2+4,9,Font.BOLD,primary?new Color(22,76,103):MUTED);
        }
    }
    private static Shape chamfer(int x,int y,int w,int h,int c) {
        Path2D p=new Path2D.Double();p.moveTo(x+c,y);p.lineTo(x+w,y);p.lineTo(x+w,y+h-c);
        p.lineTo(x+w-c,y+h);p.lineTo(x,y+h);p.lineTo(x,y+c);p.closePath();return p;
    }
    private static void key(Graphics2D g,String label,int x,int y,int w) {
        g.setColor(new Color(22,40,62));g.fillRoundRect(x,y,w,25,4,4);
        g.setColor(new Color(55,83,111));g.setStroke(new BasicStroke(1));g.drawRoundRect(x,y,w,25,4,4);
        g.setFont(font(9,Font.BOLD));g.setColor(TEXT);g.drawString(label,x+(w-g.getFontMetrics().stringWidth(label))/2,y+16);
    }
    private static Font font(int size,int style) { return new Font(Font.SANS_SERIF,style,size); }
    private static void text(Graphics2D g,String value,int x,int y,int size,int style,Color color) {
        g.setFont(font(size,style));g.setColor(color);g.drawString(value,x,y);
    }
    private static void fit(Graphics2D g,String value,int x,int y,int size,int width,Color color) {
        while(size>9 && g.getFontMetrics(font(size,Font.PLAIN)).stringWidth(value)>width)size--;
        g.setFont(font(size,Font.PLAIN));g.setColor(color);
        if(g.getFontMetrics().stringWidth(value)>width) {
            while(value.length()>1 && g.getFontMetrics().stringWidth(value+"...")>width)value=value.substring(0,value.length()-1);
            value+="...";
        }
        g.drawString(value,x,y);
    }
    private static void centered(Graphics2D g,String value,int x,int y,int size,int style,Color color) {
        g.setFont(font(size,style));g.setColor(color);g.drawString(value,x-g.getFontMetrics().stringWidth(value)/2,y);
    }
    private static void line(Graphics2D g,int x1,int y1,int x2,int y2,Color color) {
        g.setColor(color);g.setStroke(new BasicStroke(1));g.drawLine(x1,y1,x2,y2);
    }
    private static void glow(Graphics2D g,float x,float y,float r,Color color) {
        g.setPaint(new RadialGradientPaint(x,y,r,new float[]{0,1},new Color[]{color,new Color(color.getRed(),color.getGreen(),color.getBlue(),0)}));
        g.fill(new Ellipse2D.Float(x-r,y-r,r*2,r*2));
    }
    private static String clock(double seconds) { return String.format("%02d:%02d",(int)seconds/60,(int)seconds%60); }
}
