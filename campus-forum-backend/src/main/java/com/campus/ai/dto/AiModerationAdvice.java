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
    // 可空：模型未返回置信度时保持 null，避免把"缺省 0"误判为"低置信"而错误升级人工复核。
    private Double confidence;
    private List<String> reasons = new ArrayList<>();
    private String suggestedAction;
    private String modelName;

    /** 深拷贝构造，避免去重缓存复用/置信度归一化时污染原始对象。 */
    public AiModerationAdvice(AiModerationAdvice o) {
        this.riskLevel = o.riskLevel;
        this.riskTypes = new ArrayList<>(o.riskTypes == null ? List.of() : o.riskTypes);
        this.confidence = o.confidence;
        this.reasons = new ArrayList<>(o.reasons == null ? List.of() : o.reasons);
        this.suggestedAction = o.suggestedAction;
        this.modelName = o.modelName;
    }

    public AiModerationAdvice copy() {
        return new AiModerationAdvice(this);
    }
}
