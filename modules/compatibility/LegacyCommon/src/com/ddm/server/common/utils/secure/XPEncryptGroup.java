package com.ddm.server.common.utils.secure;

/**
 * @author pt
 */
public class XPEncryptGroup {

    private int[] keys = new int[]{0x234F240a, 0x165452fb, 0xf323512a, 0xb020c641, 0x5876ff34};
    public static XPEncryptGroup instance;

    public XPEncryptGroup(int key) {
        keys[0] = key;
    }

    public long encode(long data) {
        Pair p = new Pair(data);
        for (int i = 0; i < keys.length; i++) {
            p.round(keys[i]);
        }
        p.exchange();

        return p.getValue();
    }

    public long decode(long data) {
        Pair p = new Pair(data);
        for (int i = keys.length - 1; i >= 0; i--) {
            p.round(keys[i]);
        }
        p.exchange();
        return p.getValue();
    }

    public long decode(int _a, int _b) {
        Pair p = new Pair(_a, _b);

        for (int i = keys.length - 1; i >= 0; i--) {
            p.round(keys[i]);
        }
        p.exchange();
        return p.getValue();
    }

    class Pair {
        int a;
        int b;

        public Pair(int _a, int _b) {
            this.a = _a;
            this.b = _b;
        }

        public Pair(long data) {
            int a = (int) ((data & 0xffffffff00000000L) >> 32), b = (int) (data & 0xFFFFFFFF);
            this.a = a;
            this.b = b;
        }

        public void exchange() {
            int t = a;
            a = b;
            b = t;
        }

        public void round(int key) {
            int t = a;
            a = b ^ secretMap(key, a);
            b = t;
        }

        public int secretMap(int x, int y) {
            return (x * y) ^ 0x2bfbfbfb;
        }

        @Override
        public String toString() {
            return String.format("%08X%08X", a, b);
        }

        public long getValue() {
            long c = (a * 1L) << 32;
            long d = c | ((b * 1L) & 0xFFFFFFFFL);
            return d;
        }
    }

}
