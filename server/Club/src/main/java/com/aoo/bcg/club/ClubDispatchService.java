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
            case "club.CClubChangeQuitAndJoinConfig" -> changeAuditSettings(actor, requestId, payload);
            case "club.CClubChangePlayerStatus" -> changePlayerStatus(actor, requestId, payload);
            case "club.CClubKickOutNeedConfirm" -> kickGate(actor, payload);
            case "club.CClubSetMinister" -> setMinister(actor, requestId, payload);
            case "club.CClubSetPromotionMinister" -> setPromotionManager(actor, requestId, payload);
            case "club.CClubChangePlayerRemarkName" -> changeRemark(actor, requestId, payload);
            case "club.CClubGetUplevelPromotion" -> getPromotionParent(actor, payload);
            case "club.CClubChangePromotionBelong", "club.CClubPartnerChange" -> changePromotionParent(actor, requestId, payload);
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
            case "club.CClubMemberSportsPointInfo" -> sportsPointInfo(actor, payload);
            case "club.CClubSportsPointUpdate", "club.CClubSubordinateLevelSportsPointUpdate" -> sportsPointUpdate(actor, requestId, payload);
            case "club.CClubGetCaseSprotsChange" -> caseSportsChange(actor, requestId, payload);
            case "club.CClubDynamic" -> dynamicRows(actor, payload);
            case "club.CClubSportsPointDynamicByPid", "club.CClubSportsPointMemberDynamicByPid",
                    "club.CClubSportsPointChangeRecordByPid" -> sportsDynamic(actor, payload);
            case "union.CUnionCreate" -> createUnion(actor, requestId, payload);
            case "union.CUnionJoin" -> joinUnion(actor, requestId, payload);
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
            requireManager(current, actor);
            Map<String, Object> settings = new LinkedHashMap<>(current.settings());
            settings.put("legacyWrite." + action + "." + number(payload, "pid", number(payload, "opPid", actor)), new LinkedHashMap<>(payload));
            Map<Long, JdbcClubService.MemberExtraState> extras = new LinkedHashMap<>(current.memberExtras());
            long pid = number(payload, "pid", number(payload, "opPid", 0));
            if (pid > 0 && current.members().containsKey(pid)) {
                JdbcClubService.MemberExtraState old = extra(current, pid);
                boolean promotion = old.promotionManager();
                long up = old.upPlayerId();
                if (action.contains("SubordinateLevelAppoint") || action.contains("PromotionLevelPidAdd")) promotion = true;
                if (action.contains("SubordinateLevelDelete") || action.contains("CancleCaption")) promotion = false;
                long newParent = number(payload, "upLevelId", number(payload, "partnerPid", up));
                if (newParent > 0) up = newParent;
                extras.put(pid, new JdbcClubService.MemberExtraState(old.remarkName(), promotion, up,
                        decimal(old.sportsPoint()), decimal(old.caseSportsPoint()), decimal(old.warningPoint()),
                        (int) number(payload, "value", old.eliminatePoint())));
            }
            return JdbcClubService.copy(current, current.name(), current.status(), current.members(),
                    current.templates(), current.tables(), current.invites(), current.records(),
                    current.ledger(), settings, current.applications(), extras, current.groupings(),
                    current.roomBans(), current.viewedRooms());
        });
        return Map.of("saved", true, "clubId", state.id());
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
        List<Map<String, Object>> rows = state.members().keySet().stream()
                .map(pid -> promotionRow(state, pid))
                .filter(row -> Boolean.TRUE.equals(row.get("promotion")) || ((Number) row.get("pid")).longValue() == actor)
                .toList();
        return Map.of("clubPromotionLevelItemList", rows, "clubTeamListInfoList", rows,
                "showList", List.of(1, 2, 3), "showListSecond", List.of(), "dateType", List.of(0, 1, 2, 3));
    }

    private Map<String, Object> promotionRow(JdbcClubService.State state, long pid) {
        JdbcClubService.MemberExtraState extra = extra(state, pid);
        Map<String, Object> row = new LinkedHashMap<>(profile(pid));
        row.put("promotion", extra.promotionManager() || extra.upPlayerId() > 0);
        row.put("number", state.members().keySet().stream().filter(member -> extra(state, member).upPlayerId() == pid).count());
        row.put("setCount", state.records().size());
        row.put("entryFee", 0);
        row.put("actualEntryFee", 0);
        row.put("shareType", 0);
        row.put("shareValue", 0);
        row.put("shareFixedValue", 0);
        row.put("scorePoint", decimal(extra.sportsPoint()));
        row.put("sportsPoint", decimal(extra.sportsPoint()));
        row.put("sumSportsPoint", decimal(extra.sportsPoint()));
        row.put("level", extra.promotionManager() ? 1 : 0);
        row.put("myisminister", ministerCode(state.members().get(pid)));
        row.put("sportsPointWarning", decimal(extra.warningPoint()));
        row.put("personalSportsPointWarning", decimal(extra.warningPoint()));
        row.put("warnStatus", decimal(extra.warningPoint()).signum() == 0 ? 0 : 1);
        row.put("alivePoint", extra.eliminatePoint());
        row.put("alivePointStatus", extra.eliminatePoint() == 0 ? 0 : 1);
        row.put("eliminatePoint", extra.eliminatePoint());
        row.put("clubSign", state.id());
        row.put("createId", ownerId(state));
        return row;
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
            settings.put("unionInitSports", number(payload, "initSports", 2000));
            settings.put("unionMatchRate", number(payload, "matchRate", 0));
            settings.put("unionOutSports", number(payload, "outSports", 0));
            settings.put("unionPrizeType", number(payload, "prizeType", 2));
            settings.put("unionRanking", number(payload, "ranking", 0));
            settings.put("unionValue", number(payload, "value", 0));
            settings.put("unionState", 0);
            settings.put("unionMembers", List.of(unionMemberRow(current, actor, 3)));
            return JdbcClubService.copy(current, current.name(), current.status(), current.members(),
                    current.templates(), current.tables(), current.invites(), current.records(),
                    current.ledger(), settings, current.applications(), current.memberExtras(),
                    current.groupings(), current.roomBans(), current.viewedRooms());
        });
        return unionInfo(state);
    }

    private Number joinUnion(long actor, String requestId, Map<String, Object> payload) {
        long clubId = clubId(payload);
        long unionSign = number(payload, "unionSign", number(payload, "unionId", 0));
        if (unionSign <= 0) throw new IllegalArgumentException("unionSign is required");
        JdbcClubService.State ownerClub = unionBySign(unionSign)
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
        boolean audit = setting(target, "joinNeedExamine", 1) != 0;
        JdbcClubService.State changed = clubs.update(key(requestId, "club.CClubJoin"), target.id(), current -> {
            if (current.members().containsKey(actor)) return current;
            List<JdbcClubService.ApplicationState> apps = new ArrayList<>(current.applications());
            Optional<JdbcClubService.ApplicationState> pending = apps.stream()
                    .filter(app -> app.playerId() == actor && "JOIN".equals(app.type()) && "PENDING".equals(app.status()))
                    .findFirst();
            if (pending.isPresent()) return current;
            apps.add(new JdbcClubService.ApplicationState(actor, "JOIN", audit ? "PENDING" : "APPROVED", clock.instant()));
            Map<Long, String> members = new LinkedHashMap<>(current.members());
            if (!audit) members.put(actor, "MEMBER");
            return JdbcClubService.copy(current, current.name(), current.status(), members, current.templates(),
                    current.tables(), current.invites(), current.records(), current.ledger(), current.settings(),
                    apps, current.memberExtras(), current.groupings(), current.roomBans(), current.viewedRooms());
        });
        return Map.of("joinStatus", audit ? 16 : 0, "club", clubSummary(changed, actor));
    }

    private Map<String, Object> findPlayer(long actor, Map<String, Object> payload) {
        JdbcClubService.State state = requireMember(clubId(payload), actor);
        long pid = number(payload, "pid", 0);
        if (pid <= 0) throw new IllegalArgumentException("pid is required");
        return Map.of("player", profile(pid), "state", state.members().containsKey(pid) ? 1 : 0);
    }

    private Map<String, Object> invitePlayer(long actor, String requestId, Map<String, Object> payload) {
        long clubId = clubId(payload);
        long pid = number(payload, "pid", 0);
        if (pid <= 0) throw new IllegalArgumentException("pid is required");
        JdbcClubService.InviteState invite = clubs.invite(key(requestId, "club.CClubFindPIDAdd"), clubId, actor, pid, 604800);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", invite.token());
        result.put("playerId", invite.playerId());
        result.put("expiresAt", invite.expiresAt().toString());
        return result;
    }

    private List<Map<String, Object>> members(long actor, Map<String, Object> payload) {
        JdbcClubService.State state = requireMember(clubId(payload), actor);
        int pageType = (int) number(payload, "pageType", 0);
        int page = Math.max(1, (int) number(payload, "pageNum", 1));
        String query = text(payload, "query", "").trim();
        boolean losePoint = number(payload, "losePoint", 0) != 0;
        List<Map<String, Object>> rows;
        if (pageType == 1 || pageType == 2) {
            String type = pageType == 1 ? "JOIN" : "QUIT";
            rows = state.applications().stream()
                    .filter(app -> type.equals(app.type()) && "PENDING".equals(app.status()))
                    .map(app -> memberRow(state, app.playerId(), pageType))
                    .toList();
        } else {
            rows = state.members().keySet().stream()
                    .map(pid -> memberRow(state, pid, pageType))
                    .filter(row -> !losePoint || decimal(extra(state, ((Number) row.get("pid")).longValue()).sportsPoint()).signum() < 0)
                    .toList();
        }
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

    private Map<String, Object> changeAuditSettings(long actor, String requestId, Map<String, Object> payload) {
        long clubId = clubId(payload);
        JdbcClubService.State state = clubs.update(key(requestId, "club.CClubChangeQuitAndJoinConfig"), clubId, current -> {
            requireManager(current, actor);
            Map<String, Object> settings = new LinkedHashMap<>(current.settings());
            settings.put("joinNeedExamine", number(payload, "joinNeedExamine", setting(current, "joinNeedExamine", 1)));
            settings.put("quitNeedExamine", number(payload, "quitNeedExamine", setting(current, "quitNeedExamine", 1)));
            return JdbcClubService.copy(current, current.name(), current.status(), current.members(),
                    current.templates(), current.tables(), current.invites(), current.records(), current.ledger(),
                    settings, current.applications(), current.memberExtras(), current.groupings(),
                    current.roomBans(), current.viewedRooms());
        });
        return clubDetail(state, actor);
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

    private Map<String, Object> changePlayerStatus(long actor, String requestId, Map<String, Object> payload) {
        long clubId = clubId(payload);
        long pid = number(payload, "pid", 0);
        int status = (int) number(payload, "status", 0);
        if (pid <= 0) throw new IllegalArgumentException("pid is required");
        if (status == 8) return clubDetail(clubs.kick(key(requestId, "club.CClubChangePlayerStatus.kick"), clubId, actor, pid), actor);
        JdbcClubService.State state = clubs.update(key(requestId, "club.CClubChangePlayerStatus." + status), clubId, current -> {
            if (status == 0x40 && pid == actor) requireMember(current, actor); else requireManager(current, actor);
            if ("OWNER".equals(current.members().get(pid))) throw new SecurityException("owner cannot leave through member status");
            Map<Long, String> members = new LinkedHashMap<>(current.members());
            List<JdbcClubService.ApplicationState> apps = new ArrayList<>();
            for (JdbcClubService.ApplicationState app : current.applications()) {
                if (app.playerId() == pid && (status == 4 || status == 2) && "JOIN".equals(app.type()) && "PENDING".equals(app.status()))
                    apps.add(new JdbcClubService.ApplicationState(pid, "JOIN", status == 4 ? "APPROVED" : "REJECTED", clock.instant()));
                else if (app.playerId() == pid && status == 64 && "QUIT".equals(app.type()) && "PENDING".equals(app.status()))
                    apps.add(new JdbcClubService.ApplicationState(pid, "QUIT", "APPROVED", clock.instant()));
                else apps.add(app);
            }
            if (status == 4) members.put(pid, "MEMBER");
            if (status == 64 || status == 0x40) members.remove(pid);
            return JdbcClubService.copy(current, current.name(), current.status(), members, current.templates(),
                    current.tables(), current.invites(), current.records(), current.ledger(), current.settings(),
                    apps, current.memberExtras(), current.groupings(), current.roomBans(), current.viewedRooms());
        });
        return clubDetail(state, actor);
    }

    private Map<String, Object> kickGate(long actor, Map<String, Object> payload) {
        JdbcClubService.State state = requireMember(clubId(payload), actor);
        long pid = number(payload, "pid", 0);
        JdbcClubService.MemberExtraState extra = extra(state, pid);
        boolean hasPoint = decimal(extra.sportsPoint()).signum() != 0 || decimal(extra.caseSportsPoint()).signum() != 0;
        return Map.of("type", hasPoint ? 1 : 0, "sportsPoint", decimal(extra.sportsPoint()), "caseSportsPoint", decimal(extra.caseSportsPoint()));
    }

    private Map<String, Object> setMinister(long actor, String requestId, Map<String, Object> payload) {
        int minister = (int) number(payload, "minister", 0);
        String role = minister == 1 || minister == 3 ? "ADMIN" : "MEMBER";
        JdbcClubService.State state = clubs.setRole(key(requestId, "club.CClubSetMinister"), clubId(payload), actor, number(payload, "pid", 0), role);
        return clubDetail(state, actor);
    }

    private Map<String, Object> setPromotionManager(long actor, String requestId, Map<String, Object> payload) {
        long pid = number(payload, "pid", 0);
        boolean enabled = number(payload, "minister", 0) != 0;
        JdbcClubService.State state = updateExtra(actor, requestId, "club.CClubSetPromotionMinister", payload, pid,
                old -> new JdbcClubService.MemberExtraState(old.remarkName(), enabled, old.upPlayerId(),
                        decimal(old.sportsPoint()), decimal(old.caseSportsPoint()), decimal(old.warningPoint()), old.eliminatePoint()));
        return clubDetail(state, actor);
    }

    private Map<String, Object> changeRemark(long actor, String requestId, Map<String, Object> payload) {
        long pid = number(payload, "remarkID", number(payload, "pid", 0));
        String remark = text(payload, "remarkName", "");
        JdbcClubService.State state = updateExtra(actor, requestId, "club.CClubChangePlayerRemarkName", payload, pid,
                old -> new JdbcClubService.MemberExtraState(remark, old.promotionManager(), old.upPlayerId(),
                        decimal(old.sportsPoint()), decimal(old.caseSportsPoint()), decimal(old.warningPoint()), old.eliminatePoint()));
        return memberRow(state, pid, 0);
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
                        decimal(old.sportsPoint()), decimal(old.caseSportsPoint()), decimal(old.warningPoint()), old.eliminatePoint()));
        return memberRow(state, pid, 0);
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
        JdbcClubService.State state = requireMember(clubId(payload), actor);
        List<Map<String, Object>> projected = queryHallClubRooms(state.id());
        return projected.isEmpty() ? state.tables().stream().map(table -> tableRow(state, table)).toList() : projected;
    }

    private Map<String, Object> roomDetail(long actor, Map<String, Object> payload) {
        List<Map<String, Object>> rooms = roomList(actor, payload);
        String key = String.valueOf(payload.getOrDefault("roomKey", ""));
        return rooms.stream().filter(room -> key.equals(String.valueOf(room.get("roomKey"))))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("room not found"));
    }

    private List<Map<String, Object>> quickRoomConfigs(long actor, Map<String, Object> payload) {
        JdbcClubService.State state = requireMember(clubId(payload), actor);
        return enabledTemplates(state).stream().map(template -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("tagId", template.index());
            row.put("configId", template.index());
            row.put("gameIndex", template.index());
            row.put("gameId", gameNumber(template.game()));
            row.put("gameType", template.game());
            row.put("name", template.name());
            row.put("roomName", template.name());
            row.put("rules", template.rules());
            row.put("setCount", ruleNumber(template.rules(), "setCount", 0));
            row.put("playerNum", ruleNumber(template.rules(), "playerNum", 0));
            row.put("size", ruleNumber(template.rules(), "playerNum", 0));
            return row;
        }).toList();
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
        return state.members().keySet().stream().limit(200).map(pid -> Map.of("player", profile(pid), "isBan", selected.contains(pid))).toList();
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

    private Map<String, Object> sportsPointInfo(long actor, Map<String, Object> payload) {
        JdbcClubService.State state = requireMember(clubId(payload), actor);
        long pid = number(payload, "opPid", number(payload, "pid", actor));
        JdbcClubService.MemberExtraState target = extra(state, pid);
        JdbcClubService.MemberExtraState mine = extra(state, actor);
        BigDecimal allow = "OWNER".equals(state.members().get(actor)) ? new BigDecimal("1000000") : decimal(mine.sportsPoint()).max(BigDecimal.ZERO);
        return Map.of("sportsPoint", decimal(target.sportsPoint()), "caseSportsPoint", decimal(target.caseSportsPoint()), "allowSportsPoint", allow);
    }

    private Map<String, Object> sportsPointUpdate(long actor, String requestId, Map<String, Object> payload) {
        long pid = number(payload, "opPid", number(payload, "pid", 0));
        int type = (int) number(payload, "type", 0);
        BigDecimal value = decimal(payload.get("value"));
        if (value.signum() <= 0) throw new IllegalArgumentException("sports point value is required");
        JdbcClubService.State state = clubs.update(key(requestId, "club.CClubSportsPointUpdate"), clubId(payload), current -> {
            requireManager(current, actor);
            if (!current.members().containsKey(pid)) throw new IllegalArgumentException("club member missing");
            JdbcClubService.MemberExtraState old = extra(current, pid);
            BigDecimal changed = decimal(old.sportsPoint()).add(type == 0 ? value : value.negate());
            Map<Long, JdbcClubService.MemberExtraState> extras = new LinkedHashMap<>(current.memberExtras());
            extras.put(pid, new JdbcClubService.MemberExtraState(old.remarkName(), old.promotionManager(), old.upPlayerId(),
                    changed, decimal(old.caseSportsPoint()), decimal(old.warningPoint()), old.eliminatePoint()));
            List<JdbcClubService.LedgerState> ledger = new ArrayList<>(current.ledger());
            ledger.add(new JdbcClubService.LedgerState(key(requestId, "ledger"), pid, type == 0 ? value : value.negate(), "SPORTS_POINT", clock.instant()));
            return JdbcClubService.copy(current, current.name(), current.status(), current.members(),
                    current.templates(), current.tables(), current.invites(), current.records(), List.copyOf(ledger),
                    current.settings(), current.applications(), extras, current.groupings(),
                    current.roomBans(), current.viewedRooms());
        });
        return Map.of("type", type, "value", value, "changedValue", decimal(extra(state, pid).sportsPoint()));
    }

    private Map<String, Object> caseSportsChange(long actor, String requestId, Map<String, Object> payload) {
        BigDecimal value = decimal(payload.get("value"));
        int type = (int) number(payload, "type", 0);
        if (value.signum() <= 0) throw new IllegalArgumentException("case sports value is required");
        JdbcClubService.State state = clubs.update(key(requestId, "club.CClubGetCaseSprotsChange"), clubId(payload), current -> {
            requireMember(current, actor);
            JdbcClubService.MemberExtraState old = extra(current, actor);
            BigDecimal sports = decimal(old.sportsPoint());
            BigDecimal box = decimal(old.caseSportsPoint());
            if (type == 0 && sports.compareTo(value) < 0) throw new IllegalArgumentException("sports point insufficient");
            if (type == 1 && box.compareTo(value) < 0) throw new IllegalArgumentException("case sports point insufficient");
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
        return Map.of("sportsPoint", decimal(changed.sportsPoint()), "caseSportsPoint", decimal(changed.caseSportsPoint()));
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
        rows.addAll(sportsDynamicRows(state, 0));
        rows.sort((a, b) -> String.valueOf(b.get("createTime")).compareTo(String.valueOf(a.get("createTime"))));
        return page(rows, (int) number(payload, "pageNum", 1));
    }

    private List<Map<String, Object>> sportsDynamic(long actor, Map<String, Object> payload) {
        JdbcClubService.State state = requireMember(clubId(payload), actor);
        long pid = number(payload, "pid", number(payload, "opPid", 0));
        return page(sportsDynamicRows(state, pid), (int) number(payload, "pageNum", 1));
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
        return state.members().keySet().stream().map(pid -> {
            int sum = state.records().stream().mapToInt(record -> record.scores().getOrDefault(pid, 0)).sum();
            long size = state.records().stream().filter(record -> record.scores().containsKey(pid)).count();
            Map<String, Object> row = new LinkedHashMap<>(profile(pid));
            row.put("point", sum);
            row.put("sportsPoint", decimal(extra(state, pid).sportsPoint()));
            row.put("size", size);
            row.put("winner", state.records().stream().filter(record -> record.scores().getOrDefault(pid, 0) > 0).count());
            return row;
        }).toList();
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
        out.put("joinNeedExamine", setting(state, "joinNeedExamine", 1));
        out.put("quitNeedExamine", setting(state, "quitNeedExamine", 1));
        out.put("showOnlinePlayerNum", setting(state, "showOnlinePlayerNum", 1));
        out.put("showUplevelId", 1);
        out.put("showClubSign", 1);
        long unionId = setting(state, "unionId", 0);
        out.put("unionId", unionId);
        out.put("unionSign", setting(state, "unionSign", 0));
        out.put("unionName", text(state.settings(), "unionName", ""));
        out.put("unionPostType", unionId > 0 ? unionPostType(state, actor) : -1);
        out.put("levelPromotion", 1);
        out.put("invite", 1);
        out.put("gameList", state.templates().stream().map(template -> gameNumber(template.game())).filter(value -> value > 0).toList());
        out.put("sportsPoint", decimal(extra(state, actor).sportsPoint()));
        out.put("caseSportsPoint", decimal(extra(state, actor).caseSportsPoint()));
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
        out.put("playerClubCard", 0);
        out.put("skinType", setting(state, "skinType", 0));
        out.put("unionId", setting(state, "unionId", 0));
        out.put("unionSign", setting(state, "unionSign", 0));
        return out;
    }

    private Map<String, Object> memberRow(JdbcClubService.State state, long pid, int pageType) {
        JdbcClubService.MemberExtraState extra = extra(state, pid);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("pid", pid);
        row.put("status", pageType == 1 ? 1 : pageType == 2 ? 64 : 0);
        row.put("minister", ministerCode(state.members().get(pid)));
        row.put("playerClubCard", 0);
        row.put("sportsPoint", decimal(extra.sportsPoint()));
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
        cfg.put("gameIndex", template.index());
        cfg.put("playerNum", ruleNumber(template.rules(), "playerNum", 0));
        cfg.put("paymentRoomCardType", ruleNumber(template.rules(), "paymentRoomCardType", 0));
        cfg.put("clubWinnerPayConsume", ruleNumber(template.rules(), "clubWinnerPayConsume", 0));
        cfg.put("dataJsonCfg", template.rules());
        row.put("bRoomConfigure", cfg);
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
        row.put("configName", "亲友圈房间");
        row.put("valueType", 0);
        row.put("roomCard", 0);
        row.put("unionId", 0);
        row.put("roomSportsConsume", 0);
        row.put("isViewed", state.viewedRooms().contains(record.roomId()));
        row.put("playerList", record.scores().entrySet().stream().map(entry -> {
            Map<String, Object> player = new LinkedHashMap<>(profile(entry.getKey()));
            player.put("point", entry.getValue());
            player.put("sportsPoint", entry.getValue());
            player.put("clubName", state.name());
            return player;
        }).toList());
        return row;
    }

    private List<Map<String, Object>> sportsDynamicRows(JdbcClubService.State state, long pid) {
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
                    item.put("sportsPoint", row.amount());
                    item.put("id", row.businessKey());
                    item.put("winLoseValue", row.amount());
                    item.put("consumeValue", 0);
                    item.put("eliminatePoint", 0);
                    item.put("pidCurValue", decimal(extra(state, row.playerId()).sportsPoint()));
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
        String sql = "SELECT room_id,game_id,play_version,state,club_template_code,updated_at FROM aoo_hall_room WHERE club_id=? AND state IN ('OPEN','PLAYING') ORDER BY updated_at DESC,room_id DESC LIMIT 200";
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
                    row.put("roomName", Objects.toString(rows.getString("club_template_code"), "亲友圈房间"));
                    row.put("setId", 0);
                    row.put("setCount", 0);
                    row.put("playerNum", 0);
                    row.put("posList", List.of());
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

    private Optional<JdbcClubService.State> unionBySign(long unionSign) {
        return clubs.list().stream()
                .filter(state -> "ACTIVE".equals(state.status()))
                .filter(state -> setting(state, "unionSign", 0) == unionSign && setting(state, "unionId", 0) > 0)
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
        out.put("initSports", setting(state, "unionInitSports", 2000));
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
        row.put("sportsPoint", decimal(extra(state, actor).sportsPoint()));
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
        target.put("unionInitSports", setting(source, "unionInitSports", 2000));
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
        String sql = "SELECT player_id,nickname,avatar_url,gender_code FROM player_profile WHERE player_id=?";
        try (Connection connection = source.getConnection(); PreparedStatement query = connection.prepareStatement(sql)) {
            query.setLong(1, pid);
            try (ResultSet rows = query.executeQuery()) {
                if (!rows.next()) return Map.of();
                String name = rows.getString("nickname");
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("pid", pid);
                out.put("id", pid);
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

    private static long gameNumber(String game) {
        if (game == null || game.isBlank()) return 0;
        try { return Long.parseLong(game); }
        catch (NumberFormatException ignored) { return 0; }
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
