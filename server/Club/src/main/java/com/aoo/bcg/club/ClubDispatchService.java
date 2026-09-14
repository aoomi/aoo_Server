package com.aoo.bcg.club;

import com.aoo.bcg.billing.JdbcBillingService;
import com.aoo.bcg.common.id.DistributedIdGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

/** Bridges historical club actions onto the authoritative JDBC club aggregate. */
public final class ClubDispatchService {
    private static final int PAGE_SIZE = 20;
    private static final long CLUB_CREATE_COST_CRYSTAL = 100;
    private final DataSource source;
    private final JdbcClubService clubs;
    private final JdbcBillingService billing;
    private final ObjectMapper json;
    private final Clock clock;

    public ClubDispatchService(DataSource source, ObjectMapper json, Clock clock) {
        this(source, json, clock, new DistributedIdGenerator(37, clock));
    }

    ClubDispatchService(DataSource source, ObjectMapper json, Clock clock, java.util.function.LongSupplier ledgerIds) {
        this.source = Objects.requireNonNull(source);
        this.json = Objects.requireNonNull(json);
        this.clock = Objects.requireNonNull(clock);
        this.clubs = new JdbcClubService(source, json, clock);
        this.billing = new JdbcBillingService(source, Objects.requireNonNull(ledgerIds), clock);
    }

    public Object dispatch(long actor, String requestId, String action, Map<String, Object> packet) {
        String canonical = action == null ? "" : action.replaceFirst("^Club\\.", "club.");
        Map<String, Object> payload = packet == null ? Map.of() : packet;
        return switch (canonical) {
            case "club.CGetClubListMin", "club.CGetClubListMin2" -> clubList(actor);
            case "club.CClubTop" -> pinClub(actor, requestId, payload);
            case "club.CClubCreate" -> createClub(actor, requestId, payload);
            case "club.CGetClubListById" -> clubDetail(requireMember(clubId(payload), actor), actor);
            case "club.CClubJoin" -> joinClub(actor, requestId, payload);
            case "club.CClubFindPIDInfo" -> findPlayer(actor, payload);
            case "club.CClubFindPIDAdd" -> invitePlayer(actor, requestId, payload);
            case "club.CClubGetMemberManage" -> members(actor, payload);
            case "club.CClubOnlinePlayerCount" -> onlineCount(payload);
            case "club.CClubGetShowOnlinePlayerNum" -> Map.of("showOnlinePlayerNum", setting(requireMember(clubId(payload), actor), "showOnlinePlayerNum", 1));
            case "club.CClubChangeShowOnlinePlayerNum" -> changeSetting(actor, requestId, payload, "showOnlinePlayerNum", number(payload, "showOnlinePlayerNum", 1));
            case "club.CClubChangeDimondsAttention", "club.CClubChangeDiamondsAttention" -> changeDiamondsAttention(actor, requestId, payload);
            case "club.CClubChangePlayerStatus" -> changePlayerStatus(actor, requestId, payload);
            case "club.CClubKickOutNeedConfirm" -> kickGate(actor, payload);
            case "club.CClubSetMinister" -> setMinister(actor, requestId, payload);
            case "club.CClubSetPromotionMinister" -> setPromotionManager(actor, requestId, payload);
            case "club.CClubChangePlayerRemarkName" -> changeRemark(actor, requestId, payload);
            case "club.CClubGetUplevelPromotion" -> getPromotionParent(actor, payload);
            case "club.CClubChangePromotionBelong", "club.CClubPartnerChange" -> changePromotionParent(actor, requestId, payload);
            case "club.CClubSubordinateList" -> subordinateList(actor, payload);
            case "club.CClubGetAllRoomMin" -> roomList(actor, payload);
            case "club.CClubRoomInfoDetails" -> roomDetail(actor, payload);
            case "club.CClubRoomConfigItemList" -> quickRoomConfigs(actor, payload);
            case "club.CClubGetCreateGameSet" -> managedRoomConfigs(actor, payload);
            case "club.CClubCreateGameSet", "club.CClubRoomCfgSave" -> saveRoomConfig(actor, requestId, payload);
            case "club.CClubCreateGameSetChangge" -> changeRoomConfig(actor, requestId, payload);
            case "club.CClubGroupingList" -> groupingList(actor, payload);
            case "club.CClubGroupingAdd" -> addGrouping(actor, requestId, payload);
            case "club.CClubGroupingRemove" -> removeGrouping(actor, requestId, payload);
            case "club.CClubGroupingMemberList" -> groupingMembers(actor, payload);
            case "club.CClubGroupingPidFind" -> groupingFind(actor, payload);
            case "club.CClubGroupingPidAdd" -> groupingPid(actor, requestId, payload, true);
            case "club.CClubGroupingPidRemove" -> groupingPid(actor, requestId, payload, false);
            case "club.CClubBanRoomConigList" -> banRoomList(actor, payload);
            case "club.CClubBanRoomConigOp" -> banRoomSave(actor, requestId, payload);
            case "club.CClubMemberClubCentInfo" -> clubCentInfo(actor, payload);
            case "club.CClubSubordinateLevelSportsPoint" -> subordinateSportsPointInfo(actor, payload);
            case "club.CClubCentUpdate", "club.CClubSubordinateLevelClubCentUpdate",
                    "club.CClubSubordinateLevelSportsPointUpdate", "union.CUnionClubCentUpdate" -> clubCentUpdate(actor, requestId, payload);
            case "club.CClubGetCaseSprotsChange" -> caseClubCentChange(actor, requestId, payload);
            case "club.CClubDynamic" -> dynamicRows(actor, payload);
            case "club.CClubCentDynamicByPid", "club.CClubCentMemberDynamicByPid",
                    "club.CClubCentChangeRecordByPid" -> clubCentDynamic(actor, payload);
            case "union.CUnionCreate" -> createUnion(actor, requestId, payload);
            case "union.CUnionGetConfig" -> unionConfig(actor, payload);
            case "union.CUnionSetConfig" -> setUnionConfig(actor, requestId, payload);
            case "union.CUnionJoin" -> joinUnion(actor, requestId, payload);
            case "union.CUnionDissolve" -> dissolveUnion(actor, requestId, payload);
            case "union.CUnionRoomCfgList" -> unionRoomConfigs(actor, payload);
            case "union.CUnionRoomCfgCount" -> unionRoomConfigCount(actor, payload);
            case "union.CUnionRoomCfgUpdate" -> updateUnionRoomConfig(actor, requestId, payload);
            case "union.CUnionRoomConfigItemList" -> unionQuickRoomConfigs(actor, payload);
            case "union.CUnionCreateRoom" -> saveUnionRoomConfig(actor, requestId, payload);
            case "union.CUnionGetAllRoomMin" -> roomList(actor, payload);
            case "union.CUnionRoomInfoDetails" -> roomDetail(actor, payload);
            case "club.CClubTotalInfo" -> recordSummary(actor, payload);
            case "club.CClubGetRecord" -> records(actor, payload);
            case "club.CClubRoomIdOperation" -> markRoomViewed(actor, requestId, payload);
            case "club.CClubPersonalRecordGetPageNum" -> Map.of("pageNumTotal", Math.max(1, pages(requireMember(clubId(payload), actor).records().size())));
            case "club.CClubPersonalRecord" -> personalRecords(actor, payload);
            case "club.CClubMemberRoomRecord" -> Map.of("pRoomRecords", personalRecords(actor, payload));
            case "club.CClubPlayerRecord", "club.CClubMemberRoomRecordList" -> playerRecordRows(actor, payload);
            case "club.CClubPlayerRecordCount" -> playerRecordCount(actor, payload);
            case "club.CClubSChoolReport" -> schoolReport(actor, payload);
            case "club.CClubClose" -> clubDetail(clubs.dissolve(key(requestId, canonical), clubId(payload), actor), actor);
            case "club.CClubGetCompetitionTime" -> competitionTimes();
            default -> promotionOrUnsupported(actor, requestId, canonical, payload);
        };
    }

    public List<Map<String, Object>> invitations(long actor) {
        return clubs.list().stream()
                .filter(state -> "ACTIVE".equals(state.status()))
                .flatMap(state -> state.invites().stream()
                        .filter(invite -> invite.playerId() == actor && !invite.accepted()
                                && invite.expiresAt().isAfter(clock.instant()))
                        .map(invite -> {
                            Map<String, Object> row = new LinkedHashMap<>();
                            row.put("clubId", state.id());
                            row.put("clubName", state.name());
                            row.put("token", invite.token());
                            row.put("expiresAt", invite.expiresAt().toString());
                            return row;
                        }))
                .toList();
    }

    private Object promotionOrUnsupported(long actor, String requestId, String action, Map<String, Object> payload) {
        if (action.startsWith("union.")) return unionReadShape(action);
        if (!action.startsWith("club.")) throw new IllegalArgumentException("unsupported club action");
        if (action.contains("PidInfo") || action.endsWith("Info")) return promotionInfo(actor, payload);
        if (action.contains("LevelList") || action.contains("TeamList")) return promotionList(actor, payload);
        if (action.contains("Ranked")) return Map.of("clubRankInfoList", List.of(), "clubCompetitionRankedItems", List.of(), "pageNumTotal", 1);
        if (action.contains("Dynamic")) return List.of();
        if (action.contains("Record") || action.contains("Report")) return Map.of("items", List.of(), "list", List.of(), "pageNumTotal", 1);
        if (isPersistentPromotionWrite(action)) return persistPromotionWrite(actor, requestId, action, payload);
        throw new IllegalArgumentException("unsupported club action: " + action);
    }

    private boolean isPersistentPromotionWrite(String action) {
        return action.contains("Promotion") || action.contains("Share") || action.contains("Warning")
                || action.contains("AlivePoint") || action.contains("EliminatePoint")
                || action.contains("SubordinateLevel") || action.contains("Caption")
                || action.contains("ChangeTotalPointShowStatus") || action.contains("SavePromotionShowLits")
                || action.contains("FixedShare");
    }

    private Object unionReadShape(String action) {
        if (action.contains("Notify")) return Map.of("items", List.of());
        if (action.contains("List") || action.contains("Dynamic") || action.contains("Record")
                || action.contains("Report") || action.contains("GetAllRoom")) return List.of();
        if (action.contains("Config") || action.contains("Info") || action.contains("Sum")) return Map.of();
        throw new IllegalArgumentException("unsupported union action: " + action);
    }

    private Object persistPromotionWrite(long actor, String requestId, String action, Map<String, Object> payload) {
        long clubId = clubId(payload);
        JdbcClubService.State state = clubs.update(key(requestId, action), clubId, current -> {
            boolean directPromotionInvite = action.endsWith("CClubPromotionPidAdd");
            if (directPromotionInvite) requirePromotionInvitePermission(current, actor);
            else requireManager(current, actor);
            Map<String, Object> settings = new LinkedHashMap<>(current.settings());
            settings.put("legacyWrite." + action + "." + number(payload, "pid", number(payload, "opPid", actor)), new LinkedHashMap<>(payload));
            Map<Long, String> members = new LinkedHashMap<>(current.members());
            Map<Long, JdbcClubService.MemberExtraState> extras = new LinkedHashMap<>(current.memberExtras());
            long pid = number(payload, "pid", number(payload, "opPid", 0));
            if (directPromotionInvite && pid > 0 && !members.containsKey(pid)) members.put(pid, "MEMBER");
            if (pid > 0 && members.containsKey(pid)) {
                JdbcClubService.MemberExtraState old = extras.getOrDefault(pid,
                        new JdbcClubService.MemberExtraState("", false, 0, BigDecimal.ZERO,
                                BigDecimal.ZERO, BigDecimal.ZERO, 0));
                boolean promotion = old.promotionManager();
                long up = old.upPlayerId();
                if (directPromotionInvite) up = actor;
                if (action.contains("SubordinateLevelAppoint") || action.contains("PromotionLevelPidAdd")) promotion = true;
                if (action.contains("SubordinateLevelDelete") || action.contains("CancleCaption")) promotion = false;
                long newParent = number(payload, "upLevelId", number(payload, "partnerPid", up));
                if (newParent > 0) up = newParent;
                extras.put(pid, new JdbcClubService.MemberExtraState(old.remarkName(), promotion, up,
                        decimal(old.clubCent()), decimal(old.caseClubCent()), decimal(old.warningPoint()),
                        (int) number(payload, "value", old.eliminatePoint())));
            }
            return JdbcClubService.copy(current, current.name(), current.status(), Map.copyOf(members),
                    current.templates(), current.tables(), current.invites(), current.records(),
                    current.ledger(), settings, current.applications(), extras, current.groupings(),
                    current.roomBans(), current.viewedRooms());
        });
        return Map.of("saved", true, "clubId", state.id());
    }

    private void requirePromotionInvitePermission(JdbcClubService.State state, long actor) {
        requireMember(state, actor);
        String role = state.members().get(actor);
        JdbcClubService.MemberExtraState actorExtra = extra(state, actor);
        if (!"OWNER".equals(role) && !"ADMIN".equals(role) && !"MANAGER".equals(role)
                && !actorExtra.promotionManager()) {
            throw new SecurityException("promotion invite permission required");
        }
    }

