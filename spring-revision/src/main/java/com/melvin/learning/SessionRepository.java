package com.melvin.learning;

import org.springframework.stereotype.Repository;

@Repository
public class SessionRepository {

    public final String findPlayerName(final String sessionId) {
        return "Melvin";
    }
}
