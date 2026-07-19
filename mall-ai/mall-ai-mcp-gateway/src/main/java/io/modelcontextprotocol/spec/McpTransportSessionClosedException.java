package io.modelcontextprotocol.spec;

import reactor.util.annotation.Nullable;

public class McpTransportSessionClosedException extends RuntimeException {

    public McpTransportSessionClosedException(@Nullable String sessionId) {
        super(sessionId != null ? "MCP session with ID %s has been closed".formatted(sessionId)
            : "MCP session has been closed");
    }

}