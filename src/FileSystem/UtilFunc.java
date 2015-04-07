package FileSystem;

public class UtilFunc {

    public static String byteToString(byte[] _byte) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < _byte.length; ++i) {
            str.append(String.format("%02X", _byte[i]));
        }
        return str.toString();
    }

    public static byte[] stringToByte(String str) {
        byte[] data = new byte[str.length()];

        for (int i = 0; i < str.length(); i += 2) {
            data[i / 2] = (byte) ((Character.digit(str.charAt(i), 16) << 4)
                    + Character.digit(str.charAt(i + 1), 16));
        }
        return data;
    }
}
