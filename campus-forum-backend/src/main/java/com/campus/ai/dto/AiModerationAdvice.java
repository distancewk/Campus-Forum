package com.campus.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiModerationAdvice {
    private String riskLevel;
    private List<String> riskTypes = new ArrayList<>();
    private double confidence;
    private List<String> reasons = new ArrayList<>();
    private String suggestedAction;
    private String modelName;
}
