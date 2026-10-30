package jsproto.c2s.cclass.club;

import lombok.Data;

import java.io.Serializable;

@Data
public class ClubZhongZhiLevel implements Serializable {
    private int levelZhongZhi;

    public ClubZhongZhiLevel(int levelZhongZhi) {
        this.levelZhongZhi = levelZhongZhi;
    }
}
