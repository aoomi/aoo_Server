package com.ddm.server.common;

public class Scid {

    public static long getScid(int sid, int cid) {
        if (cid == 0) {
            return 0L;
        }
        return sid * 1000_0000_0000L + cid % 1000_0000_0000L;
    }

    public static int getCid(long scid) {
        return (int) (scid % 1000_0000_0000L);
    }

    public static int getSid(long scid) {
        return (int) (scid / 1000_0000_0000L);
    }

    public static long getSgid(int sid, int gid) {
        if (gid == 0) {
            return 0L;
        }
        return sid * 1000_0000_0000L + gid % 1000_0000_0000L;
    }

    public static int getSidBySgid(long sgid) {
        return (int) (sgid / 1000_0000_0000L);
    }

    public static int getGid(long sgid) {
        return (int) (sgid % 1000_0000_0000L);
    }
}
