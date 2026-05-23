package com.smartflow.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@JsonDeserialize(builder = DashboardResponse.DashboardResponseBuilder.class)
public class DashboardResponse {

    private final EmailData email;
    private final CalendarData calendar;
    private final DriveData drive;
    private final List<ConversationSummaryDto> conversations;

    @JsonPOJOBuilder(withPrefix = "")
    public static class DashboardResponseBuilder {}

    @Getter
    @Builder
    @JsonDeserialize(builder = EmailData.EmailDataBuilder.class)
    public static class EmailData {
        private final int unreadCount;

        @JsonPOJOBuilder(withPrefix = "")
        public static class EmailDataBuilder {}
    }

    @Getter
    @Builder
    @JsonDeserialize(builder = CalendarData.CalendarDataBuilder.class)
    public static class CalendarData {
        private final List<Event> todayEvents;

        @JsonPOJOBuilder(withPrefix = "")
        public static class CalendarDataBuilder {}

        @Getter
        @Builder
        @JsonDeserialize(builder = Event.EventBuilder.class)
        public static class Event {
            private final String id;
            private final String title;
            private final LocalDateTime startTime;
            private final LocalDateTime endTime;
            private final String location;

            @JsonPOJOBuilder(withPrefix = "")
            public static class EventBuilder {}
        }
    }

    @Getter
    @Builder
    @JsonDeserialize(builder = DriveData.DriveDataBuilder.class)
    public static class DriveData {
        private final List<File> recentFiles;

        @JsonPOJOBuilder(withPrefix = "")
        public static class DriveDataBuilder {}

        @Getter
        @Builder
        @JsonDeserialize(builder = File.FileBuilder.class)
        public static class File {
            private final String id;
            private final String name;
            private final String mimeType;
            private final LocalDateTime modifiedTime;
            private final String webViewLink;

            @JsonPOJOBuilder(withPrefix = "")
            public static class FileBuilder {}
        }
    }
}
