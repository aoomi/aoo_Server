package com.aoo.bcg.social;

import java.util.Objects;
import static com.aoo.bcg.social.SocialModels.*;

/** Server-side policy boundary for social, invitations, notifications and mail. */
public final class SocialService {
    private final JdbcSocialRepository repository;
    public SocialService(JdbcSocialRepository repository){this.repository=Objects.requireNonNull(repository);}
    public FriendRequest request(long actor,long recipient,String dedupe){different(actor,recipient);required(dedupe,"dedupeKey");FriendRequest r=repository.request(actor,recipient);repository.notify(recipient,"FRIEND_REQUEST",dedupe,"{\"requestId\":"+r.id()+",\"from\":"+actor+"}");return r;}
    public FriendRequest decide(long actor,long requestId,boolean accept,String dedupe){required(dedupe,"dedupeKey");FriendRequest r=repository.decide(actor,requestId,accept);repository.notify(r.requesterId(),accept?"FRIEND_ACCEPTED":"FRIEND_REJECTED",dedupe,"{\"requestId\":"+requestId+",\"by\":"+actor+"}");return r;}
    public void delete(long actor,long other){different(actor,other);repository.deleteFriend(actor,other);}
    public void block(long actor,long other){different(actor,other);repository.block(actor,other);}
    public void unblock(long actor,long other){different(actor,other);repository.unblock(actor,other);}
    public java.util.List<Friend> friends(long actor){return repository.friends(actor);}
    public java.util.List<FriendRequest> pendingRequests(long actor){return repository.pendingRequests(actor);}
    public void presence(long actor,String state,Long roomId,String visibility){if(roomId!=null&&roomId<=0)throw new IllegalArgumentException("roomId must be positive");repository.setPresence(actor,state,roomId,visibility);}
    public Presence presence(long actor,long subject){return repository.presence(actor,subject);}
    public Notification roomInvite(long actor,long recipient,long roomId,String dedupe){different(actor,recipient);if(roomId<=0)throw new IllegalArgumentException("roomId must be positive");required(dedupe,"dedupeKey");if(!repository.areFriends(actor,recipient)||repository.isBlocked(actor,recipient))throw new SecurityException("room invitation allowed only between unblocked friends");return repository.notify(recipient,"ROOM_INVITE",dedupe,"{\"roomId\":"+roomId+",\"from\":"+actor+"}");}
    public Notification systemMessage(SocialPrincipal principal,long recipient,String dedupe,String payload){if(!principal.system())throw new SecurityException("SYSTEM role required");required(dedupe,"dedupeKey");required(payload,"payload");return repository.notify(recipient,"SYSTEM",dedupe,payload);}
    public Mail systemMail(SocialPrincipal principal,long recipient,String dedupe,String subject,String body){if(!principal.system())throw new SecurityException("SYSTEM role required");required(dedupe,"dedupeKey");required(subject,"subject");required(body,"body");return repository.mail(recipient,dedupe,subject,body);}
    public Feed<Notification> notifications(long actor,long cursor,int limit){page(cursor,limit);return repository.notifications(actor,cursor,limit);}
    public Feed<Mail> mails(long actor,long cursor,int limit){page(cursor,limit);return repository.mails(actor,cursor,limit);}
    public Mail mail(long actor,long id){if(id<=0)throw new IllegalArgumentException("mailId must be positive");return repository.mailDetail(actor,id);}
    public Feed<Notice> notices(long actor,long cursor,int limit){page(cursor,limit);return repository.notices(actor,cursor,limit);}
    public RedDots redDots(long actor){return repository.redDots(actor);}
    public long read(long actor,String stream,long cursor){return repository.advanceReadCursor(actor,stream,cursor);}
    private static void different(long a,long b){if(a<=0||b<=0||a==b)throw new IllegalArgumentException("distinct positive accounts required");}
    private static void required(String value,String name){if(value==null||value.isBlank()||value.length()>4096)throw new IllegalArgumentException(name+" required");}
    private static void page(long cursor,int limit){if(cursor<0||limit<1||limit>200)throw new IllegalArgumentException("cursor/limit out of range");}
}
