package FileSystem;

public class UtilFunc {

    public static String byteToString(byte[] _byte) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < _byte.length; ++i) {
            str.append(String.format("%02X", _byte[i]));
        }
        return str.toString();
    }
}
