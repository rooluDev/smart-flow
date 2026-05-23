package com.smartflow.service.mcp;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class McpServerRegistry {

    @Value("${mcp.gmail.url}")
    private String gmailUrl;

    @Value("${mcp.calendar.url}")
    private String calendarUrl;

    @Value("${mcp.drive.url}")
    private String driveUrl;

    public static final String GMAIL_SERVER_NAME    = "gmail";
    public static final String CALENDAR_SERVER_NAME = "calendar";
    public static final String DRIVE_SERVER_NAME    = "drive";
}
