package com.campus.ai.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class AiSessionVO {
    private Long id;
    private String question;
    private String answer;
    private String answerStatus;
    private Integer latencyMs;
    private LocalDateTime createdAt;
    private List<AiCitationVO> citations = new ArrayList<>();
}
