package com.smartflow.mapper;

import com.smartflow.dto.response.ConversationSummaryDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DashboardMapper {

    List<ConversationSummaryDto> findConversationSummaries(
            @Param("userId") Long userId,
            @Param("limit")  int limit
    );
}
