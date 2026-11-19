package jsproto.c2s.cclass.union;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** 联盟业务唯一语义词汇；旧协议、表字段和 MQ 键不在此处破坏性改名。 */
public final class AllianceBusinessTerms {
    public static final String Alliance = "联盟";
    public static final String AllianceOwner = "盟主";
    public static final String ClubOwner = "圈主";
    public static final String Ally = "盟友";
    public static final String AllianceManager = "联盟管理";
    public static final String ClubManager = "圈管理";
    public static final String Partner = "合伙人";
    public static final String Captain = "队长";
    public static final String Member = "成员";

    private static final Map<String, String> LegacyAliases;

    static {
        Map<String, String> aliases = new HashMap<>();
        aliases.put("联盟", Alliance);
        aliases.put("盟主", AllianceOwner);
        aliases.put("圈主", ClubOwner);
        aliases.put("联盟管理", AllianceManager);
        aliases.put("圈管理", ClubManager);
        aliases.put("队长", Captain);
        aliases.put("成员", Member);
        LegacyAliases = Collections.unmodifiableMap(aliases);
    }

    private AllianceBusinessTerms() {
    }

    /** 兼容读取旧检索词；新索引与新展示只写规范名称。 */
    public static String normalizeSearchTerm(String value) {
        return LegacyAliases.getOrDefault(value, value);
    }
}
