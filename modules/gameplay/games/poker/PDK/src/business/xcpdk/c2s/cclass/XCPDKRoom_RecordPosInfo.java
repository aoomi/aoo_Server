package business.xcpdk.c2s.cclass;

import jsproto.c2s.cclass.pk.PKRoom_RecordPosInfo;

import java.util.ArrayList;
import java.util.List;

public class XCPDKRoom_RecordPosInfo extends PKRoom_RecordPosInfo {
	public List<Integer> pointList = new ArrayList<>();
	/**
	 * 增加每局分数
	 * @param point
	 */
	public void addToPoint(int point){
		pointList.add(point);
	}

	public List<Integer> getPointList() {
		return pointList;
	}
}
