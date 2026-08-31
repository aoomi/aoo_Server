package cenum;

public class Page {

    /**
     * 每页大小
     */
    public final static int PAGE_SIZE = 20;

    /**
     * 每页大小8
     */
    public final static int PAGE_SIZE_8 = 8;

    /**
     * 每页大小10
     */
    public final static int PAGE_SIZE_10 = 10;

    /**
     * 每页大小15
     */
    public final static int PAGE_SIZE_15 = 15;

    /**
     * 每页大小30
     */
    public final static int PAGE_SIZE_30 = 30;
    /**
     * 每页大小100
     */
    public final static int PAGE_SIZE_100 = 100;
    /**
     * 每页大小400
     */
    public final static int PAGE_SIZE_400 = 400;

    /**
     * 获取当前页码
     *
     * @param pageNum 页数
     * @return
     */
    public static int getPageNum(int pageNum) {
        return getPageNum(pageNum, PAGE_SIZE);
    }

    /**
     * 获取当前页码
     *
     * @param pageNum 页数
     * @return
     */
    public static int getPageNumEight(int pageNum) {
        return getPageNum(pageNum, PAGE_SIZE_8);
    }

    /**
     * 获取当前页码
     *
     * @param pageNum  页数
     * @param pageSize 每页大小
     * @return
     */
    public static int getPageNum(int pageNum, int pageSize) {
        if (pageNum <= 0) {
            pageNum = 0;
        } else if (pageNum > 0) {
            pageNum = (pageNum - 1) * pageSize;
        }
        return pageNum;
    }

}
