import business.global.pk.teenpatti.TEENPATTIRoomEnum;

/**
 * @author zhujianming
 * @date 2021-07-21 10:52
 */
public class RefTeenpattiType {
    public int id;
    public String typeName;
    public int combat;
    public int cardValue1;
    public int cardValue2;
    public int cardValue3;
    public TEENPATTIRoomEnum.CardType type;

    @Override
    public String toString() {
        return "RefTeenpattiType{" +
                "id=" + id +
                ", typeName='" + typeName + '\'' +
                ", combat=" + combat +
                ", cardValue1=" + cardValue1 +
                ", cardValue2=" + cardValue2 +
                ", cardValue3=" + cardValue3 +
                ", type=" + type +
                '}';
    }

    public RefTeenpattiType(int id, String typeName, TEENPATTIRoomEnum.CardType type, int combat, int cardValue1, int cardValue2, int cardValue3) {
        this.id = id;
        this.typeName = typeName;
        this.type = type;
        this.combat = combat;
        this.cardValue1 = cardValue1;
        this.cardValue2 = cardValue2;
        this.cardValue3 = cardValue3;
    }

}
