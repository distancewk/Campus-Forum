package com.campus.ai.dto;

import com.campus.post.dto.PostListVO;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiAskResponse {
    private String answerStatus;
    private String answer;
    private List<AiCitationVO> citations = new ArrayList<>();
    private List<PostListVO> relatedPosts = new ArrayList<>();
}
