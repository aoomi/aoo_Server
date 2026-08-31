package business.global.pk.njpdk;

import com.aoo.bcg.common.config.LegacyConfigGuard;
import com.ddm.server.common.utils.Txt2Utils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/*
 * 资阳跑得快 配置文件
 * @author zaf
 * */
public class NJPDKConfigMgr {
    public static final String fileName = "NJPDKConfig.txt";
    public static final String filePath = "conf/";
    private Map<String, String> configMap = new HashMap<String, String>();
    protected int God_Card;
    protected ArrayList<Integer> Private_Card1;
    protected ArrayList<Integer> Private_Card2;
    protected ArrayList<Integer> Private_Card3;
    protected ArrayList<Integer> Private_Card4;

    public NJPDKConfigMgr() {
        this(resolveDefault());
    }

    NJPDKConfigMgr(Path configFile) {
        Path file = configFile.toAbsolutePath().normalize();
        if (!Files.isRegularFile(file)) throw new IllegalStateException("NJPDK config not found: " + file);
        this.configMap = Txt2Utils.txt2Map(file.getParent().toString() + "/", file.getFileName().toString(), "GBK");
        this.God_Card = LegacyConfigGuard.range(configMap, "NJPDK", "God_Card", 0, 1);
        this.Private_Card1 = list("Private_Card1");
        this.Private_Card2 = list("Private_Card2");
        this.Private_Card3 = list("Private_Card3");
        this.Private_Card4 = list("Private_Card4");
    }

    private static Path resolveDefault() {
        Path service = Path.of(filePath, fileName);
        return Files.isRegularFile(service) ? service : Path.of("server", "NJPDK", filePath, fileName);
    }

    private ArrayList<Integer> list(String key) {
        return Txt2Utils.String2ListInteger(LegacyConfigGuard.required(configMap, "NJPDK", key));
    }

    /**
     * @return god_Card
     */
    public boolean isGodCard() {
        return God_Card == 1;
    }

    /**
     * @return private_Card1
     */
    public ArrayList<Integer> getPrivate_Card1() {
        return Private_Card1;
    }

    /**
     * @return private_Card2
     */
    public ArrayList<Integer> getPrivate_Card2() {
        return Private_Card2;
    }

    /**
     * @return private_Card3
     */
    public ArrayList<Integer> getPrivate_Card3() {
        return Private_Card3;
    }

    /**
     * @return private_Card4
     */
    public ArrayList<Integer> getPrivate_Card4() {
        return Private_Card4;
    }
}
