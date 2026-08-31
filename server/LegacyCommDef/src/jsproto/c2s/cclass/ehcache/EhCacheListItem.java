package jsproto.c2s.cclass.ehcache;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
public class EhCacheListItem<T> implements Serializable {
    /**
     * 缓存数组列表
     */
    private List<T> ehCacheList;

    public EhCacheListItem(List<T> ehCacheList) {
        this.ehCacheList = ehCacheList;
    }
}
