package business.global.pk.zjh;

import com.ddm.server.common.utils.Txt2Utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Validated immutable view of the legacy ZJH configuration. */
public final class ZJHConfigMgr {
    public static final String fileName = "ZJHConfig.txt";
    public static final String filePath = "conf/";

    private final List<Integer> difenList;
    private final List<Integer> bottomPointList;
    private final List<Integer> topPointList;
    private final List<Integer> comporeCountList;
    private final List<Integer> xiQianBeiShuList;
    private final List<Integer> lunShuShangXian;
    private final int robotOpenCard;
    private final int robotQiPai;
    private final int robotJiaZhu;
    private final int addScoreAll;
    private final List<Integer> robotJiaZhuList;

    public ZJHConfigMgr() {
        this(resolveDefault());
    }

    public ZJHConfigMgr(Path configFile) {
        Path absolute = configFile.toAbsolutePath().normalize();
        if (!Files.isRegularFile(absolute)) {
            throw new IllegalStateException("ZJH config not found: " + absolute);
        }
        Map<String, String> values = Txt2Utils.txt2Map(
                absolute.getParent().toString() + "/", absolute.getFileName().toString(), "GBK");
        difenList = requiredList(values, "difenList");
        bottomPointList = requiredList(values, "bottomPoint");
        topPointList = requiredList(values, "topPoint");
        comporeCountList = requiredList(values, "comporeCount");
        xiQianBeiShuList = requiredList(values, "xiQianBeiShu");
        lunShuShangXian = requiredList(values, "lunshushangxian");
        robotOpenCard = probability(values, "robotOpenCard");
        robotQiPai = probability(values, "robotQiPai");
        robotJiaZhu = probability(values, "robotJiaZhu");
        addScoreAll = positive(values, "addScoreAll");
        robotJiaZhuList = requiredList(values, "robotJiaZhuList");
    }

    private static Path resolveDefault() {
        Path serviceConfig = Path.of(filePath, fileName);
        return Files.isRegularFile(serviceConfig) ? serviceConfig : Path.of("server", "ZJH", filePath, fileName);
    }

    private static List<Integer> requiredList(Map<String, String> values, String key) {
        String raw = values.get(key);
        if (raw == null) throw new IllegalStateException("Missing ZJH config key: " + key);
        List<Integer> parsed = Txt2Utils.String2ListInteger(raw);
        if (parsed == null || parsed.isEmpty()) throw new IllegalStateException("Empty ZJH config key: " + key);
        return List.copyOf(parsed);
    }

    private static int probability(Map<String, String> values, String key) {
        int value = integer(values, key);
        if (value < 0 || value > 100) throw new IllegalStateException("ZJH probability out of range: " + key);
        return value;
    }

    private static int positive(Map<String, String> values, String key) {
        int value = integer(values, key);
        if (value <= 0) throw new IllegalStateException("ZJH value must be positive: " + key);
        return value;
    }

    private static int integer(Map<String, String> values, String key) {
        String raw = values.get(key);
        if (raw == null) throw new IllegalStateException("Missing ZJH config key: " + key);
        try { return Integer.parseInt(raw); }
        catch (NumberFormatException error) { throw new IllegalStateException("Invalid ZJH integer: " + key, error); }
    }

    public ArrayList<Integer> getEndPointList() { return new ArrayList<>(difenList); }
    public ArrayList<Integer> getBottomPointList() { return new ArrayList<>(bottomPointList); }
    public ArrayList<Integer> getTopPointList() { return new ArrayList<>(topPointList); }
    public ArrayList<Integer> getComporeCountList() { return new ArrayList<>(comporeCountList); }
    public ArrayList<Integer> getXiQianBeiShuList() { return new ArrayList<>(xiQianBeiShuList); }
    public ArrayList<Integer> getLunShuShangXian() { return new ArrayList<>(lunShuShangXian); }
    public ArrayList<Integer> getRobotJiaZhuList() { return new ArrayList<>(robotJiaZhuList); }
    public int getRobotOpenCard() { return robotOpenCard; }
    public int getRobotQiPai() { return robotQiPai; }
    public int getRobotJiaZhu() { return robotJiaZhu; }
    public int getAddScoreAll() { return addScoreAll; }
}
