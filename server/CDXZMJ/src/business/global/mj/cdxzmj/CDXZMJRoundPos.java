package business.global.mj.cdxzmj;

import business.global.mj.AbsMJSetRound;
import business.global.mj.template.xueZhan.MJTemplateXueZhanRoundPos;
	
/**	
 * 一个round回合中，可能同时等待多个pos进行操作，eg:抢杠胡	
 *	
 * @author Administrator	
 */	
public class CDXZMJRoundPos extends MJTemplateXueZhanRoundPos {
	
    public CDXZMJRoundPos(AbsMJSetRound round, int opPos) {	
        super(round, opPos);	
    }
    public CDXZMJSetPos getPos() {	
        return (CDXZMJSetPos) super.getPos();	
    }	
}		
