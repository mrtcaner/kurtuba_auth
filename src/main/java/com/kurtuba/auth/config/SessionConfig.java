package com.kurtuba.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.config.SessionRepositoryCustomizer;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;

@Configuration
@EnableJdbcHttpSession
public class SessionConfig {

    @Value("${spring.datasource.hikari.schema}")
    String schema;

    @Bean
    public TableNameCustomizer tableNameCustomizer() {
        return new TableNameCustomizer();
    }

    class TableNameCustomizer
            implements SessionRepositoryCustomizer<JdbcIndexedSessionRepository> {

        @Override
        public void customize(JdbcIndexedSessionRepository sessionRepository) {
            sessionRepository.setTableName(schema + ".SPRING_SESSION");
        }

    }
}

