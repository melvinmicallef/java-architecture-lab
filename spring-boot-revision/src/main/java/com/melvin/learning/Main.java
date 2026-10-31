package com.melvin.learning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Main {

    public static void main(final String[] args) {
        try (final var context = SpringApplication.run(Main.class, args)) {
            final var service = context.getBean(SessionService.class);

            System.out.println("3. " + service.describeSession("session-1"));
        }
    }
}
