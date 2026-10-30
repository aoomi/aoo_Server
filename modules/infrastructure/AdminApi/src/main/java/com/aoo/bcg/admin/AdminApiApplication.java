package com.aoo.bcg.admin;

import com.aoo.bcg.config.GameProfilePublicationService;
import com.aoo.bcg.config.InMemoryGameProfileRepository;
import com.aoo.bcg.config.RegionalGameCatalog;
import com.aoo.bcg.config.PublishedGameProfile;
import com.aoo.bcg.billing.JdbcReconciliationService;
import com.aoo.bcg.billing.ReconciliationService;
import com.aoo.bcg.common.event.JdbcOutboxRepository;
import com.aoo.bcg.common.event.OutboxRelay;
import com.aoo.bcg.common.event.RocketMqOutboxPublisher;
import com.aoo.bcg.common.event.ScheduledOutboxRelay;
import com.aoo.bcg.common.persistence.DriverManagerDataSource;
import com.aoo.bcg.common.config.DeploymentEnvironment;
import com.aoo.bcg.common.config.EnvironmentSecretProvider;
import com.aoo.bcg.common.config.ProductionConfigPolicy;
import com.aoo.bcg.common.config.RuntimeConfigKey;
import com.aoo.bcg.common.config.SecretResolver;
import com.aoo.bcg.common.config.StrictRuntimeConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.DriverManager;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

/** Dedicated management control plane. It must never share the player gateway. */
public final class AdminApiApplication {
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    private final AdminRequestAuthenticator requestAuthenticator;
    private final GameProfilePublicationService.ProfileRepository profileRepository;
    private final GameProfilePublicationService profiles;
    private final GameProfileReleaseWorkflow releaseWorkflow;
    private final AdminResourceRepository resources;
    private final AdminAuthorizationService authorization;
    private final AdminAuthorizationAudit authorizationAudit;
    private final AdminPermissionCatalog permissionCatalog=new AdminPermissionCatalog();
    private final AdminSecondaryApprovalVerifier approvalVerifier;
    private final GameInvestigationService investigationService;
    private final AdminCommandValidator commandValidator = new AdminCommandValidator();
    private final ReconciliationService reconciliation;
    private final SensitiveExportService sensitiveExports;
    private final AutoCloseable outboxRuntime;
    private final AdminMapProxy mapProxy;
    private final JdbcAdminSessionService browserSessions;
    private final String allowedOrigin;

