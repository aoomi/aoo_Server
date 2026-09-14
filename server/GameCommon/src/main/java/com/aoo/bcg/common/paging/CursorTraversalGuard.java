package com.aoo.bcg.common.paging;
import java.util.HashSet;
import java.util.Set;
/** Enforces cursor progress, duplicate-page rejection and a finite page budget. */
public final class CursorTraversalGuard {
    private final int maximumPages; private int pages; private final Set<String> seen=new HashSet<>();
    public CursorTraversalGuard(int maximumPages){if(maximumPages<1||maximumPages>100_000)throw new IllegalArgumentException("page budget must be 1..100000");this.maximumPages=maximumPages;}
    public void accept(String previousCursor,String nextCursor,int itemCount,boolean hasMore){
        if(++pages>maximumPages)throw new IllegalStateException("pagination page budget exceeded");
        if(itemCount<0)throw new IllegalArgumentException("negative page size");
        if(!hasMore){if(nextCursor!=null&&!nextCursor.isBlank())throw new IllegalStateException("terminal page cannot expose cursor");return;}
        if(itemCount==0||nextCursor==null||nextCursor.isBlank())throw new IllegalStateException("non-terminal page must contain items and cursor");
        if(nextCursor.equals(previousCursor)||!seen.add(nextCursor))throw new IllegalStateException("pagination cursor did not advance");
    }
    public int pages(){return pages;}
}
