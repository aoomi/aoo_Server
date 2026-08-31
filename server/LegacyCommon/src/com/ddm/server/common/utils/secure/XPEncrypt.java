package com.ddm.server.common.utils.secure;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;

public class XPEncrypt {

    public static final int ALIGN = Long.BYTES; // bytes

    public static final int MAGIC = 0x45677654;

    private byte[] eData;
    private ByteBuffer eBuffer;
    private int eSize;
    private int key;

    public XPEncrypt(int key) {
        this.key = key;
    }

    public ByteBuffer encrypt(ByteBuffer data) {
        return encrypt(data.array());
    }

    public ByteBuffer encrypt(byte[] data) {
        if (data == null) {
            return null;
        }

        this.eData = data;
        paddingAndMakeBuffer();
        return doEncrypt();
    }

    public ByteBuffer decrypt(byte[] data) {
        ByteBuffer dst = ByteBuffer.allocate(data.length);
        ByteBuffer src = ByteBuffer.wrap(data);
        XPEncryptGroup group = new XPEncryptGroup(key);

        if (data.length % ALIGN != 0) {
            return null;
        }

        while (src.hasRemaining()) {
            dst.putLong(group.decode(src.getLong()));
        }

        int dataSize = dst.getInt(data.length - 12);
        // verify checksum
        int sum = 0;
        dst.rewind();
        while (dst.remaining() > 8) {
            sum += dst.getInt();
        }

        if (dst.getInt(data.length - 8) != MAGIC || dst.getInt(data.length - 4) != sum) {
            // very bad
            // System.err.println("bad encoding");
            return null;
        }

        dst.limit(dataSize);
        dst.rewind();
        return dst;
    }

    public ByteArrayInputStream decrypt(ByteArrayInputStream input) throws Exception {
        int length = input.available();

        if (length % ALIGN != 0) {
            throw new Exception("消息字节流错误，非刚好8位字节流");
        }

        ByteBuffer dst = ByteBuffer.allocate(length);
        XPEncryptGroup group = new XPEncryptGroup(key);

        byte[] ibuff = new byte[Integer.BYTES];
        while (input.available() > 0) {
            input.read(ibuff);
            int hv = toInt(ibuff); // 高位

            input.read(ibuff);
            int lv = toInt(ibuff); // 低位
            dst.putLong(group.decode(hv, lv));
        }

        int dataSize = dst.getInt(length - 12);
        // verify checksum
        int sum = 0;
        dst.rewind();
        while (dst.remaining() > 8) {
            sum += dst.getInt();
        }

        if (dst.getInt(length - 8) != MAGIC || dst.getInt(length - 4) != sum) {
            throw new Exception("消息字节流错误，check sum错误");// very bad System.err.println("bad encoding");
        }

        dst.limit(dataSize);
        dst.rewind();
        return new ByteArrayInputStream(dst.array(), 0, dataSize);
    }

    private ByteBuffer doEncrypt() {
        ByteBuffer ret = ByteBuffer.allocate(eSize);
        XPEncryptGroup group = new XPEncryptGroup(key);
        this.eBuffer.rewind();
        while (this.eBuffer.hasRemaining()) {
            ret.putLong(group.encode(this.eBuffer.getLong()));
        }
        ret.rewind();
        return ret;
    }

    private void paddingAndMakeBuffer() {
        // this.eData should not be null
        // skip null check for speed
        int dataSize = this.eData.length;
        int append = 0;
        int zeros;

        append = 11 - (((dataSize & 7) + 3) & 7);
        zeros = append - 4;

        eSize = dataSize + append + 8; // 8 bytes for checksum
        eBuffer = ByteBuffer.allocate(eSize);
        eBuffer.put(this.eData); // prevent copy ?
        // put zeros, 7 at most
        for (int i = 0; i < zeros; i++) {
            eBuffer.put((byte) 0);
        }

        eBuffer.putInt(dataSize);

        // make checksum
        eBuffer.rewind();
        int checksum = 0;
        while (eBuffer.remaining() > 8) {
            checksum += eBuffer.getInt();
        }
        eBuffer.putInt(eSize - 8, MAGIC);
        eBuffer.putInt(eSize - 4, checksum);
    }

    private int toInt(byte[] src) {
        int value;
        value = (((int) (src[3] & 0xFF)) | //
                ((int) (src[2] & 0xFF) << 8) | //
                ((int) (src[1] & 0xFF) << 16) | //
                ((int) (src[0] & 0xFF) << 24));
        return value;
    }
}
