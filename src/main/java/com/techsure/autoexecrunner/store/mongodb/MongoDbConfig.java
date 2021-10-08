package com.techsure.autoexecrunner.store.mongodb;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Objects;

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
    @Bean
    public MongoClient mongoClient() {
        return MongoClients.create("mongodb://"+ host +":"+port);
    }
    @Bean
    public MongoTemplate mongoTemplate() {
        return new MongoTemplate(mongoClient(), database);
    }
}