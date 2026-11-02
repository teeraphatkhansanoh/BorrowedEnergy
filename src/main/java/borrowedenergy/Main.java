package borrowedenergy;

import borrowedenergy.ui.GamePanel;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class Main {
    private Main() { }
    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("Borrowed Energy needs a graphical desktop. Run ./run.command on your Mac.");
            return;
        }
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) { /* The game paints its own interface. */ }
            JFrame window = new JFrame("Borrowed Energy");
            GamePanel game = new GamePanel();
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setContentPane(game);
            window.setMinimumSize(new Dimension(960, 650));
            window.pack();
            var screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
            if (window.getWidth() > screen.width || window.getHeight() > screen.height)
                window.setSize(Math.min(window.getWidth(),screen.width), Math.min(window.getHeight(),screen.height));
            window.setLocationRelativeTo(null);
            window.addWindowFocusListener(new java.awt.event.WindowAdapter() {
                @Override public void windowLostFocus(java.awt.event.WindowEvent event) { game.onFocusLost(); }
            });
            window.setVisible(true);
            game.requestFocusInWindow();
            game.start();
        });
    }
}
