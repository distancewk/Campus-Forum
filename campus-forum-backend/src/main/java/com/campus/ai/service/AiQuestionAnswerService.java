package com.campus.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.ai.client.AiProviderClient;
import com.campus.ai.client.AiProviderException;
import com.campus.ai.config.AiProperties;
import com.campus.ai.dto.AiAskResponse;
import com.campus.ai.dto.AiChatMessage;
import com.campus.ai.dto.AiCitationVO;
import com.campus.ai.dto.AiSessionVO;
import com.campus.ai.dto.RetrievedChunk;
import com.campus.ai.entity.AiQaCitation;
import com.campus.ai.entity.AiQaSession;
import com.campus.ai.mapper.AiQaCitationMapper;
import com.campus.ai.mapper.AiQaSessionMapper;
import com.campus.common.enums.ResultCode;
import com.campus.common.exception.BusinessException;
import com.campus.common.response.PageQuery;
import com.campus.common.response.PageResult;
import com.campus.common.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AiQuestionAnswerService {
    private static final String STATUS_ANSWERED = "ANSWERED";
    private static final String STATUS_INSUFFICIENT_SOURCES = "INSUFFICIENT_SOURCES";
    private static final String STATUS_FAILED = "FAILED";
    private static final int SNIPPET_MAX_LENGTH = 160;

    private final AiProviderClient aiProviderClient;
    private final KnowledgeRetriever knowledgeRetriever;
    private final AiQaSessionMapper sessionMapper;
    private final AiQaCitationMapper citationMapper;
    private final AiProperties properties;
    private final RedisUtil redisUtil;

    public AiAskResponse ask(Long userId, String question) {
        long started = System.currentTimeMillis();
        enforceRateLimit(userId);
        try {
            List<RetrievedChunk> chunks = knowledgeRetriever.retrieve(question).stream()
                    .filter(chunk -> chunk.getScore() >= properties.getQa().getMinScore())
                    .limit(properties.getQa().getMaxSources())
                    .toList();
            if (chunks.isEmpty()) {
                return saveAndReturnInsufficient(userId, question, started);
            }
            String answer = aiProviderClient.createChatCompletion(buildMessages(question, chunks));
            return saveAndReturnAnswered(userId, question, answer, chunks, started);
        } catch (AiProviderException e) {
            return saveAndReturnFailed(userId, question, started);
        }
    }

    public PageResult<AiSessionVO> listSessions(Long userId, PageQuery query) {
        Page<AiQaSession> page = new Page<>(query.getPage(), query.getSize());
        IPage<AiQaSession> result = sessionMapper.selectPage(page, new LambdaQueryWrapper<AiQaSession>()
                .eq(AiQaSession::getUserId, userId)
                .orderByDesc(AiQaSession::getCreatedAt));
        List<AiSessionVO> records = result.getRecords().stream()
                .map(this::toSessionVO)
                .toList();
        return new PageResult<>(records, result.getTotal(), query.getPage(), query.getSize());
    }

    public AiSessionVO getSession(Long userId, Long sessionId) {
        AiQaSession session = sessionMapper.selectOne(new LambdaQueryWrapper<AiQaSession>()
                .eq(AiQaSession::getId, sessionId)
                .eq(AiQaSession::getUserId, userId));
        if (session == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "问答记录不存在");
        }
        AiSessionVO vo = toSessionVO(session);
        List<AiQaCitation> citations = citationMapper.selectList(new LambdaQueryWrapper<AiQaCitation>()
                .eq(AiQaCitation::getSessionId, sessionId)
                .orderByAsc(AiQaCitation::getId));
        vo.setCitations(citations.stream().map(this::toCitationVO).toList());
        return vo;
    }

    private void enforceRateLimit(Long userId) {
        if (redisUtil == null || userId == null) {
            return;
        }
        String key = "ai:qa:hour:" + userId + ":" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHH"));
        Long count = redisUtil.increment(key);
        if (count != null && count == 1L) {
            redisUtil.expire(key, 1, TimeUnit.HOURS);
        }
        if (count != null && count > properties.getQa().getRateLimitPerHour()) {
            throw new BusinessException(429, "AI 问答请求过于频繁，请稍后再试");
        }
    }

    private List<AiChatMessage> buildMessages(String question, List<RetrievedChunk> chunks) {
        StringBuilder sourceBuilder = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            RetrievedChunk chunk = chunks.get(i);
            sourceBuilder.append("来源 ").append(i + 1).append("：")
                    .append(chunk.getTitle()).append('\n')
                    .append(chunk.getContent()).append("\n\n");
        }
        return List.of(
                new AiChatMessage("system", "你是校园论坛智能问答助手。只能基于用户提供的引用资料回答。资料不足时必须说明暂未找到足够可靠资料。不要编造地点、流程、时间、联系人或政策。"),
                new AiChatMessage("user", "问题：" + question + "\n\n可用引用资料：\n" + sourceBuilder)
        );
    }

    private AiAskResponse saveAndReturnInsufficient(Long userId, String question, long started) {
        AiAskResponse response = new AiAskResponse();
        response.setAnswerStatus(STATUS_INSUFFICIENT_SOURCES);
        response.setAnswer("暂未找到足够可靠的校园论坛资料。你可以换个关键词搜索相关帖子，或到对应板块发帖提问。");
        saveSession(userId, question, response.getAnswer(), STATUS_INSUFFICIENT_SOURCES, started);
        return response;
    }

    private AiAskResponse saveAndReturnFailed(Long userId, String question, long started) {
        AiAskResponse response = new AiAskResponse();
        response.setAnswerStatus(STATUS_FAILED);
        response.setAnswer("AI 服务暂时不可用，请稍后再试。");
        saveSession(userId, question, response.getAnswer(), STATUS_FAILED, started);
        return response;
    }

    private AiAskResponse saveAndReturnAnswered(Long userId, String question, String answer,
                                                List<RetrievedChunk> chunks, long started) {
        AiAskResponse response = new AiAskResponse();
        response.setAnswerStatus(STATUS_ANSWERED);
        response.setAnswer(answer);
        response.setCitations(chunks.stream().map(this::toCitationVO).toList());

        AiQaSession session = saveSession(userId, question, answer, STATUS_ANSWERED, started);
        if (session != null && citationMapper != null) {
            for (AiCitationVO citation : response.getCitations()) {
                citationMapper.insert(toCitationEntity(session.getId(), citation));
            }
        }
        return response;
    }

    private AiQaSession saveSession(Long userId, String question, String answer, String status, long started) {
        if (sessionMapper == null) {
            return null;
        }
        AiQaSession session = new AiQaSession();
        session.setUserId(userId);
        session.setQuestion(question);
        session.setAnswer(answer);
        session.setAnswerStatus(status);
        session.setLatencyMs((int) Math.min(System.currentTimeMillis() - started, Integer.MAX_VALUE));
        session.setCreatedAt(LocalDateTime.now());
        sessionMapper.insert(session);
        return session;
    }

    private AiCitationVO toCitationVO(RetrievedChunk chunk) {
        AiCitationVO citation = new AiCitationVO();
        citation.setChunkId(chunk.getChunkId());
        citation.setSourceType(chunk.getSourceType());
        citation.setSourceId(chunk.getSourceId());
        citation.setTitle(chunk.getTitle());
        citation.setSnippet(snippet(chunk.getContent()));
        citation.setScore(chunk.getScore());
        return citation;
    }

    private AiCitationVO toCitationVO(AiQaCitation entity) {
        AiCitationVO citation = new AiCitationVO();
        citation.setChunkId(entity.getChunkId());
        citation.setSourceType(entity.getSourceType());
        citation.setSourceId(entity.getSourceId());
        citation.setTitle(entity.getTitle());
        citation.setSnippet(entity.getSnippet());
        citation.setScore(entity.getScore() == null ? 0 : entity.getScore());
        return citation;
    }

    private AiSessionVO toSessionVO(AiQaSession session) {
        AiSessionVO vo = new AiSessionVO();
        vo.setId(session.getId());
        vo.setQuestion(session.getQuestion());
        vo.setAnswer(session.getAnswer());
        vo.setAnswerStatus(session.getAnswerStatus());
        vo.setLatencyMs(session.getLatencyMs());
        vo.setCreatedAt(session.getCreatedAt());
        return vo;
    }

    private AiQaCitation toCitationEntity(Long sessionId, AiCitationVO citation) {
        AiQaCitation entity = new AiQaCitation();
        entity.setSessionId(sessionId);
        entity.setChunkId(citation.getChunkId());
        entity.setSourceType(citation.getSourceType());
        entity.setSourceId(citation.getSourceId());
        entity.setTitle(citation.getTitle());
        entity.setSnippet(citation.getSnippet());
        entity.setScore(citation.getScore());
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }

    private String snippet(String content) {
        if (content == null) {
            return "";
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= SNIPPET_MAX_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, SNIPPET_MAX_LENGTH) + "...";
    }
}
