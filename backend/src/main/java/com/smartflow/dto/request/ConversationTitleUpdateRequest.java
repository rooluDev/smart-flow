package com.smartflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ConversationTitleUpdateRequest {

    @NotBlank
    @Size(min = 1, max = 200)
    private String title;
}
