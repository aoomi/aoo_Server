package business.global.pk.zypk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.ddm.server.common.utils.Txt2Utils;

/*
 * 跑得快 配置文件
 * @author zaf
 * */
public class ZYPKConfigMgr {
	public static final String fileName = "PDKConfig.txt";
	public static final String filePath = "conf/";
	private Map<String, String> configMap = new HashMap<String, String>();
	private ArrayList<Integer> handleCard; //底分
	private ArrayList<ArrayList<Integer>> deleteCard; //底注
	private int jiPaiFen = 0;
	private int paiDuoTongShu = 0;
	private int guDingFen = 0;
	private int aiOpenCard = 50;
	private int aiAddDouble = 50;
	private int robClosePointByNotGuDingFen = 50;
	private int robClosePointByGuDingFen = 50;
	private int robCloseAddDouble = 50;
	private ArrayList<Integer> addDoubleList; 
	private ArrayList<Integer> backerPointList; 
	private ArrayList<Integer> maxAddDoubleList; 
	private int maxRoomAddDouble; 
	public ZYPKConfigMgr(){
		this.configMap = Txt2Utils.txt2Map(filePath, fileName, "GBK");
		this.handleCard = Txt2Utils.String2ListInteger(this.configMap.get("handleCard"));
		this.deleteCard = Txt2Utils.String2Array(this.configMap.get("deleteCard"));
		this.jiPaiFen = Integer.valueOf(this.configMap.get("jiPaiFen"));
		this.paiDuoTongShu = Integer.valueOf(this.configMap.get("paiDuoTongShu"));
		this.guDingFen = Integer.valueOf(this.configMap.get("guDingFen"));
		this.aiOpenCard = Integer.valueOf(this.configMap.get("aiOpenCard"));
		this.aiAddDouble = Integer.valueOf(this.configMap.get("aiAddDouble"));
		this.robClosePointByGuDingFen = Integer.valueOf(this.configMap.get("robClosePointByGuDingFen"));
		this.robClosePointByNotGuDingFen = Integer.valueOf(this.configMap.get("robClosePointByNotGuDingFen"));
		this.robCloseAddDouble = Integer.valueOf(this.configMap.get("robCloseAddDouble"));
		this.addDoubleList = Txt2Utils.String2ListInteger(this.configMap.get("addDoubleList"));
		this.backerPointList = Txt2Utils.String2ListInteger(this.configMap.get("backerPointList"));
		this.maxAddDoubleList = Txt2Utils.String2ListInteger(this.configMap.get("maxAddDoubleList"));
		this.maxRoomAddDouble = Integer.valueOf(this.configMap.get("maxRoomAddDouble"));
	}
	/**
	 * @return handleCard
	 */
	public ArrayList<Integer> getHandleCard() {
		return handleCard;
	}
	/**
	 * @return deleteCard
	 */
	public ArrayList<ArrayList<Integer>> getDeleteCard() {
		return deleteCard;
	}
	/**
	 * @return jiPaiFen
	 */
	public int getJiPaiFen() {
		return jiPaiFen;
	}
	/**
	 * @return paiDuoTongShu
	 */
	public int getPaiDuoTongShu() {
		return paiDuoTongShu;
	}
	/**
	 * @return guDingFen
	 */
	public int getGuDingFen() {
		return guDingFen;
	}
	/**
	 * @return aiOpenCard
	 */
	public int getAiOpenCard() {
		return aiOpenCard;
	}
	/**
	 * @return aiAddDouble
	 */
	public int getAiAddDouble() {
		return aiAddDouble;
	}
	/**
	 * @return addDoubleList
	 */
	public ArrayList<Integer> getAddDoubleList() {
		return addDoubleList;
	}
	/**
	 * @return robClosePointByNotGuDingFen
	 */
	public int getRobClosePointByNotGuDingFen() {
		return robClosePointByNotGuDingFen;
	}
	/**
	 * @return robClosePointByGuDingFen
	 */
	public int getRobClosePointByGuDingFen() {
		return robClosePointByGuDingFen;
	}
	/**
	 * @return robCloseAddDouble
	 */
	public int getRobCloseAddDouble() {
		return robCloseAddDouble;
	}
	/**
	 * @return maxRoomAddDouble
	 */
	public int getMaxRoomAddDouble() {
		return maxRoomAddDouble;
	}
	/**
	 * @return backerPointList
	 */
	public ArrayList<Integer> getBackerPointList() {
		return backerPointList;
	}
	/**
	 * @return maxAddDoubleList
	 */
	public ArrayList<Integer> getMaxAddDoubleList() {
		return maxAddDoubleList;
	}
}
