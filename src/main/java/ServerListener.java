import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

public class ServerListener implements Runnable {

    static DatagramSocket serverSocket = null;
    static ArrayList<String> receivers = new ArrayList<String>();
    static HashMap<String, Integer> receiversPorts = new HashMap<String, Integer>();
    static HashMap<String, InetAddress> receiverAddrs = new HashMap<String, InetAddress>();

    public ServerListener() throws SocketException, UnknownHostException {
        serverSocket = new DatagramSocket(9876);
        new Thread(this).start();
    }

    public void helloRequestHandler(int port, String name, InetAddress addr) {
        if (receiversPorts.containsKey(name)) {
            System.out.println("WARNING!" + name + " is already connected to the server.");
            ByteBuffer sendData = ByteBuffer.allocate(4);
            sendData.putInt(500); // Sends 'error' message indicating that the name is already taken.
            DatagramPacket sendPacket = new DatagramPacket(sendData.array(), sendData.capacity(), addr, port);
            try {
                serverSocket.send(sendPacket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {

            System.out.println(name + " is connected to the server.");

            ByteBuffer sendData = ByteBuffer.allocate(4);
            sendData.putInt(200); // Sends 'ok' message indicating that the client is added successfully.
            DatagramPacket sendPacket = new DatagramPacket(sendData.array(), sendData.capacity(), addr, port);
            try {
                serverSocket.send(sendPacket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run() {

        while (true) {
            byte[] buff = new byte[100];
            DatagramPacket receivePacket = new DatagramPacket(buff, buff.length);
            try {
                serverSocket.receive(receivePacket);
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            int port = receivePacket.getPort();
            String sentence = new String(receivePacket.getData(), 0, receivePacket.getLength());
            InetAddress addr = receivePacket.getAddress();
            System.out.println("RECEIVED: " + sentence + " from " + port);
            String[] splitted_message = sentence.split(" ");

            if (splitted_message[0].equals("Hello")) {
                helloRequestHandler(port,splitted_message[1], addr);
            } else if (splitted_message[0].equals("READY")) {

                receiversPorts.put(splitted_message[1],port);
                receiverAddrs.put(splitted_message[1],addr);
                receivers.add(splitted_message[1]);

            }

        }

    }



}


