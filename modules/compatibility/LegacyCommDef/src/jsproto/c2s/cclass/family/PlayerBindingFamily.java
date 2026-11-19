package jsproto.c2s.cclass.family;

public class PlayerBindingFamily {
    private long familyId;
    private String familyName;
    private String referer;
    private String refererName;

    public PlayerBindingFamily() {
        super();
    }

    public PlayerBindingFamily(long familyId, String familyName) {
        super();
        this.familyId = familyId;
        this.familyName = familyName;
    }

    public PlayerBindingFamily(long familyId, String familyName, String referer,
                               String refererName) {
        super();
        this.familyId = familyId;
        this.familyName = familyName;
        this.referer = referer;
        this.refererName = refererName;
    }

    public long getFamilyId() {
        return familyId;
    }

    public void setFamilyId(long familyId) {
        this.familyId = familyId;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public String getReferer() {
        return referer;
    }

    public void setReferer(String referer) {
        this.referer = referer;
    }

    public String getRefererName() {
        return refererName;
    }

    public void setRefererName(String refererName) {
        this.refererName = refererName;
    }


}