    private Object promotionInfo(long actor, Map<String, Object> payload) {
        long pid = number(payload, "pid", number(payload, "opPid", actor));
        Map<String, Object> player = profile(pid);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("player", player);
        out.put("sign", true);
        out.put("reservedValue", 0);
        out.put("minShareValue", 0);
        out.put("minShareFixedValue", 0);
        out.put("minAllowShareToValue", 0);
        out.put("promotionShareSectionItems", List.of(section(1, 0, 100, 0)));
        return out;
    }

    private Map<String, Object> promotionList(long actor, Map<String, Object> payload) {
        JdbcClubService.State state = requireMember(clubId(payload), actor);
        List<Map<String, Object>> rows = selfFirst(directMembers(state, actor), actor).stream()
                .map(pid -> promotionRow(state, pid))
                .toList();
        return Map.of("clubPromotionLevelItemList", rows, "clubTeamListInfoList", rows,
                "showList", List.of(1, 2, 3), "showListSecond", List.of(), "dateType", List.of(0, 1, 2, 3));
    }

    private Map<String, Object> promotionRow(JdbcClubService.State state, long pid) {
        JdbcClubService.MemberExtraState extra = extra(state, pid);
        Map<String, Object> row = new LinkedHashMap<>(profile(pid));
        if (extra.remarkName() != null && !extra.remarkName().isBlank()) row.put("name", extra.remarkName());
        row.put("promotion", extra.promotionManager() || extra.upPlayerId() > 0);
        row.put("number", state.members().keySet().stream().filter(member -> extra(state, member).upPlayerId() == pid).count());
        row.put("setCount", state.records().size());
        row.put("entryFee", 0);
        row.put("actualEntryFee", 0);
        row.put("shareType", 0);
        row.put("shareValue", 0);
        row.put("shareFixedValue", 0);
        BigDecimal balance = memberClubCent(state, pid);
        row.put("scorePoint", balance);
        row.put("clubCent", balance);
        row.put("sumClubCent", balance);
        row.put("level", extra.promotionManager() ? 1 : 0);
        row.put("myisminister", ministerCode(state.members().get(pid)));
        row.put("clubCentWarning", decimal(extra.warningPoint()));
        row.put("personalClubCentWarning", decimal(extra.warningPoint()));
        row.put("warnStatus", decimal(extra.warningPoint()).signum() == 0 ? 0 : 1);
        row.put("alivePoint", extra.eliminatePoint());
        row.put("alivePointStatus", extra.eliminatePoint() == 0 ? 0 : 1);
        row.put("eliminatePoint", extra.eliminatePoint());
        row.put("clubSign", state.id());
        row.put("createId", ownerId(state));
        return row;
    }

    /** 2.22 队长页面：返回目标队长本人及其直属下级，响应必须保持为数组。 */
    private List<Map<String, Object>> subordinateList(long actor, Map<String, Object> payload) {
        JdbcClubService.State state = requireMember(clubId(payload), actor);
        long requestedPid = number(payload, "pid", actor);
        long parentPid = state.members().containsKey(requestedPid) ? requestedPid : state.members().keySet().stream()
                .filter(memberPid -> Objects.equals(String.valueOf(profile(memberPid).get("pid")), Long.toString(requestedPid)))
                .findFirst().orElse(0L);
        if (parentPid <= 0) throw new IllegalArgumentException("subordinate parent not found");
        if (parentPid != actor) requireManager(state, actor);

        String query = text(payload, "query", "").trim();
        List<Map<String, Object>> rows = selfFirst(state.members().keySet(), actor).stream()
                .filter(pid -> pid == parentPid || extra(state, pid).upPlayerId() == parentPid)
                .map(pid -> {
                    Map<String, Object> row = new LinkedHashMap<>(profile(pid));
                    row.put("accountId", pid);
                    row.put("curActiveValue", extra(state, pid).eliminatePoint());
                    return row;
                })
                .filter(row -> query.isBlank()
                        || String.valueOf(row.get("pid")).contains(query)
                        || String.valueOf(row.get("name")).contains(query))
                .toList();
        return page(rows, (int) number(payload, "pageNum", 1));
    }

    private Map<String, Object> createClub(long actor, String requestId, Map<String, Object> payload) {
        String name = text(payload, "clubName", text(payload, "name", ""));
        if (name.isBlank()) throw new IllegalArgumentException("club name is required");
        RuntimeException last = null;
        for (int attempt = 0; attempt < 16; attempt++) {
            long id = 100000 + Math.floorMod(clock.millis() + actor * 31 + attempt * 7919, 900000);
            try {
                JdbcClubService.State state = clubs.create(key(requestId, "club.CClubCreate." + attempt), id, actor, name,
                        connection -> billing.debit(connection, key(requestId, "club.CClubCreate.billing"), actor,
                                "CRYSTAL", CLUB_CREATE_COST_CRYSTAL, "CLUB_CREATE"));
                Map<String, Object> result = clubDetail(state, actor);
                long balance = currencyBalance(actor, "CRYSTAL");
                result.put("diamond", balance);
                result.put("roomCard", balance);
                return result;
            } catch (RuntimeException failure) {
                last = failure;
                if (!String.valueOf(failure.getMessage()).contains("club exists")) throw failure;
            }
        }
        throw last == null ? new IllegalStateException("club id allocation failed") : last;
    }

    private Map<String, Object> createUnion(long actor, String requestId, Map<String, Object> payload) {
        long clubId = clubId(payload);
        String name = text(payload, "unionName", text(payload, "name", "")).strip();
        if (name.isBlank()) throw new IllegalArgumentException("union name is required");
        long unionSign = allocateUnionSign(actor, clubId);
        JdbcClubService.State state = clubs.update(key(requestId, "union.CUnionCreate"), clubId, current -> {
            requireMember(current, actor);
            if (ownerId(current) != actor) throw new SecurityException("club owner required");
            if (setting(current, "unionId", 0) > 0) throw new IllegalStateException("club already joined union");
            Map<String, Object> settings = new LinkedHashMap<>(current.settings());
            settings.put("unionId", unionSign);
            settings.put("unionSign", unionSign);
            settings.put("unionName", name);
            settings.put("unionOwnerId", actor);
            settings.put("unionOwnerClubId", clubId);
            settings.put("unionJoin", number(payload, "join", 0));
            settings.put("unionQuit", number(payload, "quit", 0));
            long unionTotalScore = UnionTotalScore.require(payload.get("unionTotalScore"));
            long outSports = number(payload, "outSports", 0);
            if (outSports > unionTotalScore) throw new IllegalArgumentException("联盟淘汰值不能大于联盟总分");
            settings.put("unionTotalScore", unionTotalScore);
            settings.put("unionMatchRate", number(payload, "matchRate", 0));
            settings.put("unionOutSports", outSports);
            settings.put("unionPrizeType", number(payload, "prizeType", 2));
            settings.put("unionRanking", number(payload, "ranking", 0));
            settings.put("unionValue", number(payload, "value", 0));
            settings.put("unionState", 0);
            // 升级联盟只切换展示域，原亲友圈模板必须留在服务端。
            // 记录升级瞬间的最大序号，联盟只读取之后创建的独立模板。
            settings.put("unionTemplateStartIndex", current.templates().stream()
                    .mapToInt(JdbcClubService.TemplateState::index).max().orElse(0));
            settings.put("unionMembers", List.of(unionMemberRow(current, actor, 3)));
            return JdbcClubService.copy(current, current.name(), current.status(), current.members(),
                    current.templates(), current.tables(), current.invites(), current.records(),
                    current.ledger(), settings, current.applications(), current.memberExtras(),
                    current.groupings(), current.roomBans(), current.viewedRooms());
        });
        return unionInfo(state);
    }

    private Map<String, Object> unionConfig(long actor, Map<String, Object> payload) {
        return unionInfo(requireMember(clubId(payload), actor));
    }

