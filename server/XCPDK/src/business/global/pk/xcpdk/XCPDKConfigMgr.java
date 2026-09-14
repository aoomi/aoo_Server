package business.global.pk.xcpdk;

import com.ddm.server.common.utils.Txt2Utils;
import com.aoo.bcg.common.config.LegacyConfigGuard;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/*
 * 跑得快 配置文件
 * @author zaf
 * */
public class XCPDKConfigMgr {
	public static final String fileName = "XCPDKConfig.txt";
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
	protected int God_Card ;
	protected ArrayList<Integer> Private_Card1;
	protected ArrayList<Integer> Private_Card2;
	protected ArrayList<Integer> Private_Card3;
	protected ArrayList<Integer> Private_Card4;
	protected int CardType ;

	public XCPDKConfigMgr(){
		this(resolveDefault());
	}

	XCPDKConfigMgr(Path configFile){
		Path file = configFile.toAbsolutePath().normalize();
		if (!Files.isRegularFile(file)) throw new IllegalStateException("XCPDK config not found: " + file);
		this.configMap = Txt2Utils.txt2Map(file.getParent().toString() + "/", file.getFileName().toString(), "GBK");
		for (String key : new String[]{"handleCard", "deleteCard", "addDoubleList", "backerPointList", "maxAddDoubleList", "Private_Card1", "Private_Card2", "Private_Card3", "Private_Card4"}) LegacyConfigGuard.required(configMap, "XCPDK", key);
		this.handleCard = Txt2Utils.String2ListInteger(this.configMap.get("handleCard"));
		this.deleteCard = Txt2Utils.String2Array(this.configMap.get("deleteCard"));
		this.jiPaiFen = Integer.valueOf(this.configMap.get("jiPaiFen"));
		this.paiDuoTongShu = Integer.valueOf(this.configMap.get("paiDuoTongShu"));
		this.guDingFen = Integer.valueOf(this.configMap.get("guDingFen"));
		this.aiOpenCard = LegacyConfigGuard.range(configMap, "XCPDK", "aiOpenCard", 0, 100);
		this.aiAddDouble = LegacyConfigGuard.range(configMap, "XCPDK", "aiAddDouble", 0, 100);
		this.robClosePointByGuDingFen = Integer.valueOf(this.configMap.get("robClosePointByGuDingFen"));
		this.robClosePointByNotGuDingFen = Integer.valueOf(this.configMap.get("robClosePointByNotGuDingFen"));
		this.robCloseAddDouble = Integer.valueOf(this.configMap.get("robCloseAddDouble"));
		this.addDoubleList = Txt2Utils.String2ListInteger(this.configMap.get("addDoubleList"));
		this.backerPointList = Txt2Utils.String2ListInteger(this.configMap.get("backerPointList"));
		this.maxAddDoubleList = Txt2Utils.String2ListInteger(this.configMap.get("maxAddDoubleList"));
		this.maxRoomAddDouble = Integer.valueOf(this.configMap.get("maxRoomAddDouble"));
		this.God_Card = LegacyConfigGuard.range(configMap, "XCPDK", "God_Card", 0, 1);
		this.Private_Card1 = Txt2Utils.String2ListInteger(this.configMap.get("Private_Card1"));
		this.Private_Card2 = Txt2Utils.String2ListInteger(this.configMap.get("Private_Card2"));
		this.Private_Card3 = Txt2Utils.String2ListInteger(this.configMap.get("Private_Card3"));
		this.Private_Card4 = Txt2Utils.String2ListInteger(this.configMap.get("Private_Card4"));
		this.CardType = this.configMap.containsKey("CardType")?Integer.valueOf(this.configMap.get("CardType")):1;
	}

	private static Path resolveDefault() {
		Path service = Path.of(filePath, fileName);
		return Files.isRegularFile(service) ? service : Path.of("server", "XCPDK", filePath, fileName);
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

	/**
	 * @return god_Card
	 */
	public boolean isGodCard() {
		return God_Card == 1;
	}
	/**
	 * @return private_Card1
	 */
	public ArrayList<Integer> getPrivate_Card1() {
		return Private_Card1;
	}
	/**
	 * @return private_Card2
	 */
	public ArrayList<Integer> getPrivate_Card2() {
		return Private_Card2;
	}
	/**
	 * @return private_Card3
	 */
	public ArrayList<Integer> getPrivate_Card3() {
		return Private_Card3;
	}
	/**
	 * @return private_Card4
	 */
	public ArrayList<Integer> getPrivate_Card4() {
		return Private_Card4;
	}

	/**
	 * 是否是十进制
	 * @return
	 */
	public boolean isDecimalism() {
		return CardType==2;
	}
}
