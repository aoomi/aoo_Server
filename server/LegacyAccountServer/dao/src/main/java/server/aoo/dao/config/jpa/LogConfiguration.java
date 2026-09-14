package server.aoo.dao.config.jpa;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.hibernate.autoconfigure.HibernateProperties;
import org.springframework.boot.hibernate.autoconfigure.HibernateSettings;
import org.springframework.boot.jpa.autoconfigure.JpaProperties;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.persistence.EntityManager;
import javax.sql.DataSource;
import java.util.Map;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = {"server.aoo.dao.mapper.log"},
        value = "server.aoo.dao.mapper.log",
        entityManagerFactoryRef = "logEntityManagerFactory",
        transactionManagerRef = "logTransactionManager",
        repositoryFactoryBeanClass = CustomJpaRepositoryFactoryBean.class
)
public class LogConfiguration {

    @Autowired
    @Qualifier("logDataSource")
    private DataSource logDataSource;
    @Autowired
    private JpaProperties jpaProperties;
    @Autowired
    private HibernateProperties hibernateProperties;

    @Bean(name = "logEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean logEntityManagerFactory(
            EntityManagerFactoryBuilder builder) {
        LocalContainerEntityManagerFactoryBean em = builder
                .dataSource(logDataSource)
                .packages("server.aoo.dao.entity.log")
                .persistenceUnit("log")
                .properties(getVendorProperties())
                .build();
        return em;
    }

    @Bean(name = "logTransactionManager")
    @Primary
    PlatformTransactionManager logTransactionManager(EntityManagerFactoryBuilder builder) {
        return new JpaTransactionManager(logEntityManagerFactory(builder).getObject());
    }

    @Primary
    @Bean(name = "logEntityManager")
    public EntityManager entityManager(EntityManagerFactoryBuilder builder) {
        return logEntityManagerFactory(builder).getObject().createEntityManager();
    }

    private Map<String, Object> getVendorProperties() {
        return hibernateProperties.determineHibernateProperties(jpaProperties.getProperties(), new HibernateSettings());
    }
}