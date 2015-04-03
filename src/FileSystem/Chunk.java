package FileSystem;

public class Chunk {

    private byte[] byte_array;
	public int chunkNO;//TODO ?

    public Chunk(int num, int byteSize) {
        byte_array = new byte[byteSize];
    }

    public Chunk(int number, byte[] body) {
		this.byte_array = body;
		this.chunkNO = number;
	}

	public void delete() {
        byte_array = null;
    }
    
    public byte[] getBytes(){
        return byte_array;
    }

}
