import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class SenderWindow {
    private JFrame frame;
    static JLabel img;

    ServerListener sListener;
    ServerSender sSender;

    public static void main(String[] args) throws IOException {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    SenderWindow window = new SenderWindow();
                    window.frame.setVisible(true);


                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public SenderWindow() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame();
        frame.setBounds(100, 100, 1024, 728);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        img = new JLabel();
        frame.getContentPane().add(img, BorderLayout.CENTER);

        JMenuBar menuBar = new JMenuBar();
        frame.setJMenuBar(menuBar);

        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);

        JMenuItem Start = new JMenuItem("Share Screen");
        Start.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    sListener = new ServerListener();
                    sSender = new ServerSender();
                    JOptionPane.showMessageDialog(null, "You can minimize the screen, screen casting has started successfully. ", "Successful", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Something is wrong!", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        fileMenu.add(Start);
    }
}