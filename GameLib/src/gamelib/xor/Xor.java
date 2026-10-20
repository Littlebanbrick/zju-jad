package gamelib.xor;

public final class Xor {
    private Xor() {}

    private static final byte[] KEY = {
        (byte) 0x13,
        (byte) 0x67,
        (byte) 0x78,
        (byte) 0x91
    };

    public static byte[] xor(byte[] data) {
        byte[] output = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            output[i] = 
                (byte) (data[i] ^ KEY[i % KEY.length]);
        }
        return output;
    }
}
