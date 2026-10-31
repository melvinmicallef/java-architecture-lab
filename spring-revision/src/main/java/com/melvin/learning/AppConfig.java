package com.melvin.learning;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public SessionRepository sessionRepository() {
        return new SessionRepository();
    }

    @Bean
    public SessionRepository backupSessionRepository() {
        return new SessionRepository();
    }

    @Bean(initMethod = "init", destroyMethod = "destroy")
    public SessionService sessionService(
            @Qualifier("sessionRepository") SessionRepository repository) {
        return new SessionService(repository);
    }
}
