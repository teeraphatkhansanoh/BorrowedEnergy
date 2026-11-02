package borrowedenergy;

import borrowedenergy.model.EnergyCore;
import borrowedenergy.model.GameModel;
import borrowedenergy.model.Entity;
import borrowedenergy.model.Relay;
import borrowedenergy.input.InputState;
import borrowedenergy.ui.GamePanel;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import javax.swing.Action;
import javax.swing.SwingUtilities;

/** Exercises the real Swing key bindings without a window or a running game timer. */
public final class UiInputTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(UiInputTest::run);
        System.out.println("PASS: " + checks + " UI input checks (headless)");
    }

    private static void run() {
        GamePanel panel = new GamePanel();
        screen(panel, "TITLE");
        press(panel, "ENTER");
        release(panel, "ENTER");
        screen(panel, "PLAYING");

        press(panel, "ESCAPE");
        screen(panel, "PAUSED");
        press(panel, "ESCAPE");
        screen(panel, "PAUSED");
        release(panel, "ESCAPE");
        press(panel, "ESCAPE");
        screen(panel, "PLAYING");
        press(panel, "ESCAPE");
        screen(panel, "PLAYING");
        release(panel, "ESCAPE");

        press(panel, "P");
        screen(panel, "PAUSED");
        press(panel, "P");
        screen(panel, "PAUSED");
        release(panel, "P");

        press(panel, "H");
        screen(panel, "HELP");
        press(panel, "H");
        screen(panel, "HELP");
        release(panel, "H");
        press(panel, "H");
        screen(panel, "PAUSED");
        press(panel, "H");
        screen(panel, "PAUSED");
        release(panel, "H");

        press(panel, "H");
        release(panel, "H");
        screen(panel, "HELP");
        press(panel, "ENTER");
        screen(panel, "PAUSED");
        press(panel, "ENTER");
        screen(panel, "PAUSED");
        release(panel, "ENTER");
        press(panel, "ENTER");
        release(panel, "ENTER");
        screen(panel, "PLAYING");

        press(panel, "H");
        release(panel, "H");
        press(panel, "SPACE");
        screen(panel, "PAUSED");
        press(panel, "SPACE");
        screen(panel, "PAUSED");
        release(panel, "SPACE");
        press(panel, "SPACE");
        release(panel, "SPACE");
        screen(panel, "PLAYING");

        GameModel model = (GameModel) field(panel, "model");
        model.player().setPosition(model.core().x(), model.core().y());
        press(panel, "E");
        check(model.core().state() == EnergyCore.State.CARRIED, "fresh E press acquires core");
        var socket = model.circuits().get(0).socket();
        model.player().setPosition(socket.x(), socket.y());
        press(panel, "E");
        check(!socket.hasCore(), "held E repeat does not insert at new object");
        release(panel, "E");
        press(panel, "E");
        check(socket.hasCore(), "fresh E press inserts core");
        release(panel, "E");

        press(panel, "ESCAPE");
        release(panel, "ESCAPE");
        screen(panel, "PAUSED");
        press(panel, "E");
        check(socket.hasCore(), "E while paused does not retrieve core");
        press(panel, "ESCAPE");
        release(panel, "ESCAPE");
        screen(panel, "PLAYING");
        press(panel, "E");
        check(socket.hasCore(), "E held across resume needs release before interaction");
        release(panel, "E");
        press(panel, "E");
        release(panel, "E");
        check(!socket.hasCore() && model.player().holdingCore(), "fresh E after resume retrieves core");

        press(panel, "W");
        panel.onFocusLost();
        screen(panel, "PAUSED");
        var input = (borrowedenergy.input.InputState) field(panel, "input");
        check(input.dx() == 0 && input.dy() == 0, "focus loss clears movement");

        press(panel, "ENTER");
        release(panel, "ENTER");
        press(panel, "R");
        release(panel, "R");
        screen(panel, "RESTART");
        press(panel, "ESCAPE");
        release(panel, "ESCAPE");
        screen(panel, "PLAYING");
        check(model.player().holdingCore(), "cancel restart preserves progress");
        press(panel, "R");
        release(panel, "R");
        press(panel, "ENTER");
        release(panel, "ENTER");
        screen(panel, "PLAYING");
        check(model.core().state() == EnergyCore.State.GROUND, "confirmed restart resets model");
        campaignNavigation();
        firingAndDeathScreen();
    }

    private static void campaignNavigation() {
        GamePanel panel = new GamePanel();
        press(panel, "ENTER"); release(panel, "ENTER");
        GameModel model = (GameModel) field(panel, "model");
        press(panel, "ENTER"); release(panel, "ENTER");
        check(model.stageIndex() == 0, "Enter during play never skips a level");
        finishStage(model);
        tick(panel);
        screen(panel, "STAGE_CLEAR");
        check(model.stageIndex() == 0, "cleared level waits on summary until player continues");
        press(panel, "ESCAPE"); release(panel, "ESCAPE");
        screen(panel, "STAGE_CLEAR");
        press(panel, "ENTER");
        screen(panel, "PLAYING");
        check(model.stageIndex() == 1 && !model.stageComplete(), "continue loads only the next full map");
        press(panel, "ENTER");
        check(model.stageIndex() == 1, "held continue key cannot skip another level");
        release(panel, "ENTER");

        use(model, model.core());
        press(panel, "R"); release(panel, "R");
        press(panel, "ESCAPE"); release(panel, "ESCAPE");
        check(model.stageIndex() == 1 && model.player().holdingCore(), "cancel level restart preserves later-level progress");
        press(panel, "R"); release(panel, "R");
        press(panel, "ENTER"); release(panel, "ENTER");
        screen(panel, "PLAYING");
        check(model.stageIndex() == 1 && model.core().state() == EnergyCore.State.GROUND,
                "confirmed restart keeps earlier levels completed");

        finishStage(model); tick(panel); screen(panel, "STAGE_CLEAR");
        press(panel, "SPACE"); release(panel, "SPACE");
        check(model.stageIndex() == 2, "Space starts the hard level after medium extraction");
        screen(panel, "PLAYING");
        finishStage(model); tick(panel); screen(panel, "WON");
        check(model.won(), "only final extraction opens campaign victory");
        double finishedTime=model.elapsedSeconds();
        for (String key : new String[]{"ENTER","SPACE","ESCAPE","R","H"}) {
            press(panel,key);release(panel,key);tick(panel);
            screen(panel,"WON");
            check(model.stageIndex()==2 && model.won(),"ending never loops to level one on "+key);
        }
        panel.setSize(1280,800);
        mouse(panel,MouseEvent.MOUSE_PRESSED,640,620,MouseEvent.BUTTON1);
        mouse(panel,MouseEvent.MOUSE_RELEASED,640,620,MouseEvent.BUTTON1);
        screen(panel,"WON");
        check(model.stageIndex()==2 && model.won(),"clicking outside Home does not restart completed campaign");
        check(model.elapsedSeconds()==finishedTime,"ending keeps final statistics frozen");
        mouse(panel,MouseEvent.MOUSE_PRESSED,640,577,MouseEvent.BUTTON1);
        mouse(panel,MouseEvent.MOUSE_RELEASED,640,577,MouseEvent.BUTTON1);
        screen(panel,"TITLE");
        check(model.stageIndex()==2 && model.won(),"Home returns to title without starting a new run");
        tick(panel);screen(panel,"TITLE");
        check(model.elapsedSeconds()==finishedTime,"Home keeps the completed run paused until Start");
        mouse(panel,MouseEvent.MOUSE_PRESSED,260,465,MouseEvent.BUTTON1);
        mouse(panel,MouseEvent.MOUSE_RELEASED,260,465,MouseEvent.BUTTON1);
        screen(panel,"PLAYING");
        check(model.stageIndex()==0 && !model.won() && model.lives()==5,"explicit Start from Home begins a fresh easy level");
    }

    private static void finishStage(GameModel model) {
        if (!model.player().holdingCore()) use(model, model.core());
        if (model.stageIndex() == 2) {
            use(model, model.gun());
            InputState fire = new InputState(); fire.setFiring(true);
            for (var robot : model.robots()) {
                model.player().setPosition(robot.x()-2, robot.y());
                for (int i=0; i<40 && robot.alive(); i++) {
                    model.setAim(robot.x(), robot.y()); model.update(.05, fire);
                }
                check(!robot.alive(), "UI scenario defeats pursuing robot with real shots");
            }
            model.clearAim();
        }
        for (var circuit : model.circuits()) {
            for (Relay relay : circuit.relays()) while (!relay.aligned()) relay.rotate();
            use(model, circuit.socket());
            var safe = model.checkpoints().get(0);
            model.player().setPosition(safe.x(), safe.y());
            for (int i = 0; i < 100; i++) model.update(.05, new InputState());
            check(circuit.complete(), "UI scenario circuit charges");
            if (circuit != model.circuits().get(model.circuits().size()-1)) use(model, circuit.socket());
        }
        check(model.core().state()==EnergyCore.State.SOCKETED,"UI regression leaves final battery connected before exit");
        model.player().setPosition(model.exitX(), model.exitY());
        model.update(.05, new InputState());
        check(model.stageComplete(), "UI scenario reaches valid extraction");
    }

    private static void firingAndDeathScreen() {
        GamePanel panel = new GamePanel(); panel.setSize(1280,800);
        press(panel,"ENTER"); release(panel,"ENTER");
        GameModel model = (GameModel)field(panel,"model");
        InputState input = (InputState)field(panel,"input");
        press(panel,"F");check(input.firing(),"F holds firing while playing");
        release(panel,"F");check(!input.firing(),"releasing F stops firing");
        press(panel,"F");press(panel,"ESCAPE");release(panel,"ESCAPE");
        check(!input.firing(),"pause clears held fire");
        release(panel,"F");press(panel,"ENTER");release(panel,"ENTER");

        mouse(panel,MouseEvent.MOUSE_PRESSED,600,400,MouseEvent.BUTTON1);
        check(input.firing(),"left mouse inside map holds firing");
        check(Math.abs(model.aimX()-(600-64)/32.0)<1e-8,"mouse aim converts screen to map coordinates");
        press(panel,"F");mouse(panel,MouseEvent.MOUSE_RELEASED,600,400,MouseEvent.BUTTON1);
        check(input.firing(),"releasing mouse preserves held F firing");
        release(panel,"F");check(!input.firing(),"both firing sources released stop shooting");
        mouse(panel,MouseEvent.MOUSE_PRESSED,600,400,MouseEvent.BUTTON1);
        panel.onFocusLost();screen(panel,"PAUSED");
        check(!input.firing(),"losing focus clears mouse fire");
        press(panel,"ENTER");release(panel,"ENTER");

        for(int i=0;i<5;i++) {
            model.player().setPosition(10.5,13.5); model.update(.05,new InputState());
        }
        check(model.stageFailed() && model.lives()==0,"five hits fail current stage");
        tick(panel);screen(panel,"GAME_OVER");
        press(panel,"F");check(!input.firing(),"death screen blocks shooting");release(panel,"F");
        press(panel,"ESCAPE");release(panel,"ESCAPE");screen(panel,"GAME_OVER");
        press(panel,"ENTER");screen(panel,"PLAYING");
        check(model.stageIndex()==0 && model.lives()==5 && !model.stageFailed(),"retry restores same level with five lives");
        press(panel,"ENTER");check(model.lives()==5,"held retry key cannot reset again");release(panel,"ENTER");

        finishStage(model);tick(panel);press(panel,"ENTER");release(panel,"ENTER");
        check(model.stageIndex()==1,"death retry navigation retains campaign progression");
        for(int i=0;i<5;i++) {
            model.player().setPosition(17.5,8.5);model.update(.05,new InputState());
        }
        tick(panel);screen(panel,"GAME_OVER");
        mouse(panel,MouseEvent.MOUSE_PRESSED,640,478,MouseEvent.BUTTON1);
        mouse(panel,MouseEvent.MOUSE_RELEASED,640,478,MouseEvent.BUTTON1);
        screen(panel,"PLAYING");
        check(model.stageIndex()==1 && model.lives()==5,"mouse retry restarts medium level instead of whole campaign");
    }

    private static void mouse(GamePanel panel,int event,int x,int y,int button) {
        panel.dispatchEvent(new MouseEvent(panel,event,System.currentTimeMillis(),0,x,y,1,false,button));
    }

    private static void use(GameModel model, Entity target) {
        model.player().setPosition(target.x(), target.y()); model.interact();
    }
    private static void tick(GamePanel panel) {
        try {
            var method = GamePanel.class.getDeclaredMethod("tick");
            method.setAccessible(true); method.invoke(panel);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    private static void press(GamePanel panel, String key) { action(panel, key + "-down"); }
    private static void release(GamePanel panel, String key) { action(panel, key + "-up"); }
    private static void action(GamePanel panel, String id) {
        Action action = panel.getActionMap().get(id);
        if (action == null) throw new AssertionError("Missing key action " + id);
        action.actionPerformed(new ActionEvent(panel, ActionEvent.ACTION_PERFORMED, id));
    }
    private static void screen(GamePanel panel, String expected) {
        check(field(panel, "screen").toString().equals(expected), "expected screen " + expected);
    }
    private static Object field(GamePanel panel, String name) {
        try {
            Field field = GamePanel.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(panel);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