    private Map<String, Object> setUnionConfig(long actor, String requestId, Map<String, Object> payload) {
        long unionTotalScore = UnionTotalScore.require(payload.get("unionTotalScore"));
        long outSports = number(payload, "outSports", 0);
        if (outSports > unionTotalScore) throw new IllegalArgumentException("联盟淘汰值不能大于联盟总分");
        JdbcClubService.State state = clubs.update(key(requestId, "union.CUnionSetConfig"), clubId(payload), current -> {
            requireManager(current, actor);
            BigDecimal allocated = current.members().keySet().stream()
                    .filter(pid -> pid != ownerId(current))
                    .map(pid -> decimal(extra(current, pid).clubCent()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (allocated.compareTo(BigDecimal.valueOf(unionTotalScore)) > 0) {
                throw new IllegalArgumentException("联盟总分不能低于已分配积分");
            }
            Map<String, Object> settings = new LinkedHashMap<>(current.settings());
            settings.put("unionName", text(payload, "name", text(settings, "unionName", "")));
            settings.put("unionJoin", number(payload, "join", setting(current, "unionJoin", 0)));
            settings.put("unionQuit", number(payload, "quit", setting(current, "unionQuit", 0)));
            settings.put("unionTotalScore", unionTotalScore);
            settings.put("unionMatchRate", number(payload, "matchRate", setting(current, "unionMatchRate", 0)));
            settings.put("unionOutSports", outSports);
            settings.put("unionPrizeType", number(payload, "prizeType", setting(current, "unionPrizeType", 2)));
            settings.put("unionRanking", number(payload, "ranking", setting(current, "unionRanking", 0)));
            settings.put("unionValue", number(payload, "value", setting(current, "unionValue", 0)));
            settings.put("unionState", number(payload, "state", setting(current, "unionState", 0)));
            settings.put("unionJoinClubSameUnion", number(payload, "joinClubSameUnion", setting(current, "unionJoinClubSameUnion", 0)));
            settings.put("unionTableNum", number(payload, "tableNum", setting(current, "unionTableNum", 0)));
            return JdbcClubService.copy(current, current.name(), current.status(), current.members(), current.templates(),
                    current.tables(), current.invites(), current.records(), current.ledger(), settings,
                    current.applications(), current.memberExtras(), current.groupings(), current.roomBans(), current.viewedRooms());
        });
        return unionInfo(state);
    }

    private Number joinUnion(long actor, String requestId, Map<String, Object> payload) {
        long clubId = clubId(payload);
        long unionSign = number(payload, "unionSign", number(payload, "unionId", 0));
        if (unionSign <= 0) throw new IllegalArgumentException("unionSign is required");
        JdbcClubService.State ownerClub = unionByIdentifier(unionSign)
                .orElseThrow(() -> new IllegalArgumentException("union not found"));
        long unionId = setting(ownerClub, "unionId", unionSign);
        long ownerClubId = setting(ownerClub, "unionOwnerClubId", ownerClub.id());
        if (ownerClubId == clubId) return 0;
        JdbcClubService.State applicant = requireMember(clubId, actor);
        if (ownerId(applicant) != actor) throw new SecurityException("club owner required");
        if (setting(applicant, "unionId", 0) == unionId) return 0;
        if (setting(applicant, "unionId", 0) > 0) throw new IllegalStateException("club already joined union");
        if (setting(ownerClub, "unionJoin", 0) == 0) {
            clubs.update(key(requestId, "union.CUnionJoin.audit"), ownerClubId, current -> {
                Map<String, Object> settings = new LinkedHashMap<>(current.settings());
                settings.put("unionApplications", withUnionApplication(settings.get("unionApplications"), applicant, actor));
                return JdbcClubService.copy(current, current.name(), current.status(), current.members(),
                        current.templates(), current.tables(), current.invites(), current.records(),
                        current.ledger(), settings, current.applications(), current.memberExtras(),
                        current.groupings(), current.roomBans(), current.viewedRooms());
            });
            return 1;
        }
        clubs.update(key(requestId, "union.CUnionJoin.member"), clubId, current -> {
            requireMember(current, actor);
            if (ownerId(current) != actor) throw new SecurityException("club owner required");
            Map<String, Object> settings = new LinkedHashMap<>(current.settings());
            copyUnionSettings(ownerClub, settings);
            settings.put("unionMemberType", 1);
            return JdbcClubService.copy(current, current.name(), current.status(), current.members(),
                    current.templates(), current.tables(), current.invites(), current.records(),
                    current.ledger(), settings, current.applications(), current.memberExtras(),
                    current.groupings(), current.roomBans(), current.viewedRooms());
        });
        clubs.update(key(requestId, "union.CUnionJoin.owner"), ownerClubId, current -> {
            Map<String, Object> settings = new LinkedHashMap<>(current.settings());
            settings.put("unionMembers", withUnionMember(settings.get("unionMembers"), applicant, actor, 1));
            return JdbcClubService.copy(current, current.name(), current.status(), current.members(),
                    current.templates(), current.tables(), current.invites(), current.records(),
                    current.ledger(), settings, current.applications(), current.memberExtras(),
                    current.groupings(), current.roomBans(), current.viewedRooms());
        });
        return 0;
    }

    private Number dissolveUnion(long actor, String requestId, Map<String, Object> payload) {
        long clubId = clubId(payload);
        JdbcClubService.State ownerClub = requireMember(clubId, actor);
        long unionId = setting(ownerClub, "unionId", 0);
        long requestedUnionId = number(payload, "unionId", unionId);
        long ownerClubId = setting(ownerClub, "unionOwnerClubId", ownerClub.id());
        long unionOwnerId = setting(ownerClub, "unionOwnerId", ownerId(ownerClub));
        if (unionId <= 0 || requestedUnionId != unionId || ownerClubId != ownerClub.id() || unionOwnerId != actor) {
            throw new SecurityException("union owner required");
        }
        List<JdbcClubService.State> members = clubs.list().stream()
                .filter(state -> "ACTIVE".equals(state.status()))
                .filter(state -> setting(state, "unionId", 0) == unionId)
                .filter(state -> setting(state, "unionOwnerClubId", state.id()) == ownerClubId)
                // Clear member clubs first and the owner last. If the process is interrupted,
                // the still-present owner identity keeps a retry authoritative and recoverable.
                .sorted(Comparator.comparing(state -> state.id() == ownerClubId))
                .toList();
        for (JdbcClubService.State member : members) {
            clubs.update(key(requestId, "union.CUnionDissolve." + member.id()), member.id(), current -> {
                Map<String, Object> settings = new LinkedHashMap<>(current.settings());
                settings.keySet().removeIf(name -> name.startsWith("union"));
                return JdbcClubService.copy(current, current.name(), current.status(), current.members(),
                        current.templates(), current.tables(), current.invites(), current.records(), current.ledger(),
                        settings, current.applications(), current.memberExtras(), current.groupings(),
                        current.roomBans(), current.viewedRooms());
            });
        }
        return 0;
    }

    private List<Map<String, Object>> clubList(long actor) {
        return clubs.list().stream()
                .filter(state -> "ACTIVE".equals(state.status()) && state.members().containsKey(actor))
                .sorted((left, right) -> Long.compare(setting(right, "top." + actor, 0), setting(left, "top." + actor, 0)))
                .map(state -> clubSummary(state, actor))
                .toList();
    }

    private List<Map<String, Object>> pinClub(long actor, String requestId, Map<String, Object> payload) {
        long clubId = clubId(payload);
        clubs.update(key(requestId, "club.CClubTop"), clubId, current -> {
            requireMember(current, actor);
            Map<String, Object> settings = new LinkedHashMap<>(current.settings());
            settings.put("top." + actor, clock.millis());
            return JdbcClubService.copy(current, current.name(), current.status(), current.members(),
                    current.templates(), current.tables(), current.invites(), current.records(),
                    current.ledger(), settings, current.applications(), current.memberExtras(),
                    current.groupings(), current.roomBans(), current.viewedRooms());
        });
        return clubList(actor);
    }

    private Map<String, Object> joinClub(long actor, String requestId, Map<String, Object> payload) {
        long clubSign = number(payload, "clubSign", 0);
        JdbcClubService.State target = clubs.list().stream().filter(state -> state.id() == clubSign && "ACTIVE".equals(state.status())).findFirst().orElse(null);
        if (target == null) return Map.of("joinStatus", 1);
        if (target.members().containsKey(actor)) return Map.of("joinStatus", 32, "club", clubSummary(target, actor));
        JdbcClubService.State changed = clubs.update(key(requestId, "club.CClubJoin"), target.id(), current -> {
            if (current.members().containsKey(actor)) return current;
            Map<Long, String> members = new LinkedHashMap<>(current.members());
            members.put(actor, "MEMBER");
            return JdbcClubService.copy(current, current.name(), current.status(), members, current.templates(),
                    current.tables(), current.invites(), current.records(), current.ledger(), current.settings(),
                    current.applications(), current.memberExtras(), current.groupings(), current.roomBans(), current.viewedRooms());
        });
        return Map.of("joinStatus", 0, "club", clubSummary(changed, actor));
    }

    private Map<String, Object> findPlayer(long actor, Map<String, Object> payload) {
        JdbcClubService.State state = requireMember(clubId(payload), actor);
        String displayId = text(payload, "pid", "").trim();
        long accountId = accountIdByDisplayId(displayId);
        return Map.of("player", profile(accountId), "state", state.members().containsKey(accountId) ? 1 : 0);
    }

    private Map<String, Object> invitePlayer(long actor, String requestId, Map<String, Object> payload) {
        long clubId = clubId(payload);
        String displayId = text(payload, "pid", "").trim();
        long pid = accountIdByDisplayId(displayId);
        JdbcClubService.State state = clubs.addPromotionInvitedMember(
                key(requestId, "club.CClubFindPIDAdd"), clubId, actor, pid);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("playerId", displayId);
        result.put("accountId", pid);
        result.put("joined", 1);
        result.put("upPlayerId", actor);
        result.put("member", memberRow(state, pid));
        return result;
    }

    /** UI 使用公开玩家 ID；俱乐部成员关系必须保存不可变的内部账号 ID。 */
    private long accountIdByDisplayId(String displayId) {
        if (displayId == null || !displayId.matches("[1-9]\\d*")) {
            throw new IllegalArgumentException("player display ID is required");
        }
        String sql = "SELECT account_id FROM aoo_account_identity "
                + "WHERE identity_type='DISPLAY_ID' AND normalized_value=? AND status='ACTIVE'";
        try (Connection connection = source.getConnection()) {
            try (PreparedStatement query = connection.prepareStatement(sql)) {
                query.setString(1, displayId);
                try (ResultSet rows = query.executeQuery()) {
                    if (rows.next()) {
                        long accountId = rows.getLong(1);
                        if (rows.next()) throw new IllegalStateException("duplicate active player display ID");
                        return accountId;
                    }
                }
            }
            // 迁移前的账号可能没有 DISPLAY_ID 身份行；只允许回退到真实存在的账号，禁止创建虚构成员。
            try (PreparedStatement query = connection.prepareStatement("SELECT account_id FROM aoo_account WHERE account_id=?")) {
                query.setLong(1, Long.parseLong(displayId));
                try (ResultSet rows = query.executeQuery()) {
                    if (rows.next()) return rows.getLong(1);
                }
            }
            throw new IllegalArgumentException("player display ID not found");
        } catch (SQLException failure) {
            throw new IllegalStateException("player display ID lookup failed", failure);
        }
    }

    private List<Map<String, Object>> members(long actor, Map<String, Object> payload) {
        JdbcClubService.State state = requireMember(clubId(payload), actor);
        int page = Math.max(1, (int) number(payload, "pageNum", 1));
        String query = text(payload, "query", "").trim();
        boolean losePoint = number(payload, "losePoint", 0) != 0;
        Collection<Long> visibleMembers = visibleMembers(state, actor);
        List<Map<String, Object>> rows = selfFirst(visibleMembers, actor).stream()
                .map(pid -> memberRow(state, pid))
                .filter(row -> !losePoint || decimal(extra(state, ((Number) row.get("pid")).longValue()).clubCent()).signum() < 0)
                .toList();
        if (!query.isBlank()) {
            rows = rows.stream().filter(row -> {
                long pid = ((Number) row.get("pid")).longValue();
                String name = String.valueOf(((Map<?, ?>) row.get("shortPlayer")).get("name"));
                return Long.toString(pid).contains(query) || name.contains(query);
            }).toList();
        }
        int from = Math.min(rows.size(), (page - 1) * PAGE_SIZE);
        int to = Math.min(rows.size(), from + PAGE_SIZE);
        return rows.subList(from, to);
    }

    private Object onlineCount(Map<String, Object> payload) {
        long clubId = clubId(payload);
        try (Connection connection = source.getConnection();
             PreparedStatement query = connection.prepareStatement("SELECT COUNT(*) FROM aoo_club_member WHERE club_id=? AND member_status='ACTIVE' AND online=1")) {
            query.setLong(1, clubId);
            try (ResultSet rows = query.executeQuery()) { return rows.next() ? rows.getLong(1) : 0; }
        } catch (SQLException failure) {
            if (missingTable(failure)) return 0;
            throw new IllegalStateException("club online count failed", failure);
        }
    }

    private Map<String, Object> changeSetting(long actor, String requestId, Map<String, Object> payload, String name, long value) {
        JdbcClubService.State state = clubs.update(key(requestId, "club.setting." + name), clubId(payload), current -> {
            requireManager(current, actor);
            Map<String, Object> settings = new LinkedHashMap<>(current.settings());
            settings.put(name, value);
            return JdbcClubService.copy(current, current.name(), current.status(), current.members(),
                    current.templates(), current.tables(), current.invites(), current.records(), current.ledger(),
                    settings, current.applications(), current.memberExtras(), current.groupings(),
                    current.roomBans(), current.viewedRooms());
        });
        return clubDetail(state, actor);
    }

    private Map<String, Object> changeDiamondsAttention(long actor, String requestId, Map<String, Object> payload) {
        long clubId = clubId(payload);
        JdbcClubService.State state = clubs.update(key(requestId, "club.CClubChangeDiamondsAttention"), clubId, current -> {
            requireManager(current, actor);
            long minister = number(payload, "diamondsAttentionMinister", setting(current, "diamondsAttentionMinister", 500));
            long all = number(payload, "diamondsAttentionAll", setting(current, "diamondsAttentionAll", 100));
            if (minister < 0 || all < 0) throw new IllegalArgumentException("diamond attention must not be negative");
            Map<String, Object> settings = new LinkedHashMap<>(current.settings());
            settings.put("diamondsAttentionMinister", minister);
            settings.put("diamondsAttentionAll", all);
            return JdbcClubService.copy(current, current.name(), current.status(), current.members(),
                    current.templates(), current.tables(), current.invites(), current.records(), current.ledger(),
                    settings, current.applications(), current.memberExtras(), current.groupings(),
                    current.roomBans(), current.viewedRooms());
        });
        return clubDetail(state, actor);
    }

    private Map<String, Object> changePlayerStatus(long actor, String requestId, Map<String, Object> payload) {
        long clubId = clubId(payload);
        int status = (int) number(payload, "status", 0);
        // 0x40 是当前认证成员主动退出；禁止使用展示 ID 或请求中的其他成员 ID 决定权限主体。
        long pid = status == 0x40 ? actor : number(payload, "pid", 0);
        if (pid <= 0) throw new IllegalArgumentException("pid is required");
        if (status == 8) return clubDetail(clubs.kick(key(requestId, "club.CClubChangePlayerStatus.kick"), clubId, actor, pid), actor);
        if (status != 0x40) throw new IllegalArgumentException("unsupported member status action");
        JdbcClubService.State state = clubs.update(key(requestId, "club.CClubChangePlayerStatus." + status), clubId, current -> {
            requireMember(current, actor);
            if ("OWNER".equals(current.members().get(pid))) throw new SecurityException("owner cannot leave through member status");
            Map<Long, String> members = new LinkedHashMap<>(current.members());
            members.remove(pid);
            return JdbcClubService.copy(current, current.name(), current.status(), members, current.templates(),
                    current.tables(), current.invites(), current.records(), current.ledger(), current.settings(),
                    current.applications(), current.memberExtras(), current.groupings(), current.roomBans(), current.viewedRooms());
        });
        return clubDetail(state, actor);
    }

    private Map<String, Object> kickGate(long actor, Map<String, Object> payload) {
        JdbcClubService.State state = requireMember(clubId(payload), actor);
        long pid = number(payload, "pid", 0);
        JdbcClubService.MemberExtraState extra = extra(state, pid);
        boolean hasPoint = decimal(extra.clubCent()).signum() != 0 || decimal(extra.caseClubCent()).signum() != 0;
        return Map.of("type", hasPoint ? 1 : 0, "clubCent", decimal(extra.clubCent()), "caseClubCent", decimal(extra.caseClubCent()));
    }

    private Map<String, Object> setMinister(long actor, String requestId, Map<String, Object> payload) {
        int minister = (int) number(payload, "minister", 0);
        if (minister != 0 && minister != 1) throw new IllegalArgumentException("unsupported club administrator role");
        String role = minister == 1 ? "ADMIN" : "MEMBER";
        JdbcClubService.State state = clubs.setRole(key(requestId, "club.CClubSetMinister"), clubId(payload), actor, number(payload, "pid", 0), role);
        return clubDetail(state, actor);
    }

    private Map<String, Object> setPromotionManager(long actor, String requestId, Map<String, Object> payload) {
        long pid = number(payload, "pid", 0);
        boolean enabled = number(payload, "minister", 0) != 0;
        JdbcClubService.State state = updateExtra(actor, requestId, "club.CClubSetPromotionMinister", payload, pid,
                old -> new JdbcClubService.MemberExtraState(old.remarkName(), enabled, old.upPlayerId(),
                        decimal(old.clubCent()), decimal(old.caseClubCent()), decimal(old.warningPoint()), old.eliminatePoint()));
        return clubDetail(state, actor);
    }

    private Map<String, Object> changeRemark(long actor, String requestId, Map<String, Object> payload) {
        long pid = number(payload, "remarkID", number(payload, "pid", 0));
        String remark = text(payload, "remarkName", "");
        JdbcClubService.State state = updateExtra(actor, requestId, "club.CClubChangePlayerRemarkName", payload, pid,
                old -> new JdbcClubService.MemberExtraState(remark, old.promotionManager(), old.upPlayerId(),
                        decimal(old.clubCent()), decimal(old.caseClubCent()), decimal(old.warningPoint()), old.eliminatePoint()));
        return memberRow(state, pid);
    }

    private Map<String, Object> getPromotionParent(long actor, Map<String, Object> payload) {
        JdbcClubService.State state = requireMember(clubId(payload), actor);
        long pid = number(payload, "pid", 0);
        long up = extra(state, pid).upPlayerId();
        return Map.of("player", up > 0 ? profile(up) : Map.of());
    }

    private Map<String, Object> changePromotionParent(long actor, String requestId, Map<String, Object> payload) {
        long pid = number(payload, "pid", 0);
        long up = number(payload, "upLevelId", number(payload, "partnerPid", 0));
        JdbcClubService.State state = updateExtra(actor, requestId, "club.CClubChangePromotionBelong", payload, pid,
                old -> new JdbcClubService.MemberExtraState(old.remarkName(), old.promotionManager(), up,
                        decimal(old.clubCent()), decimal(old.caseClubCent()), decimal(old.warningPoint()), old.eliminatePoint()));
        return memberRow(state, pid);
    }

    private JdbcClubService.State updateExtra(long actor, String requestId, String action, Map<String, Object> payload,
            long pid, java.util.function.Function<JdbcClubService.MemberExtraState, JdbcClubService.MemberExtraState> change) {
        if (pid <= 0) throw new IllegalArgumentException("pid is required");
        return clubs.update(key(requestId, action), clubId(payload), current -> {
            requireManager(current, actor);
            if (!current.members().containsKey(pid)) throw new IllegalArgumentException("club member missing");
            Map<Long, JdbcClubService.MemberExtraState> extras = new LinkedHashMap<>(current.memberExtras());
            extras.put(pid, change.apply(extra(current, pid)));
            return JdbcClubService.copy(current, current.name(), current.status(), current.members(),
                    current.templates(), current.tables(), current.invites(), current.records(), current.ledger(),
                    current.settings(), current.applications(), extras, current.groupings(),
                    current.roomBans(), current.viewedRooms());
        });
    }

    private List<Map<String, Object>> roomList(long actor, Map<String, Object> payload) {
        JdbcClubService.State memberClub = requireMember(clubId(payload), actor);
        long unionId = number(payload, "unionId", 0);
        final JdbcClubService.State state;
        if (unionId > 0) {
            long expectedUnionId = setting(memberClub, "unionId", 0);
            if (expectedUnionId != unionId) throw new SecurityException("active union membership required");
            state = unionByIdentifier(unionId).orElseThrow(() -> new IllegalStateException("union not found"));
        } else state = memberClub;
        List<Map<String, Object>> projected = queryHallClubRooms(state.id());
        if (unionId <= 0) {
            return projected.isEmpty()
                    ? state.tables().stream().map(table -> tableRow(state, table)).toList() : projected;
        }
        List<JdbcClubService.TemplateState> templates = unionTemplates(state);
        Set<String> templateIds = new HashSet<>(templates.stream().map(JdbcClubService.TemplateState::id).toList());
        Set<Integer> templateIndexes = new HashSet<>(templates.stream().map(JdbcClubService.TemplateState::index).toList());
        if (projected.isEmpty()) {
            return state.tables().stream().filter(table -> templateIds.contains(table.templateId()))
                    .map(table -> tableRow(state, table)).toList();
        }
        return projected.stream().filter(room -> {
            String templateCode = text(room, "templateCode", text(room, "clubTemplateCode", ""));
            int templateIndex = (int) number(room, "tagId", number(room, "configId", number(room, "gameIndex", 0)));
            return templateIds.contains(templateCode) || templateIndexes.contains(templateIndex);
        }).toList();
    }

    private Map<String, Object> roomDetail(long actor, Map<String, Object> payload) {
        List<Map<String, Object>> rooms = roomList(actor, payload);
        String key = String.valueOf(payload.getOrDefault("roomKey", ""));
        Map<String, Object> room = rooms.stream()
                .filter(item -> key.equals(String.valueOf(item.get("roomKey"))))
                .findFirst().orElse(null);
        if (room == null) {
            // In 2.2.2 an occupied room and its reusable empty template coexist.
            // The active-room projection therefore must not make the template's
            // detail request disappear merely because at least one real room exists.
            List<Map<String, Object>> templates = number(payload, "unionId", 0) > 0
                    ? unionQuickRoomConfigs(actor, payload) : quickRoomConfigs(actor, payload);
            room = templates.stream().filter(item -> key.equals(String.valueOf(item.get("roomKey")))
                            || key.equals(String.valueOf(item.get("configId")))
                            || key.equals(String.valueOf(item.get("gameIndex")))
                            || key.equals(String.valueOf(item.get("tagId"))))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("room not found"));
        }
        JdbcClubService.State memberClub = requireMember(clubId(payload), actor);
        Map<String, Object> detail = new LinkedHashMap<>(room);
        Object rules = room.get("rules");
        if (rules instanceof String encoded) {
            try { rules = json.readValue(encoded, Map.class); }
            catch (Exception ignored) { rules = Map.of(); }
        }
        if (!(rules instanceof Map<?, ?>)) rules = room.getOrDefault("bRoomConfigure", Map.of());
        detail.put("clubId", memberClub.id());
        detail.put("unionId", number(payload, "unionId", 0));
        detail.put("name", memberClub.name());
        detail.put("roomID", numberValue(room.getOrDefault("roomId", 0)));
        detail.put("roomKey", room.getOrDefault("roomKey", key));
        detail.put("roomCfg", rules);
        detail.putIfAbsent("posList", List.of());
        detail.put("isManage", setting(memberClub, "minister", 0) > 0 || ownerId(memberClub) == actor ? 1 : 0);
        return detail;
    }

    private List<Map<String, Object>> quickRoomConfigs(long actor, Map<String, Object> payload) {
        JdbcClubService.State state = requireMember(clubId(payload), actor);
        return quickRoomConfigs(state);
    }

    private List<Map<String, Object>> unionQuickRoomConfigs(long actor, Map<String, Object> payload) {
        JdbcClubService.State memberClub = requireMember(clubId(payload), actor);
        long expectedUnionId = setting(memberClub, "unionId", 0);
        long requestedUnionId = number(payload, "unionId", 0);
        if (expectedUnionId <= 0 || requestedUnionId != expectedUnionId) {
            throw new SecurityException("active union membership required");
        }
        JdbcClubService.State ownerClub = unionByIdentifier(requestedUnionId)
                .orElseThrow(() -> new IllegalStateException("union not found"));
        return quickRoomConfigs(ownerClub, enabledUnionTemplates(ownerClub));
    }

    private List<Map<String, Object>> quickRoomConfigs(JdbcClubService.State state) {
        return quickRoomConfigs(state, enabledTemplates(state));
    }

    private List<Map<String, Object>> quickRoomConfigs(
            JdbcClubService.State state, List<JdbcClubService.TemplateState> templates) {
        return templates.stream().map(template -> {
            Map<String, Object> row = new LinkedHashMap<>();
            String gameCode = ruleText(template.rules(), "gameCode", template.game());
            int playerNum = ruleNumber(template.rules(), "playerNum",
                    ruleNumber(template.rules(), "playerCount", 0));
            row.put("tagId", template.index());
            row.put("templateCode", template.id());
            row.put("configId", template.index());
            row.put("gameIndex", template.index());
            row.put("gameId", gameNumber(gameCode));
            row.put("gameType", gameCode);
            row.put("gameCode", gameCode);
            row.put("name", template.name());
            row.put("roomName", template.name());
            row.put("rules", template.rules());
            try { row.put("bRoomConfigure", json.readValue(template.rules(), Map.class)); }
            catch (Exception ignored) { row.put("bRoomConfigure", Map.of()); }
            row.put("setCount", ruleNumber(template.rules(), "setCount",
                    ruleNumber(template.rules(), "roundCount", 0)));
            row.put("playerNum", playerNum);
            row.put("size", playerNum);
            return row;
        }).toList();
    }

    /** Authoritative template snapshot used by Hall when a member taps an idle club desk. */
    public Map<String, Object> roomTemplateForEntry(long actor, long clubId, long gameIndex) {
        JdbcClubService.State state = requireMember(clubId, actor);
        JdbcClubService.TemplateState template = state.templates().stream()
                .filter(item -> item.index() == gameIndex).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("club room template not found"));
        Object decoded;
        try { decoded = json.readValue(template.rules(), Map.class); }
        catch (Exception failure) { throw new IllegalStateException("club room template rules are invalid", failure); }
        if (!(decoded instanceof Map<?, ?> raw)) throw new IllegalStateException("club room template rules are invalid");
        Map<String, Object> rules = new LinkedHashMap<>();
        raw.forEach((key, value) -> rules.put(String.valueOf(key), value));
        // 历史模板主字段可能保存数字 gameId（如 90005），但规则快照已经携带
        // 唯一稳定业务编码 LS201。进房必须使用业务编码查询 Catalog，不能把代理主键
        // 当作 game_code，否则模板能展示却无法创建实体桌。
        String gameCode = Objects.toString(rules.get("gameCode"), "").strip();
        if (gameCode.isBlank()) gameCode = template.game();
        return Map.of("clubId", state.id(), "ownerId", ownerId(state), "templateCode", template.id(),
                "gameCode", gameCode, "gameIndex", template.index(), "rules", Map.copyOf(rules));
    }

