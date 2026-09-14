package business.global.pk.zjh;

import java.util.ArrayList;

import jsproto.c2s.cclass.pk.BasePocker;
import jsproto.c2s.cclass.pk.BasePockerLogic;

public class ZJHGameLogic {
	public final static int MAX_COUNT = 3;
	//扑克类型
	public final static int ZJH_VALUE		=0;										//混合牌型
	
	public final static int ZJH_DUIZI		=101;									//对子
	public final static int ZJH_SHUNZI		=102;									//顺子
	public final static int ZJH_JINHUA		=103;									//金华
	public final static int ZJH_SHUNJIN		=104;									//顺金
	public final static int ZJH_PAOZI      	=105;                                	//豹子
	public final static int ZJH_TESHU      	=106;                                	//特殊
	
	public final static byte especialCardList[] = {0x02,0x03,0x05};
	
	public final static byte especialShunZiList[] = {0x0E,0x02,0x03};
	
	//获取牌值
	public static int GetCardValue(Integer card){
		return BasePocker.getCardValue(card);
	}
	
	//获取牌颜色
	public static int GetCardColor(Integer card){
		return BasePocker.getCardColor(card);
	}
	
	//获取类型
	@SuppressWarnings({ "unchecked" })
	public static int GetCardType( ArrayList<Integer> list)
	{
		if(list.size() != MAX_COUNT) return ZJH_VALUE;
		//豹子
		byte bSameCount = 0;
		ArrayList<Integer> tempCardData = (ArrayList<Integer>) list.clone();
				
		if(isEspecialCard(tempCardData, especialCardList) && !isTongSe(tempCardData, MAX_COUNT)) return ZJH_TESHU;
		
		

		tempCardData.sort(BasePockerLogic.sorterBigToSmallNotTrump);
		int bSecondValue = GetCardValue(tempCardData.get(MAX_COUNT/2));
		for(byte i=0;i<MAX_COUNT;i++)
		{
			if(bSecondValue == GetCardValue(tempCardData.get(i)))
			{
				bSameCount++;
			}
		}
		if(bSameCount==MAX_COUNT)return ZJH_PAOZI;
		
		if(isShunZi(tempCardData, MAX_COUNT) && isTongSe(tempCardData, MAX_COUNT)) return ZJH_SHUNJIN;
		if(isTongSe(tempCardData, MAX_COUNT)) return ZJH_JINHUA;
		if(isShunZi(tempCardData, MAX_COUNT) ) return ZJH_SHUNZI;
		if(bSameCount == 2) return ZJH_DUIZI;
		
		for(byte i=0;i<MAX_COUNT;i++)
		{
			int nextIndex = (i+1)%MAX_COUNT;
			if(GetCardValue(tempCardData.get(nextIndex)) + 1  == GetCardValue(tempCardData.get(i))  && 
					GetCardColor(tempCardData.get(nextIndex)) + 1  == GetCardColor(tempCardData.get(i)) )
			{
				bSameCount++;
			}
		}
		if(bSameCount==MAX_COUNT)return ZJH_SHUNJIN;

		return ZJH_VALUE;
	}


