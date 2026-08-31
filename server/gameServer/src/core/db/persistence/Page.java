package core.db.persistence;

import java.util.Collections;
import java.util.List;

/**
 * Framework-neutral pagination result.
 */
public final class Page<T> {
    private final List<T> list;
    private final int pageNumber;
    private final int pageSize;
    private final int totalPage;
    private final int totalRow;

    public Page(List<T> list, int pageNumber, int pageSize, int totalPage, int totalRow) {
        this.list = list == null ? Collections.emptyList() : List.copyOf(list);
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.totalPage = totalPage;
        this.totalRow = totalRow;
    }

    public List<T> getList() {
        return list;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getTotalPage() {
        return totalPage;
    }

    public int getTotalRow() {
        return totalRow;
    }
}
