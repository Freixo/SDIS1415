package FileSystem;

public class Chunk {

    public byte[] byte_array;

    public Chunk(int byteSize) {
        byte_array = new byte[byteSize];
    }

    void delete() {
        byte_array = null;
    }

}
