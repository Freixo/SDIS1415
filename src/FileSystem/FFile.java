/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package FileSystem;

import static FileSystem.UtilFunc.byteToString;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.MessageDigest;

/**
 *
 * @author Freixo
 */
public class FFile {

    public String Name;
    public String Version;
    public String FileId;
    public Chunk[] Chunks;
    public int Size;
    public static String[] units = {"B", "KB", "MB", "GB"};
    public int ReplicationDeg = 1;

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

            Chunks = new Chunk[file.available() / 64000 + 1];

            for (int i = 0; file.available() > 0; ++i) {
                Chunk ch;
                if (file.available() > 64000) {
                    ch = new Chunk(64000);
                } else {
                    ch = new Chunk(file.available());
                }

                Chunks[i] = ch;
                file.read(Chunks[i].byte_array);
            }

            file.close();
        }//File Not Found//File Not Found//File Not Found//File Not Found
        catch (Exception e) {
            Size = 0;
            System.err.println("File '" + fileName + "' not found");
            FillChunks();
        }
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
            for (int i = 0; i < Chunks.length; ++i) {
                FileOutputStream ofile = new FileOutputStream("Chunks/" + FileId + "." + i);
                ofile.write(Chunks[i].byte_array);
                ofile.close();
            }
        } catch (Exception e) {
            System.err.println("Couldn't create Chunks");
        }
    }

    public void ChunkstoFile() {
        try {
            FileOutputStream ofile = new FileOutputStream("Share/" + Name);
            for (int i = 0; i < Chunks.length; ++i) {
                ofile.write(Chunks[i].byte_array);
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
            Chunks = new Chunk[size];
            for (int i = 0; i < size; ++i) {
                try {
                    FileInputStream ifile = new FileInputStream("Chunks/" + FileId + "." + i);
                    Chunks[i] = new Chunk(ifile.available());
                    Size += ifile.available();
                    ifile.read(Chunks[i].byte_array);
                    ifile.close();
                } catch (Exception e) {
                    System.err.println("Couldn't open chunk no " + i);
                }
            }
        }
    }

    public void delete() {
        for (int i = 0; i < Chunks.length; ++i) {
            Chunks[i].delete();
        }

        Chunks = null;

    }

}
