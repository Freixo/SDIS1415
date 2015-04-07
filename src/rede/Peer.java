package rede;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.sql.Time;
import java.util.Random;

import FileSystem.Chunk;
import FileSystem.FFile;
import static FileSystem.UtilFunc.byteToString;
import Message.Message;
import static java.lang.Thread.sleep;

import java.security.MessageDigest;
import java.util.Scanner;
import java.util.Vector;

//import com.sun.corba.se.impl.javax.rmi.CORBA.Util;
// multicast
abstract class M extends Thread {

    private static final int messageMaxSize = 128000;
    protected MulticastSocket socket;
    protected InetAddress group;
    protected int port;
    private boolean finished = false;

    public M(String adr, int port, String name) throws IOException {
        super(name);
        InetAddress group = InetAddress.getByName(adr);
        socket = new MulticastSocket(port);
        socket.joinGroup(group);
        this.group = group;
        this.port = port;
        start();
    }

    public void run() {
        byte[] buffer = new byte[messageMaxSize];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        while (!finished) {
            try {
                socket.receive(packet);
                new Handler(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    ;
	
	public void finish() {
        this.finished = true;
    }

    public void sendMessage(byte[] msg) {
        DatagramPacket msgPacket = new DatagramPacket(msg, msg.length, group, port);
        try {
            socket.send(msgPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class Handler extends Thread {

    String msg_received;
    Message m;

    public Handler(byte[] buffer) {

        int index = 0;
        for (int c = 0; c < buffer.length - 3; c++) {
            if (buffer[c] == 13 && buffer[c + 1] == 10
                    && buffer[c + 2] == 13 && buffer[c + 3] == 10) {

                index = c + 4;
                break;
            }
        }

        msg_received = new String(buffer, 0, index - 4);
        System.out.println("Message received with size " + index + ": " + msg_received);
        m = new Message(msg_received, buffer, index);
        start();
    }

    public void run() {
        //do something with message
        if (msg_received.toUpperCase().startsWith("STORED")) {
            handleStored();
        } else if (msg_received.toUpperCase().startsWith("GETCHUNK")) {
            handleGetChunk();
        } else if (msg_received.toUpperCase().startsWith("DELETE")) {
            handleDelete();
        } else if (msg_received.toUpperCase().startsWith("REMOVED")) {
            handleRemoved();
        } else if (msg_received.toUpperCase().startsWith("PUTCHUNK")) {
            handlePutChunk();
        } else if (msg_received.toUpperCase().startsWith("CHUNK")) {
            handleChunk();
        }
    }

    private void handleChunk() {
        String FileID = m.getFileID();
        int ChunkNO = m.getChunkNo();
        Chunk c = m.getChunk();
        Util.getInstance().notifyChunk(new ChunkPair(FileID, ChunkNO), c);
    }

    private void handlePutChunk() {
        String FileID = m.getFileID();
        int ChunkNO = m.getChunkNo();
        Util.getInstance().notifyPutChunk(new ChunkPair(FileID, ChunkNO));
        if (Util.getInstance().haveFile(FileID) && !Util.getInstance().getFile(FileID).has(ChunkNO)) {
            try {
                Thread.sleep(Util.getInstance().rand());
                if (Util.getInstance().getCount(new ChunkPair(FileID, ChunkNO)) < Util.getInstance().getFile(FileID).getReplicationDeg()) {
                    saveChunk();
                }
            } catch (InterruptedException e) {
            }
        }
    }

    private void handleRemoved() {
        if (Util.getInstance().haveFile(m.getFileID())) {
            String FileID = m.getFileID();
            int ChunkNO = m.getChunkNo();
            int repDeg = Util.getInstance().getFile(FileID).getReplicationDeg();
            byte[] body = Util.getInstance().getFile(FileID).getChunkBody(ChunkNO);
            int q = Util.getInstance().remove(new ChunkPair(FileID, ChunkNO));
            if (q < repDeg && Util.getInstance().getFile(FileID).has(ChunkNO)) {
                Util.getInstance().listMe2(new ChunkPair(FileID, ChunkNO), this);
                try {
                    Thread.sleep(Util.getInstance().rand());
                    sendPutchunk(repDeg, body);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    private void handleDelete() {
        String FileID = m.getFileID();
        Util.getInstance().deleteFile(FileID);
    }

    private void handleGetChunk() {
        String FileID = m.getFileID();
        int ChunkNO = m.getChunkNo();
        if (Util.getInstance().haveFile(FileID) && Util.getInstance().getFile(FileID).has(ChunkNO)) {
            Util.getInstance().listMe(new ChunkPair(FileID, ChunkNO), this);
            try {
                Thread.sleep(Util.getInstance().rand());
                sendChunk();
            } catch (InterruptedException e) {
            }
        }
    }

    private void handleStored() {
        String FileID = m.getFileID();
        int ChunkNO = m.getChunkNo();
        Util.getInstance().add(new ChunkPair(FileID, ChunkNO));
    }

    private void saveChunk() {
        Chunk c = new Chunk(m.getChunkNo(), m.getBody());
        Util.getInstance().saveChunk(c, m.getFileID(), m.getDegree(), m.getVersion());
        sendStored();
    }

    private void sendStored() {
        String FileID = m.getFileID();
        int ChunkNO = m.getChunkNo();
        //ao enviar tenho que incrementar o meu contador em 1 tambem.
        Util.getInstance().add(new ChunkPair(FileID, ChunkNO));
        //enviar
        Message msg = new Message(Message.Type.STORED);
        msg.setVersion(m.getVersion());
        msg.setFileID(FileID);
        msg.setChunkNo(ChunkNO);
        Util.getInstance().sendMessage(Util.channel.MC, msg.createMessage());
    }

    private void sendPutchunk(int repDeg, byte[] body) {
        Message msg = new Message(Message.Type.PUTCHUNK);
        msg.setVersion(m.getVersion());
        msg.setFileID(m.getFileID());
        msg.setChunkNo(m.getChunkNo());
        msg.setReplicationDeg(repDeg);
        msg.setBody(body);
        Util.getInstance().sendMessage(Util.channel.MDB, msg.createMessage());
    }

    private void sendChunk() {
        Message msg = new Message(Message.Type.PUTCHUNK);
        msg.setVersion(m.getVersion());
        msg.setFileID(m.getFileID());
        msg.setChunkNo(m.getChunkNo());
        msg.setBody(Util.getInstance().getFile(m.getFileID()).getChunkBody(m.getChunkNo()));
        Util.getInstance().sendMessage(Util.channel.MDR, msg.createMessage());
    }
}

// multicast control
class MC extends M {

    public MC(String adr, int port) throws IOException {
        super(adr, port, "multicast control");
    }
    /*
     public void run() {
     super.run();
     }*/
}

// multicast data backup
class MDB extends M {

    public MDB(String adr, int port) throws IOException {
        super(adr, port, "multicast data backup");
    }
}

// multicast data restore 
class MDR extends M {

    public MDR(String adr, int port) throws IOException {
        super(adr, port, "multicast data restore");
    }
}

public class Peer extends Thread {

    private static Util util;

    public static void main(String[] args) throws IOException {

        util = Util.getInstance();

        util.setMC(new MC(args[0], Integer.valueOf(args[1])));
        util.setMDB(new MDB(args[2], Integer.valueOf(args[3])));
        util.setMDR(new MDR(args[4], Integer.valueOf(args[5])));
        new Peer().start();
    }

    public Peer() {

    }

    public void run() {

        while (true) {
            Message msg;
            Vector<byte[]> messages = new Vector<byte[]>();
            Scanner in = new Scanner(System.in);

            int input;
            do {
                System.out.println("Que tarefa deseja efetuar?");
                System.out.println("1.  BACKUP");
                System.out.println("2.  RESTORE");
                System.out.println("3.  DELETE");
                System.out.println("4.  SPACE REQUEST");
                System.out.println("5.  Quit");
                try {
                    input = in.nextInt();
                } catch (Exception e) {
                    System.err.println("Invalid Input");
                    input = 0;
                    in.nextLine();
                }
            } while (input > 5 || input < 1);
            if (input == 5) {
                break;
            }
            msg = new Message(Message.Type.values()[input - 1]);

            System.out.println("Qual o nome do ficheiro?");
            String name = in.next();
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("SHA-256");
                md.update(name.getBytes("ASCII"));
                msg.setFileID(byteToString(md.digest()));
            } catch (Exception ex) {
                System.err.println("Error digesting message");
                return;
            }
            System.out.println("Qual a versão do ficheiro?");//TODO isto � automatico! (ou nem e preciso pq so ha 1 versao)
            String version = in.next();

            msg.setVersion(version);
            int chunkNo = 0;
            int repDeg = 0;
            FFile file;
            switch (input) {
                case 1:
                    do {
                        System.out.println("Qual o numero de Réplicas pretende?");
                        try {
                            repDeg = in.nextInt();
                        } catch (Exception e) {
                            System.err.println("Invalid Input");
                            input = 0;
                            in.nextLine();
                        }
                    } while (input < 1);
                    msg.setReplicationDeg(repDeg);

                    if (Util.getInstance().haveFile(msg.getFileID())) {
                        Message deleteMsg = new Message(Message.Type.DELETE);
                        deleteMsg.setFileID(msg.getFileID());
                        deleteMsg.setVersion(Util.getInstance().getFile(msg.getFileID()).getVersion());
                        Util.getInstance().sendMessage(Util.channel.MC, deleteMsg.createMessage());
                        Util.getInstance().deleteFile(msg.getFileID());
                    }
                    //Util.getInstance().sendMessage(Util.channel.MC, msg.createMessage());//?

                    file = new FFile(name, version, repDeg);
                    Util.getInstance().addFile(file);//TODO guardar antes num map so para os do proprio user?
                    for (int i = 0; i < file.getChunks().length; ++i) {
                        msg.setChunkNo(i);
                        msg.setBody(file.getChunks()[i].getBytes());
                        messages.add(msg.createMessage());
                    }

                    for (int i = 0; i < messages.size(); ++i) {
                        Util.getInstance().sendMessage(Util.channel.MDB, messages.get(i));
                        int time = 500;
                        do {
                            try {
                                sleep(time);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            time *= 2;
                        } while (Util.getInstance().getCount(new ChunkPair(file.getFileId(), chunkNo)) < repDeg && time <= 8000);
                    }
                    break;
                case 2:
                    if (Util.getInstance().haveFile(msg.getFileID())) {//TODO mudar para uma lista que tenhas so os meus?
                        file = Util.getInstance().getFile(msg.getFileID());
                        for (int i = 0; i < file.getChunks().length; ++i) {
                            msg.setChunkNo(file.getChunks()[i].getChunkNO());
                            Util.getInstance().listMeW(new ChunkPair(msg.getFileID(), msg.getChunkNo()), this);
                            Util.getInstance().sendMessage(Util.channel.MC, msg.createMessage());
                            try {
                                this.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            Chunk asked = Util.getInstance().getChunk(new ChunkPair(msg.getFileID(), msg.getChunkNo()));
                            Util.getInstance().saveChunk(asked, msg.getFileID(), Util.getInstance().getFile(msg.getFileID()).getReplicationDeg(), msg.getVersion());
                        }
                    }
                    break;
                case 3:
                    Util.getInstance().deleteFile(msg.getFileID());
                    Util.getInstance().sendMessage(Util.channel.MC, msg.createMessage());
                    break;
                case 4:
                    /*TODO remover chunk e nao o ficheiro inteiro!
                     * pedir qual e o chunk NO e apagar esse / enviar a mensagem sobre ESSE chunkNO
                     */
                    if (Util.getInstance().haveFile(msg.getFileID())) {
                        file = Util.getInstance().getFile(msg.getFileID());
                        for (int i = 0; i < file.getChunks().length; ++i) {
                            msg.setChunkNo(i);
                            Util.getInstance().sendMessage(Util.channel.MC, msg.createMessage());
                        }
                        Util.getInstance().deleteFile(msg.getFileID());
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
