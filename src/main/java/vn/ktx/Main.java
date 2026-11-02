package vn.ktx;

import javax.swing.SwingUtilities;

import vn.ktx.ui.MainFrame;
import vn.ktx.ui.LoginDialog;
import vn.ktx.ui.ExtensionPanel;
import vn.ktx.ui.UiTheme;
import vn.ktx.model.UserSession;
import javax.swing.JFrame;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            UiTheme.install();
            openSession();
        });
    }

    public static void logout(java.awt.Component component) {
        java.awt.Window window = SwingUtilities.getWindowAncestor(component);
        if (window == null && component instanceof java.awt.Window w) window = w;
        if (window != null) window.dispose();
        SwingUtilities.invokeLater(Main::openSession);
    }

    private static void openSession() {
            UserSession session = LoginDialog.login(null);
            if (session == null) return;
            if (session.isStaff()) new MainFrame(session).setVisible(true);
            else {
                JFrame frame = new JFrame("Cổng sinh viên KTX · " + session.studentId());
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setContentPane(new ExtensionPanel(session));
                frame.setMinimumSize(new java.awt.Dimension(1180, 740));
                frame.setSize(1440, 920);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
    }
}
