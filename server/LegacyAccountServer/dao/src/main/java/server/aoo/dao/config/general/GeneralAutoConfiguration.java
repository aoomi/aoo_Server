package server.aoo.dao.config.general;

import com.ddm.server.common.utils.CommonConfigUtils;
import server.aoo.dao.utils.ConfigUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GeneralProperties.class)
public class GeneralAutoConfiguration {

    @Autowired
    private GeneralProperties activityProperties;

    @Bean
    public GeneralProperties setGeneralConfiguration() {
        ConfigUtils.setConfig(this.activityProperties);
        CommonConfigUtils.setCommonConfig(ConfigUtils.getCommonConfigInfo());
        return this.activityProperties;
    }

}
