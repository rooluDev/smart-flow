package com.smartflow.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StreamChunkResponse {

    private final String type;
    private final String content;
    private final Long messageId;

    public static StreamChunkResponse chunk(String content) {
        return StreamChunkResponse.builder().type("CHUNK").content(content).build();
    }

    public static StreamChunkResponse mcpCall(String mcpName) {
        return StreamChunkResponse.builder().type("MCP_CALL").content(mcpName).build();
    }

    public static StreamChunkResponse done(Long messageId) {
        return StreamChunkResponse.builder().type("DONE").messageId(messageId).build();
    }

    public static StreamChunkResponse error(String message) {
        return StreamChunkResponse.builder().type("ERROR").content(message).build();
    }
}