    private Map<String, Object> managedRoomConfigs(long actor, Map<String, Object> payload) {
        JdbcClubService.State state = requireMember(clubId(payload), actor);
        List<Map<String, Object>> rows = state.templates().stream().map(template -> managedRoom(state, template)).toList();
        long playing = roomList(actor, payload).stream().filter(row -> "PLAYING".equals(row.get("state"))).count();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("clubId", state.id());
        out.put("clubCreateGameSets", rows);
        out.put("waitRoomCount", Math.max(0, roomList(actor, payload).size() - playing));
        out.put("playingRoomCount", playing);
        return out;
    }

    private Map<String, Object> saveRoomConfig(long actor, String requestId, Map<String, Object> payload) {
        long clubId = clubId(payload);
        int gameIndex = (int) number(payload, "gameIndex", 0);
        String templateId = text(payload, "templateCode", gameIndex > 0 ? "template-" + gameIndex : UUID.randomUUID().toString());
        String name = text(payload, "roomName", text(payload, "name", "玩法配置" + (gameIndex > 0 ? gameIndex : "")));
        String game = text(payload, "gameType", text(payload, "gameId", "0"));
        String rules = text(payload, "rules", "");
        if (rules.isBlank()) {
            try { rules = json.writeValueAsString(payload); }
            catch (Exception failure) { rules = "{}"; }
        }
        JdbcClubService.State state = clubs.saveTemplate(key(requestId, "club.CClubCreateGameSet"), clubId, actor, templateId, name, game, rules);
        return managedRoom(state, state.templates().stream().filter(template -> template.id().equals(templateId)).findFirst().orElseThrow());
    }

    private List<Map<String, Object>> unionRoomConfigs(long actor, Map<String, Object> payload) {
        JdbcClubService.State state = requireUnionManager(payload, actor);
        int page = Math.max(1, (int) number(payload, "pageNum", 1));
        int classType = (int) number(payload, "classType", 0);
        return unionTemplates(state).stream()
                .filter(template -> classType == 0 || gameClass(template.game()) == classType)
                .skip((long) (page - 1) * PAGE_SIZE).limit(PAGE_SIZE)
                .map(template -> unionRoomConfig(state, template)).toList();
    }

    private Map<String, Object> unionRoomConfigCount(long actor, Map<String, Object> payload) {
        JdbcClubService.State state = requireUnionManager(payload, actor);
        int classType = (int) number(payload, "classType", 0);
        List<JdbcClubService.TemplateState> unionTemplates = unionTemplates(state);
        Set<String> unionTemplateIds = new HashSet<>(unionTemplates.stream()
                .map(JdbcClubService.TemplateState::id).toList());
        long roomCount = state.tables().stream().filter(table -> !"DISSOLVED".equals(table.status()))
                .filter(table -> unionTemplateIds.contains(table.templateId()))
                .filter(table -> classType == 0 || unionTemplates.stream()
                        .filter(template -> template.id().equals(table.templateId()))
                        .anyMatch(template -> gameClass(template.game()) == classType)).count();
        return Map.of("roomCount", roomCount, "playerCount", 0,
                "sort", setting(state, "unionRoomSort", 0));
    }

