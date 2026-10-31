package com.melvin.learning;

public class SessionService {

    private final SessionRepository repository;

    public SessionService(final SessionRepository repository) {
        this.repository = repository;
        System.out.println("1. SessionService constructed");
    }

    public void init() {
        System.out.println("2. SessionService initialized");
    }

    public void destroy() {
        System.out.println("4. SessionService destroyed");
    }

    public String describeSession(final String sessionId) {
        return "Session " + sessionId
                + " belongs to " + repository.findPlayerName(sessionId);
    }
}
