//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package server.aoo.dao.config.util;

import java.io.Serializable;
import java.util.List;

public class Page<T> implements Serializable {
    private static final long serialVersionUID = -5395997221963176643L;
    private List<T> list;
    private int pageNumber;
    private int pageSize;
    private int totalPage;
    private int totalRow;

    public Page(List<T> list, int pageNumber, int pageSize, int totalPage, int totalRow) {
        this.list = list;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.totalPage = totalPage;
        this.totalRow = totalRow;
    }

    public Page() {
    }

    public List<T> getList() {
        return this.list;
    }

    public int getPageNumber() {
        return this.pageNumber;
    }

    public int getPageSize() {
        return this.pageSize;
    }

    public int getTotalPage() {
        return this.totalPage;
    }

    public int getTotalRow() {
        return this.totalRow;
    }

    public boolean isFirstPage() {
        return this.pageNumber == 1;
    }

    public boolean isLastPage() {
        return this.pageNumber >= this.totalPage;
    }

    @Override
    public String toString() {
        StringBuilder msg = new StringBuilder();
        msg.append("pageNumber : ").append(this.pageNumber);
        msg.append("\npageSize : ").append(this.pageSize);
        msg.append("\ntotalPage : ").append(this.totalPage);
        msg.append("\ntotalRow : ").append(this.totalRow);
        return msg.toString();
    }
}
