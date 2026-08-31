package core.timing;

import business.sdk.WeChatManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class TimingProcessor {

    @Autowired
    private WeChatManager weChatManager;

    /**
     * 每30分钟执行
     */
    @Scheduled(cron ="0 0/30 * * * ?")
    public void every30Minu() {
        this.weChatManager.onHalfHour();
    }
}
