package com.techsure.autoexecrunner.store.mongodb;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * @author lvzk
 * @since 2021/10/8 18:10
 **/
@Configuration
@PropertySource("classpath:application.properties")
public class MongoDbConfig {
    @Value("${spring.data.mongodb.host}")
    private String host;
    @Value("${spring.data.mongodb.port}")
    private String port;
    @Value("${spring.data.mongodb.database}")
    private String database;
    @Value("${spring.data.mongodb.username}")
    private String username;
    @Value("${spring.data.mongodb.password}")
    private String password;
    @Bean
    public MongoClient mongoClient() {
        String uri = "mongodb://" + username + ":" +
                password + "@" +
                host + ":" +
                port + "/" +
                database;
        return MongoClients.create(uri);
    }
    @Bean
    public MongoTemplate mongoTemplate() {
        return new MongoTemplate(mongoClient(), database);
    }
}