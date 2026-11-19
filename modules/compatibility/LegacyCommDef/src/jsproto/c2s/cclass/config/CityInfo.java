package jsproto.c2s.cclass.config;

import lombok.Data;

/**
 * 城市信息
 *
 * @author Administrator
 */
@Data
public class CityInfo {
    private long Id;
    private int Type;
    private int Ascription;
    private String Name;
    private int Popular;
    private int DefaultCity;
    private String Game;

    public CityInfo() {
        super();
    }

    public CityInfo(long Id, int Type, int Ascription, String Name, int Popular, int DefaultCity, String Game) {
        this.Id = Id;
        this.Type = Type;
        this.Ascription = Ascription;
        this.Name = Name;
        this.Popular = Popular;
        this.DefaultCity = DefaultCity;
        this.Game = Game;
    }

}
