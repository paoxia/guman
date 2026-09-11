package io.github.paoxia.guman.chat;

public record ChatRequest(String message, String userId, String sessionId) {

    public ChatRequest {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
        userId = defaultIfBlank(userId, "anonymous");
        sessionId = defaultIfBlank(sessionId, "default");
    }

    private static String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
