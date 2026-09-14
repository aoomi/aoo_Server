/*
 * Decompiled with CFR 0.152.
 */
package BaseCommon;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

public final class ByteUtils {
    public static final short UNSIGNED_MAX_VALUE = 255;

    public static short toUnsigned(byte b) {
        return (short)(b < 0 ? 256 + b : b);
    }

    public static String toHexAscii(byte b) {
        StringWriter sw = new StringWriter(2);
        ByteUtils.addHexAscii(b, sw);
        return sw.toString();
    }

    public static String toHexAscii(byte[] bytes) {
        int len = bytes.length;
        StringWriter sw = new StringWriter(len * 2);
        int i = 0;
        while (i < len) {
            ByteUtils.addHexAscii(bytes[i], sw);
            ++i;
        }
        return sw.toString();
    }

    public static byte[] fromHexAscii(String s) throws NumberFormatException {
        try {
            int len = s.length();
            if (len % 2 != 0) {
                throw new NumberFormatException("Hex ascii must be exactly two digits per byte.");
            }
            int out_len = len / 2;
            byte[] out = new byte[out_len];
            int i = 0;
            StringReader sr = new StringReader(s);
            while (i < out_len) {
                int val = 16 * ByteUtils.fromHexDigit(sr.read()) + ByteUtils.fromHexDigit(sr.read());
                out[i++] = (byte)val;
            }
            return out;
        }
        catch (IOException e) {
            throw new InternalError("IOException reading from StringReader?!?!");
        }
    }

    static void addHexAscii(byte b, StringWriter sw) {
        short ub = ByteUtils.toUnsigned(b);
        int h1 = ub / 16;
        int h2 = ub % 16;
        sw.write(ByteUtils.toHexDigit(h1));
        sw.write(ByteUtils.toHexDigit(h2));
    }

    private static int fromHexDigit(int c) throws NumberFormatException {
        if (c >= 48 && c < 58) {
            return c - 48;
        }
        if (c >= 65 && c < 71) {
            return c - 55;
        }
        if (c >= 97 && c < 103) {
            return c - 87;
        }
        throw new NumberFormatException(String.valueOf(39 + c) + "' is not a valid hexadecimal digit.");
    }

    private static char toHexDigit(int h) {
        char out = h <= 9 ? (char)(h + 48) : (char)(h + 55);
        return out;
    }

    private ByteUtils() {
    }
}

