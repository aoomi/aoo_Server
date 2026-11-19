package jsproto.c2s.cclass.pk;

public class Victory {
    private int pos;
    private int num;


    public Victory() {
        super();
    }

    public Victory(int pos, int num) {
        super();
        this.pos = pos;
        this.num = num;
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }

    @Override
    public String toString() {
        return "Victory [pos=" + pos + ", num=" + num + "]";
    }


}
