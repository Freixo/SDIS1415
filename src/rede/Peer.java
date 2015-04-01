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
	
	/*protected void sendReply(String message) {
		DatagramPacket msgPacket = new DatagramPacket(message.getBytes(), message.getBytes().length, group, port);
		try {
			socket.send(msgPacket);
		} catch (IOException e) { e.printStackTrace();}
	}*/
}

class Handler extends Thread {
	
	String msg;
	
	public Handler(byte[] buffer) {
		
		int i = buffer.length-1;
		while(buffer[i] == 0)//get the actual message size
			i--;
		msg = new String(buffer, 0, i+1);
		start();
	}
	
	public void run() {
		//TODO usar a mensagem
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
		String FileID="";
		int ChunkNO=0;
		Chunk c=null;
		Util.getInstance().notifyChunk(new ChunkPair(FileID, ChunkNO), c);
	}

	private void handlePutChunk() {
		String FileID="";
		int ChunkNO=0;
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
		String FileID="";
		int ChunkNO=0;
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
		String FileID="";
		Util.getInstance().deleteFile(FileID);
	}

	private void handleGetChunk() {
		String FileID="";
		int ChunkNO=0;
		Util.getInstance().listMe(new ChunkPair(FileID, ChunkNO), this);
		try {
			Thread.sleep(Util.getInstance().rand());
			sendChunk();
		} catch (InterruptedException e) { }
	}

	private void saveChunk() {
		// TODO Auto-generated method stub
		sendStored();
	}
	
	private void sendStored() {
		String FileID="";
		int ChunkNO=0;
		Util.getInstance().add(new ChunkPair(FileID, ChunkNO));
		// TODO Auto-generated method stub
		
	}

	private void sendPutchunk() {
		// TODO Auto-generated method stub
		
	}

	private void sendChunk() {
		// TODO Auto-generated method stub
		
	}

	private void handleStored() {
		String FileID="";
		int ChunkNO=0;
		Util.getInstance().add(new ChunkPair(FileID, ChunkNO));
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
	/*
	public void run() {

		String receivedMessage = receiveMessage();
		
		//do something with message
	}*/
}

// multicast data restore 
class MDR extends M {
	
	public MDR(String adr, int port) throws IOException {
		super(adr, port,"multicast data restore");
	}
	/*
	public void run() {

		String receivedMessage = receiveMessage();
		
		//do something with message
	}*/
}


public class Peer {

	private static Util util;
	
	public static void main(String[] args) throws IOException {
		
		util = Util.getInstance();
		
		util.setMC( new MC(args[0],Integer.valueOf(args[1])));
		util.setMDB( new MDB(args[2],Integer.valueOf(args[3])));
		util.setMDR( new MDR(args[4],Integer.valueOf(args[5])));
		new Peer();
	}
	
	public Peer () {
		
	}
}