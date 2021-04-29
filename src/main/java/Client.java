import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class Client implements Runnable{
    DatagramSocket clientSocket = null;
    InetAddress IPAddress = null;

    String receiverName = null;
    boolean isNameTaken = false;
    int helloRequestCounter = 0;

    int timeoutCounter = 0;

    ArrayList<byte[]> bufferList = new ArrayList<byte[]>();

    static boolean connected = false;

    BufferedImage image;

    static boolean imageSaverFlag = false;

    String homePath;

    static boolean isBusy = false;

    public Client(String name, String addr) throws IOException {

        this.receiverName = name;

        System.out.println("Client is running...");

        clientSocket = new DatagramSocket();
        clientSocket.setSoTimeout(20);

        IPAddress = InetAddress.getByName(addr);

        sendHello();
        setHomePath();

        connected = true;
        clientSocket.setSoTimeout(100);

        new Thread(this).start();

    }

    public void setHomePath() {
        FileSystemView filesys = FileSystemView.getFileSystemView();
        File[] roots = filesys.getRoots();
        this.homePath = filesys.getHomeDirectory().toString();
    }



    public boolean sendHello() throws IOException {

        if (helloRequestCounter == 100) {
            JOptionPane.showMessageDialog(null, "Server is down! Exiting...", "Information", JOptionPane.INFORMATION_MESSAGE);
            System.exit(0);
        }
        helloRequestCounter++;

        byte[] receiveData = new byte[64008];

        if (isNameTaken == true) {
            receiverName = JOptionPane.showInputDialog("Enter your name");
        }


        String sentence = "Hello " + receiverName;

        ByteBuffer data = ByteBuffer.allocate(sentence.getBytes().length);
        data.put(sentence.getBytes());

        DatagramPacket sendPacket = new DatagramPacket(data.array(), data.capacity(), IPAddress, 9876);
        clientSocket.send(sendPacket);

        data = ByteBuffer.allocate(4);
        data.clear();
        data.rewind();

        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

        try {
            clientSocket.receive(receivePacket);

            for (int i = 0; i < 4; i++) {
                data.put(receivePacket.getData()[i]);
            }
            int result = data.getInt(0);
            if (result == 200) {

            } else if (result == 500) {
                isNameTaken = true;
                sendHello();
                return false;
            }

        } catch (Exception ex) {
            sendHello();
            return false;
        }

        data.clear();
        data.rewind();

        sentence = "READY " + receiverName;
        data = ByteBuffer.allocate(sentence.getBytes().length);
        data.put(sentence.getBytes());

        sendPacket = new DatagramPacket(data.array(), data.capacity(), IPAddress, 9876);
        clientSocket.send(sendPacket);

        return true;
    }

    public void sendFIN() throws IOException {
        String sentence = "FIN " + receiverName;
        ByteBuffer data = ByteBuffer.allocate(sentence.getBytes().length);
        data.put(sentence.getBytes());
        DatagramPacket sendPacket = new DatagramPacket(data.array(), data.capacity(), IPAddress, 9876);
        clientSocket.send(sendPacket);
    }

    public void receivePackets() throws IOException {

        timeoutCounter++;
        byte[] receiveData = new byte[64020];
        Checksum checksum = new CRC32();
        int expectedChunkNumber = 1;
        int numberOfPackets = 0;
        ByteBuffer buffer = null;

        while (true) {

            DatagramPacket receivedPacket = new DatagramPacket(receiveData, receiveData.length);
            try {
                clientSocket.receive(receivedPacket);
                timeoutCounter = 0;
            } catch (Exception ex) {
                if (timeoutCounter == 300) {
                    JOptionPane.showMessageDialog(null, "Server is down! Exiting...", "Information", JOptionPane.INFORMATION_MESSAGE);
                    System.exit(0);
                }
                return;
            }

            int packetLength = receivedPacket.getLength();

            ByteBuffer data = ByteBuffer.allocate(packetLength);
            data.put(receiveData, 0, packetLength);

            data.rewind();

            long checksumVal = data.getLong();
            int receivedChunkNumber = data.getInt();

            if (receivedChunkNumber == -1) {
                checksum.reset();
                checksum.update(data.array(), 8, data.capacity() - 8);
                long calculatedChecksum = checksum.getValue();

                if ((calculatedChecksum == checksumVal) && (numberOfPackets + 1 == expectedChunkNumber)) {
                    break;
                }
                return;
            }


            int payloadSize = data.getInt();
            int totalSize = data.getInt();

            data.rewind();
            checksum.reset();
            checksum.update(data.array(), 20, payloadSize);
            long calculatedChecksum = checksum.getValue();

            if (calculatedChecksum != checksumVal) {
                System.out.println("***************");
                System.out.println("EXPECTED CHECKSUM : " + calculatedChecksum);
                System.out.println("RECEIVED CHECKSUM : " + checksumVal);
                return;
            }
            if (receivedChunkNumber != expectedChunkNumber) {
                System.out.println("PACKET LOSS");
                return;
            }

            if (expectedChunkNumber == 1) {
                buffer = ByteBuffer.allocate(totalSize);
                numberOfPackets = (totalSize / 64000) + 1;
            }
            data.rewind();
            buffer.put(data.array(), 20, payloadSize);

            expectedChunkNumber++;

            data.clear();

        }

        if (buffer != null) {
            ByteArrayInputStream bais = new ByteArrayInputStream(buffer.array());
            image = ImageIO.read(bais);
            ReceiverWindow.img.setIcon(new ImageIcon(image));
        }


    }

    @Override
    public void run() {

        while (true) {
            try {
                receivePackets();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
