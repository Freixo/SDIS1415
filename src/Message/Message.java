/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Message;

import FileSystem.Chunk;
import FileSystem.FFile;
import FileSystem.UtilFunc;
import java.time.Clock;

public class Message {

    public enum Type {

        PUTCHUNK, GETCHUNK, DELETE, REMOVED, CHUNK, STORED//, ISDELETED, HAVECHUNK, LISTENINGFOR
    }
    private final Type type;

    private static final String CRLF = "\r\n";
    private String fileID = null;
    private String version;
    private int chunkNo;
    private int replicationDeg;
    private byte[] body = new byte[0];

    public Message(Type type) {
        this.type = type;
    }

    public Message(String msg, byte[] b, int index) {
        String[] vars = msg.split(" ");
        switch (vars[0]) {
            case "PUTCHUNK":
                type = Type.PUTCHUNK;
                version = vars[1];
                fileID = vars[2];
                chunkNo = Integer.valueOf(vars[3]);
                replicationDeg = Integer.valueOf(vars[4]);
                body = new byte[64000];
                System.arraycopy(b, index, body, 0, body.length);
                break;
            case "GETCHUNK":
                type = Type.GETCHUNK;
                version = vars[1];
                fileID = vars[2];
                chunkNo = Integer.valueOf(vars[3]);
                break;
            case "DELETE":
                type = Type.DELETE;
                version = vars[1];
                fileID = vars[2];
                break;
            case "REMOVED":
                type = Type.REMOVED;
                version = vars[1];
                fileID = vars[2];
                chunkNo = Integer.valueOf(vars[3]);
                break;
            case "CHUNK":
                type = Type.CHUNK;
                version = vars[1];
                fileID = vars[2];
                chunkNo = Integer.valueOf(vars[3]);
                body = new byte[64000];
                System.arraycopy(b, index, body, 0, body.length);
                break;
            case "STORED":
                type = Type.STORED;
                version = vars[1];
                fileID = vars[2];
                chunkNo = Integer.valueOf(vars[3]);
                break;
            default:
                System.err.println("Unknown Message Received!");
                type = null;
                break;
        }
    }

    public void setChunkNo(int c) {
        chunkNo = c;
    }

    public void setFileID(byte[] f) {
        fileID = UtilFunc.byteToString(f);
    }

    public void setFileID(String s) {
        fileID = s;
    }

    public void setVersion(int i, int j) {
        version = i + "." + j;
    }

    public void setVersion(String v) {
        version = v;
    }

    public void setReplicationDeg(int i) {
        replicationDeg = i;
    }

    public void setBody(byte[] b) {
        body = b;
    }

    public byte[] createMessage() {
        String str = "";
        switch (type) {
            case PUTCHUNK:
                str = "PUTCHUNK " + version + " " + fileID + " " + chunkNo + " " + replicationDeg + " " + CRLF + CRLF;
                break;
            case STORED:
                str = "STORED " + version + " " + fileID + " " + chunkNo + " " + CRLF + CRLF;
                break;
            case GETCHUNK:
                str = "GETCHUNK " + version + " " + fileID + " " + chunkNo + " " + CRLF + CRLF;
                break;
            case CHUNK:
                str = "CHUNK " + version + " " + fileID + " " + chunkNo + " " + CRLF + CRLF;
                break;
            case DELETE:
                str = "DELETE " + version + " " + fileID + " " + CRLF + CRLF;
            case REMOVED:
                str = "REMOVED " + version + " " + fileID + " " + chunkNo + " " + CRLF + CRLF;
            default:
                break;
        }
        System.out.println("Sending message: "+str);
        byte[] final_msg = new byte[str.getBytes().length + this.body.length];
        System.arraycopy(str.getBytes(), 0, final_msg, 0, str.getBytes().length);
        System.arraycopy(this.body, 0, final_msg, str.getBytes().length, this.body.length);
        return final_msg;
    }
    
    public String getFileID() {
		return fileID;
	}

	public int getChunkNo() {
		return chunkNo;
	}

	public byte[] getBody() {
		return body;
	}
	
	public Chunk getChunk() {
		return new Chunk(chunkNo, body);
	}

    public static void main(String[] args) throws InterruptedException {

        FFile f = new FFile("AC.jpg", "1.0", 1);
        f.print();
        f.ChunkstoFile();

        Message m = new Message(Type.PUTCHUNK);

        m.setBody(f.getChunks()[1].getBytes());
        m.setVersion(1, 0);
        m.setChunkNo(1);
        m.setFileID(f.getFileId());
        m.setReplicationDeg(f.getReplicationDeg());

        System.out.println(m.createMessage());
    }

	public String getVersion() {
		return this.version;
	}

	public int getDegree() {
		return this.replicationDeg;
	}
}