    private Map<String, Object> saveUnionRoomConfig(long actor, String requestId, Map<String, Object> payload) {
        JdbcClubService.State current = requireUnionManager(payload, actor);
        if (!current.settings().containsKey("unionTemplateStartIndex")) {
            current = initializeUnionTemplateBoundary(current, actor, requestId);
        }
        int gameIndex = (int) number(payload, "gameIndex", 0);
        String templateId = unionTemplates(current).stream().filter(template -> template.index() == gameIndex)
                .map(JdbcClubService.TemplateState::id).findFirst().orElseGet(() -> UUID.randomUUID().toString());
        String name = text(payload, "roomName", "玩法配置");
        String game = text(payload, "gameCode", text(payload, "gameId", "0"));
        String rules;
        try { rules = json.writeValueAsString(payload); }
        catch (Exception failure) { throw new IllegalArgumentException("invalid union room configuration", failure); }
        JdbcClubService.State state = clubs.saveTemplate(key(requestId, "union.CUnionCreateRoom"),
                current.id(), actor, templateId, name, game, rules);
        JdbcClubService.TemplateState saved = state.templates().stream()
                .filter(template -> template.id().equals(templateId)).findFirst().orElseThrow();
        return unionRoomConfig(state, saved);
    }

    private Map<String, Object> unionRoomConfig(JdbcClubService.State state, JdbcClubService.TemplateState template) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", template.index());
        row.put("roomName", template.name());
        row.put("gameId", gameNumber(template.game()));
        row.put("playingCount", state.tables().stream().filter(table -> table.templateId().equals(template.id())
                && "PLAYING".equals(table.status())).count());
        row.put("isSelect", setting(state, "templateStatus." + template.index(), 0) == 0);
        row.put("status", setting(state, "templateStatus." + template.index(), 0));
        try { row.put("bRoomConfigure", json.readValue(template.rules(), Map.class)); }
        catch (Exception ignored) { row.put("bRoomConfigure", Map.of()); }
        return row;
    }

    private List<Map<String, Object>> updateUnionRoomConfig(long actor, String requestId, Map<String, Object> payload) {
        JdbcClubService.State ownerClub = requireUnionManager(payload, actor);
        int gameIndex = (int) number(payload, "unionRoomCfgId", 0);
        int status = (int) number(payload, "status", -1);
        if (gameIndex <= 0 || status < 0 || status > 2) throw new IllegalArgumentException("invalid union room configuration update");
        JdbcClubService.State state = clubs.update(key(requestId, "union.CUnionRoomCfgUpdate"), ownerClub.id(), current -> {
            requireManager(current, actor);
            boolean exists = unionTemplates(current).stream().anyMatch(template -> template.index() == gameIndex);
            if (!exists) throw new IllegalStateException("union room configuration not found");
            List<JdbcClubService.TemplateState> templates = status == 2
                    ? current.templates().stream().filter(template -> template.index() != gameIndex).toList()
                    : current.templates();
            Map<String, Object> settings = new LinkedHashMap<>(current.settings());
            settings.remove("templateStatus." + gameIndex);
            if (status != 2) settings.put("templateStatus." + gameIndex, status);
            return JdbcClubService.copy(current, current.name(), current.status(), current.members(),
                    templates, current.tables(), current.invites(), current.records(), current.ledger(),
                    settings, current.applications(), current.memberExtras(), current.groupings(),
                    current.roomBans(), current.viewedRooms());
        });
        return unionTemplates(state).stream().map(template -> unionRoomConfig(state, template)).toList();
    }

    private JdbcClubService.State requireUnionManager(Map<String, Object> payload, long actor) {
        long requested = number(payload, "unionId", 0);
        if (requested <= 0) throw new SecurityException("active union membership required");
        JdbcClubService.State ownerClub = unionByIdentifier(requested)
                .orElseThrow(() -> new SecurityException("active union membership required"));
        requireManager(ownerClub, actor);
        return ownerClub;
    }

    private int gameClass(String game) {
        String normalized = Objects.toString(game, "").toUpperCase(Locale.ROOT);
        if (normalized.matches("^[A-Z]+1\\d{2}$")) return 1;
        if (normalized.matches("^[A-Z]+2\\d{2}$")) return 2;
        long gameId = gameNumber(normalized);
        return gameId >= 200 && gameId < 300 ? 2 : gameId >= 100 && gameId < 200 ? 1 : 0;
    }

    private Map<String, Object> changeRoomConfig(long actor, String requestId, Map<String, Object> payload) {
        int gameIndex = (int) number(payload, "gameIndex", 0);
        int status = (int) number(payload, "status", 0);
        JdbcClubService.State state = clubs.update(key(requestId, "club.CClubCreateGameSetChangge"), clubId(payload), current -> {
            requireManager(current, actor);
            List<JdbcClubService.TemplateState> templates = status == 2
                    ? current.templates().stream().filter(template -> template.index() != gameIndex).toList()
                    : current.templates();
            Map<String, Object> settings = new LinkedHashMap<>(current.settings());
            if (status != 2) settings.put("templateStatus." + gameIndex, status);
            return JdbcClubService.copy(current, current.name(), current.status(), current.members(),
                    templates, current.tables(), current.invites(), current.records(), current.ledger(),
                    settings, current.applications(), current.memberExtras(), current.groupings(),
                    current.roomBans(), current.viewedRooms());
        });
        return managedRoomConfigs(actor, Map.of("clubId", state.id()));
    }

    private List<Map<String, Object>> groupingList(long actor, Map<String, Object> payload) {
        JdbcClubService.State state = requireMember(clubId(payload), actor);
        long one = number(payload, "pidOne", 0);
        long two = number(payload, "pidTwo", 0);
        return state.groupings().stream()
                .filter(group -> (one <= 0 || group.playerIds().contains(one)) && (two <= 0 || group.playerIds().contains(two)))
                .map(group -> groupingRow(state, group))
                .toList();
    }

    private List<Map<String, Object>> addGrouping(long actor, String requestId, Map<String, Object> payload) {
        JdbcClubService.State state = clubs.update(key(requestId, "club.CClubGroupingAdd"), clubId(payload), current -> {
            requireManager(current, actor);
            long id = current.groupings().stream().mapToLong(JdbcClubService.GroupingState::id).max().orElse(0) + 1;
            List<JdbcClubService.GroupingState> groups = new ArrayList<>(current.groupings());
            groups.add(new JdbcClubService.GroupingState(id, List.of()));
            return JdbcClubService.copy(current, current.name(), current.status(), current.members(),
                    current.templates(), current.tables(), current.invites(), current.records(), current.ledger(),
                    current.settings(), current.applications(), current.memberExtras(), List.copyOf(groups),
                    current.roomBans(), current.viewedRooms());
        });
        return groupingList(actor, Map.of("clubId", state.id()));
    }

    private List<Map<String, Object>> removeGrouping(long actor, String requestId, Map<String, Object> payload) {
        long groupId = number(payload, "groupingId", 0);
        JdbcClubService.State state = clubs.update(key(requestId, "club.CClubGroupingRemove"), clubId(payload), current -> {
            requireManager(current, actor);
            return JdbcClubService.copy(current, current.name(), current.status(), current.members(),
                    current.templates(), current.tables(), current.invites(), current.records(), current.ledger(),
                    current.settings(), current.applications(), current.memberExtras(),
                    current.groupings().stream().filter(group -> group.id() != groupId).toList(),
                    current.roomBans(), current.viewedRooms());
        });
        return groupingList(actor, Map.of("clubId", state.id()));
    }

    private List<Map<String, Object>> groupingMembers(long actor, Map<String, Object> payload) {
        JdbcClubService.State state = requireMember(clubId(payload), actor);
        long groupId = number(payload, "groupingId", 0);
        Set<Long> selected = state.groupings().stream().filter(group -> group.id() == groupId).findFirst()
                .map(group -> new LinkedHashSet<>(group.playerIds())).orElseGet(LinkedHashSet::new);
        return selfFirst(state.members().keySet(), actor).stream().limit(200)
                .map(pid -> Map.of("player", profile(pid), "isBan", selected.contains(pid))).toList();
    }

    private Map<String, Object> groupingFind(long actor, Map<String, Object> payload) {
        JdbcClubService.State state = requireMember(clubId(payload), actor);
        long pid = number(payload, "pid", 0);
        if (!state.members().containsKey(pid)) throw new IllegalArgumentException("club member missing");
        return profile(pid);
    }

    private List<Map<String, Object>> groupingPid(long actor, String requestId, Map<String, Object> payload, boolean add) {
        long groupId = number(payload, "groupingId", 0);
        long pid = number(payload, "pid", 0);
        JdbcClubService.State state = clubs.update(key(requestId, "club.CClubGroupingPid" + (add ? "Add" : "Remove")), clubId(payload), current -> {
            requireManager(current, actor);
            if (!current.members().containsKey(pid)) throw new IllegalArgumentException("club member missing");
            List<JdbcClubService.GroupingState> groups = new ArrayList<>();
            boolean found = false;
            for (JdbcClubService.GroupingState group : current.groupings()) {
                if (group.id() != groupId) { groups.add(group); continue; }
                found = true;
                LinkedHashSet<Long> players = new LinkedHashSet<>(group.playerIds());
                if (add && players.size() >= 15 && !players.contains(pid)) throw new IllegalArgumentException("grouping full");
                if (add) players.add(pid); else players.remove(pid);
                groups.add(new JdbcClubService.GroupingState(group.id(), List.copyOf(players)));
            }
            if (!found) throw new IllegalArgumentException("grouping missing");
            return JdbcClubService.copy(current, current.name(), current.status(), current.members(),
                    current.templates(), current.tables(), current.invites(), current.records(), current.ledger(),
                    current.settings(), current.applications(), current.memberExtras(), groups,
                    current.roomBans(), current.viewedRooms());
        });
        return groupingList(actor, Map.of("clubId", state.id()));
    }

    private Map<String, Object> banRoomList(long actor, Map<String, Object> payload) {
        JdbcClubService.State state = requireMember(clubId(payload), actor);
        long pid = number(payload, "opPid", number(payload, "pid", 0));
        JdbcClubService.BanRoomState ban = state.roomBans().getOrDefault(pid, new JdbcClubService.BanRoomState(false, Set.of()));
        List<Map<String, Object>> rows = state.templates().stream().map(template -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("configId", template.index());
            row.put("gameId", gameNumber(template.game()));
            row.put("roomName", template.name());
            row.put("dataJsonCfg", template.rules());
            row.put("isBan", ban.all() || ban.configIds().contains((long) template.index()) ? 1 : 0);
            return row;
        }).toList();
        return Map.of("isAll", ban.all() ? 1 : 0, "unionBanRoomConfigBOList", rows);
    }

    private Map<String, Object> banRoomSave(long actor, String requestId, Map<String, Object> payload) {
        long pid = number(payload, "opPid", number(payload, "pid", 0));
        boolean all = number(payload, "isAll", 0) != 0;
        Set<Long> ids = numbers(payload.get("configIdList"));
        JdbcClubService.State state = clubs.update(key(requestId, "club.CClubBanRoomConigOp"), clubId(payload), current -> {
            requireManager(current, actor);
            if (!current.members().containsKey(pid)) throw new IllegalArgumentException("club member missing");
            Map<Long, JdbcClubService.BanRoomState> bans = new LinkedHashMap<>(current.roomBans());
            bans.put(pid, new JdbcClubService.BanRoomState(all, ids));
            return JdbcClubService.copy(current, current.name(), current.status(), current.members(),
                    current.templates(), current.tables(), current.invites(), current.records(), current.ledger(),
                    current.settings(), current.applications(), current.memberExtras(), current.groupings(),
                    bans, current.viewedRooms());
        });
        return banRoomList(actor, Map.of("clubId", state.id(), "opPid", pid));
    }

    private Map<String, Object> clubCentInfo(long actor, Map<String, Object> payload) {
        JdbcClubService.State state = requireMember(clubId(payload), actor);
        long pid = number(payload, "opPid", number(payload, "pid", actor));
        JdbcClubService.MemberExtraState target = extra(state, pid);
        BigDecimal allow = allowClubCent(state, actor);
        return Map.of("clubCent", decimal(target.clubCent()), "caseClubCent", decimal(target.caseClubCent()), "allowClubCent", allow);
    }

    private Map<String, Object> subordinateSportsPointInfo(long actor, Map<String, Object> payload) {
        JdbcClubService.State state = requireMember(clubId(payload), actor);
        long pid = number(payload, "opPid", number(payload, "pid", 0));
        if (!directMembers(state, actor).contains(pid)) throw new SecurityException("direct subordinate required");
        return Map.of("sportsPoint", decimal(extra(state, pid).clubCent()),
                "allowSportsPoint", allowClubCent(state, actor));
    }

    private BigDecimal allowClubCent(JdbcClubService.State state, long actor) {
        return memberClubCent(state, actor).max(BigDecimal.ZERO);
    }

    /**
     * 联盟总分是守恒池。盟主余额由总分扣除所有其他成员余额得到，既避免重复
     * 保存权威值，也能自动修复旧数据中盟主 memberExtras 被写成 0 的情况。
     */
    private BigDecimal memberClubCent(JdbcClubService.State state, long pid) {
        if (!"OWNER".equals(state.members().get(pid))) return decimal(extra(state, pid).clubCent());
        BigDecimal allocated = state.members().keySet().stream()
                .filter(memberId -> memberId != pid)
                .map(memberId -> decimal(extra(state, memberId).clubCent()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return BigDecimal.valueOf(setting(state, "unionTotalScore", 0)).subtract(allocated);
    }

    private Map<String, Object> clubCentUpdate(long actor, String requestId, Map<String, Object> payload) {
        long pid = number(payload, "opPid", number(payload, "pid", 0));
        int type = (int) number(payload, "type", 0);
        BigDecimal value = decimal(payload.get("value"));
        if (value.signum() <= 0) throw new IllegalArgumentException("club cent value is required");
        JdbcClubService.State state = clubs.update(key(requestId, "club.CClubCentUpdate"), clubId(payload), current -> {
            requireManager(current, actor);
            if (!current.members().containsKey(pid)) throw new IllegalArgumentException("club member missing");
            if (type != 0 && type != 1) throw new IllegalArgumentException("unsupported club cent action");
            boolean unionTransfer = setting(current, "unionId", 0) > 0;
            if (unionTransfer && pid == actor) throw new IllegalArgumentException("cannot transfer club cent to self");
            BigDecimal targetBalance = memberClubCent(current, pid);
            if (type == 1 && targetBalance.compareTo(value) < 0) throw new IllegalArgumentException("成员俱乐部积分不足");
            BigDecimal actorBalance = memberClubCent(current, actor);
            if (unionTransfer && type == 0 && actorBalance.compareTo(value) < 0) throw new IllegalArgumentException("可操作俱乐部积分不足");
            BigDecimal changed = targetBalance.add(type == 0 ? value : value.negate());
            BigDecimal actorChanged = actorBalance.add(type == 0 ? value.negate() : value);
            JdbcClubService.MemberExtraState old = extra(current, pid);
            JdbcClubService.MemberExtraState actorOld = extra(current, actor);
            Map<Long, JdbcClubService.MemberExtraState> extras = new LinkedHashMap<>(current.memberExtras());
            extras.put(pid, new JdbcClubService.MemberExtraState(old.remarkName(), old.promotionManager(), old.upPlayerId(),
                    changed, decimal(old.caseClubCent()), decimal(old.warningPoint()), old.eliminatePoint()));
            if (unionTransfer) extras.put(actor, new JdbcClubService.MemberExtraState(actorOld.remarkName(), actorOld.promotionManager(), actorOld.upPlayerId(),
                    actorChanged, decimal(actorOld.caseClubCent()), decimal(actorOld.warningPoint()), actorOld.eliminatePoint()));
            List<JdbcClubService.LedgerState> ledger = new ArrayList<>(current.ledger());
            ledger.add(new JdbcClubService.LedgerState(key(requestId, "ledger"), pid, type == 0 ? value : value.negate(), "CLUB_CENT", clock.instant()));
            if (unionTransfer) ledger.add(new JdbcClubService.LedgerState(key(requestId, "ledger.actor"), actor, type == 0 ? value.negate() : value, "CLUB_CENT", clock.instant()));
            return JdbcClubService.copy(current, current.name(), current.status(), current.members(),
                    current.templates(), current.tables(), current.invites(), current.records(), List.copyOf(ledger),
                    current.settings(), current.applications(), extras, current.groupings(),
                    current.roomBans(), current.viewedRooms());
        });
        return Map.of("type", type, "value", value, "changedValue", memberClubCent(state, pid),
                "operatorChangedValue", memberClubCent(state, actor));
    }

    private Map<String, Object> caseClubCentChange(long actor, String requestId, Map<String, Object> payload) {
        BigDecimal value = decimal(payload.get("value"));
        int type = (int) number(payload, "type", 0);
        if (value.signum() <= 0) throw new IllegalArgumentException("case sports value is required");
        JdbcClubService.State state = clubs.update(key(requestId, "club.CClubGetCaseSprotsChange"), clubId(payload), current -> {
            requireMember(current, actor);
            JdbcClubService.MemberExtraState old = extra(current, actor);
            BigDecimal sports = decimal(old.clubCent());
            BigDecimal box = decimal(old.caseClubCent());
            if (type == 0 && sports.compareTo(value) < 0) throw new IllegalArgumentException("club cent insufficient");
            if (type == 1 && box.compareTo(value) < 0) throw new IllegalArgumentException("case club cent insufficient");
            Map<Long, JdbcClubService.MemberExtraState> extras = new LinkedHashMap<>(current.memberExtras());
            extras.put(actor, new JdbcClubService.MemberExtraState(old.remarkName(), old.promotionManager(), old.upPlayerId(),
                    type == 0 ? sports.subtract(value) : sports.add(value),
                    type == 0 ? box.add(value) : box.subtract(value), decimal(old.warningPoint()), old.eliminatePoint()));
            return JdbcClubService.copy(current, current.name(), current.status(), current.members(),
                    current.templates(), current.tables(), current.invites(), current.records(), current.ledger(),
                    current.settings(), current.applications(), extras, current.groupings(),
                    current.roomBans(), current.viewedRooms());
        });
        JdbcClubService.MemberExtraState changed = extra(state, actor);
        return Map.of("clubCent", decimal(changed.clubCent()), "caseClubCent", decimal(changed.caseClubCent()));
    }

    private List<Map<String, Object>> dynamicRows(long actor, Map<String, Object> payload) {
        JdbcClubService.State state = requireMember(clubId(payload), actor);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (JdbcClubService.ApplicationState app : state.applications()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("execType", switch (app.status()) {
                case "PENDING" -> "JOIN".equals(app.type()) ? 4 : 5;
                case "APPROVED" -> "JOIN".equals(app.type()) ? 30 : 31;
                default -> 2;
            });
            row.put("createTime", app.createdAt().toString());
            row.put("pid", app.playerId());
            row.put("playerName", profile(app.playerId()).get("name"));
            row.put("execPid", actor);
            row.put("execName", profile(actor).get("name"));
            rows.add(row);
        }
        rows.addAll(clubCentDynamicRows(state, 0));
        rows.sort((a, b) -> String.valueOf(b.get("createTime")).compareTo(String.valueOf(a.get("createTime"))));
        return page(rows, (int) number(payload, "pageNum", 1));
    }

    private List<Map<String, Object>> clubCentDynamic(long actor, Map<String, Object> payload) {
        JdbcClubService.State state = requireMember(clubId(payload), actor);
        long pid = number(payload, "pid", number(payload, "opPid", 0));
        return page(clubCentDynamicRows(state, pid), (int) number(payload, "pageNum", 1));
    }

    private Map<String, Object> recordSummary(long actor, Map<String, Object> payload) {
        JdbcClubService.State state = requireMember(clubId(payload), actor);
        return Map.of("pageNumTotal", Math.max(1, pages(state.records().size())),
                "roomCardTotalCount", 0, "roomTotalCount", state.records().size());
    }

    private Map<String, Object> records(long actor, Map<String, Object> payload) {
        JdbcClubService.State state = requireMember(clubId(payload), actor);
        List<Map<String, Object>> rows = page(state.records().stream().map(record -> recordRow(state, record)).toList(),
                (int) number(payload, "pageNum", 1));
        return Map.of("clubRecordInfos", rows, "pageNumTotal", Math.max(1, pages(state.records().size())));
    }

    private Map<String, Object> markRoomViewed(long actor, String requestId, Map<String, Object> payload) {
        JdbcClubService.State state = clubs.update(key(requestId, "club.CClubRoomIdOperation"), clubId(payload), current -> {
            requireMember(current, actor);
            Set<String> viewed = new LinkedHashSet<>(current.viewedRooms());
            String room = String.valueOf(payload.getOrDefault("roomID", payload.getOrDefault("roomId", "")));
            if (number(payload, "type", 0) == 0) viewed.remove(room); else viewed.add(room);
            return JdbcClubService.copy(current, current.name(), current.status(), current.members(),
                    current.templates(), current.tables(), current.invites(), current.records(), current.ledger(),
                    current.settings(), current.applications(), current.memberExtras(), current.groupings(),
                    current.roomBans(), viewed);
        });
        return Map.of("clubId", state.id(), "viewed", state.viewedRooms());
    }

    private List<Map<String, Object>> personalRecords(long actor, Map<String, Object> payload) {
        JdbcClubService.State state = requireMember(clubId(payload), actor);
        return page(state.records().stream()
                .filter(record -> record.scores().containsKey(actor))
                .map(record -> recordRow(state, record))
                .toList(), (int) number(payload, "pageNum", 1));
    }

    private List<Map<String, Object>> playerRecordRows(long actor, Map<String, Object> payload) {
        JdbcClubService.State state = requireMember(clubId(payload), actor);
        return selfFirst(state.members().keySet(), actor).stream().map(pid -> {
            int sum = state.records().stream().mapToInt(record -> record.scores().getOrDefault(pid, 0)).sum();
            long size = state.records().stream().filter(record -> record.scores().containsKey(pid)).count();
            Map<String, Object> row = new LinkedHashMap<>(profile(pid));
            row.put("point", sum);
            row.put("clubCent", decimal(extra(state, pid).clubCent()));
            row.put("size", size);
            row.put("winner", state.records().stream().filter(record -> record.scores().getOrDefault(pid, 0) > 0).count());
            return row;
        }).toList();
    }

    /** Stable list policy: the authenticated account is first, all other rows retain source order. */
    private static List<Long> selfFirst(Collection<Long> ids, long actor) {
        List<Long> ordered = new ArrayList<>(ids.size());
        if (ids.contains(actor)) ordered.add(actor);
        for (long id : ids) if (id != actor) ordered.add(id);
        return List.copyOf(ordered);
    }

    /** Captains may inspect only themselves and players directly assigned to them. */
    private Collection<Long> visibleMembers(JdbcClubService.State state, long actor) {
        if (!isCaptainScope(state, actor)) return state.members().keySet();
        return state.members().keySet().stream()
                .filter(pid -> pid == actor || extra(state, pid).upPlayerId() == actor)
                .toList();
    }

    /** 2.22 captain page: current node plus only its first-level children. */
    private Collection<Long> directMembers(JdbcClubService.State state, long actor) {
        long owner = ownerId(state);
        return state.members().keySet().stream()
                .filter(pid -> pid == actor || effectiveParent(state, pid, owner) == actor)
                .toList();
    }

    private long effectiveParent(JdbcClubService.State state, long pid, long owner) {
        JdbcClubService.MemberExtraState member = extra(state, pid);
        // Earlier captain appointment packets could persist partnerPid=self. In 2.22 this node is
        // still a direct child of the creator, so normalize it at the read boundary.
        if (member.promotionManager() && member.upPlayerId() == pid) return owner;
        return member.upPlayerId();
    }

    private boolean isCaptainScope(JdbcClubService.State state, long actor) {
        String role = state.members().get(actor);
        boolean manager = "OWNER".equals(role) || "ADMIN".equals(role) || "MANAGER".equals(role);
        return !manager && extra(state, actor).promotionManager();
    }

    private Map<String, Object> playerRecordCount(long actor, Map<String, Object> payload) {
        JdbcClubService.State state = requireMember(clubId(payload), actor);
        int sum = state.records().stream().flatMap(record -> record.scores().values().stream()).mapToInt(Integer::intValue).sum();
        long winners = state.records().stream().flatMap(record -> record.scores().values().stream()).filter(value -> value > 0).count();
        return Map.of("size", state.records().size(), "winner", winners, "sumPoint", sum);
    }

    private Map<String, Object> schoolReport(long actor, Map<String, Object> payload) {
        return Map.of("totalPageNum", 1, "clubPlayerRoomAloneLogBOS", playerRecordRows(actor, payload));
    }

    private List<Map<String, Object>> competitionTimes() {
        LocalDate today = LocalDate.now(clock);
        return List.of(
                competitionTime(0, "今日", today.atStartOfDay().toInstant(ZoneOffset.UTC)),
                competitionTime(1, "昨日", today.minusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)),
                competitionTime(2, "近三日", today.minusDays(2).atStartOfDay().toInstant(ZoneOffset.UTC)),
                competitionTime(3, "近三十日", today.minusDays(29).atStartOfDay().toInstant(ZoneOffset.UTC)));
    }

    private Map<String, Object> clubSummary(JdbcClubService.State state, long actor) {
        Map<String, Object> out = baseClub(state, actor);
        out.put("playingRoomCount", state.tables().stream().filter(table -> "PLAYING".equals(table.status())).count());
        out.put("peopleNum", state.members().size());
        out.put("player", profile(ownerId(state)));
        return out;
    }

    private Map<String, Object> clubDetail(JdbcClubService.State state, long actor) {
        Map<String, Object> out = baseClub(state, actor);
        out.put("memberCount", state.members().size());
        out.put("peopleNum", state.members().size());
        out.put("showOnlinePlayerNum", setting(state, "showOnlinePlayerNum", 1));
        out.put("diamondsAttentionMinister", setting(state, "diamondsAttentionMinister", 500));
        out.put("diamondsAttentionAll", setting(state, "diamondsAttentionAll", 100));
        out.put("showUplevelId", 1);
        out.put("showClubSign", 1);
        long unionId = setting(state, "unionId", 0);
        out.put("unionId", unionId);
        out.put("unionSign", setting(state, "unionSign", 0));
        out.put("unionName", text(state.settings(), "unionName", ""));
        out.put("unionPostType", unionId > 0 ? unionPostType(state, actor) : -1);
        // Legacy 2.2.2 uses this field as a captain/promoter identity flag.
        // Returning a constant made every ordinary member pass the captain UI branch.
        out.put("levelPromotion", extra(state, actor).promotionManager() ? 1 : 0);
        out.put("invite", 1);
        out.put("gameList", state.templates().stream().map(template -> gameNumber(template.game())).filter(value -> value > 0).toList());
        out.put("clubCent", allowClubCent(state, actor));
        out.put("caseClubCent", decimal(extra(state, actor).caseClubCent()));
        out.put("player", profile(ownerId(state)));
        return out;
    }

    private Map<String, Object> baseClub(JdbcClubService.State state, long actor) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", state.id());
        out.put("clubId", state.id());
        out.put("clubID", state.id());
        out.put("clubsign", state.id());
        out.put("clubName", state.name());
        out.put("name", state.name());
        out.put("status", state.status());
        out.put("minister", ministerCode(state.members().get(actor)));
        out.put("isPromotionManage", extra(state, actor).promotionManager());
        out.put("playerClubCard", 0);
        out.put("skinType", setting(state, "skinType", 0));
        out.put("unionId", setting(state, "unionId", 0));
        out.put("unionSign", setting(state, "unionSign", 0));
        return out;
    }

    private Map<String, Object> memberRow(JdbcClubService.State state, long pid) {
        JdbcClubService.MemberExtraState extra = extra(state, pid);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("pid", pid);
        row.put("status", 0);
        row.put("minister", ministerCode(state.members().get(pid)));
        row.put("playerClubCard", 0);
        row.put("clubCent", memberClubCent(state, pid));
        row.put("isBanGame", state.roomBans().containsKey(pid));
        row.put("isPromotionManage", extra.promotionManager());
        Map<String, Object> player = new LinkedHashMap<>(profile(pid));
        if (extra.remarkName() != null && !extra.remarkName().isBlank()) player.put("name", extra.remarkName());
        row.put("shortPlayer", player);
        row.put("upShortPlayer", extra.upPlayerId() > 0 ? profile(extra.upPlayerId()) : Map.of());
        return row;
    }

    private Map<String, Object> managedRoom(JdbcClubService.State state, JdbcClubService.TemplateState template) {
        Map<String, Object> row = new LinkedHashMap<>();
        Map<String, Object> cfg = new LinkedHashMap<>();
        try {
            Object decoded = json.readValue(template.rules(), Map.class);
            if (decoded instanceof Map<?, ?> values) values.forEach((key, value) -> cfg.put(String.valueOf(key), value));
        } catch (Exception ignored) { /* Keep the stable management fields below. */ }
        cfg.put("gameIndex", template.index());
        cfg.put("playerNum", ruleNumber(template.rules(), "playerNum",
                ruleNumber(template.rules(), "playerCount", 0)));
        cfg.put("paymentRoomCardType", ruleNumber(template.rules(), "paymentRoomCardType", 0));
        cfg.put("clubWinnerPayConsume", ruleNumber(template.rules(), "clubWinnerPayConsume", 0));
        cfg.put("dataJsonCfg", template.rules());
        row.put("bRoomConfigure", cfg);
        row.put("configId", template.index());
        row.put("gameIndex", template.index());
        row.put("gameType", template.game());
        row.put("gameId", gameNumber(template.game()));
        row.put("roomCount", state.tables().stream().filter(table -> table.templateId().equals(template.id())).count());
        row.put("status", setting(state, "templateStatus." + template.index(), 0));
        row.put("roomName", template.name());
        return row;
    }

    private Map<String, Object> groupingRow(JdbcClubService.State state, JdbcClubService.GroupingState group) {
        return Map.of("groupingId", group.id(), "groupingSize", group.playerIds().size(),
                "playerList", group.playerIds().stream().filter(state.members()::containsKey).map(this::profile).toList());
    }

    private Map<String, Object> recordRow(JdbcClubService.State state, JdbcClubService.MatchState record) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("roomID", numberText(record.roomId()));
        row.put("roomKey", record.roomId());
        row.put("roomState", 0);
        row.put("endTime", record.finishedAt().toString());
        row.put("sportsDouble", 1);
        row.put("gameType", 0);
        row.put("configName", "俱乐部房间");
        row.put("valueType", 0);
        row.put("roomCard", 0);
        row.put("unionId", 0);
        row.put("roomSportsConsume", 0);
        row.put("isViewed", state.viewedRooms().contains(record.roomId()));
        row.put("playerList", record.scores().entrySet().stream().map(entry -> {
            Map<String, Object> player = new LinkedHashMap<>(profile(entry.getKey()));
            player.put("point", entry.getValue());
            player.put("clubCent", entry.getValue());
            player.put("clubName", state.name());
            return player;
        }).toList());
        return row;
    }

    private List<Map<String, Object>> clubCentDynamicRows(JdbcClubService.State state, long pid) {
        return state.ledger().stream()
                .filter(row -> pid <= 0 || row.playerId() == pid)
                .map(row -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("execType", row.amount().signum() >= 0 ? 114 : 115);
                    item.put("createTime", row.createdAt().toString());
                    item.put("time", row.createdAt().toString());
                    item.put("pid", row.playerId());
                    item.put("playerName", profile(row.playerId()).get("name"));
                    item.put("execPid", ownerId(state));
                    item.put("execName", profile(ownerId(state)).get("name"));
                    item.put("value", row.amount());
                    item.put("clubCent", row.amount());
                    item.put("id", row.businessKey());
                    item.put("winLoseValue", row.amount());
                    item.put("consumeValue", 0);
                    item.put("eliminatePoint", 0);
                    item.put("pidCurValue", decimal(extra(state, row.playerId()).clubCent()));
                    return item;
                }).toList();
    }

    private Map<String, Object> tableRow(JdbcClubService.State state, JdbcClubService.TableState table) {
        JdbcClubService.TemplateState template = state.templates().stream().filter(item -> item.id().equals(table.templateId())).findFirst()
                .orElse(new JdbcClubService.TemplateState(table.templateId(), table.displayName(), "0", "{}", 0));
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("roomId", numberText(table.id()));
        row.put("roomKey", table.id());
        row.put("gameId", gameNumber(template.game()));
        row.put("gameName", template.game());
        row.put("roomName", table.displayName());
        row.put("setId", 0);
        row.put("setCount", ruleNumber(template.rules(), "setCount", 0));
        row.put("playerNum", ruleNumber(template.rules(), "playerNum", 0));
        row.put("posList", List.of());
        row.put("state", table.status());
        return row;
    }

    private List<Map<String, Object>> queryHallClubRooms(long clubId) {
        String sql = "SELECT room_id,game_id,play_version,state,club_template_code,rules_json,updated_at FROM aoo_hall_room WHERE club_id=? AND state IN ('OPEN','PLAYING') ORDER BY updated_at DESC,room_id DESC LIMIT 200";
        try (Connection connection = source.getConnection(); PreparedStatement query = connection.prepareStatement(sql)) {
            query.setLong(1, clubId);
            List<Map<String, Object>> out = new ArrayList<>();
            try (ResultSet rows = query.executeQuery()) {
                while (rows.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("roomId", rows.getLong("room_id"));
                    row.put("roomKey", rows.getLong("room_id"));
                    row.put("gameId", rows.getLong("game_id"));
                    row.put("gameName", String.valueOf(rows.getLong("game_id")));
                    row.put("templateCode", Objects.toString(rows.getString("club_template_code"), ""));
                    row.put("roomName", Objects.toString(rows.getString("club_template_code"), "俱乐部房间"));
                    row.put("setId", 0);
                    Map<String, Object> rules;
                    try { rules = json.readValue(Objects.toString(rows.getString("rules_json"), "{}"), Map.class); }
                    catch (Exception ignored) { rules = Map.of(); }
                    row.put("setCount", numberValue(rules.getOrDefault("roundCount", rules.getOrDefault("setCount", 0))));
                    row.put("playerNum", numberValue(rules.getOrDefault("playerCount", rules.getOrDefault("playerNum", 0))));
                    row.put("posList", queryHallRoomPositions(connection, rows.getLong("room_id")));
                    row.put("state", rows.getString("state"));
                    row.put("playVersion", rows.getString("play_version"));
                    out.add(row);
                }
            }
            return out;
        } catch (SQLException failure) {
            if (missingTable(failure)) return List.of();
            throw new IllegalStateException("club room projection query failed", failure);
        }
    }

    private List<Map<String, Object>> queryHallRoomPositions(Connection connection, long roomId) throws SQLException {
        // Hall membership includes observers. Only the game authority snapshot's
        // players map represents users who have actually taken a seat.
        String sql = "SELECT state_payload FROM aoo_room_snapshot WHERE room_id=?";
        try (PreparedStatement query = connection.prepareStatement(sql)) {
            query.setLong(1, roomId);
            List<Map<String, Object>> positions = new ArrayList<>();
            try (ResultSet rows = query.executeQuery()) {
                if (!rows.next()) return positions;
                Map<String, Object> snapshot;
                try { snapshot = json.readValue(Objects.toString(rows.getString("state_payload"), "{}"), Map.class); }
                catch (Exception ignored) { return positions; }
                Object rawPlayers = snapshot.get("players");
                if (!(rawPlayers instanceof Map<?, ?> players)) return positions;
                for (Map.Entry<?, ?> entry : players.entrySet()) {
                    int seat;
                    long pid;
                    try {
                        seat = Integer.parseInt(String.valueOf(entry.getKey()));
                        pid = Long.parseLong(String.valueOf(entry.getValue()));
                    } catch (Exception ignored) { continue; }
                    if (seat < 0 || pid <= 0) continue;
                    Map<String, Object> player = new LinkedHashMap<>();
                    player.put("pid", pid);
                    player.put("pos", seat);
                    player.put("name", profile(pid).get("name"));
                    positions.add(player);
                }
            }
            positions.sort(Comparator.comparingInt(player -> numberValue(player.get("pos"))));
            return positions;
        }
    }

    private static int numberValue(Object value) {
        if (value instanceof Number number) return number.intValue();
        try { return Integer.parseInt(String.valueOf(value)); }
        catch (Exception ignored) { return 0; }
    }

    private long allocateUnionSign(long actor, long clubId) {
        Set<Long> used = new LinkedHashSet<>();
        for (JdbcClubService.State state : clubs.list()) {
            long sign = setting(state, "unionSign", 0);
            if (sign > 0) used.add(sign);
        }
        for (int attempt = 0; attempt < 64; attempt++) {
            long sign = 100000 + Math.floorMod(clock.millis() + actor * 37 + clubId * 17 + attempt * 7919, 900000);
            if (!used.contains(sign)) return sign;
        }
        throw new IllegalStateException("union sign allocation failed");
    }

    private Optional<JdbcClubService.State> unionByIdentifier(long identifier) {
        return clubs.list().stream()
                .filter(state -> "ACTIVE".equals(state.status()))
                .filter(state -> setting(state, "unionId", 0) > 0)
                .filter(state -> setting(state, "unionSign", 0) == identifier
                        || setting(state, "unionId", 0) == identifier
                        || setting(state, "unionOwnerClubId", state.id()) == identifier)
                .sorted(Comparator.comparing((JdbcClubService.State state) ->
                        setting(state, "unionOwnerClubId", state.id()) == state.id()).reversed())
                .findFirst();
    }

    private Map<String, Object> unionInfo(JdbcClubService.State state) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", setting(state, "unionId", 0));
        out.put("unionId", setting(state, "unionId", 0));
        out.put("unionSign", setting(state, "unionSign", 0));
        out.put("name", text(state.settings(), "unionName", ""));
        out.put("clubId", state.id());
        out.put("ownerId", setting(state, "unionOwnerId", ownerId(state)));
        out.put("ownerClubId", setting(state, "unionOwnerClubId", state.id()));
        out.put("join", setting(state, "unionJoin", 0));
        out.put("quit", setting(state, "unionQuit", 0));
        out.put("state", setting(state, "unionState", 0));
        out.put("joinClubSameUnion", setting(state, "unionJoinClubSameUnion", 0));
        out.put("tableNum", setting(state, "unionTableNum", 0));
        out.put("unionTotalScore", setting(state, "unionTotalScore", 2000));
        out.put("matchRate", setting(state, "unionMatchRate", 0));
        out.put("outSports", setting(state, "unionOutSports", 0));
        out.put("prizeType", setting(state, "unionPrizeType", 2));
        out.put("ranking", setting(state, "unionRanking", 0));
        out.put("value", setting(state, "unionValue", 0));
        return out;
    }

    private Map<String, Object> unionMemberRow(JdbcClubService.State state, long actor, int postType) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("clubId", state.id());
        row.put("clubSign", state.id());
        row.put("clubName", state.name());
        row.put("createId", ownerId(state));
        row.put("createName", profile(ownerId(state)).get("name"));
        row.put("unionPostType", postType);
        row.put("clubCent", memberClubCent(state, actor));
        row.put("alivePoint", extra(state, actor).eliminatePoint());
        row.put("status", 4);
        row.put("createTime", clock.instant().toString());
        return row;
    }

    private List<Map<String, Object>> withUnionMember(Object value, JdbcClubService.State club, long actor, int postType) {
        List<Map<String, Object>> rows = copyObjectRows(value, club.id());
        rows.add(unionMemberRow(club, actor, postType));
        return List.copyOf(rows);
    }

    private List<Map<String, Object>> withUnionApplication(Object value, JdbcClubService.State club, long actor) {
        List<Map<String, Object>> rows = copyObjectRows(value, club.id());
        Map<String, Object> row = unionMemberRow(club, actor, 1);
        row.put("status", 1);
        rows.add(row);
        return List.copyOf(rows);
    }

    private List<Map<String, Object>> copyObjectRows(Object value, long excludedClubId) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (value instanceof Iterable<?> existing) {
            for (Object item : existing) {
                if (!(item instanceof Map<?, ?> raw)) continue;
                if (rawNumber(raw, "clubId", 0) == excludedClubId) continue;
                Map<String, Object> copy = new LinkedHashMap<>();
                raw.forEach((key, rowValue) -> { if (key != null) copy.put(String.valueOf(key), rowValue); });
                rows.add(copy);
            }
        }
        return rows;
    }

    private void copyUnionSettings(JdbcClubService.State source, Map<String, Object> target) {
        target.put("unionId", setting(source, "unionId", 0));
        target.put("unionSign", setting(source, "unionSign", 0));
        target.put("unionName", text(source.settings(), "unionName", ""));
        target.put("unionOwnerId", setting(source, "unionOwnerId", ownerId(source)));
        target.put("unionOwnerClubId", setting(source, "unionOwnerClubId", source.id()));
        target.put("unionJoin", setting(source, "unionJoin", 0));
        target.put("unionQuit", setting(source, "unionQuit", 0));
        target.put("unionTotalScore", setting(source, "unionTotalScore", 2000));
        target.put("unionMatchRate", setting(source, "unionMatchRate", 0));
        target.put("unionOutSports", setting(source, "unionOutSports", 0));
        target.put("unionPrizeType", setting(source, "unionPrizeType", 2));
        target.put("unionRanking", setting(source, "unionRanking", 0));
        target.put("unionValue", setting(source, "unionValue", 0));
        target.put("unionState", setting(source, "unionState", 0));
    }

    private int unionPostType(JdbcClubService.State state, long actor) {
        long unionId = setting(state, "unionId", 0);
        if (unionId <= 0) return -1;
        long ownerClubId = setting(state, "unionOwnerClubId", state.id());
        long unionOwner = setting(state, "unionOwnerId", ownerId(state));
        if (state.id() == ownerClubId && actor == unionOwner) return 3;
        if (ownerId(state) == actor) return 1;
        return 0;
    }

    private JdbcClubService.State initializeUnionTemplateBoundary(
            JdbcClubService.State current, long actor, String requestId) {
        int boundary = current.templates().stream().mapToInt(JdbcClubService.TemplateState::index)
                .max().orElse(0);
        return clubs.update(key(requestId, "union.template-boundary"), current.id(), state -> {
            requireManager(state, actor);
            if (state.settings().containsKey("unionTemplateStartIndex")) return state;
            Map<String, Object> settings = new LinkedHashMap<>(state.settings());
            settings.put("unionTemplateStartIndex", boundary);
            return JdbcClubService.copy(state, state.name(), state.status(), state.members(),
                    state.templates(), state.tables(), state.invites(), state.records(), state.ledger(),
                    settings, state.applications(), state.memberExtras(), state.groupings(),
                    state.roomBans(), state.viewedRooms());
        });
    }

    /**
     * 联盟模板和升级前的亲友圈模板共用持久化聚合，但使用不可回退的序号边界隔离。
     * 这样升级不会删改亲友圈历史数据，联盟大厅与房间管理也不会泄漏旧桌子。
     */
    private List<JdbcClubService.TemplateState> unionTemplates(JdbcClubService.State state) {
        int currentMaximum = state.templates().stream().mapToInt(JdbcClubService.TemplateState::index)
                .max().orElse(0);
        int boundary = (int) setting(state, "unionTemplateStartIndex", currentMaximum);
        return state.templates().stream().filter(template -> template.index() > boundary).toList();
    }

    private List<JdbcClubService.TemplateState> enabledUnionTemplates(JdbcClubService.State state) {
        return unionTemplates(state).stream()
                .filter(template -> setting(state, "templateStatus." + template.index(), 0) == 0)
                .toList();
    }

    private List<JdbcClubService.TemplateState> enabledTemplates(JdbcClubService.State state) {
        return state.templates().stream().filter(template -> setting(state, "templateStatus." + template.index(), 0) == 0).toList();
    }

    private JdbcClubService.State requireMember(long clubId, long actor) {
        return requireMember(clubs.get(clubId), actor);
    }

    private JdbcClubService.State requireMember(JdbcClubService.State state, long actor) {
        if (!"ACTIVE".equals(state.status()) || !state.members().containsKey(actor)) throw new SecurityException("active club membership required");
        return state;
    }

    private void requireManager(JdbcClubService.State state, long actor) {
        requireMember(state, actor);
        String role = state.members().get(actor);
        if (!"OWNER".equals(role) && !"ADMIN".equals(role) && !"MANAGER".equals(role)) throw new SecurityException("manager required");
    }

    private long ownerId(JdbcClubService.State state) {
        return state.members().entrySet().stream().filter(entry -> "OWNER".equals(entry.getValue())).mapToLong(Map.Entry::getKey).findFirst().orElse(0);
    }

    private int ministerCode(String role) {
        if ("OWNER".equals(role)) return 2;
        if ("ADMIN".equals(role) || "MANAGER".equals(role)) return 1;
        return 0;
    }

    private JdbcClubService.MemberExtraState extra(JdbcClubService.State state, long pid) {
        JdbcClubService.MemberExtraState value = state.memberExtras().get(pid);
        if (value == null) return new JdbcClubService.MemberExtraState("", false, 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0);
        return value;
    }

    private Map<String, Object> profile(long pid) {
        if (pid <= 0) return Map.of();
        Map<String, Object> row = queryProfile(pid);
        if (!row.isEmpty()) return row;
        Map<String, Object> fallback = new LinkedHashMap<>();
        fallback.put("pid", pid);
        fallback.put("id", pid);
        fallback.put("name", "玩家" + pid);
        fallback.put("nickName", "玩家" + pid);
        fallback.put("iconUrl", "");
        fallback.put("headImageUrl", "");
        return fallback;
    }

    private Map<String, Object> queryProfile(long pid) {
        String sql = "SELECT i.account_id player_id,p.nickname,p.avatar_url,p.gender_code,i.normalized_value display_id "
                + "FROM aoo_account_identity i LEFT JOIN player_profile p ON p.player_id=i.account_id "
                + "WHERE i.account_id=? AND i.identity_type='DISPLAY_ID' AND i.status='ACTIVE'";
        try (Connection connection = source.getConnection(); PreparedStatement query = connection.prepareStatement(sql)) {
            query.setLong(1, pid);
            try (ResultSet rows = query.executeQuery()) {
                if (!rows.next()) return Map.of();
                String name = rows.getString("nickname");
                Map<String, Object> out = new LinkedHashMap<>();
                String displayId = rows.getString("display_id");
                out.put("pid", displayId == null || displayId.isBlank() ? pid : displayId);
                out.put("id", out.get("pid"));
                out.put("accountId", pid);
                out.put("name", name == null || name.isBlank() ? "玩家" + pid : name);
                out.put("nickName", out.get("name"));
                out.put("iconUrl", Objects.toString(rows.getString("avatar_url"), ""));
                out.put("headImageUrl", out.get("iconUrl"));
                out.put("sex", "MALE".equals(rows.getString("gender_code")) ? 1 : "FEMALE".equals(rows.getString("gender_code")) ? 2 : 0);
                return out;
            }
        } catch (SQLException failure) {
            if (missingTable(failure) || missingColumn(failure)) return Map.of();
            throw new IllegalStateException("player profile query failed", failure);
        }
    }

    private long currencyBalance(long playerId, String currency) {
        String sql = "SELECT balance FROM aoo_currency_balance WHERE player_id=? AND currency=? AND currency_scope_id=0";
        try (Connection connection = source.getConnection(); PreparedStatement query = connection.prepareStatement(sql)) {
            query.setLong(1, playerId);
            query.setString(2, currency);
            try (ResultSet rows = query.executeQuery()) {
                return rows.next() ? rows.getLong(1) : 0;
            }
        } catch (SQLException failure) {
            throw new IllegalStateException("currency balance query failed", failure);
        }
    }

    private long clubId(Map<String, Object> payload) {
        long id = number(payload, "clubId", number(payload, "id", number(payload, "clubID", 0)));
        if (id <= 0) throw new IllegalArgumentException("clubId is required");
        return id;
    }

    private String key(String requestId, String action) {
        String id = requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId;
        return "legacy-wss:" + action + ":" + id;
    }

    private long setting(JdbcClubService.State state, String name, long fallback) {
        return number(state.settings(), name, fallback);
    }

    private static Map<String, Object> section(long id, long begin, long end, long value) {
        return Map.of("unionSectionId", id, "beginValue", begin, "endValue", end, "allowShareToValue", value, "shareToSelfValue", value);
    }

    private static Map<String, Object> competitionTime(int type, String name, Instant start) {
        return Map.of("type", type, "name", name, "beginTime", start.toString());
    }

    private static <T> List<T> page(List<T> rows, int page) {
        int from = Math.min(rows.size(), Math.max(0, page - 1) * PAGE_SIZE);
        return rows.subList(from, Math.min(rows.size(), from + PAGE_SIZE));
    }

    private static int pages(int size) {
        return Math.max(1, (size + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private static long number(Map<String, ?> map, String key, long fallback) {
        Object value = map.get(key);
        if (value instanceof Number number) return number.longValue();
        if (value instanceof String text && !text.isBlank()) {
            try { return Long.parseLong(text.trim()); }
            catch (NumberFormatException ignored) { return fallback; }
        }
        return fallback;
    }

    private static long rawNumber(Map<?, ?> map, String key, long fallback) {
        Object value = map.get(key);
        if (value instanceof Number number) return number.longValue();
        if (value instanceof String text && !text.isBlank()) {
            try { return Long.parseLong(text.trim()); }
            catch (NumberFormatException ignored) { return fallback; }
        }
        return fallback;
    }

    private static String text(Map<String, ?> map, String key, String fallback) {
        Object value = map.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private static BigDecimal decimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal decimal) return decimal;
        if (value instanceof Number number) return BigDecimal.valueOf(number.doubleValue());
        try { return new BigDecimal(String.valueOf(value)); }
        catch (NumberFormatException ignored) { return BigDecimal.ZERO; }
    }

    private static Set<Long> numbers(Object value) {
        if (!(value instanceof Iterable<?> items)) return Set.of();
        Set<Long> out = new LinkedHashSet<>();
        for (Object item : items) {
            if (item instanceof Number number) out.add(number.longValue());
            else if (item != null) try { out.add(Long.parseLong(String.valueOf(item))); }
            catch (NumberFormatException ignored) { }
        }
        return out;
    }

    private int ruleNumber(String rules, String key, int fallback) {
        if (rules == null || rules.isBlank()) return fallback;
        try {
            Object raw = json.readValue(rules, Map.class).get(key);
            if (raw instanceof Number number) return number.intValue();
            if (raw instanceof String text && !text.isBlank()) return Integer.parseInt(text);
        } catch (Exception ignored) { }
        return fallback;
    }

    private String ruleText(String rules, String key, String fallback) {
        if (rules == null || rules.isBlank()) return fallback;
        try {
            Object raw = json.readValue(rules, Map.class).get(key);
            if (raw != null && !String.valueOf(raw).isBlank()) return String.valueOf(raw);
        } catch (Exception ignored) { }
        return fallback;
    }

    private static long gameNumber(String game) {
        if (game == null || game.isBlank()) return 0;
        try { return Long.parseLong(game); }
        catch (NumberFormatException ignored) {
            String canonical = game.strip().toUpperCase(Locale.ROOT);
            if (canonical.matches("^[A-Z]+\\d{3}$")) return Long.parseLong(canonical.replaceFirst("^[A-Z]+", ""));
            return 0;
        }
    }

    private static long numberText(String value) {
        try { return Long.parseLong(value); }
        catch (Exception ignored) { return 0; }
    }

    private static boolean missingTable(SQLException e) {
        String state = e.getSQLState();
        String message = String.valueOf(e.getMessage()).toLowerCase(Locale.ROOT);
        return "42S02".equals(state) || "42102".equals(state) || message.contains("table") && message.contains("not found");
    }

    private static boolean missingColumn(SQLException e) {
        String state = e.getSQLState();
        String message = String.valueOf(e.getMessage()).toLowerCase(Locale.ROOT);
        return "42S22".equals(state) || "42122".equals(state) || message.contains("column") && message.contains("not found");
    }
}
