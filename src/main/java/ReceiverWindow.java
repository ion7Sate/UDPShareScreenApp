import java.awt.EventQueue;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JLabel;
import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ReceiverWindow {

    private JFrame frame;
    static JLabel img;

    Client receiver;

    public static void main(String[] args) throws IOException {
        EventQueue.invokeLater(() -> {
            try {
                ReceiverWindow window = new ReceiverWindow();
                window.frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public ReceiverWindow() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame();
        frame.setBounds(100, 100, 1024, 728);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    if (Client.connected) {
                        receiver.sendFIN();
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });

        JMenuBar menuBar = new JMenuBar();
        frame.setJMenuBar(menuBar);

        JMenu mnNewMenu = new JMenu("File");
        menuBar.add(mnNewMenu);

        JMenuItem connect = new JMenuItem("Connect");
        connect.addActionListener(e -> {
            try {
                String addr = JOptionPane.showInputDialog("Enter the sender's IP");
                String name = JOptionPane.showInputDialog("Enter your name");
                if (addr.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "The address field can't be empty!", "Error", JOptionPane.ERROR_MESSAGE);
                } else if (name.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "The name field can't be empty!", "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    receiver = new Client(name, addr);
                }

            } catch (Exception e1) {
                JOptionPane.showMessageDialog(null, "The IP you entered could not be resolved!", "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
        });
        mnNewMenu.add(connect);

        img = new JLabel();
        frame.getContentPane().add(img, BorderLayout.CENTER);
    }

}