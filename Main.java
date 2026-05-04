import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new GameWindow();
            } catch (Exception e) {
                System.err.println("Error launching game:");
                e.printStackTrace();
            }
        });
    }
}
