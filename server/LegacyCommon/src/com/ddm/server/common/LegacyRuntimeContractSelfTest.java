package com.ddm.server.common;

import com.ddm.server.common.redis.RedisUtil;
import com.ddm.server.common.rocketmq.MqProducerMgr;
import com.ddm.server.common.utils.CommTime;
import java.time.Instant;
import java.util.TimeZone;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;

/** Dependency-free executable assertions for the legacy runtime contracts. */
public final class LegacyRuntimeContractSelfTest {
    public static void main(String[] args) {
        Instant before=Instant.parse("2026-08-23T23:30:00Z");
        Instant after=Instant.parse("2026-08-24T00:30:00Z");
        TimeZone original=CommTime.timezone();
        CommTime.setTimezone(TimeZone.getTimeZone("Asia/Shanghai"));
        check(CommTime.accountingDay(before).toString().equals("2026-08-24"),"Shanghai before UTC boundary");
        check(CommTime.accountingDay(after).toString().equals("2026-08-24"),"Shanghai after UTC boundary");
        check(CommTime.sameAccountingDay(before,after),"business-zone UTC boundary");
        CommTime.setTimezone(TimeZone.getTimeZone("UTC"));
        check(!CommTime.sameAccountingDay(before,after),"setTimezone updates new and legacy contract");
        CommTime.setTimezone(original);
        TimeZone leaked=CommTime.timezone();leaked.setID("GMT+13:00");
        check(!CommTime.businessZone().getId().equals("GMT+13:00"),"timezone returns defensive clone");
        Thread writer=new Thread(()->{for(int i=0;i<1000;i++)CommTime.setTimezone(TimeZone.getTimeZone((i&1)==0?"UTC":"Asia/Shanghai"));});
        Thread reader=new Thread(()->{for(int i=0;i<1000;i++){String id=CommTime.businessZone().getId();check(id.equals("UTC")||id.equals("Asia/Shanghai"),"atomic time snapshot");}});
        writer.start();reader.start();try{writer.join();reader.join();}catch(InterruptedException e){throw new AssertionError(e);}CommTime.setTimezone(original);
        check(CommTime.accountingDayYmd(after).matches("\\d{8}"),"stable accounting key");
        check(RedisUtil.AtomicResult.values().length==4,"redis atomic unknown state");
        check(RedisUtil.classifySetNxFailure(false)==RedisUtil.AtomicResult.UNAVAILABLE,"redis pre-dispatch unavailable");
        check(RedisUtil.classifySetNxFailure(true)==RedisUtil.AtomicResult.UNKNOWN_APPLIED,"redis post-dispatch unknown");
        check(!RedisUtil.AvailabilityResult.unavailable().available(),"redis availability explicit");
        SendResult ok=new SendResult();ok.setSendStatus(SendStatus.SEND_OK);
        SendResult rejected=new SendResult();rejected.setSendStatus(SendStatus.FLUSH_DISK_TIMEOUT);
        check(MqProducerMgr.classify(ok)==MqProducerMgr.Confirmation.CONFIRMED,"mq SEND_OK");
        check(MqProducerMgr.classify(rejected)==MqProducerMgr.Confirmation.REJECTED,"mq non-OK");
        check(MqProducerMgr.classify(null)==MqProducerMgr.Confirmation.UNKNOWN,"mq missing response unknown");
        check(MqProducerMgr.classifyFailure(true)==MqProducerMgr.Confirmation.UNKNOWN,"mq post-dispatch exception unknown");
        check(MqProducerMgr.classifyFailure(false)==MqProducerMgr.Confirmation.UNAVAILABLE,"mq pre-dispatch exception retryable/unavailable");
        expectIllegal(()->MqProducerMgr.get().sendConfirmed("","key",null));
        expectIllegal(()->MqProducerMgr.get().sendConfirmed("topic"," ",null));
        System.out.println("LegacyRuntimeContractSelfTest: business-time/redis/mq contracts PASS zone="+CommTime.businessZone());
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static void expectIllegal(Runnable action){try{action.run();throw new AssertionError("expected invalid MQ identity");}catch(IllegalArgumentException expected){}}
}
