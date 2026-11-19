package business.xcpdk.c2s.iclass;

public class SXCPDK_UserInfo {
    private String name = "";
    private long pid = 0;
    private long totalPoint = 0;

    public SXCPDK_UserInfo(String name, long pid, long totalPoint) {
        this.name = name;
        this.pid = pid;
        this.totalPoint = totalPoint;
    }
}
