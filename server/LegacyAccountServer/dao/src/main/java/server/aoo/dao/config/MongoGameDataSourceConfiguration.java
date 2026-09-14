package server.aoo.dao.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

import java.util.concurrent.TimeUnit;

@Configuration
public class MongoGameDataSourceConfiguration {
    @Autowired
    private MongoSettingsProperties properties;

    @Bean(name = "mongoGameProperties")
    @ConfigurationProperties(prefix = "spring.datasource.druid.mongodb-game")
    public MongoDbGameProperties mongoProperties() {
        return new MongoDbGameProperties();
    }

    @Bean(name = "mongoGameClient", destroyMethod = "close")
    public MongoClient mongoClient(@Qualifier("mongoGameProperties") MongoDbGameProperties config) {
        String uri = "mongodb://" + String.join(",", config.getAddress()) + "/" + config.getDatabase();
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(uri))
                .applyToConnectionPoolSettings(pool -> pool
                        .maxSize(properties.getMaxConnectionsPerHost())
                        .minSize(properties.getMinConnectionsPerHost())
                        .maxWaitTime(properties.getMaxWaitTime(), TimeUnit.MILLISECONDS)
                        .maxConnectionIdleTime(properties.getMaxConnectionIdleTime(), TimeUnit.MILLISECONDS)
                        .maxConnectionLifeTime(properties.getMaxConnectionLifeTime(), TimeUnit.MILLISECONDS))
                .applyToSocketSettings(socket -> socket
                        .connectTimeout(properties.getConnectTimeout(), TimeUnit.MILLISECONDS)
                        .readTimeout(properties.getSocketTimeout(), TimeUnit.MILLISECONDS))
                .applyToClusterSettings(cluster -> cluster
                        .serverSelectionTimeout(properties.getServerSelectionTimeout(), TimeUnit.MILLISECONDS)
                        .localThreshold(properties.getLocalThreshold(), TimeUnit.MILLISECONDS))
                .build();
        return MongoClients.create(settings);
    }

    @Bean(name = "mongoGameFactory")
    public MongoDatabaseFactory mongoDbFactory(
            @Qualifier("mongoGameClient") MongoClient client,
            @Qualifier("mongoGameProperties") MongoDbGameProperties config) {
        return new SimpleMongoClientDatabaseFactory(client, config.getDatabase());
    }

    @Bean(name = "mongoGameTemplate")
    public MongoTemplate mongoTemplate(@Qualifier("mongoGameFactory") MongoDatabaseFactory factory) {
        return new MongoTemplate(factory);
    }
}
