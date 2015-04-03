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
import java.security.MessageDigest;
import java.util.Scanner;
import java.util.Vector;

// multicast
abstract class M extends Thread {
	
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
	
	public void run(){
		byte[] buffer = new byte[Integer.MAX_VALUE-5];
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		while(!finished) {
			try {
				socket.receive(packet);
				new Handler( buffer);
			} catch (IOException e) { e.printStackTrace();}
		}
	};
	
	public void finish() {
		this.finished  = true;
	}
	
	public void sendMessage(String msg) {
		DatagramPacket msgPacket = new DatagramPacket(msg.getBytes(), msg.getBytes().length, group, port);
		try {
			socket.send(msgPacket);
		} catch (IOException e) { e.printStackTrace();}
	}
}

class Handler extends Thread {
	
	String msg;
	Message m;
	
	public Handler(byte[] buffer) {
		
		int i = buffer.length-1;
		while(buffer[i] == 0)//get the actual message size
			i--;
		msg = new String(buffer, 0, i+1);
		m = new Message(msg);
		start();
	}
	
	public void run() {
		//do something with message
		if(msg.toUpperCase().startsWith("STORED"))
			handleStored();
		else if(msg.toUpperCase().startsWith("GETCHUNK"))
			handleGetChunk();
		else if(msg.toUpperCase().startsWith("DELETE"))
			handleDelete();
		else if(msg.toUpperCase().startsWith("REMOVED"))
			handleRemoved();
		else if(msg.toUpperCase().startsWith("PUTCHUNK"))
			handlePutChunk();
		else if(msg.toUpperCase().startsWith("CHUNK"))
			handleChunk();
	}
	
	private void handleChunk() {
		String FileID=m.getFileID();
		int ChunkNO=m.getChunkNo();
		Chunk c=m.getChunk();
		Util.getInstance().notifyChunk(new ChunkPair(FileID, ChunkNO), c);
	}

	private void handlePutChunk() {
		String FileID=m.getFileID();
		int ChunkNO=m.getChunkNo();
		Util.getInstance().notifyPutChunk(new ChunkPair(FileID, ChunkNO));
		if(Util.getInstance().getFile(FileID).has(ChunkNO)) {
			try {
				Thread.sleep(Util.getInstance().rand());
				if(Util.getInstance().getCount(new ChunkPair(FileID, ChunkNO)) < Util.getInstance().getFile(FileID).getReplicationDeg())
						saveChunk();
			} catch (InterruptedException e) { }
		}
	}

	private void handleRemoved() {
		String FileID=m.getFileID();
		int ChunkNO=m.getChunkNo();
		int q = Util.getInstance().remove(new ChunkPair(FileID, ChunkNO));
		if( q<Util.getInstance().getFile(FileID).getReplicationDeg()) {
			Util.getInstance().listMe2(new ChunkPair(FileID, ChunkNO), this);
			try {
				Thread.sleep(Util.getInstance().rand());
				sendPutchunk();
			} catch (InterruptedException e) { }
		}
	}

	private void handleDelete() {
		String FileID=m.getFileID();
		Util.getInstance().deleteFile(FileID);
	}

	private void handleGetChunk() {
		String FileID=m.getFileID();
		int ChunkNO=m.getChunkNo();
		Util.getInstance().listMe(new ChunkPair(FileID, ChunkNO), this);
		try {
			Thread.sleep(Util.getInstance().rand());
			sendChunk();
		} catch (InterruptedException e) { }
	}

	private void handleStored() {
		String FileID=m.getFileID();
		int ChunkNO=m.getChunkNo();
		Util.getInstance().add(new ChunkPair(FileID, ChunkNO));
	}

	private void saveChunk() {
		// TODO Auto-generated method stub
		sendStored();
	}
	
	private void sendStored() {
		String FileID=m.getFileID();
		int ChunkNO=m.getChunkNo();
		Util.getInstance().add(new ChunkPair(FileID, ChunkNO));
		// TODO Auto-generated method stub
		
	}

	private void sendPutchunk() {
		// TODO Auto-generated method stub
		
	}

	private void sendChunk() {
		// TODO Auto-generated method stub
		
	}
}

// multicast control
class MC extends M {
	
	public MC(String adr, int port) throws IOException {
		super(adr, port,"multicast control");
	}
	/*
	public void run() {
	}*/
}

// multicast data backup
class MDB extends M {
	
	public MDB(String adr, int port) throws IOException {
		super(adr, port,"multicast data backup");
	}
}

// multicast data restore 
class MDR extends M {
	
	public MDR(String adr, int port) throws IOException {
		super(adr, port,"multicast data restore");
	}
}


public class Peer extends Thread {

	private static Util util;
	
	public static void main(String[] args) throws IOException {
		
		util = Util.getInstance();
		
		util.setMC( new MC(args[0],Integer.valueOf(args[1])));
		util.setMDB( new MDB(args[2],Integer.valueOf(args[3])));
		util.setMDR( new MDR(args[4],Integer.valueOf(args[5])));
		new Peer().start();
	}
	
	public Peer () {
		
	}
	
	public void run() {
		
	}
}

class InputHandler {

    Message msg;
    Vector<String> messages;
    Scanner in = new Scanner(System.in);

    public void getInput() {

        int input;
        do {
            System.out.println("Que tarefa deseja efetuar?");
            System.out.println("1.  BACKUP");
            System.out.println("2.  RESTORE");
            System.out.println("3.  DELETE");
            System.out.println("4.  SPACE REQUEST");
            try {
                input = in.nextInt();
            } catch (Exception e) {
                System.err.println("Invalid Input");
                input = 0;
            }
        } while (input > 4 || input < 1);

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
        System.out.println("Qual a versão do ficheiro?");
        String version = in.next();

        msg.setVersion(version);
        int chunkNo = 0;
        int repDeg = 0;
        byte[] body = {};
        switch (input) {
            case 1:

                do {
                    System.out.println("Qual o numero de Réplicas pretende?");
                    try {
                        repDeg = in.nextInt();
                    } catch (Exception e) {
                        System.err.println("Invalid Input");
                        input = 0;
                    }
                } while (input < 1);
                msg.setReplicationDeg(repDeg);
                FFile file = new FFile(name, version, repDeg);
                 for(int i = 0 ; i < file.getChunks().length ; ++i){
                     msg.setChunkNo(i);
                     msg.setBody(file.getChunks()[i].getBytes());
                     messages.add(msg.createMessage());
                 }
                break;
            case 2:
                do {
                    System.out.println("Qual o numero da Chunk que pretende restaurar?");
                    try {
                        chunkNo = in.nextInt();
                    } catch (Exception e) {
                        System.err.println("Invalid Input");
                        input = -1;
                    }
                } while (input < 0);
                msg.setChunkNo(chunkNo);
                messages.add(msg.createMessage());
                break;
            case 3:
                messages.add(msg.createMessage());
                break;
            case 4:
                do {
                    System.out.println("Qual o numero da Chunk que pretende apagar?");
                    try {
                        chunkNo = in.nextInt();
                    } catch (Exception e) {
                        System.err.println("Invalid Input");
                        input = -1;
                    }
                } while (input < 0);
                msg.setChunkNo(chunkNo);
                messages.add(msg.createMessage());
                break;
            default:
                break;
        }

    }
}