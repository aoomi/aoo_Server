package business.global.mj.extbussiness.dto;


/**
 * 票分拓展
 */
public class StandardMJWaitingExInfo {
    public int pos = -1;
    public int piao = -1;
//    public int pao = -1;
//    public int bao = -1;
//    public int mai = -1;

    public void setPiao(Integer piao) {
        this.piao = piao;
    }

//    public void setPao(Integer pao) {
//        this.pao = pao;
//    }
//
//    public void setBao(Integer bao) {
//        this.bao = bao;
//    }
//
//    public void setMai(Integer mai) {
//        this.mai = mai;
//    }


    public int getPiao() {
        return piao;
    }
//
//    public int getRealPao() {
//        return piao>0?2:1;
//    }

//    public int getPao() {
//        return pao;
//    }

//
//    public int getBao() {
//        return bao;
//    }
//
//    public int getMai() {
//        return mai;
//    }

    public StandardMJWaitingExInfo(int pos) {
        this.pos = pos;
    }
}
