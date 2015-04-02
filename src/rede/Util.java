package rede;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import FileSystem.Chunk;
import FileSystem.FFile;

class ChunkPair {
	String fileID;
	int chunkNO;
	
	public ChunkPair(String id, int n) {
		fileID = id;
		chunkNO = n;
	}
	
	public boolean equals(String id, int n) {
		if( id == null | id == "") return false;
		if (this.fileID.equals(id) & this.chunkNO == n) return true;
		return false;
	}
	
	public boolean sameFile(String id) {
		if( id == null | id == "") return false;
		if (this.fileID.equals(id)) return true;
		return false;
	}
	
	@Override
	public boolean equals(Object obj) {
	    if (obj == null) return false;
	    if (getClass() != obj.getClass()) return false;
	    
	    final ChunkPair other = (ChunkPair) obj;
	    if (this.fileID.equals(other.fileID) & this.chunkNO == other.chunkNO) return true;
	    return false;
	}
}

public class Util {
	
	private static Util instance = null;
	private MC mc;
	private MDB mdb;
	private MDR mdr;
	private Map<ChunkPair, Integer> chunksCount = new HashMap();
	Random rand = new Random();
	private Map<ChunkPair, List<Thread>> waiters = new HashMap();//esperar por um chunk
	private Map<String, FFile> files = new HashMap();
	private Map<ChunkPair, List<Thread>> waiters2 = new HashMap();//esperar por um putchunk
	private Map<ChunkPair, List<Thread>> wwaiters = new HashMap();//esperar por um chunk (receber)
	private Map<ChunkPair, Chunk> wwaited = new HashMap();// chunk esperado

	protected Util() {
		// Exists only to defeat instantiation.
	}

	public static Util getInstance() {
		if (instance == null) {
			instance = new Util();
		}
		return instance;
	}

	public void setMC(MC mc) {
		this.mc = mc;
	}

	public void setMDB(MDB mdb) {
		this.mdb = mdb;
	}

	public void setMDR(MDR mdr) {
		this.mdr = mdr;
	}

	/*
	 * Increments the counting of that chunk in the system
	 * */
	public void add(ChunkPair pair) {
		int current = chunksCount.containsKey(pair) ? (int) chunksCount.get(pair) : 0;
		chunksCount.put(pair, current+1);
	}

	public int rand() {
		return rand.nextInt(400);
	}

	public void listMe(ChunkPair pair, Thread thread) {
		List<Thread> l = waiters.containsKey(pair) ? waiters.get(pair) : new ArrayList<Thread>();
		l.add(thread);
		waiters.put(pair, l);
	}

	public void deleteFile(String fileID) {
		files.get(fileID).delete();
		files.remove(fileID);
		for (Map.Entry<ChunkPair, Integer> entry : chunksCount.entrySet()) {
			if (entry.getKey().sameFile(fileID))
				chunksCount.remove(entry.getKey());
		}
	}

	public int remove(ChunkPair pair) {
		int count = chunksCount.containsKey(pair) ? (int) chunksCount.get(pair)-1 : 0;
		if(count == 0)
			chunksCount.remove(pair);
		else
			chunksCount.put(pair, count);
		return count;
	}

	public FFile getFile(String fileID) {
		return files.get(fileID);
	}

	public void listMe2(ChunkPair pair, Thread thread) {
		List<Thread> l = waiters2.containsKey(pair) ? waiters2.get(pair) : new ArrayList<Thread>();
		l.add(thread);
		waiters2.put(pair, l);
	}

	public void notifyPutChunk(ChunkPair chunkPair) {
		for (Entry<ChunkPair, List<Thread>> entry : waiters2.entrySet()) {
			if (entry.getKey().equals(chunkPair)) {
				for(Thread t : entry.getValue()) {
					t.interrupt();
				}
				waiters2.remove(entry.getKey());
			}
		}
	}
	
	public int getCount(ChunkPair pair) {
		int count = chunksCount.containsKey(pair) ? (int) chunksCount.get(pair)-1 : 0;
		if(count == 0)
			chunksCount.remove(pair);
		else
			chunksCount.put(pair, count);
		return count;
	}

	public void notifyChunk(ChunkPair chunkPair, Chunk chunk) {
		for (Entry<ChunkPair, List<Thread>> entry : waiters.entrySet()) {
			if (entry.getKey().equals(chunkPair)) {
				for(Thread t : entry.getValue()) {
					t.interrupt();
				}
				waiters.remove(entry.getKey());
			}
		}
		boolean saved = false;
		for (Entry<ChunkPair, List<Thread>> entry : wwaiters.entrySet()) {
			if (entry.getKey().equals(chunkPair)) {
				if(!saved) {
					wwaited.put(chunkPair, chunk);
					saved = true;
				}
				for(Thread t : entry.getValue()) {
					t.notify();
				}
				wwaiters.remove(entry.getKey());
			}
		}
	}
}