    private AdminApiApplication(String adminToken, StrictRuntimeConfig config, SecretResolver secrets) {
        AdminNonceStore nonceStore;
        GameInvestigationService.ReadOnlyRepository investigationRepository;
        GameProfileReleaseWorkflow.Store releaseStore;
        String databaseUrl = config.has(RuntimeConfigKey.ADMIN_DB_URL) ? config.require(RuntimeConfigKey.ADMIN_DB_URL) : null;
        if (databaseUrl == null || databaseUrl.isBlank())
            throw new IllegalStateException("ADMIN_DB_URL is required: AdminApi permits only durable runtime repositories");
        {
            String databaseUser = config.require(RuntimeConfigKey.ADMIN_DB_USER);
            String databasePassword = secret(config, RuntimeConfigKey.ADMIN_DB_PASSWORD, secrets);
            // JDBC_OWNERSHIP_TRANSFER: repository methods close every connection returned by this factory.
            JdbcAdminResourceRepository.ConnectionFactory connections =
                    () -> DriverManager.getConnection(databaseUrl, databaseUser, databasePassword);
            this.resources = new JdbcAdminResourceRepository(connections, json, Clock.systemUTC());
            this.profileRepository = new JdbcGameProfileRepository(connections, json);
            this.authorization = new JdbcAdminAuthorizationService(connections);
            this.authorizationAudit=new JdbcAdminAuthorizationAudit(connections);
            investigationRepository = new JdbcGameInvestigationRepository(connections, json);
            releaseStore = new JdbcGameProfileReleaseStore(connections, json);
            nonceStore = new JdbcAdminNonceStore(connections);
            // JDBC_OWNERSHIP_TRANSFER: reconciliation service owns and closes the supplied connection.
            this.reconciliation = new JdbcReconciliationService(
                    () -> DriverManager.getConnection(databaseUrl, databaseUser, databasePassword));
            this.outboxRuntime = startOutbox(databaseUrl, databaseUser, databasePassword, config);
            var exportRepository = new JdbcSensitiveExportRepository(connections, json);
            this.sensitiveExports = sensitiveExportService(exportRepository, exportRepository);
            this.browserSessions = new JdbcAdminSessionService(connections, Clock.systemUTC());
        }
        this.profiles = new GameProfilePublicationService(profileRepository, new RegionalGameCatalog(), Clock.systemUTC());
        this.releaseWorkflow = new GameProfileReleaseWorkflow((gameId, profile) -> {
            try {
                PublishedGameProfile value = json.convertValue(profile, PublishedGameProfile.class);
                return value.gameId() == gameId ? List.of() : List.of("profile gameId mismatch");
            } catch (Exception error) { return List.of("invalid published game profile"); }
        }, new GameProfileReleaseWorkflow.Publisher() {
            @Override public void activate(GameProfileReleaseWorkflow.Release release, String requestId,
                    long operatorId, String reason) {
                PublishedGameProfile profile = json.convertValue(release.profile(), PublishedGameProfile.class);
                if (profile.gameId() != release.gameId() || !profile.version().equals(release.version()))
                    throw new IllegalArgumentException("release identity mismatch");
                profiles.publish(profile, operatorId, reason, requestId);
            }
            @Override public void rollback(long gameId, String previousVersion, String requestId,
                    long operatorId, String reason) {
                profiles.rollback(gameId, previousVersion, operatorId, reason, requestId);
            }
        }, Clock.systemUTC(), releaseStore);
        this.requestAuthenticator = new AdminRequestAuthenticator(adminToken, Clock.systemUTC(), nonceStore);
        this.approvalVerifier = new AdminSecondaryApprovalVerifier(adminToken, authorization, Clock.systemUTC(), nonceStore);
        this.investigationService = new GameInvestigationService(investigationRepository, Clock.systemUTC());
        String mapKey = secret(config, RuntimeConfigKey.ADMIN_MAP_PROVIDER_KEY, secrets);
        this.mapProxy = AdminMapProxy.production(mapKey, Clock.systemUTC(),
                Duration.ofMillis(config.integer(RuntimeConfigKey.ADMIN_MAP_TIMEOUT_MS, 100, 10_000)),
                config.integer(RuntimeConfigKey.ADMIN_MAP_RATE_PER_MINUTE, 1, 10_000),
                Duration.ofSeconds(config.integer(RuntimeConfigKey.ADMIN_MAP_CACHE_SECONDS, 1, 86_400)),
                event -> System.err.println("admin_map_proxy requestId=" + event.requestId()
                        + " operatorId=" + event.operatorId() + " outcome=" + event.outcome()
                        + " cached=" + event.cached()));
        this.allowedOrigin = requiredAllowedOrigin(System.getenv("AOO_ADMIN_ALLOWED_ORIGIN"));
    }

