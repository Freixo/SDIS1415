/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package FileSystem;

import static FileSystem.UtilFunc.byteToString;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import Message.Message;

/**
 *
 * @author Freixo
 */
public class FFile {

    private String Name;
    private String Version;
    private String FileId;
    private List<Chunk> Chunks;
    private int Size;
    private static String[] units = {"B", "KB", "MB", "GB"};
    private int ReplicationDeg = 1;

    public FFile(String fileName, String version, int rep) {

        //File Found
        Name = fileName;
        Version = version;
        ReplicationDeg = rep;

        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
            md.update(Name.getBytes("ASCII"));
            FileId = byteToString(md.digest());
        } catch (Exception ex) {
            System.err.println("Error digesting message");
        }

        try {
            FileInputStream file = new FileInputStream("Share/" + Name);

            Size = file.available();

            Chunks = new ArrayList<Chunk>();//new Chunk[file.available() / 64000 + 1];

            for (int i = 0; file.available() > 0; ++i) {
                Chunk ch;
                if (file.available() > 64000) {
                    ch = new Chunk(i, 64000);
                } else {
                    ch = new Chunk(i, file.available());
                }

                Chunks.add(ch);
                file.read(Chunks.get(i).getBytes());
            }

            file.close();
        }//File Not Found//File Not Found//File Not Found//File Not Found
        catch (Exception e) {
            Size = 0;
            System.err.println("File '" + fileName + "' not found");
            FillChunks();
        }
    }

    public FFile(String fileID2, int degree, String version2, Chunk c) {
    	this.Chunks = new ArrayList<Chunk>();this.Chunks.add(c);
    	this.FileId = fileID2;
    	this.ReplicationDeg = degree;
    	this.Version = version2;
	}

	public void print() {
        String _size;
        int i = 0;
        while (Size > Math.pow(1024, i) && i < 3) {
            ++i;
        }
        double value = Size / Math.pow(1024, i - 1);
        _size = Double.toString((double) Math.round(value * 100) / 100) + " " + units[Math.max(i - 1, 0)];
        System.out.println("File Name: " + Name);
        System.out.println("Size: " + _size);
        System.out.println("ID: " + FileId);
    }

    public void FiletoChunks() {
        try {
            for (int i = 0; i < Chunks.size(); ++i) {
                FileOutputStream ofile = new FileOutputStream("Chunks/" + FileId + "." + i);
                ofile.write(Chunks.get(i).getBytes());
                ofile.close();
            }
        } catch (Exception e) {
            System.err.println("Couldn't create Chunks");
        }
    }

    public void ChunkstoFile() {
        try {
            FileOutputStream ofile = new FileOutputStream("Share/" + Name);
            for (int i = 0; i < Chunks.size(); ++i) {
                ofile.write(Chunks.get(i).getBytes());
            }
            ofile.close();
        } catch (Exception e) {
            System.err.println("Couldn't create File");
        }
    }

    public void FillChunks() {
        int size;

        for (size = 0; true; ++size) {
            try {
                FileInputStream ifile = new FileInputStream("Chunks/" + FileId + "." + size);
                ifile.close();
            } catch (Exception e) {
                break;
            }
        }
        if (size == 0) {
            System.err.println("Couldn't find Chunks");

        } else {
            Chunks = new ArrayList<Chunk>();//[size];
            for (int i = 0; i < size; ++i) {
                try {
                    FileInputStream ifile = new FileInputStream("Chunks/" + FileId + "." + i);
                    Chunks.add(new Chunk(i, ifile.available()));
                    Size += ifile.available();
                    ifile.read(Chunks.get(i).getBytes());
                    ifile.close();
                } catch (Exception e) {
                    System.err.println("Couldn't open chunk no " + i);
                }
            }
        }
    }

    public void delete() {
    	for (int i = 0; i < Chunks.size(); ++i) {
            File file = new File("Chunks/" + FileId + "." + String.valueOf(Chunks.get(i).getChunkNO()));
            file.delete();
        }
        Chunks = null;
    }

    public String getName(){
        return Name;
    }
    
    public String getVersion(){
        return Version;
    }
    
    public String getFileId(){
        return FileId;
    }
    
    public Chunk[] getChunks(){
        return (Chunk[]) Chunks.toArray();
    }
    
    public int getReplicationDeg(){
        return ReplicationDeg;
    }
    
    public int getSize(){
        return Size;
    }
    
    public void setReplicationDeg(int rd){
        ReplicationDeg = rd;
    }

	public boolean has(int chunkNO) {
		for (int i = 0; i < Chunks.size(); ++i) {
			if(Chunks.get(i).chunkNO == chunkNO)
				return true;
		}
		return false;
	}

	public byte[] getChunkBody(int chunkNO) {
		for( Chunk c : this.Chunks) {
			if( c.chunkNO == chunkNO)
				return c.getBytes();
		}
		return null;
	}

	public void addChunk(Chunk c) {
		this.Chunks.add(c);
	}
}
