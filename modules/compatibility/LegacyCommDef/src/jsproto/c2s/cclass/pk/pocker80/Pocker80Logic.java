package jsproto.c2s.cclass.pk.pocker80;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Pocker80Logic extends Pocker80 {
	//排序 大到小
	public static Comparator<Integer> sorterBigToSmallNotTrump = (left, right) -> {
		int v1 = getCardValue( left );
		int v2 = getCardValue( right );
		if (v1 == v2)
		{
			return getCardColor( right ) - getCardColor( left ) ;
		}
		return  v2 -  v1;
	};
	
	//排序 小到大
	public static Comparator<Integer> sorterSmallToBigNotTrump = (left, right) -> {
		int v1 = getCardValue( left );
		int v2 = getCardValue( right );
		if (v1 == v2)
		{
			return getCardColor( left ) - getCardColor( right ) ;
		}
		return  v1 -  v2;
	};
	
	//排序 小到大
	public static Comparator<Integer> sorterSmallToBigHaveTrump = (left, right) -> {
		if (Pocker80Logic.getCardColor(left) == Pocker80Logic.getCardColor(right)) {
			return Pocker80Logic.getCardValue(left) - Pocker80Logic.getCardValue(right);
		} else {
			if (Pocker80Logic.getCardColor(left) == Pocker80ColorType.POCKER_COLOR_TYPE_TRUMP.value() || Pocker80ColorType.POCKER_COLOR_TYPE_TRUMP.value() == Pocker80Logic.getCardColor(right)) {
				return Pocker80Logic.getCardColor(left) - Pocker80Logic.getCardColor(right);
			} else {
				return Pocker80Logic.getCardValue(left) - Pocker80Logic.getCardValue(right);
			}
		}
	};
	
	//排序 大到小
	public static Comparator<Integer> sorterBigToSmallHaveTrump = (left, right) -> {
		if (Pocker80Logic.getCardColor(left) == Pocker80Logic.getCardColor(right)) {
			return Pocker80Logic.getCardValue(right) - Pocker80Logic.getCardValue(left);
		} else {
			if (Pocker80Logic.getCardColor(left) == Pocker80ColorType.POCKER_COLOR_TYPE_TRUMP.value() || Pocker80ColorType.POCKER_COLOR_TYPE_TRUMP.value() == Pocker80Logic.getCardColor(right)) {
				return Pocker80Logic.getCardColor(right) - Pocker80Logic.getCardColor(left);
			} else {
				return Pocker80Logic.getCardValue(right) - Pocker80Logic.getCardValue(left);
			}
		}
	};
	
	/**
	 *  pockerType 获取对子 获取三条 获取四张一样 （炸弹）
	 * inList 传入数组
	 * outList 返回的对子数组
	 *return 数量
	*/
	public static int  getSameCardByType(ArrayList< ArrayList<Integer> > outList, ArrayList<Integer> inList, Pocker80ValueType pockerType){
		int count  = 0;

		ArrayList<Integer> valueList = sort(inList, false);
		for(int i = 0 ; i < valueList.size(); i++){
			ArrayList<Integer> temp = new ArrayList<Integer>(4);
			int tmp = valueList.get(i);
			//获取对子
			if( (i+1 < valueList.size() )
					&& Pocker80ValueType.POCKER_VALUE_TYPE_SUB == pockerType
					&& getCardValue(tmp) == getCardValue(valueList.get(i+1))){
				temp.add(tmp);
				temp.add(valueList.get(i+1));
				outList.add(new ArrayList<>(temp));
				temp.clear();
				i++;
				count++;
			//获取三条
			}else if( (i+2 < valueList.size() )
					&& (Pocker80ValueType.POCKER_VALUE_TYPE_THREE == pockerType )
					&& (getCardValue(tmp) == getCardValue(valueList.get(i+1)))
					&& (getCardValue(tmp) == getCardValue(valueList.get(i+2)))){
				temp.add(tmp);
				temp.add(valueList.get(i+1));
				temp.add(valueList.get(i+2));
				outList.add(new ArrayList<>(temp));
				temp.clear();
				i += 2;
				count++;
			//获取四张一样 （炸弹）
			}else if( (i+3 < valueList.size() )
					&&  (Pocker80ValueType.POCKER_VALUE_TYPE_BOMB == pockerType )){
				temp.add(tmp);
				while (i+1 < valueList.size() &&  getCardValue(tmp) == getCardValue(valueList.get(i+1))) {
					temp.add(valueList.get(i+1));
					i++;
				}
				if (temp.size() >= Pocker80ValueType.POCKER_VALUE_TYPE_BOMB.value() + 1) {
					outList.add(new ArrayList<>(temp));
					count++;
				}
				temp.clear();
			}
		}

		return count;
	}

	/**
	 *  pockerType 获取对子 获取三条 获取四张一样 （炸弹）
	 * inList 传入数组
	 * outList 返回的对子数组
	 *return 数量
	*/
	public static int  getSameCardByTypeEx(ArrayList< ArrayList<Integer> > outList, ArrayList<Integer> inList, Pocker80ValueType pockerType){
		int count  = 0;

		Map<Integer, List<Integer>> valueMap = inList.stream()
				.collect(Collectors.groupingBy(p -> Pocker80.getCardValueEx(p)));
		for (Map.Entry<Integer, List<Integer>> mEntry : valueMap.entrySet()) {
			int tempCount = mEntry.getValue().size();
			//获取单张
			if ((Pocker80ValueType.POCKER_VALUE_TYPE_SINGLE == pockerType&& tempCount == Pocker80ValueType.POCKER_VALUE_TYPE_SINGLE.value() +1)||
					(Pocker80ValueType.POCKER_VALUE_TYPE_SUB == pockerType&& tempCount == Pocker80ValueType.POCKER_VALUE_TYPE_SUB.value() +1)||
					((Pocker80ValueType.POCKER_VALUE_TYPE_THREE == pockerType )&&tempCount == Pocker80ValueType.POCKER_VALUE_TYPE_THREE.value() +1 )||
					(tempCount >= Pocker80ValueType.POCKER_VALUE_TYPE_BOMB.value() +1&& (Pocker80ValueType.POCKER_VALUE_TYPE_BOMB == pockerType )) ){

				outList.add(new ArrayList<>(mEntry.getValue()));
				count++;
			}
		}
		return count;
	}
	
	
	/**
	 *  pockerType 获取对子 获取三条 获取四张一样 （炸弹）
	 * inList 传入数组
	 * outList 返回的对子数组
	 *return 数量
	*/
	public static int  getSameCardByType(ArrayList< ArrayList<Integer> > outList, ArrayList<Integer> inList){
		Map<Integer, List<Integer>> inCardMap =  inList.stream().collect(Collectors.groupingBy(p -> Pocker80.getCardValueEx(p)));
		int count = 0;
		for (Map.Entry<Integer, List<Integer>> m: inCardMap.entrySet()) {
			count++;
			outList.add(new ArrayList<>(m.getValue()));
		}
		return count;
	}

	/**
	 * 获取顺子
	 * 	inList 传入数组
	 * 	outList 返回的对子数组
	 *	return 数量
	 * **/
	public static int getShunZi(ArrayList< ArrayList<Integer> > outList, ArrayList<Integer> inList){
		int count = 0, idx = 0;

		ArrayList<Integer> valueList = deleteValueEqual(inList);
			ArrayList<Integer> temp = new ArrayList<Integer>(5);
		for(int i = 0 ; i < valueList.size(); i++){
			temp.add(valueList.get(i));
			if(idx == 0){
				idx++;
				continue;
			}
			if(i == valueList.size() - 1)
			{
				if(temp.size() >= MIN_FLUSH_COUNT){
					outList.add(new ArrayList<>(temp));
					count++;
				}
				continue;
			}
			if(Math.abs(getCardValue(temp.get(idx)) - getCardValue(temp.get(idx-1))) == 1   ){
				idx++;
			}else{
				if(temp.size() >= MIN_FLUSH_COUNT){
					temp.remove(idx);
					outList.add(new ArrayList<>(temp));
					temp.clear();
					count++;
					idx = 0;
					i--;
				}else{
					idx = 0;
				}
			}
		}
		return count;
	}

	/**
	 * 获取顺子  不重复 固定长度
	 * 	inList 传入数组
	 * 	outList 返回的对子数组
	 *	return 数量
	 * **/
	public static int getShunZiEx(ArrayList< ArrayList<Integer> > outList, ArrayList<Integer> inList, int Count){
		ArrayList<Integer> valueList = deleteValueEqual(inList);

		for(int i = 0 ; i <= valueList.size() - Count; i++){
			ArrayList<Integer> temp = new ArrayList<Integer>(Count);
			Integer value = valueList.get(i);
			temp.add(value);
			for(int j = 1; j < Count; j++){
				if(Math.abs(getCardValue(valueList.get(i+j)) - getCardValue(valueList.get(i+j-1))) != 1  ){
					break;
				}
				temp.add(valueList.get(i+j));
			}
			if(temp.size() != Count){
				continue;
			}
			outList.add(temp);
		}
		return outList.size();
	}

	/**
	 * 获取同花顺子
	 * 	inList 传入数组
	 * 	outList 返回的对子数组
	 *	return 数量
	 * **/
	public static int getTongHuaShun(ArrayList< ArrayList<Integer> > outList, ArrayList<Integer> inList){
		int count = 0, idx = 0;
		outList.clear();
		ArrayList<Integer> valueList = sort(inList, true);
		ArrayList<Integer> temp = new ArrayList<Integer>(8);
		for(int i = 0 ; i < valueList.size(); i++){
			temp.add(valueList.get(i) );
			if(idx == 0){
				idx++;
				continue;
			}
			if(Math.abs(getCardValue(temp.get(idx)) - getCardValue(temp.get(idx-1))) == 1
					&& getCardColor(temp.get(idx)) == getCardColor(temp.get(idx-1))) {
				idx++;
			}else{
				if(temp.size() >= MIN_FLUSH_COUNT){
					temp.remove(idx);
					outList.add(new ArrayList<>(temp));
					temp.clear();
					count++;
					idx = 0;
					i--;
				}else{
					idx = 0;
				}
			}
		}
		return count;
	}

	/**
	 * 获取同花
	 * 	inList 传入数组
	 * 	outList 返回的对子数组
	 *	return 数量
	 * **/
	public static int getTongHua(ArrayList< ArrayList<Integer> > outList, ArrayList<Integer> inList){
		int count = 0, idx = 0;

		ArrayList<Integer> valueList = sort(inList, true);
		ArrayList<Integer> temp = new ArrayList<Integer>(8);
		for(int i = 0 ; i < valueList.size(); i++){
			temp.add(valueList.get(i));
			if(idx == 0){
				idx++;
				continue;
			}
			if(i == valueList.size() - 1 ){
				outList.add(new ArrayList<>(temp));
				count++;
				continue;
			}
			if(getCardColor(temp.get(idx)) == getCardColor(temp.get(idx-1) )){
				idx++;
			}else{
				temp.remove(idx);
				outList.add(new ArrayList<>(temp));
				temp.clear();
				i--;
				count++;
				idx = 0;
			}
		}
		return count;
	}

	/**
	 * 去掉相同值的牌
	 * */
	public static ArrayList<Integer> deleteValueEqual(ArrayList<Integer> pockerList){

		Map<Integer, List<Integer>> collectorsMap =  pockerList.stream().collect(Collectors.groupingBy(p -> Pocker80.getCardValueEx(p)));

		return new ArrayList<>( collectorsMap.keySet());
	}

	/**
	 * 某一张牌的个数
	 */
	public static int getCardCount(ArrayList<Integer> cardList, int card, boolean isValueEqual) {
		Map<Integer, List<Integer>> trumpMap =  new ConcurrentHashMap<>();
		List<Integer> tempList;
		if (isValueEqual) {
			trumpMap =cardList.stream().collect(Collectors.groupingBy(p -> Pocker80.getCardValueEx(p)));
			tempList =  trumpMap.get(Pocker80Logic.getCardValue(card));
		} else {
			trumpMap = cardList.stream().collect(Collectors.groupingBy(p -> p));
			tempList =  trumpMap.get(card);
		}
		if (null != tempList) {
			return tempList.size();
		}
		return 0;
	}

	/**
	 * 删除某一张牌
	 * */
	public static int  deleteSameCard(ArrayList<Integer> cardList, int card, boolean isValueEqual ) {
		int count = 0;
		for (int i = 0 ; i < cardList.size();  ) {
			if(isValueEqual && Pocker80.getCardValue(cardList.get(i)) == Pocker80.getCardValue(card)){
				count++;
				cardList.remove(i);
			}else if (!isValueEqual && cardList.get(i) == card) {
				count++;
				cardList.remove(i);
			}else{
				i++;
			}
		}
		return count;
	}
	
	/**
	 * 删除牌组信息(十进制)
	 * @return
	 */
	public static ArrayList<Integer> deleteCardDecimalism(ArrayList<Integer> cardList, ArrayList<Integer> deleteCardList) {
		//判断要删除的牌是否都在
		ArrayList<Integer> cardLists= new ArrayList<>();
		for (Integer byte1 : deleteCardList) {
			Optional<Integer> first = cardList.stream().filter(n -> Pocker80.getCardValueEx(n) == byte1).findFirst();
			first.ifPresent(found -> {
				cardList.remove(found);
				cardLists.add(found);
			});
		}
		return cardLists;
	}

	/**
	 * 删除牌组信息
	 * @return
	 */
	public static boolean deleteCard(ArrayList<Integer> cradList, ArrayList<Integer> deleteCardList) {
		boolean flag=true;
		List<Integer> notRepeat=deleteCardList.stream().distinct().collect(Collectors.toList());
		//检测牌的合理性 是否重复
		if(notRepeat.size()!=deleteCardList.size()){
			return false;
		}
		//判断要删除的牌是否都在
		for (Integer byte1 : deleteCardList) {
			if(!cradList.contains(byte1)){
				flag=false;
			}
		}
		if(flag){//牌都在的话删除
			cradList.removeAll(deleteCardList);
		}
		return flag;
	}

	/**
	 * 获取相同的牌
	 * */
	public static ArrayList<Integer>  getSameCard(ArrayList<Integer> cardList, int card, boolean isValueEqual ) {
		Map<Integer, List<Integer>> trumpMap =  new ConcurrentHashMap<>();
		if (isValueEqual) {
			trumpMap =cardList.stream().collect(Collectors.groupingBy(p -> Pocker80.getCardValueEx(p)));
			return (ArrayList<Integer>) trumpMap.get(Pocker80Logic.getCardValue(card));
		} else {
			trumpMap = cardList.stream().collect(Collectors.groupingBy(p -> p));
			return (ArrayList<Integer>) trumpMap.get(card);
		}
	}

	/**
	 * 获取固定长度的顺子  比如 3456789 获取固定长度为5 有数字34567 45678 56789
	 *	inList 传入数组
	 * 	outList 返回的对子数组
	 * 	count 固定长度
	 *	return 数量
	 * */
	public static int getShunZiByCount(ArrayList< ArrayList<Integer> > outList, ArrayList<Integer> inList, int Count){
		ArrayList<Integer> list =  sort(inList, false);
		ArrayList< ArrayList<Integer> > equallist = new ArrayList< ArrayList<Integer> >();
		getPockerEqualValue(equallist, list);
		ArrayList< ArrayList<Integer> > tempList = new ArrayList< ArrayList<Integer> >(ONE_COLOR_POCKER_COUNT);
		getShunZiEx(tempList, list, Count);

		for(int i = 0; i < tempList.size(); i++){
			ArrayList<Integer> temp = tempList.get(i);
			ArrayList< ArrayList<Integer> > tempShunZilist =  new ArrayList< ArrayList<Integer> >(ONE_COLOR_POCKER_COUNT);
			tempShunZilist.add(new ArrayList<>(temp));
			for(int j = 0; j < Count; j++){
				int num = tempShunZilist.size();
				Integer value = temp.get(j);
				ArrayList<Integer> equal = getSameCard(list, getCardValue(value), true);
 				if(null == equal || (null != equal && equal.size() == 0)){
					continue;
				}
				for(int k = 0; k < num; k++){
					ArrayList<Integer> tmp = (ArrayList<Integer>) tempShunZilist.get(k).clone();
					for(int h = 0; h < equal.size(); h++){
						if(equal.get(h).equals(value) || equal.get(h) == null){
							continue;
						}
						tmp.set(j, equal.get(h));
						tempShunZilist.add((ArrayList<Integer>) tmp.clone());
					}
				}
			}
			outList.addAll(tempShunZilist);
		}

		rechecking(outList);
		return outList.size();
	}

	//查找重复的
	public static void rechecking(ArrayList< ArrayList<Integer> > outList){
		if(outList.size() <= 1) {
            return;
        }
		for(int i = 0; i < outList.size() ;  ){
			ArrayList<Integer>  tempList = outList.remove(i);
			if(!outList.contains(tempList)){
				outList.add(i, tempList);
				i++;
			}
//			else{
//				System.out.println("重复 "+ tempList.toString());
//			}
		}
	}

	public static void recursionAdd(ArrayList< ArrayList<Integer> > outList, ArrayList<Integer> list, int  Count){
		ArrayList< ArrayList<Integer> > tempList = new ArrayList< ArrayList<Integer> >();
		tempList.add((ArrayList<Integer>) outList.clone());
		for(int i = 0; i < tempList.size(); i++){
			ArrayList<Integer> needList = tempList.get(i);
			outList.add((ArrayList<Integer>) needList.clone());

			for(int j = 0; j < Count; j++){
				Integer value = needList.get(j);
				for(int k = 0; k < list.size(); k++){
					Integer temp = list.get(k);
					if(getCardValue(value) == getCardValue(temp) && !value.equals(temp)){
						needList.set(j, temp);
						outList.add((ArrayList<Integer>) needList.clone());

					}
				}
			}
		}
	}

	/**
	 *  相同的牌放到一个数组中
	 * inList 传入数组
	 * outList 返回的对子数组
	 *return 数量
	*/
	public static int  getPockerEqualValue(ArrayList< ArrayList<Integer> > outList, ArrayList<Integer> inList){
		
		Map<Integer, List<Integer>> collectorsMap =  inList.stream().collect(Collectors.groupingBy(p -> Pocker80.getCardValueEx(p)));
		
		for (Map.Entry<Integer, List<Integer>> mEntry : collectorsMap.entrySet()) {
			outList.add(new ArrayList<>(mEntry.getValue()));
		}
		
		return collectorsMap.size();
	}

	/**
	 * 排序
	 *  flag 是否加入花色排序 true:按花色 false：不按花色
	 *  return 返回的排序后的数组
	 * */
	public static ArrayList<Integer> sort(ArrayList<Integer> pockerList, boolean flag){
		ArrayList<Integer> outList = (ArrayList<Integer>) pockerList.clone();
		for(int i = 0; i < outList.size() -1; i++){
			for(int j = 0; j < outList.size() - 1 - i; j++){
				Integer byte2 = outList.get(j+1);
				Integer byte1 = outList.get(j);
				Integer temp = byte1;
				if(flag){
					if(getCardColor(byte1) != getCardColor(byte2)){
						if(getCardColor(byte1) > getCardColor(byte2)){
							outList.set(j, byte2);
							outList.set(j+1, temp);
						}
					}else{
						if(getCardValue(byte1) > getCardValue(byte2)){
							outList.set(j, byte2);
							outList.set(j+1, temp);
						}
					}
				}else{
					if(getCardValue(byte1) != getCardValue(byte2)){
						if(getCardValue(byte1) > getCardValue(byte2)){
							outList.set(j, byte2);
							outList.set(j+1, temp);
						}
					}else{
						if(getCardColor(byte1) > getCardColor(byte2)){
							outList.set(j, byte2);
							outList.set(j+1, temp);
						}
					}
				}
			}
		}
		return outList;
	}

	/**
	 * 获取乱牌
	 * normalcount 加几幅普通牌
	 * trumpCount 带几对大小王
	 * */
	public static ArrayList<Integer> getRandomPockerList(int normalCount  , int trumpCount , Pocker80ListType pocketListType){
		int total = MAX_NORMAL_POCKER * normalCount + trumpCount * MAX_TRUMP_POCKER;
		ArrayList<Integer> list = new ArrayList<Integer>(total);
		for(int i = 0; i  < normalCount ; i++){
			if(Pocker80ListType.POCKERLISTTYPE_AFIRST == pocketListType) {
				list.addAll(Arrays.asList(PockerList_AFirst));
			}else	if(Pocker80ListType.POCKERLISTTYPE_AEND == pocketListType) {
					list.addAll(Arrays.asList(PockerList_AEnd));
			} else {
				list.addAll(Arrays.asList(PockerList_TWOEnd));
			}
		}
		for(int i = 0; i  < trumpCount ; i++){
			list.addAll(Arrays.asList(Trump_PockerList));
		}
		Collections.shuffle(list);
		return list;
	}
	
	/**
	 * 唯一 获取乱牌 normalcount 加几幅普通牌 trumpCount 带几对大小王
	 */
	public static ArrayList<Integer> getOnlyRandomPockerList(int normalCount  , int trumpCount ,Pocker80ListType pocketListType) {
		int total = MAX_NORMAL_POCKER * normalCount + trumpCount * MAX_TRUMP_POCKER;
		
		ArrayList<Integer> pocketList = new ArrayList<Integer>(MAX_NORMAL_POCKER);
		if(Pocker80ListType.POCKERLISTTYPE_AFIRST == pocketListType) {
			pocketList.addAll(Arrays.asList(PockerList_AFirst));
		}else	if(Pocker80ListType.POCKERLISTTYPE_AEND == pocketListType) {
			pocketList.addAll(Arrays.asList(PockerList_AEnd));
		} else {
			pocketList.addAll(Arrays.asList(PockerList_TWOEnd));
		}
		
		ArrayList<Integer> list = new ArrayList<Integer>(total);

		for (int i = 0; i < normalCount; i++) {
			for (Integer cardValue : pocketList) {
				list.add(cardValue.intValue() + (Pocker80.LOGIC_MASK_COLOR_MOD)*i);
			}
		}
		
		for (int i = 0; i < trumpCount; i++) {
			for (Integer cardValue : Trump_PockerList) {
				list.add(cardValue.intValue() + (Pocker80.LOGIC_MASK_COLOR_MOD)*i);
			}
		}

		Collections.shuffle(list);
		return list;
	}

	/**
	 *获取赖子
	 * **/
	public static int getRandomRazz(Pocker80ListType pocketListType){
		int index = (int) (Math.random() * ONE_COLOR_POCKER_COUNT );
		int cardValue = -1;
		if(Pocker80ListType.POCKERLISTTYPE_AFIRST == pocketListType) {
			cardValue = Pocker80.getCardValue(PockerList_AFirst[index]);
		}else	if(Pocker80ListType.POCKERLISTTYPE_AEND == pocketListType) {
			cardValue = Pocker80.getCardValue(PockerList_AEnd[index]);
		} else {
			cardValue = Pocker80.getCardValue(PockerList_TWOEnd[index]);
		}
		return cardValue;
	}

	/**
	 * 比较值的大小 包含大小王 left比rigth大返回ture否侧false
	 * */
	public static boolean compareCard(int left, int rigth){
		if( Pocker80Logic.getCardColor(left) == Pocker80Logic.getCardColor(rigth)){
			if(Pocker80Logic.getCardValue(rigth) < Pocker80Logic.getCardValue(left)){
				return true;
			}
		}else if(Pocker80Logic.getCardColor(rigth) == Pocker80ColorType.POCKER_COLOR_TYPE_TRUMP.value() ){
			
		}else if(Pocker80Logic.getCardColor(left) == Pocker80ColorType.POCKER_COLOR_TYPE_TRUMP.value()&& Pocker80Logic.getCardColor(left) != Pocker80Logic.getCardColor(rigth)){
			return true;
		}else if( Pocker80Logic.getCardValue(rigth) < Pocker80Logic.getCardValue(left)){
			return true;
		}
		return false;
	}
	
	
	
	/**
	 * 二维数组返回一维数组
	 * */
	public static ArrayList<Integer> TwoArrayListToArray(ArrayList<ArrayList<Integer>> cardList) {
		ArrayList<Integer> tempList = new ArrayList<>();
		for (ArrayList<Integer>  list: cardList) {
			tempList.addAll(list);
		}
		return tempList;
	}
	
	
	
	/**
	 * 数字排序
	 * 
	 * @param list
	 * @param asc
	 *            T 升序（1..2）,F降序（2..1）
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void getSort(List list, boolean asc) {
		// 字符串排序
		Collections.sort(list);
		Collections.sort(list, new Comparator() {
			@Override
			public int compare(Object o1, Object o2) {
				if (asc) {
					return Integer.compare(Pocker80.getCardValue((Integer) o1), Pocker80.getCardValue((Integer) o2));
				} else {
					return Integer.compare(Pocker80.getCardValue((Integer) o2), Pocker80.getCardValue((Integer) o1));
				}
			}
		});
	}

	/**
	 * 顺子
	 * 
	 * @param numbers
	 *            进行顺子检查
	 * @return
	 */
	public static boolean isContinuous(List<Integer> numbers) {
		double sum = 0;// 等差数列求和
		int count = 0;
		if (numbers == null || numbers.size() <= 0) {
			return false;
		}
		ArrayList<Integer> list = new ArrayList<Integer>();
		for (int i = 0; i < numbers.size(); i++) {
			count += numbers.get(i);
			list.add(numbers.get(i));
		}
		int len = list.size();

		Collections.sort(list);
		int num = len - 1;

		if (Math.abs(list.get(0) - list.get(len - 1)) > num) {
			return false;
		}
		for (int i = 0; i < len - 1; i++) {
			if (list.get(i).equals(list.get(i + 1))) {
				return false;
			}
		}

		sum = Double.parseDouble(String.valueOf(len * (list.get(0) + list.get(len - 1)) / 2.0));
		if (count != sum) {
			return false;
		}

		return true;
	}
    /**
     * 顺子
     *
     * @param numbers 进行顺子检查
     * @return
     */
    public static boolean isContinuousPass(List<Integer> numbers, List<Integer> passList) {
        double sum = 0;// 等差数列求和
        int count = 0;
        if (numbers == null || numbers.size() <= 0) {
            return false;
        }
        ArrayList<Integer> list = new ArrayList<Integer>();
        for (int i = 0; i < numbers.size(); i++) {
            count += numbers.get(i);
            list.add(numbers.get(i));
        }
        Collections.sort(list);
        List<Integer> ewaiList = new ArrayList<>();
        for (Integer iii : passList) {
            if (iii > list.get(0) && iii < list.get(list.size() - 1)) {
                ewaiList.add(iii);
                count += iii;
            }
        }
        list.addAll(ewaiList);
        Collections.sort(list);
        int len = list.size();
        int num = len - 1;

        if (Math.abs(list.get(0) - list.get(len - 1)) > num) {
            return false;
        }
        for (int i = 0; i < len - 1; i++) {
            if (list.get(i).equals(list.get(i + 1))) {
                return false;
            }
        }

        sum = Double.parseDouble(String.valueOf(len * (list.get(0) + list.get(len - 1)) / 2.0));
        if (count != sum) {
            return false;
        }

        return true;
    }
}
