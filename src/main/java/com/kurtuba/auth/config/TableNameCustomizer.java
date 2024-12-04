package com.kurtuba.auth.config;

import org.springframework.session.config.SessionRepositoryCustomizer;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;

public class TableNameCustomizer
        implements SessionRepositoryCustomizer<JdbcIndexedSessionRepository> {

    @Override
    public void customize(JdbcIndexedSessionRepository sessionRepository) {
        sessionRepository.setTableName("kurtuba.SPRING_SESSION");
    }

}
