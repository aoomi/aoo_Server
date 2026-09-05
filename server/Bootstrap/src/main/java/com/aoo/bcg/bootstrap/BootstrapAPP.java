package com.aoo.bcg.bootstrap;

import com.aoo.bcg.gamespi.GameProvider;
import com.aoo.bcg.gamespi.GameRegistry;
import com.aoo.bcg.common.readiness.ServiceReadinessGate;
import com.aoo.bcg.hall.HallApplication;
import com.aoo.bcg.hall.http.HallHttpRoutes;
import com.aoo.bcg.hall.http.HallRequestAuthenticator;
import com.aoo.bcg.hall.room.JdbcHallRepository;
import com.aoo.bcg.club.ClubHttpRoutes;
import com.aoo.bcg.club.JdbcClubService;
import com.aoo.bcg.common.persistence.DriverManagerDataSource;
import com.aoo.bcg.common.id.DistributedIdGenerator;
import com.aoo.bcg.billing.*;
import com.aoo.bcg.ranking.*;
import com.aoo.bcg.inventory.*;
import com.aoo.bcg.referral.*;
import com.aoo.bcg.activity.*;
import com.aoo.bcg.media.*;
import com.aoo.bcg.social.*;
import com.aoo.bcg.matchmaking.*;
import com.aoo.bcg.hall.room.JdbcHallRepository;
import com.aoo.bcg.hall.http.HallRequestAuthenticator;
import com.aoo.bcg.account.AccountHttpRoutes;
import com.aoo.bcg.account.JdbcAccountSessionService;
import com.aoo.bcg.account.IdentityHttpRoutes;
import com.aoo.bcg.account.TrustedDevicePinService;
import com.aoo.bcg.account.UnifiedIdentityService;
import com.aoo.bcg.privacy.*;
import com.aoo.bcg.profile.*;
import com.aoo.bcg.support.*;
import com.aoo.bcg.version.*;
import com.aoo.bcg.spectator.*;
import com.aoo.bcg.inventory.*;
import com.aoo.bcg.share.*;
import com.aoo.bcg.telemetry.*;
import com.aoo.bcg.gifting.*;
import com.aoo.bcg.identity.*;
import com.aoo.bcg.luckdraw.*;
import com.aoo.bcg.roomsafety.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.time.Clock;
import javax.sql.DataSource;
import java.util.Arrays;
import java.util.ServiceLoader;
import java.util.List;
import java.util.Map;

public final class BootstrapAPP {
    private BootstrapAPP() {}

    public static void main(String[] args) throws Exception {
        if (args.length == 0 || "catalog".equalsIgnoreCase(args[0])) {
            loadRegistry().descriptors().forEach(System.out::println);
            return;
        }

        String[] serviceArgs = Arrays.copyOfRange(args, 1, args.length);
        if ("hall".equalsIgnoreCase(args[0])) {
            startHall(serviceArgs);
        } else if ("club".equalsIgnoreCase(args[0])) {
            startClub();
        } else if ("billing".equalsIgnoreCase(args[0])) {
            startBilling();
        } else if ("ranking".equalsIgnoreCase(args[0])) {
            startRanking();
        } else if ("referral".equalsIgnoreCase(args[0])) {
            startReferral();
        } else if ("media".equalsIgnoreCase(args[0])) {
            startMedia();
        } else if ("social".equalsIgnoreCase(args[0])) {
            startSocial();
        } else if ("competition".equalsIgnoreCase(args[0])) {
            startCompetition();
        } else if ("account".equalsIgnoreCase(args[0])) {
            startAccount();
        } else if ("support".equalsIgnoreCase(args[0])) {
            startSupport();
        } else if ("activity".equalsIgnoreCase(args[0])) {
            startActivity();
        } else if ("luckdraw".equalsIgnoreCase(args[0])) {
            startLuckDraw();
        } else if ("version".equalsIgnoreCase(args[0])) {
            startVersion();
        } else if ("spectator".equalsIgnoreCase(args[0])) {
            startSpectator();
        } else if ("inventory".equalsIgnoreCase(args[0])) {
            startInventory();
        } else if ("telemetry".equalsIgnoreCase(args[0])) {
            startTelemetry();
        } else if ("gifting".equalsIgnoreCase(args[0])) {
            startGifting();
        } else if ("identity-verification".equalsIgnoreCase(args[0])) {
            startIdentityVerification();
        } else if ("external-platform".equalsIgnoreCase(args[0])) {
            ExternalPlatformBootstrap.start();
        } else if ("room-safety".equalsIgnoreCase(args[0])) {
            startRoomSafety();
        } else {
            GameProvider provider = loadRegistry().require(args[0]);
            provider.serviceLauncher().orElseThrow(() ->
                    new IllegalArgumentException("game has no standalone service: " + provider.descriptor().code()))
                    .launch(serviceArgs);
        }
        // The HTTP services use virtual-thread executors. Those workers do not keep
        // the JVM alive, so retain the bootstrap process after mounting its routes.
        new java.util.concurrent.CountDownLatch(1).await();
    }

