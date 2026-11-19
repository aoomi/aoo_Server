package com.aoo.bcg.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.aoo.bcg.common.persistence.DriverManagerDataSource;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.RejectedExecutionHandlers;

import javax.sql.DataSource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.*;

/** Production HTTP/WebSocket ingress. TLS is terminated by the trusted edge proxy. */
public final class GatewayApplication {
    static final String TICKET_PATH="/api/v2/gateway/ws_ticket", WS_PATH="/api/v2/gateway/ws";
    static final String CORS_ALLOW_HEADERS="Authorization,Content-Type,Idempotency-Key,X-Aoo-Api-Version,X-Player-Id,X-Device-Id,X-Page-Instance-Id,X-Client-Channel,X-Client-Version,X-Idempotency-Key,X-Trace-Id";
    static final List<String> PROXY_IDENTITY_HEADERS=List.of("Authorization","Content-Type","Idempotency-Key","X-Player-Id","X-Device-Id","X-Page-Instance-Id","X-Client-Channel","X-Client-Version","X-Idempotency-Key","X-Trace-Id");
    static final AttributeKey<ConnectionIdentity> IDENTITY=AttributeKey.valueOf("gateway.identity");
    static final AttributeKey<String> CORS_ORIGIN=AttributeKey.valueOf("gateway.cors-origin");
    private GatewayApplication(){}
    /** Package-local redacted diagnostics; authorization remains exclusively in LocalPreviewVersionAccess. */
    static Map<String,Object> diagnoseLocalPreview(String method, String path, String origin,
                                                    java.net.InetSocketAddress peer, boolean forwardedHeaders,
                                                    String authorization, Object deviceId) {
        return LocalPreviewVersionAccess.diagnose(method, path, origin, peer, forwardedHeaders, authorization, deviceId);
    }
    public static void main(String[] args)throws Exception{
        String url=required("gateway.database.url","GATEWAY_DATABASE_URL"),user=required("gateway.database.user","GATEWAY_DATABASE_USER"),password=value("gateway.database.password","GATEWAY_DATABASE_PASSWORD","");
        int port=Integer.parseInt(value("gateway.http.port","GATEWAY_HTTP_PORT","8080"));
        Set<String> origins=Set.of(required("gateway.official.origins","GATEWAY_OFFICIAL_ORIGINS").split(","));
        boolean allowInsecureLanPreview=Boolean.parseBoolean(value("gateway.allow.insecure.lan.preview","GATEWAY_ALLOW_INSECURE_LAN_PREVIEW","false"));
        DataSource source=new DriverManagerDataSource(url,user,password);GatewayDatabaseBootstrap.verify(source);
        Clock clock=Clock.systemUTC();JdbcWsTicketService tickets=new JdbcWsTicketService(source,clock);ObjectMapper json=new ObjectMapper().findAndRegisterModules();
        var providers=ServiceLoader.load(GatewayRuntimeProvider.class).stream().toList();if(providers.size()!=1)throw new IllegalStateException("exactly one GatewayRuntimeProvider is required, found "+providers.size());GatewayRuntimeProvider.Runtime production=providers.getFirst().get().create(source,json,clock);java.util.function.Supplier<GatewayWebSocketFrameHandler.Factory> webSockets=()->production.factory(json,clock);String internalToken=required("gateway.internal.token","GATEWAY_INTERNAL_TOKEN");int internalPort=Integer.parseInt(value("gateway.internal.port","GATEWAY_INTERNAL_PORT","18080"));var internalBind=internalBindAddress(System.getProperty("gateway.internal.bind-address"),System.getenv("GATEWAY_INTERNAL_BIND_ADDRESS"));var internalAuthority=startInternalAuthority(internalPort,new DeterministicAuthorityErrorBoundary(production.authority()),production.authority(),internalToken,json,internalBind);
        // The local JDK HttpServer closes idle HTTP/1.1 sockets sooner than the
        // HttpClient default pool lifetime. Reusing such a half-closed socket makes
        // the first POST after an idle period fail before any response headers arrive.
        // Keep the client-side pool lifetime below the upstream idle timeout; do not
        // retry login or other mutating requests because the upstream may have applied them.
        int upstreamKeepaliveSeconds=Integer.parseInt(value("gateway.upstream.keepalive.seconds","GATEWAY_UPSTREAM_KEEPALIVE_SECONDS","4"));
        if(upstreamKeepaliveSeconds<1||upstreamKeepaliveSeconds>30)throw new IllegalArgumentException("gateway upstream keepalive seconds must be between 1 and 30");
        System.setProperty("jdk.httpclient.keepalive.timeout",Integer.toString(upstreamKeepaliveSeconds));
        HttpClient http=HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(java.time.Duration.ofSeconds(3)).build();
        LocalPreviewVersionAccess preview=LocalPreviewVersionAccess.configured(value("gateway.preview.profile","GATEWAY_PREVIEW_PROFILE",""),value("gateway.preview.descriptor.path","GATEWAY_PREVIEW_DESCRIPTOR_PATH",""),value("gateway.preview.version.bearer.path","GATEWAY_PREVIEW_VERSION_BEARER_PATH",""),value("gateway.preview.peers","GATEWAY_PREVIEW_PEERS",""),clock,json);
        URI version=URI.create(value("gateway.version.url","GATEWAY_VERSION_URL","http://127.0.0.1:8095"));
        URI account=URI.create(value("gateway.account.url","GATEWAY_ACCOUNT_URL","http://127.0.0.1:8096"));
        URI hall=URI.create(value("gateway.hall.url","GATEWAY_HALL_URL","http://127.0.0.1:8093"));
        URI social=URI.create(value("gateway.social.url","GATEWAY_SOCIAL_URL","http://127.0.0.1:8097"));
        EventLoopGroup boss=new NioEventLoopGroup(1),workers=new NioEventLoopGroup();
        DefaultEventExecutorGroup authorityWorkers=new DefaultEventExecutorGroup(8,
                Thread.ofPlatform().name("gateway-authority-",0).factory(),4096,
                RejectedExecutionHandlers.reject());
        Runtime.getRuntime().addShutdownHook(new Thread(()->{internalAuthority.stop(0);production.authority().close();authorityWorkers.shutdownGracefully();boss.shutdownGracefully();workers.shutdownGracefully();},"gateway-shutdown"));
        new ServerBootstrap().group(boss,workers).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>(){protected void initChannel(SocketChannel ch){ch.pipeline().addLast(new HttpServerCodec(),new HttpObjectAggregator(16*1024),new Ingress(tickets,json,origins,authorityWorkers,webSockets,http,version,account,hall,social,allowInsecureLanPreview,production.authority(),preview));}}).bind(port).sync().channel().closeFuture().sync();
    }

    static final class Ingress extends SimpleChannelInboundHandler<FullHttpRequest>{
        private final JdbcWsTicketService tickets;private final ObjectMapper json;private final Set<String> origins;private final Executor authorityWorkers;private final java.util.function.Supplier<GatewayWebSocketFrameHandler.Factory> webSockets;private final HttpClient http;private final URI version;private final URI account;private final URI hall;private final URI social;private final URI gifting=URI.create(value("gateway.gifting.url","GATEWAY_GIFTING_URL","http://127.0.0.1:8101"));private final URI media=URI.create(value("gateway.media.url","GATEWAY_MEDIA_URL","http://127.0.0.1:8102"));private final boolean allowInsecureLanPreview;private final GatewayRuntimeProvider.RoomAuthority readinessAuthority;private final LocalPreviewVersionAccess preview;
        Ingress(JdbcWsTicketService tickets,ObjectMapper json,Set<String> origins,Executor authorityWorkers,GatewayWebSocketFrameHandler.Factory webSockets){this(tickets,json,origins,authorityWorkers,()->webSockets,HttpClient.newHttpClient(),URI.create("http://127.0.0.1:8095"),URI.create("http://127.0.0.1:8096"),URI.create("http://127.0.0.1:8093"),URI.create("http://127.0.0.1:8097"),false,null);}
        Ingress(JdbcWsTicketService tickets,ObjectMapper json,Set<String> origins,Executor authorityWorkers,java.util.function.Supplier<GatewayWebSocketFrameHandler.Factory> webSockets,HttpClient http,URI version,URI account,URI hall,URI social){this(tickets,json,origins,authorityWorkers,webSockets,http,version,account,hall,social,false,null);}
        Ingress(JdbcWsTicketService tickets,ObjectMapper json,Set<String> origins,Executor authorityWorkers,java.util.function.Supplier<GatewayWebSocketFrameHandler.Factory> webSockets,HttpClient http,URI version,URI account,URI hall,URI social,boolean allowInsecureLanPreview){this(tickets,json,origins,authorityWorkers,webSockets,http,version,account,hall,social,allowInsecureLanPreview,null);}
        Ingress(JdbcWsTicketService tickets,ObjectMapper json,Set<String> origins,Executor authorityWorkers,java.util.function.Supplier<GatewayWebSocketFrameHandler.Factory> webSockets,HttpClient http,URI version,URI account,URI hall,URI social,boolean allowInsecureLanPreview,GatewayRuntimeProvider.RoomAuthority readinessAuthority){this(tickets,json,origins,authorityWorkers,webSockets,http,version,account,hall,social,allowInsecureLanPreview,readinessAuthority,LocalPreviewVersionAccess.disabled());}
        Ingress(JdbcWsTicketService tickets,ObjectMapper json,Set<String> origins,Executor authorityWorkers,java.util.function.Supplier<GatewayWebSocketFrameHandler.Factory> webSockets,HttpClient http,URI version,URI account,URI hall,URI social,boolean allowInsecureLanPreview,GatewayRuntimeProvider.RoomAuthority readinessAuthority,LocalPreviewVersionAccess preview){this.preview=Objects.requireNonNull(preview);this.tickets=tickets;this.json=json;this.authorityWorkers=Objects.requireNonNull(authorityWorkers);this.webSockets=Objects.requireNonNull(webSockets);this.http=Objects.requireNonNull(http);this.version=Objects.requireNonNull(version);this.account=Objects.requireNonNull(account);this.hall=Objects.requireNonNull(hall);this.social=Objects.requireNonNull(social);this.allowInsecureLanPreview=allowInsecureLanPreview;this.readinessAuthority=readinessAuthority;this.origins=origins.stream().map(String::strip).filter(s->!s.isEmpty()).collect(java.util.stream.Collectors.toUnmodifiableSet());if(this.origins.isEmpty())throw new IllegalArgumentException("official origins required");}
        protected void channelRead0(ChannelHandlerContext ctx,FullHttpRequest req){req.retain();try{authorityWorkers.execute(()->{try{process(ctx,req);}finally{req.release();}});}catch(RejectedExecutionException saturated){req.release();send(ctx,HttpResponseStatus.SERVICE_UNAVAILABLE,HttpResult.failure(GatewayErrorCode.BACKPRESSURE,Map.of(),trace(req),time()));}}
        private void process(ChannelHandlerContext ctx,FullHttpRequest req){String trace=trace(req);try{
            QueryStringDecoder uri=new QueryStringDecoder(req.uri());if(WS_PATH.equals(uri.path()))assertNoWsQuery(uri);
            if(HttpRoutePolicy.isGatewayReadinessRoute(uri.path())){gatewayReadiness(ctx,req,uri);return;}
            if(uri.path().startsWith("/internal/")){send(ctx,HttpResponseStatus.NOT_FOUND,Map.of("code","NOT_FOUND","traceId",trace));return;}
            String requestOrigin=req.headers().get(HttpHeaderNames.ORIGIN);if(requestOrigin!=null&&origins.contains(requestOrigin))ctx.channel().attr(CORS_ORIGIN).set(requestOrigin);
            if(req.method()==HttpMethod.OPTIONS){official(req);preflight(ctx,req);return;}
            if(HttpRoutePolicy.isRetiredEntry(uri.path())){send(ctx,HttpResponseStatus.GONE,HttpResult.failure(GatewayErrorCode.LEGACY_ENTRY_DISABLED,Map.of(),trace,time()));return;}
            if(!HttpRoutePolicy.isProductionRoute(uri.path())){send(ctx,HttpResponseStatus.NOT_FOUND,HttpResult.failure(GatewayErrorCode.INVALID_ENVELOPE,Map.of(),trace,time()));return;}
            if("/api/v2/version/check".equals(uri.path())){official(req);requireTls(req);HttpRoutePolicy.requireVersion(req.headers().get(HttpRoutePolicy.API_VERSION_HEADER));String serverAuthorization=null;if(preview.enabled()){try{serverAuthorization=preview.authorize(req.method().name(),req.uri(),req.headers().get(HttpHeaderNames.ORIGIN),ctx.channel().remoteAddress() instanceof java.net.InetSocketAddress peer?peer:null,req.headers().contains("Forwarded")||req.headers().contains("X-Forwarded-For")||req.headers().contains("X-Forwarded-Proto")||req.headers().contains("X-Real-IP"),req.headers().get(HttpHeaderNames.AUTHORIZATION),body(req).get("deviceId"));}catch(SecurityException rejected){throw new GatewayTicketException(GatewayErrorCode.UNAUTHORIZED);}}proxy(ctx,req,version.resolve("/v1/version/check"),"version",true,serverAuthorization);return;}
            if(uri.path().startsWith("/api/v2/account/")){official(req);requireTls(req);HttpRoutePolicy.requireVersion(req.headers().get(HttpRoutePolicy.API_VERSION_HEADER));proxy(ctx,req,account.resolve(req.uri()),"account",false);return;}
            if(uri.path().startsWith("/api/v2/player-profile/")){official(req);requireTls(req);HttpRoutePolicy.requireVersion(req.headers().get(HttpRoutePolicy.API_VERSION_HEADER));String suffix=req.uri().substring("/api/v2/player-profile".length());proxy(ctx,req,account.resolve("/v1/player-profile"+suffix),"account",false);return;}
            if(uri.path().startsWith("/api/v2/hall/")){official(req);requireTls(req);HttpRoutePolicy.requireVersion(req.headers().get(HttpRoutePolicy.API_VERSION_HEADER));proxy(ctx,req,hall.resolve(req.uri()),"hall",false);return;}
            if(uri.path().startsWith("/api/v2/media/")){official(req);requireTls(req);HttpRoutePolicy.requireVersion(req.headers().get(HttpRoutePolicy.API_VERSION_HEADER));String suffix=req.uri().substring("/api/v2/media".length());proxy(ctx,req,media.resolve("/v1/media"+suffix),"media",true);return;}
            if(Set.of("/api/v2/social","/api/v2/notifications","/api/v2/mail","/api/v2/notices","/api/v2/red-dots").contains(uri.path())){official(req);requireTls(req);HttpRoutePolicy.requireVersion(req.headers().get(HttpRoutePolicy.API_VERSION_HEADER));proxy(ctx,req,social.resolve(req.uri().replaceFirst("^/api/v2","/v1")),"social",false);return;}
            if(uri.path().equals("/api/v2/gifts")||uri.path().startsWith("/api/v2/gifts/")){official(req);requireTls(req);HttpRoutePolicy.requireVersion(req.headers().get(HttpRoutePolicy.API_VERSION_HEADER));proxy(ctx,req,gifting.resolve(req.uri().replaceFirst("^/api/v2","/v1")),"gifting",true);return;}
            if(TICKET_PATH.equals(uri.path())){if(req.method()!=HttpMethod.POST){send(ctx,HttpResponseStatus.METHOD_NOT_ALLOWED,HttpResult.failure(GatewayErrorCode.INVALID_ENVELOPE,Map.of(),trace,time()));return;}requireTls(req);HttpRoutePolicy.requireVersion(req.headers().get(HttpRoutePolicy.API_VERSION_HEADER));String origin=official(req);String device=header(req,"X-Device-Id"),page=header(req,"X-Page-Instance-Id"),idem=header(req,"X-Idempotency-Key"),access=bearer(req);var issued=tickets.issue(access,device,origin,page,idem);send(ctx,HttpResponseStatus.OK,HttpResult.success(Map.of("wsTicket",issued.ticket(),"expiresInSeconds",JdbcWsTicketService.TTL.toSeconds(),"wsUrl",WS_PATH),trace,time()));return;}
            if(WS_PATH.equals(uri.path())){if(req.method()!=HttpMethod.GET)throw new IllegalArgumentException("websocket upgrade requires GET");requireTls(req);assertNoWsQuery(uri);String origin=official(req),ticket=webSocketProtocol(req,"aoo.ticket."),page=webSocketProtocol(req,"aoo.page.");ConnectionIdentity identity=tickets.consume(ticket,origin,page);ctx.channel().attr(IDENTITY).set(identity);String location="wss://"+header(req,HttpHeaderNames.HOST)+WS_PATH;WebSocketServerHandshaker handshaker=new WebSocketServerHandshakerFactory(location,"aoo.v2",true,64*1024).newHandshaker(req);if(handshaker==null){WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());return;}GatewayWebSocketFrameHandler.Factory gameFrames=webSockets.get();GatewayWebSocketFrameHandler business=gameFrames.create(identity);if(authorityWorkers instanceof EventExecutorGroup group)ctx.pipeline().addLast(group,"gateway-v2-business",business);else ctx.pipeline().addLast("gateway-v2-business",business);handshaker.handshake(ctx.channel(),req).addListener(future->{if(future.isSuccess()){GatewaySessionRegistry.global().connected(ctx.channel(),identity,json,Clock.systemUTC());ChannelHandlerContext businessContext=ctx.pipeline().context("gateway-v2-business");businessContext.executor().execute(()->{try{business.activateRoomScope(businessContext);}catch(RuntimeException failure){System.err.printf("gateway room socket bind failed roomId=%s playerId=%d cause=%s%n",String.valueOf(identity.roomId()),identity.userId(),failure.getMessage());ctx.close();}});}else{GatewaySessionRegistry.global().disconnected(ctx.channel());if(ctx.pipeline().get("gateway-v2-business")!=null)ctx.pipeline().remove("gateway-v2-business");ctx.close();}});return;}
            throw new IllegalStateException("allowlisted route has no handler");
        }catch(RiskAdmissionException e){send(ctx,HttpResponseStatus.FORBIDDEN,HttpResult.failure(e.code(),Map.of(),trace,time()));}catch(GatewayTicketException e){String reason=e instanceof JdbcWsTicketService.TicketAuthorizationException rejected?" reason="+rejected.reason():"";System.err.println("gateway request rejected code="+e.code()+reason+" path="+new QueryStringDecoder(req.uri()).path()+" origin="+req.headers().get(HttpHeaderNames.ORIGIN)+" trace="+trace);send(ctx,e.code()==GatewayErrorCode.UNAUTHORIZED?HttpResponseStatus.UNAUTHORIZED:HttpResponseStatus.FORBIDDEN,HttpResult.failure(e.code(),Map.of(),trace,time()));}catch(HttpRoutePolicy.UnsupportedApiVersionException e){send(ctx,HttpResponseStatus.UPGRADE_REQUIRED,HttpResult.failure(GatewayErrorCode.API_VERSION_REQUIRED,Map.of(),trace,time()));}catch(IllegalArgumentException|SecurityException e){System.err.println("gateway request rejected reason="+e.getMessage()+" path="+new QueryStringDecoder(req.uri()).path()+" origin="+req.headers().get(HttpHeaderNames.ORIGIN)+" trace="+trace);send(ctx,HttpResponseStatus.BAD_REQUEST,HttpResult.failure(GatewayErrorCode.INVALID_ENVELOPE,Map.of(),trace,time()));}catch(RuntimeException e){GatewayErrorCode code=GatewayErrorCode.fromFailure(e);System.err.println("gateway request failed code="+code.registryCode()+" type="+e.getClass().getSimpleName()+" reason="+e.getMessage()+" path="+new QueryStringDecoder(req.uri()).path()+" trace="+trace);send(ctx,HttpResponseStatus.INTERNAL_SERVER_ERROR,HttpResult.failure(code,Map.of(),trace,time()));}}
        private Map<String,Object> body(FullHttpRequest req){try{return json.readValue(io.netty.buffer.ByteBufUtil.getBytes(req.content()),new com.fasterxml.jackson.core.type.TypeReference<>(){});}catch(java.io.IOException e){throw new IllegalArgumentException("invalid JSON",e);}}
        private String official(FullHttpRequest req){String origin=header(req,HttpHeaderNames.ORIGIN);if(!origins.contains(origin))throw new GatewayTicketException(GatewayErrorCode.WS_TICKET_REJECTED);return origin;}
        private void requireTls(FullHttpRequest req){String proto=req.headers().get("X-Forwarded-Proto");if("https".equalsIgnoreCase(proto)||"wss".equalsIgnoreCase(proto))return;String host=Optional.ofNullable(req.headers().get(HttpHeaderNames.HOST)).orElse("").split(":",2)[0];if(host.equals("127.0.0.1")||host.equals("localhost")||host.equals("[::1]"))return;String origin=req.headers().get(HttpHeaderNames.ORIGIN);if(allowInsecureLanPreview&&origin!=null&&origins.contains(origin)&&isPrivateIpv4(host)&&URI.create(origin).getHost().equals(host))return;throw new SecurityException("trusted TLS edge required");}
        private static boolean isPrivateIpv4(String host){String[]parts=host.split("\\.");if(parts.length!=4)return false;try{int a=Integer.parseInt(parts[0]),b=Integer.parseInt(parts[1]);return a==10||(a==172&&b>=16&&b<=31)||(a==192&&b==168);}catch(NumberFormatException invalid){return false;}}
        private static void assertNoWsQuery(QueryStringDecoder uri){if(!uri.parameters().isEmpty())throw new IllegalArgumentException("websocket URL query parameters forbidden");}
        private static String webSocketProtocol(FullHttpRequest request,String prefix){List<String>matches=Arrays.stream(header(request,HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL).split(",")).map(String::strip).filter(value->value.startsWith(prefix)&&value.length()>prefix.length()).toList();if(matches.size()!=1)throw new IllegalArgumentException("missing websocket authentication protocol");return matches.getFirst().substring(prefix.length());}
        private static String bearer(FullHttpRequest r){String v=header(r,HttpHeaderNames.AUTHORIZATION);if(!v.startsWith("Bearer ")||v.length()==7)throw new GatewayTicketException(GatewayErrorCode.UNAUTHORIZED);return v.substring(7);}
        private static String header(FullHttpRequest r,CharSequence n){String v=r.headers().get(n);if(v==null||v.isBlank())throw new IllegalArgumentException("missing header "+n);return v;}
        private static String trace(FullHttpRequest r){String v=r.headers().get("X-Trace-Id");return v==null||v.isBlank()?UUID.randomUUID().toString():v;}
        private com.aoo.bcg.gamespi.time.AuthoritativeTimeSource time(){return new com.aoo.bcg.gamespi.time.AuthoritativeTimeSource(Clock.systemUTC());}
        private void preflight(ChannelHandlerContext c,FullHttpRequest req){FullHttpResponse out=new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,HttpResponseStatus.NO_CONTENT);cors(c,out);out.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS,"GET,POST,PUT,OPTIONS").set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS,CORS_ALLOW_HEADERS).set(HttpHeaderNames.ACCESS_CONTROL_MAX_AGE,"600").setInt(HttpHeaderNames.CONTENT_LENGTH,0);c.writeAndFlush(out).addListener(ChannelFutureListener.CLOSE);}
        private void proxy(ChannelHandlerContext c,FullHttpRequest req,URI target,String service,boolean versionRequest){proxy(c,req,target,service,versionRequest,null);}
        private void proxy(ChannelHandlerContext c,FullHttpRequest req,URI target,String service,boolean versionRequest,String serverAuthorization){
            String requestTrace=trace(req);
            try{
                HttpRequest.Builder b=HttpRequest.newBuilder(target).timeout(java.time.Duration.ofSeconds(8)).method(req.method().name(),req.content().isReadable()?HttpRequest.BodyPublishers.ofByteArray(io.netty.buffer.ByteBufUtil.getBytes(req.content())):HttpRequest.BodyPublishers.noBody());
                for(String name:PROXY_IDENTITY_HEADERS){String value=req.headers().get(name);if(value!=null)b.header(name,value);}
                if(serverAuthorization!=null){if(!versionRequest)throw new IllegalStateException("preview only permits version service");b.setHeader("Authorization",serverAuthorization);}
                if(versionRequest)b.header("X-API-Version",HttpRoutePolicy.API_VERSION);else b.header(HttpRoutePolicy.API_VERSION_HEADER,HttpRoutePolicy.API_VERSION);
                // Upstream I/O must not occupy the bounded authority worker pool. A slow
                // Hall or Account request previously held one of its eight threads for up
                // to eight seconds and could starve room-authority work behind HTTP proxying.
                http.sendAsync(b.build(),HttpResponse.BodyHandlers.ofByteArray()).whenComplete((response,failure)->c.executor().execute(()->{
                    if(failure!=null){upstreamFailure(c,service,target,requestTrace,failure);return;}
                    FullHttpResponse out=new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,HttpResponseStatus.valueOf(response.statusCode()),io.netty.buffer.Unpooled.wrappedBuffer(response.body()));
                    out.headers().set(HttpHeaderNames.CONTENT_TYPE,response.headers().firstValue("Content-Type").orElse("application/json; charset=utf-8")).setInt(HttpHeaderNames.CONTENT_LENGTH,response.body().length).set(HttpHeaderNames.CACHE_CONTROL,"no-store");
                    cors(c,out);c.writeAndFlush(out).addListener(ChannelFutureListener.CLOSE);
                }));
            }catch(Exception failure){upstreamFailure(c,service,target,requestTrace,failure);}
        }
        private void upstreamFailure(ChannelHandlerContext c,String service,URI target,String requestTrace,Throwable failure){
            Throwable cause=failure instanceof CompletionException&&failure.getCause()!=null?failure.getCause():failure;
            String display=Map.of("version","版本服务","account","账号服务","hall","大厅服务","social","社交服务").getOrDefault(service,"业务服务");
            System.err.println("gateway upstream unavailable service="+service+" trace="+requestTrace+" target="+target+" cause="+cause.getClass().getSimpleName()+":"+cause.getMessage());
            send(c,HttpResponseStatus.BAD_GATEWAY,Map.of("code","UPSTREAM_UNAVAILABLE","message",display+"暂时不可用，请稍后重试","traceId",requestTrace,"data",Map.of("service",service)));
        }
        private void gatewayReadiness(ChannelHandlerContext c,FullHttpRequest req,QueryStringDecoder uri){
            if(req.method()!=HttpMethod.GET){sendReadiness(c,HttpResponseStatus.METHOD_NOT_ALLOWED,false,"READINESS_METHOD_NOT_ALLOWED");return;}
            if(!uri.parameters().isEmpty()||req.content().isReadable()){sendReadiness(c,HttpResponseStatus.BAD_REQUEST,false,"READINESS_REQUEST_INVALID");return;}
            GatewayRuntimeProvider.Readiness readiness=GatewayRuntimeProvider.Readiness.notReady();
            if(readinessAuthority!=null){try{readiness=Optional.ofNullable(readinessAuthority.readiness()).orElse(GatewayRuntimeProvider.Readiness.notReady());}catch(RuntimeException ignored){readiness=GatewayRuntimeProvider.Readiness.notReady();}}
            sendReadiness(c,readiness.ready()?HttpResponseStatus.OK:HttpResponseStatus.SERVICE_UNAVAILABLE,readiness.ready(),readiness.ready()?"GATEWAY_READY":"GATEWAY_NOT_READY");
        }
        private void sendReadiness(ChannelHandlerContext c,HttpResponseStatus status,boolean ready,String code){
            try{byte[]bytes=json.writeValueAsBytes(Map.of("status",ready?"ready":"not_ready","code",code));FullHttpResponse out=new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,status,io.netty.buffer.Unpooled.wrappedBuffer(bytes));out.headers().set(HttpHeaderNames.CONTENT_TYPE,"application/json; charset=utf-8").setInt(HttpHeaderNames.CONTENT_LENGTH,bytes.length).set(HttpHeaderNames.CACHE_CONTROL,"no-store").set("X-Content-Type-Options","nosniff");c.writeAndFlush(out).addListener(ChannelFutureListener.CLOSE);}catch(Exception failure){c.close();}
        }
        private void cors(ChannelHandlerContext c,FullHttpResponse out){String origin=c.channel().attr(CORS_ORIGIN).get();if(origin!=null)out.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN,origin).set(HttpHeaderNames.VARY,"Origin");}
        private void send(ChannelHandlerContext c,HttpResponseStatus status,Object body){try{byte[] bytes=json.writeValueAsBytes(body);FullHttpResponse out=new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,status,io.netty.buffer.Unpooled.wrappedBuffer(bytes));out.headers().set(HttpHeaderNames.CONTENT_TYPE,"application/json; charset=utf-8").setInt(HttpHeaderNames.CONTENT_LENGTH,bytes.length).set(HttpHeaderNames.CACHE_CONTROL,"no-store");cors(c,out);c.writeAndFlush(out).addListener(ChannelFutureListener.CLOSE);}catch(Exception failure){c.close();}}
    }
    /** Internal listeners default to loopback; deployments may explicitly select a private IPv4 interface. */
    static java.net.InetAddress internalBindAddress(String propertyValue, String environmentValue) throws java.net.UnknownHostException {
        return internalBindAddress(propertyValue != null ? propertyValue : environmentValue);
    }

    static java.net.InetAddress internalBindAddress(String configured) throws java.net.UnknownHostException {
        String literal = configured == null ? "127.0.0.1" : configured;
        String[] parts = literal.split("\\.", -1);
        if (parts.length != 4) throw new IllegalArgumentException("internal bind must be a numeric loopback or private IPv4 address");
        int[] octets = new int[4];
        byte[] bytes = new byte[4];
        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].matches("0|[1-9][0-9]{0,2}"))
                throw new IllegalArgumentException("internal bind must be a numeric loopback or private IPv4 address");
            octets[i] = Integer.parseInt(parts[i]);
            if (octets[i] > 255) throw new IllegalArgumentException("invalid internal bind IPv4 address");
            bytes[i] = (byte) octets[i];
        }
        boolean allowed = octets[0] == 127 || octets[0] == 10
                || (octets[0] == 172 && octets[1] >= 16 && octets[1] <= 31)
                || (octets[0] == 192 && octets[1] == 168);
        if (!allowed) throw new IllegalArgumentException("internal bind must be loopback or private IPv4");
        return java.net.InetAddress.getByAddress(bytes);
    }

        static com.sun.net.httpserver.HttpServer startInternalAuthority(int port,GatewayRuntimeProvider.RoomAuthority authority,String token,ObjectMapper json)throws Exception{return startInternalAuthority(port,authority,authority,token,json);}
        static com.sun.net.httpserver.HttpServer startInternalAuthority(int port,GatewayRuntimeProvider.RoomAuthority authority,GatewayRuntimeProvider.RoomAuthority readinessAuthority,String token,ObjectMapper json)throws Exception{return startInternalAuthority(port,authority,readinessAuthority,token,json,internalBindAddress(null));}
        static com.sun.net.httpserver.HttpServer startInternalAuthority(int port,GatewayRuntimeProvider.RoomAuthority authority,GatewayRuntimeProvider.RoomAuthority readinessAuthority,String token,ObjectMapper json,java.net.InetAddress bindAddress)throws Exception{bindAddress=internalBindAddress(Objects.requireNonNull(bindAddress,"internal bind address").getHostAddress());var server=com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress(bindAddress,port),64);server.createContext("/internal/v1/health/room-authority",exchange->handleInternalReadiness(exchange,readinessAuthority,token,json));server.createContext("/internal/v1/session/replace",exchange->{int status=200;Object data;try{String supplied=Optional.ofNullable(exchange.getRequestHeaders().getFirst("X-Aoo-Internal-Token")).orElse("");if(!"POST".equals(exchange.getRequestMethod())||!java.security.MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),supplied.getBytes(StandardCharsets.UTF_8)))throw new SecurityException("internal session replacement rejected");Map<String,Object>body=json.readValue(exchange.getRequestBody(),new com.fasterxml.jackson.core.type.TypeReference<>(){});long accountId=((Number)body.get("accountId")).longValue();String sessionId=String.valueOf(body.get("sessionId"));long authGeneration=((Number)body.get("authGeneration")).longValue();if(accountId<=0||sessionId.isBlank()||authGeneration<0)throw new IllegalArgumentException("invalid session replacement");data=Map.of("closed",GatewaySessionRegistry.global().replace(accountId,sessionId,authGeneration,json,Clock.systemUTC()));}catch(SecurityException denied){status=403;data=Map.of("code","INTERNAL_SESSION_FORBIDDEN");}catch(Exception failure){status=400;data=Map.of("code","INTERNAL_SESSION_INVALID");}byte[]bytes=json.writeValueAsBytes(data);exchange.sendResponseHeaders(status,bytes.length);try(var output=exchange.getResponseBody()){output.write(bytes);}});server.createContext("/internal/v1/room-authority",exchange->{String trace=Optional.ofNullable(exchange.getRequestHeaders().getFirst("X-Trace-Id")).orElse(UUID.randomUUID().toString());int status=200;Object data;try{String supplied=Optional.ofNullable(exchange.getRequestHeaders().getFirst("X-Aoo-Internal-Token")).orElse("");if(!"POST".equals(exchange.getRequestMethod())||!java.security.MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),supplied.getBytes(StandardCharsets.UTF_8)))throw new SecurityException("internal authority rejected");Map<String,Object>body=json.readValue(exchange.getRequestBody(),new com.fasterxml.jackson.core.type.TypeReference<>(){});String path=exchange.getRequestURI().getPath();if(path.endsWith("/create"))data=authority.create(body);else if(path.endsWith("/recover"))data=authority.recover(body);else if(path.endsWith("/join"))data=authority.join(body);else if(path.endsWith("/leave"))data=authority.leave(body);else if(path.endsWith("/remove")){data=authority.destroy(body);status=destroyHttpStatus(data);}else{status=404;data=Map.of("code","NOT_FOUND");}}catch(RoomAuthorityBusinessError business){status=business.status();data=Map.of("code",business.code(),"message",business.getMessage());}catch(SecurityException denied){status=403;data=Map.of("code","INTERNAL_AUTHORITY_FORBIDDEN");}catch(Exception failure){status=500;Throwable root=failure;while(root.getCause()!=null)root=root.getCause();System.err.println("room authority failed path="+exchange.getRequestURI().getPath()+" trace="+trace+" cause="+root.getClass().getSimpleName()+":"+String.valueOf(root.getMessage()));data=Map.of("code","INTERNAL_AUTHORITY_FAILED","message",failure.getClass().getSimpleName());}byte[]bytes=json.writeValueAsBytes(status==200?Map.of("code","OK","data",data,"traceId",trace):Map.of("code",errorCode(data),"data",data,"traceId",trace));exchange.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");exchange.sendResponseHeaders(status,bytes.length);try(var output=exchange.getResponseBody()){output.write(bytes);}});server.setExecutor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());server.start();return server;}
        private static void handleInternalReadiness(com.sun.net.httpserver.HttpExchange exchange,GatewayRuntimeProvider.RoomAuthority authority,String token,ObjectMapper json){
            int status=503;boolean ready=false;String code="ROOM_AUTHORITY_NOT_READY";
            try{
                String supplied=Optional.ofNullable(exchange.getRequestHeaders().getFirst("X-Aoo-Internal-Token")).orElse("");
                boolean authenticated=java.security.MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),supplied.getBytes(StandardCharsets.UTF_8));
                if(!authenticated){status=403;code="INTERNAL_AUTHORITY_FORBIDDEN";}
                else if(!"GET".equals(exchange.getRequestMethod())){status=405;code="READINESS_METHOD_NOT_ALLOWED";}
                else if(!"/internal/v1/health/room-authority".equals(exchange.getRequestURI().getPath())||Optional.ofNullable(exchange.getRequestURI().getRawQuery()).isPresent()||hasRequestBody(exchange)){status=400;code="READINESS_REQUEST_INVALID";}
                else {GatewayRuntimeProvider.Readiness result=authority==null?GatewayRuntimeProvider.Readiness.notReady():Optional.ofNullable(authority.readiness()).orElse(GatewayRuntimeProvider.Readiness.notReady());ready=result.ready();status=ready?200:503;code=ready?"ROOM_AUTHORITY_READY":"ROOM_AUTHORITY_NOT_READY";}
            }catch(RuntimeException ignored){status=503;code="ROOM_AUTHORITY_NOT_READY";}
            try{byte[]bytes=json.writeValueAsBytes(Map.of("status",ready?"ready":"not_ready","code",code));exchange.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");exchange.getResponseHeaders().set("Cache-Control","no-store");exchange.getResponseHeaders().set("X-Content-Type-Options","nosniff");exchange.sendResponseHeaders(status,bytes.length);try(var output=exchange.getResponseBody()){output.write(bytes);}}catch(Exception ignored){exchange.close();}
        }
        private static boolean hasRequestBody(com.sun.net.httpserver.HttpExchange exchange){
            if(exchange.getRequestHeaders().containsKey("Transfer-encoding"))return true;
            String length=exchange.getRequestHeaders().getFirst("Content-length");
            try{return length!=null&&!length.isBlank()&&Long.parseLong(length)>0;}catch(NumberFormatException ignored){return true;}
        }
        private static int destroyHttpStatus(Object raw){if(!(raw instanceof Map<?,?> result))return 500;Object value=result.get("status");String status=value==null?"":String.valueOf(value);return switch(status){case "DESTROYED","ALREADY_DESTROYED"->200;case "CAS_CONFLICT"->409;case "NOT_FOUND"->404;default->500;};}
        private static String errorCode(Object raw){if(!(raw instanceof Map<?,?> result))return "INTERNAL_AUTHORITY_FAILED";Object value=result.get("code");return value==null?"INTERNAL_AUTHORITY_FAILED":String.valueOf(value);}
    private static String required(String p,String e){String v=value(p,e,null);if(v==null||v.isBlank())throw new IllegalStateException("missing "+p+" / "+e);return v;}
    private static String value(String p,String e,String fallback){String v=System.getProperty(p);if(v==null||v.isBlank())v=System.getenv(e);return v==null||v.isBlank()?fallback:v;}
}
