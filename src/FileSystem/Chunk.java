package FileSystem;

public class Chunk {

    private byte[] byte_array;
	public int chunkNO;//TODO ?

    public Chunk(int byteSize) {
        byte_array = new byte[byteSize];
    }

    public void delete() {
        byte_array = null;
    }
    
    public byte[] getBytes(){
        return byte_array;
    }

}
