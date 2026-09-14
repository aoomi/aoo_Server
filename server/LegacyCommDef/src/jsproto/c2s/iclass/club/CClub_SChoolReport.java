package jsproto.c2s.iclass.club;

import lombok.Data;

import java.util.List;

@Data
public class CClub_SChoolReport {
    private long clubId;
    private long unionId;
    private List<Long> roomIDList;
    private boolean isAll;
    private int getType;
    private int pageNum;
    private String query;
}
