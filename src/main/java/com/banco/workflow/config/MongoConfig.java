package com.banco.workflow.config;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.util.StringUtils;

@Configuration
public class MongoConfig {

    @Bean
    MongoDatabaseFactory mongoDatabaseFactory(MongoClient mongoClient, Environment environment) {
        String configuredDatabase = environment.getProperty("spring.data.mongodb.database");
        if (StringUtils.hasText(configuredDatabase)) {
            return new SimpleMongoClientDatabaseFactory(mongoClient, configuredDatabase);
        }

        String connectionUri = environment.getProperty("spring.data.mongodb.uri");
        if (StringUtils.hasText(connectionUri)) {
            String databaseFromUri = new ConnectionString(connectionUri).getDatabase();
            if (StringUtils.hasText(databaseFromUri)) {
                return new SimpleMongoClientDatabaseFactory(mongoClient, databaseFromUri);
            }
        }

        throw new IllegalStateException(
                "No MongoDB database name was configured. Define spring.data.mongodb.database or include it in MONGODB_URI."
        );
    }
}
