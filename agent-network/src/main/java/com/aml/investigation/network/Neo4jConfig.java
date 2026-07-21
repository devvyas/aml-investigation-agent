package com.aml.investigation.network;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Kept separate from NetworkAgentConfig deliberately — database connectivity and AiService
 * wiring are different concerns that change for different reasons.
 */
@Configuration
public class Neo4jConfig {

    @Bean(destroyMethod = "close")
    public Driver neo4jDriver(
            @Value("${aml.network.neo4j.uri}") String uri,
            @Value("${aml.network.neo4j.username}") String username,
            @Value("${aml.network.neo4j.password}") String password) {
        return GraphDatabase.driver(uri, AuthTokens.basic(username, password));
    }
}
