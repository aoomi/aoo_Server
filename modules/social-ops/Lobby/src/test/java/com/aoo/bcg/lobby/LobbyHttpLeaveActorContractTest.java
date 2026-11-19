package com.aoo.bcg.lobby;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.*;
import core.replay.RecordReplayQueryService;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.net.*;
import java.time.Clock;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

final class LobbyHttpLeaveActorContractTest {
    private static final ObjectMapper JSON=new ObjectMapper();
    @Test void handlerPassesOnlyVerifiedPrincipalAndNonAuthorizingIdentity() throws Exception {
        var db=LobbyRequestAuthenticatorTest.sessionDatabase();var repository=new CapturingRepository();
        Exchange exchange=call(repository,new LobbyRequestAuthenticator(db,Clock.systemUTC()),
                JdbcLobbyRepositoryLeaveIdentityContractTest.publicIdentity(),"device-42");
        assertEquals(200,exchange.status);assertEquals(1,repository.calls);
        assertEquals(new LobbyRequestAuthenticator.Principal(42,"account-session-42"),repository.actor);
        assertEquals(18,repository.body.size());assertFalse(repository.body.containsKey("permission"));
    }
    @Test void publicBodyCannotForgeAnyTrustedFieldOrUnknownField() throws Exception {
        var auth=new LobbyRequestAuthenticator(LobbyRequestAuthenticatorTest.sessionDatabase(),Clock.systemUTC());
        for(String field:List.of("accountId","sessionId","actorSessionId","accountSessionId","actorType","permission","trustedActor","_actor","extra")){
            var body=JdbcLobbyRepositoryLeaveIdentityContractTest.publicIdentity();body.put(field,"forged");
            var repository=new CapturingRepository();Exchange exchange=call(repository,auth,body,"device-42");
            assertEquals(400,exchange.status,field);assertEquals(0,repository.calls,field);
        }
    }
    @Test void authenticationCannotBeSkippedByAnExistingLocalReceipt() throws Exception {
        for(String update:List.of("UPDATE aoo_account_session SET revoked_at=CURRENT_TIMESTAMP",
                "UPDATE aoo_account_session SET access_expires_at=DATEADD('DAY',-1,CURRENT_TIMESTAMP)",
                "UPDATE aoo_account_session SET auth_generation=2",
                "UPDATE aoo_account SET banned_until=DATEADD('DAY',1,CURRENT_TIMESTAMP)")){
            var db=LobbyRequestAuthenticatorTest.sessionDatabase();try(var c=db.getConnection();var s=c.createStatement()){s.execute(update);}
            var repository=new CapturingRepository();
            assertEquals(401,call(repository,new LobbyRequestAuthenticator(db,Clock.systemUTC()),JdbcLobbyRepositoryLeaveIdentityContractTest.publicIdentity(),"device-42").status);
            assertEquals(0,repository.calls);
        }
        for(String device:Arrays.asList(null,"other-device")){
            var repository=new CapturingRepository();var auth=new LobbyRequestAuthenticator(LobbyRequestAuthenticatorTest.sessionDatabase(),Clock.systemUTC());
            assertEquals(401,call(repository,auth,JdbcLobbyRepositoryLeaveIdentityContractTest.publicIdentity(),device).status);
            assertEquals(0,repository.calls);
        }
    }
    private static Exchange call(CapturingRepository repository,LobbyRequestAuthenticator auth,Map<String,Object> body,String device)throws Exception{
        Exchange exchange=new Exchange(JSON.writeValueAsBytes(body),device);
        var method=LobbyHttpRoutes.class.getDeclaredMethod("handle",JdbcLobbyRepository.class,LobbyRequestAuthenticator.class,
                ObjectMapper.class,LobbyRoomCreateSaga.class,RecordReplayQueryService.class,HttpExchange.class);
        method.setAccessible(true);method.invoke(null,repository,auth,JSON,null,null,exchange);return exchange;
    }
    private static final class CapturingRepository extends JdbcLobbyRepository {
        int calls;LobbyRequestAuthenticator.Principal actor;Map<String,Object> body;
        CapturingRepository() throws Exception {super(LobbyRequestAuthenticatorTest.sessionDatabase(),JSON,Clock.systemUTC(),
                new LobbyMembershipAuthorityPort(){
                    public Map<String,Object> join(long r,long a,int seat,String v,String key,String trace,Map<String,Object> body){throw new AssertionError("unexpected join");}
                    public Map<String,Object> leave(long r,long a,String v,String key,String trace,Map<String,Object> body){throw new AssertionError("unexpected leave");}
                });}
        @Override public Map<String,Object> leaveAuthenticated(LobbyRequestAuthenticator.Principal actor,String key,long room,String trace,Map<String,Object> body){
            calls++;this.actor=actor;this.body=body;
            // Simulates an already-persisted success: authentication must precede this return.
            return Map.of("roomId",room,"accountId",actor.accountId(),"status","LEFT");
        }
    }
    private static final class Exchange extends HttpExchange {
        final Headers request=new Headers(),response=new Headers();final InputStream body;final ByteArrayOutputStream output=new ByteArrayOutputStream();int status;
        Exchange(byte[] body,String device){this.body=new ByteArrayInputStream(body);request.set("Authorization","Bearer unit-access");request.set("X-Idempotency-Key","leave-test");if(device!=null)request.set("X-Device-Id",device);}
        @Override public Headers getRequestHeaders(){return request;}@Override public Headers getResponseHeaders(){return response;}
        @Override public URI getRequestURI(){return URI.create("/api/v2/hall/rooms/901001/leave");}@Override public String getRequestMethod(){return "POST";}
        @Override public HttpContext getHttpContext(){return null;}@Override public void close(){}@Override public InputStream getRequestBody(){return body;}
        @Override public OutputStream getResponseBody(){return output;}@Override public void sendResponseHeaders(int code,long length){status=code;}
        @Override public InetSocketAddress getRemoteAddress(){return new InetSocketAddress("127.0.0.1",0);}@Override public InetSocketAddress getLocalAddress(){return getRemoteAddress();}
        @Override public int getResponseCode(){return status;}@Override public String getProtocol(){return "HTTP/1.1";}@Override public Object getAttribute(String name){return null;}
        @Override public void setAttribute(String name,Object value){}@Override public void setStreams(InputStream input,OutputStream output){}@Override public HttpPrincipal getPrincipal(){return null;}
    }
}
