package server.aoo.dao.config.jpa;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * 数据源配置
 */
@Configuration
public class DataSourceConfiguration {

    @Bean()
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource.game")
    public DataSource gameDataSource() {
        return createSource();
    }

    @Bean()
    @ConfigurationProperties(prefix = "spring.datasource.log")
    public DataSource logDataSource() {
        return createSource();
    }


    public DruidDataSource createSource(){
        DruidDataSource dataSource = DataSourceBuilder.create().type(DruidDataSource.class).build();
        String validationQuery = dataSource.getValidationQuery();
        if (validationQuery != null) {
            dataSource.setTestOnBorrow(true);
            dataSource.setValidationQuery(validationQuery);
        }
        try {
            //开启Druid的监控统计功能，mergeStat代替stat表示sql合并,wall表示防御SQL注入攻击
            dataSource.setFilters("mergeStat,wall,log4j2");
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", e);
        }
        return dataSource;
    }

}