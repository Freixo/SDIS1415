/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Message;

import FileSystem.FFile;
import FileSystem.UtilFunc;

/**
 *
 * @author Utilizador
 */
public class Message {

    public enum Type {

        PUTCHUNK, GETCHUNK, DELETE, REMOVED, CHUNK, STORED//, ISDELETED, HAVECHUNK, LISTENINGFOR
    }
    public final Type type;

    private static final String CRLF = "DA";
    private String fileID = null;
    private String version;
    private int chunkNo;
    private int replicationDeg;
    private byte[] body = null;

    public Message(Type type) {
        this.type = type;
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

    public String createMessage() {
        String str = "";
        switch (type) {
            case PUTCHUNK:
                str = "PUTCHUNK " + version + " " + fileID + " " + chunkNo + " " + replicationDeg + " " + CRLF + CRLF + body;
                break;
            case STORED:
                str = "STORED " + version + " " + fileID + " " + chunkNo + " " + CRLF + CRLF;
                break;
            case GETCHUNK:
                str = "GETCHUNK " + version + " " + fileID + " " + chunkNo + " " + CRLF + CRLF;
                break;
            case CHUNK:
                str = "CHUNK " + version + " " + fileID + " " + chunkNo + " " + CRLF + CRLF + body;
                break;
            case DELETE:
                str = "DELETE " + version + " " + fileID + " " + CRLF + CRLF;
            case REMOVED:
                str = "REMOVED " + version + " " + fileID + " " + chunkNo + " " + CRLF + CRLF;
            default:
                break;
        }
        return str;
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
}