    static HttpServer startRoomSafety() throws Exception {
        String url=required("room.safety.database.url","ROOM_SAFETY_DATABASE_URL","room-safety");
        String user=required("room.safety.database.user","ROOM_SAFETY_DATABASE_USER","room-safety");
        String password=value("room.safety.database.password","ROOM_SAFETY_DATABASE_PASSWORD","");
        String secret=required("room.safety.auth.secret","ROOM_SAFETY_AUTH_SECRET","room-safety");
        int port=Integer.parseInt(value("room.safety.http.port","ROOM_SAFETY_HTTP_PORT","8104"));
        var source=new DriverManagerDataSource(url,user,password);
        try(var c=source.getConnection();var q=c.prepareStatement("SELECT 1 FROM aoo_hall_room_member LIMIT 1")){q.executeQuery();}
        Clock clock=Clock.systemUTC();ObjectMapper json=new ObjectMapper().findAndRegisterModules();
        HttpServer server=HttpServer.create(new InetSocketAddress(port),128);
        new RoomSafetyHttpRoutes(new JdbcRoomSafetyService(source,clock),new HallRequestAuthenticator(secret,clock)::authenticate,json).mount(server);
        server.setExecutor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());server.start();
        System.out.println("room safety/report routes mounted by Bootstrap on "+port);return server;
    }

    static HttpServer startGifting() throws Exception {
        String url=required("gifting.database.url","GIFTING_DATABASE_URL","gifting"),user=required("gifting.database.user","GIFTING_DATABASE_USER","gifting"),password=value("gifting.database.password","GIFTING_DATABASE_PASSWORD","");
        String auth=required("gifting.auth.hmac","GIFTING_AUTH_HMAC","gifting"),billingToken=required("billing.internal.token","BILLING_INTERNAL_TOKEN","gifting"),inventoryToken=required("inventory.auth.token","INVENTORY_AUTH_TOKEN","gifting");
        String billingUrl=value("billing.base.url","BILLING_BASE_URL","http://127.0.0.1:8095"),inventoryUrl=value("inventory.base.url","INVENTORY_BASE_URL","http://127.0.0.1:8096");int port=Integer.parseInt(value("gifting.http.port","GIFTING_HTTP_PORT","8101"));
        var source=new DriverManagerDataSource(url,user,password);try(var c=source.getConnection();var q=c.prepareStatement("SELECT 1 FROM gift_policy LIMIT 1")){q.executeQuery();}
        Clock clock=Clock.systemUTC();ObjectMapper json=new ObjectMapper().findAndRegisterModules();var repository=new JdbcGiftRepository(source,json,clock);var assets=new HttpAssetAuthority(java.net.URI.create(billingUrl),java.net.URI.create(inventoryUrl),billingToken,inventoryToken,json);var service=new GiftingService(repository,assets,new JdbcRiskAuthority(source,clock));
        HttpServer server=HttpServer.create(new InetSocketAddress(port),128);new GiftingHttpRoutes(service,new PlayerAuthenticator(auth,clock),json).mount(server);server.setExecutor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());server.start();System.out.println("player gifting routes mounted by Bootstrap on "+port);return server;
    }

    static HttpServer startLuckDraw() throws Exception {
        String url=required("luckdraw.database.url","LUCKDRAW_DATABASE_URL","luckdraw");String user=required("luckdraw.database.user","LUCKDRAW_DATABASE_USER","luckdraw");String password=value("luckdraw.database.password","LUCKDRAW_DATABASE_PASSWORD","");String secret=required("luckdraw.auth.secret","LUCKDRAW_AUTH_SECRET","luckdraw");String billingUrl=required("luckdraw.billing.url","LUCKDRAW_BILLING_URL","luckdraw");String billingToken=required("luckdraw.billing.token","LUCKDRAW_BILLING_TOKEN","luckdraw");String inventoryUrl=required("luckdraw.inventory.url","LUCKDRAW_INVENTORY_URL","luckdraw");String inventoryToken=required("luckdraw.inventory.token","LUCKDRAW_INVENTORY_TOKEN","luckdraw");int port=Integer.parseInt(value("luckdraw.http.port","LUCKDRAW_HTTP_PORT","8101"));
        var source=new DriverManagerDataSource(url,user,password);try(var c=source.getConnection();var q=c.prepareStatement("SELECT 1 FROM aoo_luck_draw_campaign LIMIT 1")){q.executeQuery();}Clock clock=Clock.systemUTC();ObjectMapper json=new ObjectMapper().findAndRegisterModules();var rewards=new HttpRewardPort(java.net.URI.create(billingUrl),billingToken,java.net.URI.create(inventoryUrl),inventoryToken,json);var service=new LuckDrawService(source,rewards,json,clock,new java.security.SecureRandom());var authenticator=new HallRequestAuthenticator(secret,clock);HttpServer server=HttpServer.create(new InetSocketAddress(port),128);new LuckDrawHttpRoutes(service,authenticator::authenticate,json).mount(server);server.setExecutor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());server.start();System.out.println("luck-draw query/execute routes mounted by Bootstrap on "+port);return server;
    }

    static HttpServer startSpectator() throws Exception {
        String url=required("spectator.database.url","SPECTATOR_DATABASE_URL","spectator");String user=required("spectator.database.user","SPECTATOR_DATABASE_USER","spectator");String password=value("spectator.database.password","SPECTATOR_DATABASE_PASSWORD","");String secret=required("spectator.auth.secret","SPECTATOR_AUTH_SECRET","spectator");int port=Integer.parseInt(value("spectator.http.port","SPECTATOR_HTTP_PORT","8099"));long delay=Long.parseLong(value("spectator.delay.seconds","SPECTATOR_DELAY_SECONDS","30"));
        var source=new DriverManagerDataSource(url,user,password);try(var c=source.getConnection();var q=c.prepareStatement("SELECT 1 FROM aoo_spectator_admission LIMIT 1")){q.executeQuery();}Clock clock=Clock.systemUTC();ObjectMapper json=new ObjectMapper().findAndRegisterModules();var ports=new JdbcRoomReplayPorts(source,json);var service=new SpectatorService(source,ports,ports,json,clock,java.time.Duration.ofSeconds(delay));HttpServer server=HttpServer.create(new InetSocketAddress(port),128);new SpectatorHttpRoutes(service,json,secret,clock).mount(server);server.setExecutor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());server.start();System.out.println("spectator/game-share routes mounted by Bootstrap on "+port);return server;
    }
    static HttpServer startInventory() throws Exception {
        String url=required("inventory.database.url","INVENTORY_DATABASE_URL","inventory");
        String user=required("inventory.database.user","INVENTORY_DATABASE_USER","inventory");
        String password=value("inventory.database.password","INVENTORY_DATABASE_PASSWORD","");
        String token=required("inventory.auth.token","INVENTORY_AUTH_TOKEN","inventory");
        String billingToken=required("billing.internal.token","BILLING_INTERNAL_TOKEN","inventory");
        String billingUrl=value("billing.base.url","BILLING_BASE_URL","http://127.0.0.1:8095");
        int port=Integer.parseInt(value("inventory.http.port","INVENTORY_HTTP_PORT","8096"));
        var source=new DriverManagerDataSource(url,user,password);try(var c=source.getConnection();var q=c.prepareStatement("SELECT 1 FROM aoo_item_catalog LIMIT 1")){q.executeQuery();}
        ObjectMapper json=new ObjectMapper().findAndRegisterModules();Clock clock=Clock.systemUTC();HttpServer server=HttpServer.create(new InetSocketAddress(port),128);
        var service=new JdbcInventoryStoreService(source,new HttpBillingPort(java.net.URI.create(billingUrl),billingToken,json),json,clock);new InventoryHttpRoutes(service,json,token,clock).mount(server);
        server.setExecutor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());server.start();System.out.println("inventory/item/store/redemption routes mounted by Bootstrap on "+port);return server;
    }

    static HttpServer startTelemetry() throws Exception {
        String url=required("telemetry.database.url","TELEMETRY_DATABASE_URL","telemetry");
        String user=required("telemetry.database.user","TELEMETRY_DATABASE_USER","telemetry");
        String password=value("telemetry.database.password","TELEMETRY_DATABASE_PASSWORD","");
        String secret=required("telemetry.auth.hmac","TELEMETRY_AUTH_HMAC","telemetry");
        String reviewToken=required("telemetry.review.token","TELEMETRY_REVIEW_TOKEN","telemetry");
        int port=Integer.parseInt(value("telemetry.http.port","TELEMETRY_HTTP_PORT","8096"));
        var source=new DriverManagerDataSource(url,user,password);
        try(var c=source.getConnection();var q=c.prepareStatement("SELECT 1 FROM telemetry_signal LIMIT 1")){q.executeQuery();}
        ObjectMapper json=new ObjectMapper().findAndRegisterModules();Clock clock=Clock.systemUTC();
        HttpServer server=HttpServer.create(new InetSocketAddress(port),128);
        new TelemetryHttpRoutes(new JdbcTelemetryRiskService(source,json,clock),new TelemetryAuthenticator(secret,clock),json,reviewToken).mount(server);
        server.setExecutor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());server.start();
        System.out.println("telemetry/risk routes mounted by Bootstrap on "+port);return server;
    }

    static HttpServer startIdentityVerification() throws Exception {
        String url=required("identity.database.url","IDENTITY_DATABASE_URL","identity-verification");
        String user=required("identity.database.user","IDENTITY_DATABASE_USER","identity-verification");
        String password=value("identity.database.password","IDENTITY_DATABASE_PASSWORD","");
        String encryptionKey=required("identity.encryption.key","IDENTITY_ENCRYPTION_KEY","identity-verification");
        String tokenKey=required("identity.token.key","IDENTITY_TOKEN_KEY","identity-verification");
        String smsUrl=required("identity.sms.url","IDENTITY_SMS_URL","identity-verification");
        String smsToken=required("identity.sms.token","IDENTITY_SMS_TOKEN","identity-verification");
        int port=Integer.parseInt(value("identity.http.port","IDENTITY_HTTP_PORT","8100"));
        var source=new DriverManagerDataSource(url,user,password);try(var c=source.getConnection();var q=c.prepareStatement("SELECT 1 FROM aoo_identity_verification LIMIT 1")){q.executeQuery();}
        ObjectMapper json=new ObjectMapper().findAndRegisterModules();Clock clock=Clock.systemUTC();java.security.SecureRandom random=new java.security.SecureRandom();
        JdbcAccountSessionService accounts=new JdbcAccountSessionService(source,clock);
        var service=new JdbcIdentityVerificationService(source,new IdentityCrypto(encryptionKey,tokenKey,random),new HttpPhoneCodeSender(java.net.URI.create(smsUrl),smsToken,json),json,clock,random);
        HttpServer server=HttpServer.create(new InetSocketAddress(port),128);
        new IdentityVerificationHttpRoutes(service,(token,device,channel,version,ip)->{try{return accounts.authorize(token,new JdbcAccountSessionService.Client(device,channel,version,ip)).accountId();}catch(JdbcAccountSessionService.Unauthorized|JdbcAccountSessionService.Forbidden denied){throw new SecurityException(denied.getMessage());}},json).mount(server);
        server.setExecutor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());server.start();System.out.println("identity verification routes mounted by Bootstrap on "+port);return server;
    }

    static HttpServer startMedia() throws Exception {
        String url=required("media.database.url","MEDIA_DATABASE_URL","media");
        String user=required("media.database.user","MEDIA_DATABASE_USER","media");
        String password=value("media.database.password","MEDIA_DATABASE_PASSWORD","");
        String endpoint=required("media.s3.endpoint","MEDIA_S3_ENDPOINT","media");
        String bucket=required("media.s3.bucket","MEDIA_S3_BUCKET","media");
        String region=value("media.s3.region","MEDIA_S3_REGION","us-east-1");
        String access=required("media.s3.access-key","MEDIA_S3_ACCESS_KEY","media");
        String secret=required("media.s3.secret-key","MEDIA_S3_SECRET_KEY","media");
        String cleanup=required("media.cleanup.token","MEDIA_CLEANUP_TOKEN","media");
        String roomToken=required("media.room.token","MEDIA_ROOM_TOKEN","media");
        int port=Integer.parseInt(value("media.http.port","MEDIA_HTTP_PORT","8096"));
        var source=new DriverManagerDataSource(url,user,password);
        try(var connection=source.getConnection();var query=connection.prepareStatement("SELECT id FROM media_upload_ticket LIMIT 1")){query.executeQuery();}
        Clock clock=Clock.systemUTC();var storage=new S3ObjectStorage(java.net.URI.create(endpoint),bucket,region,access,secret);
        var service=new MediaUploadService(new JdbcMediaRepository(source),storage,new MediaPolicy(),clock,java.time.Duration.ofHours(1));
        JdbcAccountSessionService accounts=new JdbcAccountSessionService(source,clock);var sessionAuth=(MediaAuthenticator.SessionAuthorizer)(token,device,channel,version,ip)->{try{return accounts.authorize(token,new JdbcAccountSessionService.Client(device,channel,version,ip)).accountId();}catch(JdbcAccountSessionService.Unauthorized|JdbcAccountSessionService.Forbidden denied){throw new SecurityException(denied.getMessage());}};
        ObjectMapper json=new ObjectMapper().findAndRegisterModules();HttpServer server=HttpServer.create(new InetSocketAddress(port),128);new MediaHttpRoutes(service,new MediaAuthenticator(sessionAuth),json,cleanup,roomToken).mount(server);
        var profileAuth=(PlayerProfileHttpRoutes.Authenticator)sessionAuth::authorize;
        new ProfileMediaHttpRoutes(new ProfileMediaOrchestrator(service,new JdbcPlayerProfileRepository(source,clock)),profileAuth,json).mount(server);
        server.setExecutor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());server.start();
        var expiry=java.util.concurrent.Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().daemon(true).name("media-expiry-cleaner").factory());
        expiry.scheduleWithFixedDelay(()->{try{service.cleanupExpired(500);}catch(RuntimeException failure){System.err.println("media expiry cleanup failed: "+failure.getMessage());}},60,60,java.util.concurrent.TimeUnit.SECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(expiry::shutdownNow,"media-expiry-shutdown"));
        System.out.println("media routes mounted by Bootstrap on "+port);return server;
    }
    static HttpServer startCompetition() throws Exception {
        String url=required("competition.database.url","COMPETITION_DATABASE_URL","competition");String user=required("competition.database.user","COMPETITION_DATABASE_USER","competition");String password=value("competition.database.password","COMPETITION_DATABASE_PASSWORD","");String secret=required("competition.auth.secret","COMPETITION_AUTH_SECRET","competition");String internal=required("competition.internal.token","COMPETITION_INTERNAL_TOKEN","competition");int port=Integer.parseInt(value("competition.http.port","COMPETITION_HTTP_PORT","8096"));Clock clock=Clock.systemUTC();var source=new DriverManagerDataSource(url,user,password);var json=new ObjectMapper().findAndRegisterModules();var hall=new com.aoo.bcg.matchmaking.HttpHallRoomPort(java.net.URI.create(required("competition.hall.url","COMPETITION_HALL_URL","competition")),required("competition.hall.token","COMPETITION_HALL_TOKEN","competition"),json);var billing=new JdbcBillingService(source,new DistributedIdGenerator(Long.parseLong(value("competition.node.id","COMPETITION_NODE_ID","43")),clock),clock);return startCompetition(source,port,secret,internal,hall,(business,player,currency,amount,reason)->billing.credit(business,player,currency,amount,reason),clock);
    }
    static HttpServer startCompetition(DataSource source,int port,String secret,String internal,MatchmakingPorts.HallRoomPort hall,MatchmakingPorts.BillingRewardPort billing,Clock clock)throws Exception{
        try(var connection=source.getConnection();var query=connection.prepareStatement("SELECT 1 FROM aoo_match_queue LIMIT 1")){query.executeQuery();}var json=new ObjectMapper().findAndRegisterModules();var service=new JdbcCompetitionService(source,hall,billing,clock,json);HttpServer server=HttpServer.create(new InetSocketAddress(port),128);new CompetitionHttpRoutes(service,new HallRequestAuthenticator(secret,clock),json,internal).mount(server);server.setExecutor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());server.start();var expiry=java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"competition-expiry");t.setDaemon(true);return t;});expiry.scheduleAtFixedRate(()->{try{service.expire();}catch(RuntimeException error){System.err.println("competition expiry failed: "+error.getMessage());}},1,1,java.util.concurrent.TimeUnit.SECONDS);Runtime.getRuntime().addShutdownHook(new Thread(expiry::shutdownNow,"competition-expiry-shutdown"));System.out.println("competition routes mounted by Bootstrap on "+port);return server;
    }
    static HttpServer startAccount() throws Exception {
        String url=required("account.database.url","ACCOUNT_DATABASE_URL","account");
        String user=required("account.database.user","ACCOUNT_DATABASE_USER","account");
        String password=value("account.database.password","ACCOUNT_DATABASE_PASSWORD","");
        String operatorToken=required("account.operator.token","ACCOUNT_OPERATOR_TOKEN","account");
        String identitySecondFactor=required("account.identity.second.factor.token","ACCOUNT_IDENTITY_SECOND_FACTOR_TOKEN","account");
        int port=Integer.parseInt(value("account.http.port","ACCOUNT_HTTP_PORT","8096"));
        var source=new DriverManagerDataSource(url,user,password);
        try(var connection=source.getConnection();var query=connection.prepareStatement("SELECT 1 FROM aoo_account LIMIT 1")){query.executeQuery();}
        HttpServer server=HttpServer.create(new InetSocketAddress(port),128);
        Clock clock=Clock.systemUTC();ObjectMapper json=new ObjectMapper().findAndRegisterModules();
        var replacementNotifier=new com.aoo.bcg.account.HttpGatewaySessionReplacementNotifier(
                java.net.URI.create(required("account.gateway.internal.url","ACCOUNT_GATEWAY_INTERNAL_URL","account")),
                required("account.gateway.internal.token","ACCOUNT_GATEWAY_INTERNAL_TOKEN","account"),json);
        JdbcAccountSessionService accounts=new JdbcAccountSessionService(source,clock,replacementNotifier);
        boolean exposeRegistrationCode=Boolean.parseBoolean(value("account.registration.expose-code","ACCOUNT_REGISTRATION_EXPOSE_CODE","false"));
        var registrationCodes=new com.aoo.bcg.account.VerificationCodeService(clock,(destination,purpose,code,expires)->{});
        AccountHttpRoutes.mount(server,accounts,json,operatorToken,registrationCodes,exposeRegistrationCode);
        new IdentityHttpRoutes(source,new UnifiedIdentityService(source,clock),new TrustedDevicePinService(source,clock),accounts,json,operatorToken,identitySecondFactor).mount(server);
        new PrivacyHttpRoutes(new JdbcPrivacyService(source,clock,json),
                (token,device,channel,version,ip)->{try{return accounts.authorize(token,new JdbcAccountSessionService.Client(device,channel,version,ip)).accountId();}catch(JdbcAccountSessionService.Unauthorized|JdbcAccountSessionService.Forbidden denied){throw new SecurityException(denied.getMessage());}},
                json,operatorToken).mount(server);
        try(var connection=source.getConnection();var query=connection.prepareStatement("SELECT player_id FROM player_profile LIMIT 1")){query.executeQuery();}
        new PlayerProfileHttpRoutes(new JdbcPlayerProfileRepository(source,clock),
                (token,device,channel,version,ip)->{try{return accounts.authorize(token,new JdbcAccountSessionService.Client(device,channel,version,ip)).accountId();}catch(JdbcAccountSessionService.Unauthorized failure){throw new SecurityException(failure.getMessage());}},
                json).mount(server);
        server.setExecutor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());server.start();
        System.out.println("account routes mounted by Bootstrap on "+port);return server;
    }

    static HttpServer startSupport() throws Exception {
        String url=required("support.database.url","SUPPORT_DATABASE_URL","support");
        String user=required("support.database.user","SUPPORT_DATABASE_USER","support");
        String password=value("support.database.password","SUPPORT_DATABASE_PASSWORD","");
        String auth=required("support.auth.hmac","SUPPORT_AUTH_HMAC","support");
        String handoff=required("support.handoff.token","SUPPORT_HANDOFF_TOKEN","support");
        int port=Integer.parseInt(value("support.http.port","SUPPORT_HTTP_PORT","8097"));
        var source=new DriverManagerDataSource(url,user,password);try(var c=source.getConnection();var q=c.prepareStatement("SELECT 1 FROM support_case LIMIT 1")){q.executeQuery();}
        ObjectMapper json=new ObjectMapper().findAndRegisterModules();Clock clock=Clock.systemUTC();
        HttpServer server=HttpServer.create(new InetSocketAddress(port),128);
        var playerAuth=new PlayerBearerAuthenticator(auth,clock);
        new SupportHttpRoutes(new JdbcSupportRepository(source,json,clock),playerAuth,json,handoff).mount(server);
        new SupportLiveHttpRoutes(new JdbcSupportLiveRepository(source,clock),playerAuth,new SupportAgentAuthenticator(handoff),json).mount(server);
        server.setExecutor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());server.start();System.out.println("support routes mounted by Bootstrap on "+port);return server;
    }

    static HttpServer startActivity() throws Exception {
        String url=required("activity.database.url","ACTIVITY_DATABASE_URL","activity");
        String user=required("activity.database.user","ACTIVITY_DATABASE_USER","activity");
        String password=value("activity.database.password","ACTIVITY_DATABASE_PASSWORD","");
        String secret=required("activity.auth.secret","ACTIVITY_AUTH_SECRET","activity");
        int port=Integer.parseInt(value("activity.http.port","ACTIVITY_HTTP_PORT","8099"));
        long node=Long.parseLong(value("activity.billing.node.id","ACTIVITY_BILLING_NODE_ID","51"));Clock clock=Clock.systemUTC();
        var source=new DriverManagerDataSource(url,user,password);
        try(var connection=source.getConnection();var query=connection.prepareStatement("SELECT 1 FROM activity_version LIMIT 1")){query.executeQuery();}
        ObjectMapper json=new ObjectMapper().findAndRegisterModules();var billing=new JdbcBillingService(source,new DistributedIdGenerator(node,clock),clock);
        var hallAuth=new HallRequestAuthenticator(secret,clock);
        var service=new ActivityService(new JdbcActivityRepository(source,json),(business,player,currency,amount,reason)->billing.credit(business,player,currency,amount,reason),clock);
        HttpServer server=HttpServer.create(new InetSocketAddress(port),128);
        new ActivityHttpRoutes(service,authorization->{try{return hallAuth.authenticate(authorization);}catch(RuntimeException rejected){throw new SecurityException("valid account bearer assertion required");}},json).mount(server);
        server.setExecutor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());server.start();
        System.out.println("activity/mission routes mounted by Bootstrap on "+port);return server;
    }

    static HttpServer startVersion() throws Exception {
        String url=required("version.database.url","VERSION_DATABASE_URL","version");
        String user=required("version.database.user","VERSION_DATABASE_USER","version");
        String password=value("version.database.password","VERSION_DATABASE_PASSWORD","");
        String token=required("version.internal.token","VERSION_INTERNAL_TOKEN","version");
        String clientToken=required("version.client.token","VERSION_CLIENT_TOKEN","version");
        int port=Integer.parseInt(value("version.http.port","VERSION_HTTP_PORT","8095"));
        var source=new DriverManagerDataSource(url,user,password);
        try(var connection=source.getConnection();var query=connection.prepareStatement("SELECT 1 FROM aoo_client_release LIMIT 1")){query.executeQuery();}
        HttpServer server=HttpServer.create(new InetSocketAddress(port),128);
        new VersionHttpRoutes(new JdbcVersionRepository(source),new ObjectMapper().findAndRegisterModules(),clientToken,token,Clock.systemUTC()).mount(server);
        server.setExecutor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());server.start();
        System.out.println("version/notice/update routes mounted by Bootstrap on "+port);return server;
    }

    static HttpServer startHall(String[] args) throws Exception {
        HallApplication.start(args);
        String url=required("hall.database.url","HALL_DATABASE_URL","hall");
        String user=required("hall.database.user","HALL_DATABASE_USER","hall");
        String password=value("hall.database.password","HALL_DATABASE_PASSWORD","");
        String secret=required("hall.auth.secret","HALL_AUTH_SECRET","hall");
        int port=Integer.parseInt(value("hall.http.port","HALL_HTTP_PORT","8093"));
        var source=new DriverManagerDataSource(url,user,password);
        try(var connection=source.getConnection();var query=connection.prepareStatement("SELECT 1 FROM aoo_compiled_index_active LIMIT 1")){query.executeQuery();}
        ObjectMapper json=new ObjectMapper().findAndRegisterModules();HttpServer server=HttpServer.create(new InetSocketAddress(port),128);
        Clock hallClock=Clock.systemUTC();var roomAuthority=new com.aoo.bcg.hall.room.HttpRoomAuthorityPort(java.net.URI.create(required("hall.room.authority.url","HALL_ROOM_AUTHORITY_URL","hall")),required("hall.room.authority.token","HALL_ROOM_AUTHORITY_TOKEN","hall"),json);JdbcHallRepository hallRepository=new JdbcHallRepository(source,json,hallClock,roomAuthority);
        var sagaStore=new com.aoo.bcg.hall.room.JdbcRoomCreateSagaStore(source,json);
        var sagaBilling=new JdbcRoomSagaBillingPort(source,new DistributedIdGenerator(Long.parseLong(value("hall.billing.node.id","HALL_BILLING_NODE_ID","38")),hallClock),hallClock);
        var saga=new com.aoo.bcg.hall.room.RoomCreateSaga(sagaStore,sagaBilling,new com.aoo.bcg.hall.room.RoomCreateSaga.RoomPort(){public void register(com.aoo.bcg.hall.room.RoomCreateSaga.Command c){hallRepository.registerRoom(c);}public Map<String,Object>confirm(com.aoo.bcg.hall.room.RoomCreateSaga.Command c,long v){return hallRepository.confirmRoom(c.accountId(),c.roomId());}public void remove(com.aoo.bcg.hall.room.RoomCreateSaga.Command c){hallRepository.compensateCreate(c.accountId(),c.requestId(),c.roomId());}},roomAuthority);
        new com.aoo.bcg.hall.room.RoomCreateRecoveryService(saga,32).start(java.time.Duration.ofSeconds(5));
        HallHttpRoutes.mount(server,hallRepository,new HallRequestAuthenticator(source,Clock.systemUTC()),json,saga,new core.replay.RecordReplayQueryService(source,json),required("hall.internal.token","HALL_INTERNAL_TOKEN","hall"));
        String inviteKey=required("invite.signing.key","INVITE_SIGNING_KEY","hall");
        String referralUrl=required("invite.referral.url","INVITE_REFERRAL_URL","hall"),referralToken=required("invite.referral.token","INVITE_REFERRAL_TOKEN","hall");
        new InviteLinkHttpRoutes(new JdbcInviteLinkRepository(source,Clock.systemUTC()),new InviteLinkSigner(inviteKey),new InviteAccountAuthenticator(secret,Clock.systemUTC()),new HttpInviteRewardPort(java.net.URI.create(referralUrl),referralToken,json),json).mount(server);
        server.setExecutor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());server.start();System.out.println("hall routes mounted by Bootstrap on "+port);return server;
    }

    static HttpServer startBilling() throws Exception {
        String url=required("billing.database.url","BILLING_DATABASE_URL","billing");
        String user=required("billing.database.user","BILLING_DATABASE_USER","billing");
        String password=value("billing.database.password","BILLING_DATABASE_PASSWORD","");
        String token=required("billing.internal.token","BILLING_INTERNAL_TOKEN","billing");
        String callbackSecret=required("billing.callback.secret","BILLING_CALLBACK_SECRET","billing");
        String accountSecret=required("billing.account.auth.secret","BILLING_ACCOUNT_AUTH_SECRET","billing");
        String providerUrl=required("billing.provider.checkout.url","BILLING_PROVIDER_CHECKOUT_URL","billing");
        String merchantId=required("billing.provider.merchant.id","BILLING_PROVIDER_MERCHANT_ID","billing");
        String providerSecret=required("billing.provider.signing.secret","BILLING_PROVIDER_SIGNING_SECRET","billing");
        int port=Integer.parseInt(value("billing.http.port","BILLING_HTTP_PORT","8095"));
        long node=Long.parseLong(value("billing.node.id","BILLING_NODE_ID","34"));Clock clock=Clock.systemUTC();
        var source=new DriverManagerDataSource(url,user,password);try(var connection=source.getConnection();var query=connection.prepareStatement("SELECT 1 FROM aoo_currency_catalog LIMIT 1")){query.executeQuery();}
        var ids=new DistributedIdGenerator(node,clock);var billing=new JdbcBillingService(source,ids,clock);var orders=new JdbcPaymentOrderRepository(source);
        var processor=new PaymentOrderProcessor(orders,billing,new PaymentCallbackVerifier(callbackSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8)),clock);
        var queries=new JdbcBillingQueries(source);var json=new ObjectMapper().findAndRegisterModules();HttpServer server=HttpServer.create(new InetSocketAddress(port),128);new BillingHttpRoutes(queries,billing,processor,json,token,clock).mount(server);
        new PlayerPaymentHttpRoutes(queries,processor,new HostedPaymentProviderAdapter(java.net.URI.create(providerUrl),merchantId,providerSecret,clock),json,accountSecret,clock).mount(server);
        server.setExecutor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());server.start();System.out.println("billing routes mounted by Bootstrap on "+port);return server;
    }

    static HttpServer startRanking() throws Exception {
        String url=required("ranking.database.url","RANKING_DATABASE_URL","ranking");String user=required("ranking.database.user","RANKING_DATABASE_USER","ranking");String password=value("ranking.database.password","RANKING_DATABASE_PASSWORD","");String secret=required("ranking.auth.secret","RANKING_AUTH_SECRET","ranking");String internal=required("ranking.internal.token","RANKING_INTERNAL_TOKEN","ranking");int port=Integer.parseInt(value("ranking.http.port","RANKING_HTTP_PORT","8096"));Clock clock=Clock.systemUTC();var source=new DriverManagerDataSource(url,user,password);try(var c=source.getConnection();var q=c.prepareStatement("SELECT 1 FROM aoo_rank_season LIMIT 1")){q.executeQuery();}var json=new ObjectMapper().findAndRegisterModules();var billing=new JdbcBillingService(source,new DistributedIdGenerator(Long.parseLong(value("ranking.node.id","RANKING_NODE_ID","45")),clock),clock);var inventory=new JdbcInventoryStoreService(source,new BillingPort(){public void debit(String b,long p,String c,long a,String r){billing.debit(b,p,c,a,r);}public void refund(String b,long p,String c,long a,String r){billing.credit(b,p,c,a,r);}},json,clock);var rewards=new RankingAchievementService.RewardPort(){public void creditCurrency(String b,long p,String c,long a){billing.credit(b,p,c,a,"ACHIEVEMENT_REWARD");}public void grantItem(String b,long p,String i,long a){inventory.grant(b,p,i,a,null,"ACHIEVEMENT_REWARD");}};var service=new RankingAchievementService(source,rewards,clock);HttpServer server=HttpServer.create(new InetSocketAddress(port),128);new RankingAchievementHttpRoutes(service,new HallRequestAuthenticator(secret,clock)::authenticate,json,internal).mount(server);server.setExecutor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());server.start();System.out.println("ranking/achievement routes mounted by Bootstrap on "+port);return server;
    }

    static HttpServer startReferral() throws Exception {
        String url=required("referral.database.url","REFERRAL_DATABASE_URL","referral");String user=required("referral.database.user","REFERRAL_DATABASE_USER","referral");String password=value("referral.database.password","REFERRAL_DATABASE_PASSWORD","");String token=required("referral.internal.token","REFERRAL_INTERNAL_TOKEN","referral");String billingUrl=required("referral.billing.url","REFERRAL_BILLING_URL","referral");String billingToken=required("referral.billing.token","REFERRAL_BILLING_TOKEN","referral");int port=Integer.parseInt(value("referral.http.port","REFERRAL_HTTP_PORT","8099"));var source=new DriverManagerDataSource(url,user,password);try(var c=source.getConnection();var q=c.prepareStatement("SELECT 1 FROM promoter_commission_instruction LIMIT 1")){q.executeQuery();}ObjectMapper json=new ObjectMapper().findAndRegisterModules();var billing=new HttpBillingAuthorityPort(java.net.http.HttpClient.newHttpClient(),java.net.URI.create(billingUrl),billingToken,json);var service=new PromoterService(new JdbcPromoterRepository(source,Clock.systemUTC()),billing);HttpServer server=HttpServer.create(new InetSocketAddress(port),128);new ReferralHttpRoutes(service,json,token).mount(server);server.setExecutor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());server.start();System.out.println("referral/promoter routes mounted by Bootstrap on "+port);return server;
    }

    static HttpServer startSocial() throws Exception {
        String url=required("social.database.url","SOCIAL_DATABASE_URL","social");
        String user=required("social.database.user","SOCIAL_DATABASE_USER","social");
        String password=value("social.database.password","SOCIAL_DATABASE_PASSWORD","");
        int port=Integer.parseInt(value("social.http.port","SOCIAL_HTTP_PORT","8096"));
        var source=new DriverManagerDataSource(url,user,password);Clock clock=Clock.systemUTC();
        try(var connection=source.getConnection();var query=connection.prepareStatement("SELECT 1 FROM social_notification LIMIT 1")){query.executeQuery();}
        HttpServer server=HttpServer.create(new InetSocketAddress(port),128);
        var repository=new JdbcSocialRepository(source,clock);
        var accounts=new JdbcAccountSessionService(source,clock);
        SocialRequestAuthenticator authenticator=(authorization,device,channel,version,ip)->{
            if(authorization==null||!authorization.startsWith("Bearer ")||authorization.length()<=7)throw new SecurityException("bearer access token required");
            try{var principal=accounts.authorize(authorization.substring(7),new JdbcAccountSessionService.Client(device,channel,version,ip));return new SocialPrincipal(principal.accountId(),"USER");}
            catch(JdbcAccountSessionService.Unauthorized|JdbcAccountSessionService.Forbidden rejected){throw new SecurityException(rejected.getMessage());}
        };
        new SocialHttpRoutes(new SocialService(repository),authenticator,new ObjectMapper().findAndRegisterModules()).mount(server);
        server.setExecutor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());server.start();
        System.out.println("social routes mounted by Bootstrap on "+port);return server;
    }

    static HttpServer startClub() throws Exception {
        String url=required("club.database.url","CLUB_DATABASE_URL");
        String user=required("club.database.user","CLUB_DATABASE_USER");
        String password=value("club.database.password","CLUB_DATABASE_PASSWORD","");
        int port=Integer.parseInt(value("club.http.port","CLUB_HTTP_PORT","8094"));
        var source=new DriverManagerDataSource(url,user,password);
        String authorityUrl=required("club.room.authority.url","CLUB_ROOM_AUTHORITY_URL","club");
        String authorityToken=required("club.room.authority.token","CLUB_ROOM_AUTHORITY_TOKEN","club");
        String gatewayToken=required("club.gateway.token","CLUB_GATEWAY_TOKEN","club");
        return startClub(source,port,authorityUrl,authorityToken,gatewayToken);
    }

    static HttpServer startClub(DataSource source,int port) throws Exception {
        try(var connection=source.getConnection();var query=connection.prepareStatement("SELECT 1")){query.executeQuery();}
        ObjectMapper json=new ObjectMapper().findAndRegisterModules();
        HttpServer server=HttpServer.create(new InetSocketAddress(port),128);
        JdbcClubService clubs=new JdbcClubService(source,json,Clock.systemUTC());
        ClubHttpRoutes.mount(server,clubs,json);
        server.setExecutor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());server.start();
        System.out.println("club routes mounted by Bootstrap on "+port);return server;
    }
    private static HttpServer startClub(DataSource source,int port,String authorityUrl,String authorityToken,String gatewayToken)throws Exception{
        try(var connection=source.getConnection();var query=connection.prepareStatement("SELECT 1")){query.executeQuery();}ObjectMapper json=new ObjectMapper().findAndRegisterModules();JdbcClubService clubs=new JdbcClubService(source,json,Clock.systemUTC());HttpServer server=HttpServer.create(new InetSocketAddress(port),128);ClubHttpRoutes.mount(server,clubs,json,new com.aoo.bcg.club.ClubRoomKickCoordinator(new com.aoo.bcg.club.HttpRoomAuthorityPort(java.net.URI.create(authorityUrl),authorityToken,json),clubs),gatewayToken);server.setExecutor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());server.start();System.out.println("club routes mounted by Bootstrap on "+port);return server;
    }
    private static String required(String property,String environment){return required(property,environment,"club");}
    private static String required(String property,String environment,String service){String value=value(property,environment,null);if(value==null||value.isBlank())throw new IllegalStateException(environment+" is required for "+service+" bootstrap");return value;}
    private static String value(String property,String environment,String fallback){String propertyValue=System.getProperty(property);return propertyValue!=null?propertyValue:System.getenv().getOrDefault(environment,fallback);}

    static GameRegistry loadRegistry() {
        GameRegistry registry = new GameRegistry();
        ServiceReadinessGate readiness = new ServiceReadinessGate(List.of(
                ServiceReadinessGate.check("gameProviderDiscovery",
                        () -> ServiceLoader.load(GameProvider.class).forEach(provider -> {
                            if (!com.aoo.bcg.mahjong.MahjongCatalogRuntimeRegistry.isInternalRuntimeProvider(provider))
                                registry.register(provider);
                        })),
                ServiceReadinessGate.check("gameCatalogIndex", () -> {
                    GameCatalogLoader.registerMissing(registry);
                    if (registry.descriptors().isEmpty()) throw new IllegalStateException("no GameProvider found");
                })));
        readiness.verifyAndOpen();
        readiness.requireAcceptingTraffic();
        return registry;
    }
}
