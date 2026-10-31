package com.melvin.learning;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Main {

    public static void main(final String[] args) {
        try (final var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            final var service = context.getBean(SessionService.class);

            System.out.println("3. " + service.describeSession("session-1"));
        }
    }
}