	//逻辑数值
	public static  int GetCardLogicValue(int cbCardData)
	{
		//扑克属性
		int bCardValue=GetCardValue(cbCardData);

		//转换数值
		return (bCardValue>10)?(10):bCardValue;
	}

	
	@SuppressWarnings("unchecked")
	public static  boolean CompareCard( ArrayList<Integer> cbLeftData,  ArrayList<Integer> cbRightData)
	{
		if (cbLeftData.size() != MAX_COUNT || cbRightData.size() != MAX_COUNT)
		{
			return false;
		}
		//这里先进行倍数比较
		int leftType = ZJHGameLogic.GetCardType(cbLeftData);
		int RightType = ZJHGameLogic.GetCardType(cbRightData);

		if((leftType == ZJH_TESHU && RightType == ZJH_PAOZI) || (leftType == ZJH_PAOZI && RightType == ZJH_TESHU)){
			return leftType > RightType;
		}
		if(leftType == ZJH_TESHU ) {
			leftType = ZJH_VALUE;
		}
		
		if(RightType == ZJH_TESHU  ) {
			RightType = ZJH_VALUE;
		}
		
		
		if(leftType!=RightType)
			return leftType > RightType;

		ArrayList<Integer> tempLeftData = (ArrayList<Integer>) cbLeftData.clone() ;
		ArrayList<Integer> tempRightData = (ArrayList<Integer>) cbRightData.clone();
		
		tempLeftData.sort(BasePockerLogic.sorterBigToSmallNotTrump);
		tempRightData.sort(BasePockerLogic.sorterBigToSmallNotTrump);
		
		if(leftType == ZJH_DUIZI){
			int leftDuiZiCard = tempLeftData.get(1);
	    	int rightDuiZiCard = tempRightData.get(1);
	    	int cbLeftValue= ZJHGameLogic.GetCardValue(leftDuiZiCard);
	    	int cbRightValue= ZJHGameLogic.GetCardValue(rightDuiZiCard);
			if( cbLeftValue != cbRightValue ) {
				return cbLeftValue > cbRightValue;
			}
			int leftOtherCard = ZJHGameLogic.GetCardValue(tempLeftData.get(1)) == ZJHGameLogic.GetCardValue(tempLeftData.get(0)) ? 
					ZJHGameLogic.GetCardValue(tempLeftData.get(2)) : ZJHGameLogic.GetCardValue(tempLeftData.get(0));
			int rightOtherCard = ZJHGameLogic.GetCardValue(tempRightData.get(1)) == ZJHGameLogic.GetCardValue(tempRightData.get(0)) ? 
							ZJHGameLogic.GetCardValue(tempRightData.get(2)) : ZJHGameLogic.GetCardValue(tempRightData.get(0));
			cbLeftValue= ZJHGameLogic.GetCardValue(leftOtherCard);
			cbRightValue= ZJHGameLogic.GetCardValue(rightOtherCard);
			if( cbLeftValue != cbRightValue ) {
				return cbLeftValue > cbRightValue;
			}
			return false;
	    }
		
		int i = 0;
		for (; i < MAX_COUNT; i++) {
			int tempLeftCard = isEspecialCard(tempLeftData, especialShunZiList) ? tempLeftData.get((1 + i)%MAX_COUNT): tempLeftData.get(i);
			int tempRigthCard = isEspecialCard(tempRightData, especialShunZiList) ? tempRightData.get((1 + i)%MAX_COUNT): tempRightData.get(i);
			int cbLeftMaxValue= ZJHGameLogic.GetCardValue(tempLeftCard);
			int cbRightMaxValue= ZJHGameLogic.GetCardValue(tempRigthCard);
			if( cbLeftMaxValue != cbRightMaxValue ) {
				return cbLeftMaxValue > cbRightMaxValue;
			}
		}
		return false;
		
//		byte tempLeftCard = isEspecialCard(tempLeftData, especialShunZiList) ? tempLeftData.get(1): tempLeftData.get(0);
//		byte tempRigthCard = isEspecialCard(tempRightData, especialShunZiList) ? tempRightData.get(1): tempRightData.get(0);
//
//		byte cbLeftMaxValue= ZJHGameLogic.GetCardValue(tempLeftCard);
//		byte cbRightMaxValue= ZJHGameLogic.GetCardValue(tempRigthCard);
//		if( cbLeftMaxValue != cbRightMaxValue ) {
//			return cbLeftMaxValue > cbRightMaxValue;
//		}
//		//比较颜色
//		return GetCardColor(tempLeftCard) > GetCardColor(tempRigthCard);
	}
	
	
	//是否有某一张牌
	public static boolean haveCard(ArrayList<Integer> list, byte card){
		for (Integer byte1 : list) {
			if(BasePocker.getCardValue(card) == BasePocker.getCardValue(byte1))
				return true;
		}
		return false;
	}
	
	//特殊牌
	public static boolean isEspecialCard(ArrayList<Integer> list, byte especialList[]) {
		if (list.size() != MAX_COUNT ) {
			return false;
		}
		if (haveCard(list,especialList[0]) && haveCard(list,especialList[1]) && haveCard(list,especialList[2]) ) {
			return true;
		}
		return false;
	}
		
	//是否是顺子
	public static boolean isShunZi(ArrayList<Integer> list, int maxCount){
		if (isEspecialCard(list, especialShunZiList)) {
			return true;
		}
		int bSameCount = 1;
		for(byte i=0;i<maxCount-1;i++)
		{
			int nextIndex = (i+1)%maxCount;
			if( Math.abs( GetCardValue(list.get(nextIndex)) - GetCardValue(list.get(i)) ) == 1   )
			{
				bSameCount++;
			}
		}
		if(bSameCount==maxCount)return true;
		return false;
	}
	//是否是同色
	public static boolean isTongSe(ArrayList<Integer> list, int maxCount){
		int bSameCount = 1;
		for(byte i=0;i<maxCount-1;i++)
		{
			int nextIndex = (i+1)%maxCount;
			if(	GetCardColor(list.get(nextIndex))   == GetCardColor(list.get(i)) )
			{
				bSameCount++;
			}
		}
		if(bSameCount==maxCount)return true;
		return false;
	}
}
