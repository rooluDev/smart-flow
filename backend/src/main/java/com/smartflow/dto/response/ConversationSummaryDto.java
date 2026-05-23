package com.smartflow.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ConversationSummaryDto {

    private Long id;
    private String title;
    private LocalDateTime updatedAt;
    private String lastMessage;
    private String lastRole;
}
