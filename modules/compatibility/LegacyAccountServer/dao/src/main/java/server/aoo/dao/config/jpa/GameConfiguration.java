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
        basePackages = {"server.aoo.dao.mapper.game"},
        value = "server.aoo.dao.mapper.game",
        entityManagerFactoryRef = "gameEntityManagerFactory",
        transactionManagerRef = "gameTransactionManager",
        repositoryFactoryBeanClass = CustomJpaRepositoryFactoryBean.class
)
public class GameConfiguration {

    @Autowired
    @Qualifier("gameDataSource")
    private DataSource gameDataSource;
    @Autowired
    private JpaProperties jpaProperties;
    @Autowired
    private HibernateProperties hibernateProperties;

    @Bean(name = "gameEntityManagerFactory")
    @Primary
    public LocalContainerEntityManagerFactoryBean gameEntityManagerFactory(
            EntityManagerFactoryBuilder builder) {
        LocalContainerEntityManagerFactoryBean em = builder
                .dataSource(gameDataSource)
                .packages("server.aoo.dao.entity.game")
                .persistenceUnit("game")
                .properties(getVendorProperties())
                .build();
        return em;
    }

    @Primary
    @Bean(name = "gameTransactionManager")
    public PlatformTransactionManager gameTransactionManager(EntityManagerFactoryBuilder builder) {
        return new JpaTransactionManager(gameEntityManagerFactory(builder).getObject());
    }

    @Primary
    @Bean(name = "gameEntityManager")
    public EntityManager entityManager(EntityManagerFactoryBuilder builder) {
        return gameEntityManagerFactory(builder).getObject().createEntityManager();
    }

    private Map<String, Object> getVendorProperties() {
        return hibernateProperties.determineHibernateProperties(jpaProperties.getProperties(), new HibernateSettings());
    }

}