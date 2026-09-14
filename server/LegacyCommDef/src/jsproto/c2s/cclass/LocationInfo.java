package jsproto.c2s.cclass;

import java.io.Serializable;

public class LocationInfo implements Serializable {
    private int pos = -1;
    private double Latitude; //纬度
    private double Longitude; //精度
    private boolean isGetError = true;//获取是否失败
    private String Address = "";//地址
    private long pid;
    private int updateTime = 0;

    public static LocationInfo make(int pos, double Latitude, double Longitude, boolean isGetError, String Address, long pid, int updateTime) {
        LocationInfo ret = new LocationInfo();
        ret.pos = pos;
        ret.Latitude = Latitude;
        ret.Longitude = Longitude;
        ret.isGetError = isGetError;
        ret.Address = Address;
        ret.pid = pid;
        ret.updateTime = updateTime;
        return ret;
    }

    public void clear(int updateTime) {
        this.Latitude = 0D;
        this.Longitude = 0D;
        this.isGetError = true;
        this.Address = "";
        this.updateTime = updateTime;
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    public double getLatitude() {
        return Latitude;
    }

    public void setLatitude(double latitude) {
        Latitude = latitude;
    }

    public double getLongitude() {
        return Longitude;
    }

    public void setLongitude(double longitude) {
        Longitude = longitude;
    }

    public boolean isGetError() {
        return isGetError;
    }

    public void setGetError(boolean isGetError) {
        this.isGetError = isGetError;
    }

    public String getAddress() {
        return Address;
    }

    public void setAddress(String address) {
        Address = address;
    }

    public long getPid() {
        return pid;
    }

    public void setPid(long pid) {
        this.pid = pid;
    }

    public int getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(int updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return "pos=" + pos +
                ", Latitude=" + Latitude +
                ", Longitude=" + Longitude +
                ", isGetError=" + isGetError +
                ", Address='" + Address + '\'' +
                ", pid=" + pid +
                ", updateTime=" + updateTime;
    }
}
