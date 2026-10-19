package vn.ktx;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import vn.ktx.ui.MainFrame;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception exception) {
                System.err.println("Không thể áp dụng giao diện hệ thống: " + exception.getMessage());
            }
            new MainFrame().setVisible(true);
        });
    }
}
