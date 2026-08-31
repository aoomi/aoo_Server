package business.global.pk.njpdk.cardtype;

/**
 * 扑克牌型命名
 */
public enum CardType {
    APairs(1),//对子
    MultiPairs(2),//联队
    PlaneWithPairs(3),//飞机带一对
    PlaneWithTwo(4),//飞机带两张
    PlaneWithA(5),//飞机带一张
    Plane(6),//飞机
    Bomb3AWith1(7),//3A1炸弹
    Bomb3A(8),//3A1炸弹
    Bomb(9),//炸弹
    BombWith1(10),//4带1炸弹
    ThreeZoneWithPairs(11),//3带一对
    ThreeZoneWithTwo(12),//3带2张
    ThreeZone(13),//3带
    ThreeZoneWithA(14),//3带一张
    Straight(15),//顺子
    Single(16),//单张
    ;
    int value;

    CardType(int value) {
        this.value = value;
    }
}