    private AutoCloseable startOutbox(String databaseUrl, String databaseUser, String databasePassword, StrictRuntimeConfig config) {
        String nameserver = config.has(RuntimeConfigKey.MQ_NAMESERVER) ? config.require(RuntimeConfigKey.MQ_NAMESERVER) : null;
        if (nameserver == null || nameserver.isBlank()) return () -> {};
        try {
            var dataSource = new DriverManagerDataSource(databaseUrl, databaseUser, databasePassword);
            var repository = new JdbcOutboxRepository(dataSource, json);
            var publisher = new RocketMqOutboxPublisher(nameserver,
                    config.require(RuntimeConfigKey.MQ_PRODUCER_GROUP),
                    config.require(RuntimeConfigKey.MQ_GAME_PROFILE_TOPIC), json);
            var relay = new OutboxRelay(repository, publisher, Clock.systemUTC(), Duration.ofSeconds(5));
            var scheduled = new ScheduledOutboxRelay(relay, repository, Clock.systemUTC(),
                    Duration.ofSeconds(1), 200, health -> {
                        if (health.relayFailure() != null || health.backlog().failedCount() > 0)
                            System.err.println("outbox health=" + health);
                    });
            scheduled.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                scheduled.close();
                publisher.close();
            }, "aoo-admin-outbox-shutdown"));
            return () -> { scheduled.close(); publisher.close(); };
        } catch (Exception error) {
            throw new IllegalStateException("cannot start admin outbox runtime", error);
        }
    }

    public static void main(String[] args) throws IOException {
        Map<String,String> arguments = new java.util.LinkedHashMap<>();
        System.getProperties().forEach((key,value) -> {
            String name = String.valueOf(key);
            if (java.util.Arrays.stream(RuntimeConfigKey.values()).anyMatch(item -> item.canonicalName().equals(name)))
                arguments.put(name, String.valueOf(value));
        });
        var config = StrictRuntimeConfig.bind(arguments, System.getenv(), Map.of(), Map.of(), Map.of(
            RuntimeConfigKey.ADMIN_API_PORT.canonicalName(), "8088",
            RuntimeConfigKey.ADMIN_MAP_TIMEOUT_MS.canonicalName(), "1500",
            RuntimeConfigKey.ADMIN_MAP_RATE_PER_MINUTE.canonicalName(), "60",
            RuntimeConfigKey.ADMIN_MAP_CACHE_SECONDS.canonicalName(), "3600",
            RuntimeConfigKey.ADMIN_DEV_OPERATOR_ID.canonicalName(), "0",
            RuntimeConfigKey.ADMIN_DEV_PERMISSIONS.canonicalName(), "none",
            RuntimeConfigKey.MQ_PRODUCER_GROUP.canonicalName(), "aoo-admin-outbox"));
        DeploymentEnvironment environment = DeploymentEnvironment.parse(config.require(RuntimeConfigKey.ENVIRONMENT));
        Set<RuntimeConfigKey> required = environment == DeploymentEnvironment.PRODUCTION
            ? Set.of(RuntimeConfigKey.ADMIN_API_TOKEN, RuntimeConfigKey.ADMIN_DB_URL,
                RuntimeConfigKey.ADMIN_DB_USER, RuntimeConfigKey.ADMIN_DB_PASSWORD,
                RuntimeConfigKey.ADMIN_MAP_PROVIDER_KEY)
            : Set.of(RuntimeConfigKey.ADMIN_API_TOKEN, RuntimeConfigKey.ADMIN_DB_URL,
                RuntimeConfigKey.ADMIN_DB_USER, RuntimeConfigKey.ADMIN_DB_PASSWORD,
                RuntimeConfigKey.ADMIN_MAP_PROVIDER_KEY);
        ProductionConfigPolicy.verify(config, required);
        var secrets = new SecretResolver(Map.of("env", new EnvironmentSecretProvider(System.getenv())));
        String token = secret(config, RuntimeConfigKey.ADMIN_API_TOKEN, secrets);
        if (token == null || token.length() < 32) {
            throw new IllegalStateException("ADMIN_API_TOKEN must contain at least 32 characters");
        }
        int port = config.integer(RuntimeConfigKey.ADMIN_API_PORT, 1, 65535);
        new AdminApiApplication(token, config, secrets).start(port);
    }

    private static String secret(StrictRuntimeConfig config, RuntimeConfigKey key, SecretResolver resolver) {
        try (var material = resolver.resolve(config.require(key))) { return new String(material.copy()); }
    }

    private void start(int port) throws IOException {
        InetSocketAddress bindAddress = new InetSocketAddress("127.0.0.1", port);
        AdminRequestAuthenticator.requireLoopbackBoundary(bindAddress);
        HttpServer server = HttpServer.create(bindAddress, 128);
        server.createContext("/api/auth/login", this::login);
        server.createContext("/api/auth/logout", exchange -> authorized(exchange, operatorId -> logout(exchange)));
        server.createContext("/api/auth/me", exchange -> authorized(exchange, operatorId -> me(exchange, operatorId)));
        server.createContext("/api/v2/admin/game-profiles", exchange -> authorized(exchange,
                operatorId -> gameProfiles(exchange, operatorId)));
        server.createContext("/api/v2/admin/map/ip-location", exchange -> authorized(exchange,
                operatorId -> mapLocation(exchange, operatorId)));
        server.createContext("/api/v2/admin/game-profile-releases", exchange -> authorized(exchange,
                operatorId -> gameProfileReleases(exchange, operatorId)));
        server.createContext("/api/v2/admin/game-investigations", exchange -> authorized(exchange,
                operatorId -> gameInvestigation(exchange, operatorId)));
        server.createContext("/api/v2/admin/operation-switches", exchange -> authorized(exchange,
                operatorId -> resource(exchange, "operation-switches", null, operatorId)));
        server.createContext("/api/v2/admin/appeals", exchange -> authorized(exchange,
                operatorId -> resource(exchange, "appeals", "RESOLVED", operatorId)));
        server.createContext("/api/v2/admin/reconciliation", exchange -> authorized(exchange,
                operatorId -> reconciliation(exchange)));
        server.createContext("/api/v2/admin/data-lifecycle", exchange -> authorized(exchange,
                operatorId -> resource(exchange, "data-lifecycle", null, operatorId)));
        server.createContext("/api/v2/admin/sensitive-exports", exchange -> authorized(exchange,
                operatorId -> sensitiveExport(exchange, operatorId)));
        var requestExecutor = Executors.newVirtualThreadPerTaskExecutor();
        server.setExecutor(requestExecutor);
        Runtime.getRuntime().addShutdownHook(Thread.ofPlatform().name("aoo-admin-http-shutdown").unstarted(() -> {
            server.stop(1);
            requestExecutor.close();
        }));
        server.start();
    }

    private void reconciliation(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getRawQuery();
        String dateValue = query == null ? "" : java.util.Arrays.stream(query.split("&"))
                .filter(value -> value.startsWith("date="))
                .map(value -> value.substring("date=".length())).findFirst().orElse("");
        ReconciliationService.Report report = reconciliation.reconcile(LocalDate.parse(dateValue));
        write(exchange, 200, Map.of("code", 0, "msg", "success",
                "data", Map.of("items", report.differences(), "balanced", report.balanced(),
                        "businessDate", report.businessDate().toString()),
                "timestamp", Instant.now().toEpochMilli()));
    }

    private void mapLocation(HttpExchange exchange, long operatorId) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) throw new IllegalArgumentException("GET required");
        Map<String, String> parameters = AdminMapProxy.parseQuery(exchange.getRequestURI().getRawQuery());
        Object result = mapProxy.locate(parameters, operatorId,
                exchange.getRequestHeaders().getFirst("X-Request-Id"));
        write(exchange, 200, Map.of("code", 0, "msg", "success", "data", result,
                "timestamp", Instant.now().toEpochMilli()));
    }

    private SensitiveExportService sensitiveExportService(SensitiveExportService.Source source,
            SensitiveExportService.Repository repository) {
        var definition = new SensitiveExportService.Definition("club-member-support", Set.of("CLUB"), 5_000,
                Map.of("player_id", SensitiveExportService.Mask.NONE,
                        "member_status", SensitiveExportService.Mask.NONE,
                        "member_role", SensitiveExportService.Mask.NONE,
                        "phone", SensitiveExportService.Mask.PHONE,
                        "email", SensitiveExportService.Mask.EMAIL,
                        "joined_at", SensitiveExportService.Mask.NONE));
        return new SensitiveExportService(List.of(definition), source, repository,
                Clock.systemUTC(), Duration.ofMinutes(10));
    }

    private void sensitiveExport(HttpExchange exchange, long operatorId) throws IOException {
        String base = "/api/v2/admin/sensitive-exports/";
        String path = exchange.getRequestURI().getPath();
        if (!path.startsWith(base)) throw new IllegalArgumentException("export id required");
        String[] parts = path.substring(base.length()).split("/");
        if (parts.length != 2 || parts[0].isBlank()) throw new IllegalArgumentException("export action required");
        String exportId = parts[0];
        if ("GET".equals(exchange.getRequestMethod()) && "download".equals(parts[1])) {
            SensitiveExportService.Artifact artifact = sensitiveExports.download(exportId, operatorId);
            exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=admin-export-" + exportId + ".json");
            exchange.getResponseHeaders().set("X-Content-SHA256", artifact.contentHash());
            write(exchange, 200, artifact);
            return;
        }
        if (!"POST".equals(exchange.getRequestMethod())) throw new IllegalArgumentException("invalid export method");
        Map<String, Object> command = readCommand(exchange);
        String requestId = requiredText(command, "requestId");
        if (!requestId.equals(exchange.getRequestHeaders().getFirst("X-Request-Id")))
            throw new IllegalArgumentException("body and signed requestId mismatch");
        String reason = requiredText(command, "reason");
        Object result = switch (parts[1]) {
            case "request" -> sensitiveExports.request(requestId, exportId,
                    requiredText(command, "definitionCode"), requiredText(command, "scope"),
                    requiredInt(command, "rowLimit"), operatorId, reason);
            case "approve" -> sensitiveExports.approve(requestId, exportId, operatorId, reason);
            case "generate" -> {
                SensitiveExportService.Artifact artifact = sensitiveExports.export(requestId, exportId, operatorId, reason);
                yield Map.of("exportId", artifact.exportId(), "contentHash", artifact.contentHash(),
                        "rowCount", artifact.rows().size(), "generatedAt", artifact.generatedAt(),
                        "expiresAt", artifact.expiresAt());
            }
            default -> throw new IllegalArgumentException("unknown export action");
        };
        write(exchange, 200, Map.of("code", 0, "msg", "success", "data", result,
                "timestamp", Instant.now().toEpochMilli()));
    }

    private void gameInvestigation(HttpExchange exchange, long operatorId) throws IOException {
        String base = "/api/v2/admin/game-investigations/";
        String path = exchange.getRequestURI().getPath();
        if (!"GET".equals(exchange.getRequestMethod()) || !path.startsWith(base))
            throw new IllegalArgumentException("read-only investigation path required");
        long roomId = Long.parseLong(path.substring(base.length()));
        Map<String, String> query = query(exchange.getRequestURI().getRawQuery());
        long from = Long.parseLong(query.getOrDefault("from", "0"));
        long to = Long.parseLong(query.getOrDefault("to", Long.toString(from + 10_000)));
        int setId = Integer.parseInt(query.getOrDefault("set", "0"));
        String reason = exchange.getRequestHeaders().getFirst("X-Admin-Reason");
        GameInvestigationService.Evidence evidence = investigationService.investigate(
                new GameInvestigationService.Query(roomId, from, to, setId), operatorId, reason);
        write(exchange, 200, Map.of("code", 0, "msg", "success", "data", evidence,
                "timestamp", Instant.now().toEpochMilli()));
    }

    @SuppressWarnings("unchecked")
    private void gameProfiles(HttpExchange exchange, long operatorId) throws IOException {
        if ("GET".equals(exchange.getRequestMethod())) {
            items(exchange, profileRepository.active());
            return;
        }
        throw new IllegalArgumentException("profile publication requires release workflow");
    }

    @SuppressWarnings("unchecked")
    private void gameProfileReleases(HttpExchange exchange, long operatorId) throws IOException {
        String base = "/api/v2/admin/game-profile-releases/";
        String suffix = exchange.getRequestURI().getPath().substring(base.length());
        String[] parts = suffix.split("/");
        if (parts.length < 2 || parts.length > 3) throw new IllegalArgumentException("invalid release path");
        long gameId = Long.parseLong(parts[0]);
        String releaseId = parts[1];
        if ("GET".equals(exchange.getRequestMethod()) && parts.length == 2) {
            Object result = releaseWorkflow.find(releaseId)
                    .orElseThrow(() -> new IllegalArgumentException("release not found"));
            write(exchange, 200, Map.of("code", 0, "msg", "success", "data", result,
                    "timestamp", Instant.now().toEpochMilli()));
            return;
        }
        if (parts.length != 3) throw new IllegalArgumentException("release action required");
        String action = parts[2];
        Map<String, Object> command = readCommand(exchange);
        String requestId = requiredText(command, "requestId");
        if (!requestId.equals(exchange.getRequestHeaders().getFirst("X-Request-Id")))
            throw new IllegalArgumentException("body and signed requestId mismatch");
        String reason = requiredText(command, "reason");
        GameProfileReleaseWorkflow.Release result = switch (action) {
            case "draft" -> releaseWorkflow.draft(requestId, releaseId, gameId,
                    requiredText(command, "version"), (Map<String, Object>) command.get("profile"),
                    String.valueOf(command.getOrDefault("previousVersion", "")), operatorId, reason);
            case "validate" -> releaseWorkflow.validate(requestId, releaseId, operatorId, reason);
            case "submit" -> releaseWorkflow.submit(requestId, releaseId, operatorId, reason);
            case "approve" -> releaseWorkflow.approve(requestId, releaseId, operatorId, reason);
            case "canary" -> releaseWorkflow.deployCanary(requestId, releaseId,
                    requiredInt(command, "percentage"), operatorId, reason);
            case "activate" -> releaseWorkflow.activate(requestId, releaseId, operatorId, reason);
            case "rollback" -> releaseWorkflow.rollback(requestId, releaseId, operatorId, reason);
            default -> throw new IllegalArgumentException("unknown release action");
        };
        if (result.gameId() != gameId) throw new IllegalArgumentException("release gameId mismatch");
        write(exchange, 200, Map.of("code", 0, "msg", "success", "data", result,
                "timestamp", Instant.now().toEpochMilli()));
    }

    private void authorized(HttpExchange exchange, IoAction action) throws IOException {
        try {
            enforceOrigin(exchange);
            if (!Set.of("GET", "POST", "PUT").contains(exchange.getRequestMethod())) {
                write(exchange, 405, Map.of("code", 1001, "msg", "method_not_allowed"));
                return;
            }
            String requestId = exchange.getRequestHeaders().getFirst("X-Request-Id");
            if (requestId == null || requestId.isBlank() || requestId.length() > 128)
                throw new SecurityException("signed request id required");
            Map<String, String> headers = new java.util.LinkedHashMap<>();
            exchange.getRequestHeaders().forEach((name, values) ->
                    headers.put(name, values.isEmpty() ? "" : values.getFirst()));
            long operatorId = authenticate(exchange, requestId, headers);
            if (exchange.getRequestURI().getPath().startsWith("/api/auth/")) {
                action.run(operatorId);
                return;
            }
            AdminPermissionCatalog.Route route = permissionCatalog
                    .route(exchange.getRequestMethod(), exchange.getRequestURI().getPath()).orElse(null);
            String permission = route == null ? "control.denied" : route.permission();
            String targetType = route == null ? "DENIED" : route.targetType();
            String targetId = route == null ? "DENIED" : targetId(route, exchange.getRequestURI().getPath());
            boolean allowed = route != null && this.authorization
                    .allowed(operatorId, permission, targetType, targetId);
            if (allowed && route.risk() == AdminAccessPolicy.Risk.HIGH) {
                allowed = approvalVerifier.verify(operatorId, permission, targetType, targetId,
                        exchange.getRequestHeaders().getFirst("X-Admin-Approval-Id"),
                        exchange.getRequestHeaders().getFirst("X-Admin-Approver-Id"),
                        exchange.getRequestHeaders().getFirst("X-Admin-Approval-Expires"),
                        exchange.getRequestHeaders().getFirst("X-Admin-Approval-Signature"));
            }
            authorizationAudit.record(requestId,operatorId,permission,exchange.getRequestMethod(),exchange.getRequestURI().getPath(),allowed,Instant.now());
            if (!allowed) {
                write(exchange, 403, Map.of("code", 2003, "msg", "forbidden"));
                return;
            }
            action.run(operatorId);
        } catch (SecurityException error) {
            write(exchange, 401, Map.of("code", 2001, "msg", "unauthorized"));
        } catch (IllegalArgumentException error) {
            write(exchange, 400, Map.of("code", 1002, "msg", "invalid_request"));
        } catch (AdminPreconditionException error) {
            if (error.currentEtag() != null) exchange.getResponseHeaders().set("ETag", error.currentEtag());
            write(exchange, 412, Map.of("code", 2004, "msg", "version_conflict"));
        } catch (AdminMapProxy.ProxyException error) {
            write(exchange, error.status(), Map.of("code", error.code(), "msg", error.getMessage(),
                    "data", Map.of(), "timestamp", Instant.now().toEpochMilli()));
        } catch (Exception error) {
            write(exchange, 500, Map.of("code", 1003, "msg", "internal_error"));
        } finally {
            exchange.close();
        }
    }

    @SuppressWarnings("unchecked")
    private void login(HttpExchange exchange) throws IOException {
        try {
            enforceOrigin(exchange);
            if (!"POST".equals(exchange.getRequestMethod())) { write(exchange,405,Map.of("code",1001,"msg","method_not_allowed")); return; }
            Map<String,Object> body=readCommand(exchange);
            String username=requiredText(body,"username"); String password=requiredText(body,"password");
            JdbcAdminSessionService.Login login=browserSessions.login(username,password.toCharArray());
            if(login==null){write(exchange,401,Map.of("code",2001,"msg","unauthorized"));return;}
            List<String> buttons=permissionCatalog.routes().stream().filter(route->login.permissions().contains(route.permission())).map(AdminPermissionCatalog.Route::button).distinct().toList();
            exchange.getResponseHeaders().add("Set-Cookie","aoo_admin_session="+login.sessionToken()+"; HttpOnly; Secure; SameSite=Strict; Path=/; Max-Age=28800");
            write(exchange,200,Map.of("code",0,"msg","success","data",Map.of("userName",login.userName(),"roles",login.roles(),"authBtnList",buttons,"csrfToken",login.csrfToken()),"timestamp",Instant.now().toEpochMilli()));
        } catch(IllegalArgumentException error){write(exchange,400,Map.of("code",1002,"msg","invalid_request"));}
        catch(Exception error){write(exchange,500,Map.of("code",1003,"msg","internal_error"));}
        finally{exchange.close();}
    }

    private void logout(HttpExchange exchange) throws IOException {
        browserSessions.logout(cookie(exchange,"aoo_admin_session"));
        exchange.getResponseHeaders().add("Set-Cookie","aoo_admin_session=; HttpOnly; Secure; SameSite=Strict; Path=/; Max-Age=0");
        write(exchange,200,Map.of("code",0,"msg","success","data",Map.of(),"timestamp",Instant.now().toEpochMilli()));
    }

    private void me(HttpExchange exchange,long operatorId) throws IOException {
        write(exchange,200,Map.of("code",0,"msg","success","data",Map.of("operatorId",operatorId),"timestamp",Instant.now().toEpochMilli()));
    }

    private long authenticate(HttpExchange exchange,String requestId,Map<String,String> headers) {
        String token=cookie(exchange,"aoo_admin_session");
        JdbcAdminSessionService.Session session=browserSessions.authenticate(token);
        if(session!=null){
            if(!"GET".equals(exchange.getRequestMethod())&&!browserSessions.csrfMatches(session,exchange.getRequestHeaders().getFirst("X-CSRF-Token")))throw new SecurityException("csrf rejected");
            return session.operatorId();
        }
        return requestAuthenticator.authenticate(exchange.getRequestHeaders().getFirst("Authorization"),exchange.getRequestHeaders().getFirst("X-Admin-Id"),exchange.getRequestHeaders().getFirst("X-Admin-Timestamp"),exchange.getRequestHeaders().getFirst("X-Admin-Nonce"),exchange.getRequestHeaders().getFirst("X-Admin-Signature"),requestId,exchange.getRequestMethod(),signedTarget(exchange),headers);
    }

    private void enforceOrigin(HttpExchange exchange) {
        String origin=exchange.getRequestHeaders().getFirst("Origin");
        if(origin!=null&&!origin.equals(allowedOrigin))throw new SecurityException("origin rejected");
        if(origin!=null){exchange.getResponseHeaders().set("Access-Control-Allow-Origin",allowedOrigin);exchange.getResponseHeaders().set("Vary","Origin");exchange.getResponseHeaders().set("Access-Control-Allow-Credentials","true");}
    }
    private static String requiredAllowedOrigin(String value){if(value==null||!value.matches("https://[A-Za-z0-9.-]+(?::[0-9]{1,5})?"))throw new IllegalStateException("AOO_ADMIN_ALLOWED_ORIGIN must be one explicit HTTPS origin");return value;}
    private String cookie(HttpExchange exchange,String name){String raw=exchange.getRequestHeaders().getFirst("Cookie");if(raw==null)return null;for(String part:raw.split(";")){String[] pair=part.trim().split("=",2);if(pair.length==2&&pair[0].equals(name))return pair[1];}return null;}

    @SuppressWarnings("unchecked")
    private void resource(HttpExchange exchange, String resourceType, String forcedStatus, long operatorId) throws IOException {
        if ("GET".equals(exchange.getRequestMethod())) {
            items(exchange, resources.list(resourceType));
            return;
        }
        String base = "/api/v2/admin/" + resourceType + "/";
        String path = exchange.getRequestURI().getPath();
        if (!path.startsWith(base)) throw new IllegalArgumentException("resource id required");
        String suffix = path.substring(base.length());
        String id = suffix.endsWith("/resolve")
                ? suffix.substring(0, suffix.length() - "/resolve".length()) : suffix;
        if (id.isBlank() || id.contains("/")) throw new IllegalArgumentException("invalid resource id");
        byte[] body = exchange.getRequestBody().readNBytes(65_537);
        if (body.length == 0 || body.length > 65_536) throw new IllegalArgumentException("invalid body size");
        Map<String, Object> command = json.readValue(body, Map.class);
        commandValidator.validate(resourceType, id, command);
        Map<String, Object> value = resources.execute(resourceType, id, operatorId, command, forcedStatus,
                exchange.getRequestHeaders().getFirst("If-Match"));
        exchange.getResponseHeaders().set("ETag", String.valueOf(value.get("_etag")));
        write(exchange, 200, Map.of("code", 0, "msg", "success", "data", value,
                "timestamp", Instant.now().toEpochMilli()));
    }

    private void items(HttpExchange exchange, List<?> values) throws IOException {
        write(exchange, 200, Map.of("code", 0, "msg", "success",
                "data", Map.of("items", values), "timestamp", Instant.now().toEpochMilli()));
    }

    private String targetId(AdminPermissionCatalog.Route route, String path) {
        String[] pattern = route.pattern().split("/");
        String[] actual = path.split("/");
        for (int index = 0; index < Math.min(pattern.length, actual.length); index++) {
            if ("{scope}".equals(pattern[index])) return actual[index];
            if ("{id}".equals(pattern[index])) return actual[index];
        }
        return "*";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readCommand(HttpExchange exchange) throws IOException {
        byte[] body = exchange.getRequestBody().readNBytes(65_537);
        if (body.length == 0 || body.length > 65_536) throw new IllegalArgumentException("invalid body size");
        return json.readValue(body, Map.class);
    }

    private String requiredText(Map<String, Object> command, String field) {
        Object value = command.get(field);
        if (!(value instanceof String text) || text.isBlank()) throw new IllegalArgumentException(field + " required");
        return text;
    }

    private int requiredInt(Map<String, Object> command, String field) {
        Object value = command.get(field);
        if (!(value instanceof Number number)) throw new IllegalArgumentException(field + " required");
        return number.intValue();
    }

    private String signedTarget(HttpExchange exchange) {
        String rawQuery = exchange.getRequestURI().getRawQuery();
        return exchange.getRequestURI().getRawPath() + (rawQuery == null ? "" : "?" + rawQuery);
    }

    private Map<String, String> query(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) return Map.of();
        Map<String, String> values = new java.util.LinkedHashMap<>();
        for (String pair : rawQuery.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2 && parts[0].matches("[a-z]+") && parts[1].matches("[0-9]+"))
                values.put(parts[0], parts[1]);
            else throw new IllegalArgumentException("invalid investigation query");
        }
        return Map.copyOf(values);
    }

    private void write(HttpExchange exchange, int status, Object value) throws IOException {
        byte[] body = json.writeValueAsBytes(value);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
    }

    @FunctionalInterface
    private interface IoAction { void run(long operatorId) throws IOException; }
}
