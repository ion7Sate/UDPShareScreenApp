import java.awt.AWTException;
import java.awt.Graphics2D;
import java.awt.HeadlessException;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

public class ServerSender implements Runnable {

    private InetAddress IPAddress = null;


    ArrayList<ByteBuffer> bufferList = new ArrayList<ByteBuffer>();


    public ServerSender() throws IOException {
        new Thread(this).start();

    }

    public BufferedImage scale(BufferedImage imgToScale) {
        BufferedImage resized = new BufferedImage(1024, 728, imgToScale.getType());
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(imgToScale, 0, 0, 1024, 728, 0, 0, imgToScale.getWidth(),
                imgToScale.getHeight(), null);
        g.dispose();

        return resized;
    }

    private BufferedImage takeScreenShot() throws HeadlessException, AWTException {
        BufferedImage image = new Robot().createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));

        BufferedImage scaledImage = scale(image);
        return scaledImage;
    }

    public long getChecksum(byte[] arr, int offset, int length) {

        Checksum checksum = new CRC32();
        checksum.update(arr, offset, length);
        return checksum.getValue();
    }

    private void splitScreenshot(BufferedImage screenshot) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(screenshot, "png", baos);
        baos.flush();

        byte[] buffer = baos.toByteArray();
        Checksum checksum = new CRC32();

        int size = buffer.length;
        int payloadSize = 64000;
        int lastPayloadSize = buffer.length % payloadSize;
        int actualPayloadSize = payloadSize;

        ByteBuffer bb = ByteBuffer.allocate(payloadSize + 20);

        int chunkNumber = 1;
        int index = 0;

        checksum.update(buffer, index, payloadSize);
        bb.putLong(checksum.getValue());

        bb.putInt(chunkNumber);
        bb.putInt(actualPayloadSize);
        bb.putInt(size);

        for (int i = 0; i < buffer.length; i++) {

            bb.put(buffer[i]);
            if ((i + 1) % 64000 == 0) {
                bufferList.add(bb);
                bb.clear();
                checksum.reset();
                bb = ByteBuffer.allocate(payloadSize + 20);

                index += actualPayloadSize;

                if (buffer.length - i == lastPayloadSize + 1) {
                    actualPayloadSize = lastPayloadSize;
                } else {
                    actualPayloadSize = payloadSize;
                }

                chunkNumber++;


                checksum.update(buffer, index, actualPayloadSize);

                bb.putLong(checksum.getValue());
                bb.putInt(chunkNumber);
                bb.putInt(actualPayloadSize);
                bb.putInt(size);

            }
        }

        bufferList.add(bb);

        bb.clear();
        bb = ByteBuffer.allocate(4);
        bb.putInt(-1);
        checksum.reset();
        checksum.update(bb.array(), 0, 4);

        ByteBuffer qq = ByteBuffer.allocate(12);
        qq.putLong(checksum.getValue());

        qq.putInt(-1);

        bufferList.add(qq);
    }

    private void sendData() throws IOException {

        DatagramPacket sendPacket;

        if (ServerListener.receiversPorts.isEmpty()) {
            return;
        }


        for (ByteBuffer chunk : bufferList) {


            for (String name : ServerListener.receivers) {
                InetAddress addr = ServerListener.receiverAddrs.get(name);
                int port = ServerListener.receiversPorts.get(name);
                sendPacket = new DatagramPacket(chunk.array(), chunk.capacity(), addr, port);
                ServerListener.serverSocket.send(sendPacket);
            }

            long start = System.currentTimeMillis();
            long end = start + 5;
            while (System.currentTimeMillis() < end) {
            }
        }
    }

    @Override
    public void run() {

        while (true) {
            try {
                bufferList.clear();
                BufferedImage screenshot = takeScreenShot();
                SenderWindow.img.setIcon(new ImageIcon(screenshot));
                splitScreenshot(screenshot);
                sendData();
            } catch (Exception ex) {
                System.out.println("Error in ServerSender thread!");
            }

        }
    }
}